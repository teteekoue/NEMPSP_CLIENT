package com.example.nempsp.injection

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Context
import android.content.Intent
import android.graphics.Path
import android.graphics.PointF
import android.graphics.Rect
import android.media.AudioManager
import android.os.Build
import android.util.DisplayMetrics
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityWindowInfo
import com.example.nempsp.model.AnalogInjectionMode
import com.example.nempsp.model.PspButton
import com.example.nempsp.repository.InjectionPreferences
import com.example.nempsp.repository.LogRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.hypot

/**
 * Service d'accessibilité qui **joue à la place du doigt** dans PPSSPP.
 *
 * Pourquoi ce mécanisme : Android interdit à une application normale d'envoyer des événements
 * clavier/manette à une AUTRE application (`INJECT_EVENTS` est réservé au système). La seule
 * API publique capable de produire des appuis dans une application tierce est
 * [AccessibilityService.dispatchGesture]. PPSSPP reçoit donc de vrais événements tactiles,
 * exactement comme si l'écran était touché : **aucune configuration n'est nécessaire dans
 * PPSSPP**, ses contrôles tactiles par défaut suffisent.
 *
 * Fonctionnement :
 * 1. On suit la fenêtre active pour savoir si PPSSPP est au premier plan, et ses bornes réelles
 *    (indispensable sur Chromebook où PPSSPP tourne dans une fenêtre redimensionnable) : les
 *    cibles sont stockées en fractions de la fenêtre, puis converties en pixels à chaque cycle.
 * 2. À chaque cycle ([InjectionPreferences.holdMs]), on construit UN geste contenant un « doigt »
 *    par bouton maintenu. Les appuis en cours sont **prolongés** (`StrokeDescription.continueStroke`)
 *    au lieu d'être reposés : PPSSPP voit un contact continu, pas une rafale de taps.
 * 3. Un bouton relâché reçoit un trait final de quelques millisecondes qui lève le doigt.
 * 4. Hors PPSSPP (ou injection désactivée), tous les doigts sont levés : impossible d'aller
 *    cliquer dans une autre application.
 *
 * Le volume (VOL+/VOL−) passe par [AudioManager] plutôt que par le tactile, et le stick
 * analogique est converti en appuis directionnels par défaut (voir [AnalogInjectionMode]).
 */
class NemPspInjectionService : AccessibilityService() {

    companion object {
        const val TAG = "INJECTION"

        /** PPSSPP gratuit et PPSSPP Gold. */
        val PPSSPP_PACKAGES: Set<String> = setOf("org.ppsspp.ppsspp", "org.ppsspp.ppssppgold")

        /** Clé du doigt virtuel du stick analogique (les autres sont des [PspButton.name]). */
        private const val ANALOG_KEY = "__analog_stick__"

        /** Durée du trait final qui lève un doigt. */
        private const val LIFT_DURATION_MS = 10L

        /** Plafond Android : un geste ne peut pas durer plus d'une minute. */
        private const val MAX_STROKE_DURATION_MS = 59_000L

        /** `StrokeDescription.continueStroke` n'existe qu'à partir d'Android 8. */
        private val SUPPORTS_CONTINUATION: Boolean =
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O

        private val _isRunning = MutableStateFlow(false)
        val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

        private val _gameFocused = MutableStateFlow(false)
        val gameFocused: StateFlow<Boolean> = _gameFocused.asStateFlow()

        private val _injectedGestures = MutableStateFlow(0L)
        val injectedGestures: StateFlow<Long> = _injectedGestures.asStateFlow()

        private val _lastMessage = MutableStateFlow("Service d'injection non démarré")
        val lastMessage: StateFlow<String> = _lastMessage.asStateFlow()

        /** Instance vivante, pour le bouton « Tester » de l'interface. */
        @Volatile
        var instance: NemPspInjectionService? = null
            private set
    }

    // Le service tourne dans le même processus que l'interface : le thread principal suffit,
    // c'est d'ailleurs celui attendu par dispatchGesture.
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private var loopJob: Job? = null
    private var preferences: InjectionPreferences? = null
    private var audioManager: AudioManager? = null

    /** Doigts posés : clé → dernier trait, indispensable pour prolonger l'appui. */
    private val fingers = mutableMapOf<String, GestureDescription.StrokeDescription>()

    /** Position actuelle de chaque doigt (détection de mouvement du stick). */
    private val fingerPoints = mutableMapOf<String, PointF>()

    /** Centre du stick, point de départ du doigt analogique. */
    private var analogOrigin: PointF? = null

    @Volatile
    private var gameWindowBounds: Rect? = null

    private var volumeUpHeld = false
    private var volumeDownHeld = false

    // ------------------------------------------------------------------
    // Cycle de vie
    // ------------------------------------------------------------------

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        preferences = InjectionPreferences(this)
        audioManager = getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        _isRunning.value = true
        _lastMessage.value = "Service connecté, en attente de PPSSPP"
        startLoop()
        LogRepository.success(
            TAG,
            "Service d'accessibilité NEMPSP connecté : l'injection tactile dans PPSSPP est armée"
        )
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> onWindowChanged(event.packageName?.toString())
            AccessibilityEvent.TYPE_WINDOWS_CHANGED -> if (_gameFocused.value) refreshBounds()
        }
    }

    override fun onInterrupt() {
        // Aucun retour sonore/haptique à fournir : on ne peut pas être interrompu.
    }

    override fun onUnbind(intent: Intent?): Boolean {
        shutdown("Service délié par le système")
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        shutdown("Service arrêté")
        super.onDestroy()
    }

    private fun shutdown(reason: String) {
        runCatching { liftAll() }
        loopJob?.cancel()
        loopJob = null
        scope.cancel()
        InjectionBus.reset()
        _isRunning.value = false
        _gameFocused.value = false
        _lastMessage.value = reason
        instance = null
        LogRepository.info(TAG, reason)
    }

    // ------------------------------------------------------------------
    // Fenêtre de jeu
    // ------------------------------------------------------------------

    private fun onWindowChanged(packageName: String?) {
        if (packageName == null) return
        val isGame = PPSSPP_PACKAGES.contains(packageName)
        if (isGame) {
            refreshBounds()
            if (!_gameFocused.value) {
                _gameFocused.value = true
                _lastMessage.value = "PPSSPP au premier plan"
                LogRepository.success(TAG, "PPSSPP au premier plan : injection tactile active")
            }
        } else if (_gameFocused.value) {
            _gameFocused.value = false
            _lastMessage.value = "PPSSPP en arrière-plan"
            LogRepository.info(TAG, "PPSSPP remplacé par $packageName : doigts relâchés, injection en pause")
            liftAll()
        }
    }

    /**
     * Bornes de la fenêtre application active. `getWindows()` ne donne pas le nom du paquet sans
     * accès au contenu des fenêtres, mais comme on ne l'appelle que lorsque PPSSPP vient de prendre
     * le premier plan, la fenêtre active EST celle du jeu.
     */
    private fun refreshBounds() {
        val bounds = runCatching {
            windows
                .firstOrNull { it.isActive && it.type == AccessibilityWindowInfo.TYPE_APPLICATION }
                ?.let { window -> Rect().also { window.getBoundsInScreen(it) } }
        }.getOrNull()

        if (bounds != null && bounds.width() > 0 && bounds.height() > 0) {
            gameWindowBounds = bounds
        }
    }

    private fun resolveBounds(): Rect {
        gameWindowBounds?.let { return it }
        refreshBounds()
        gameWindowBounds?.let { return it }
        return displayBounds()
    }

    private fun displayBounds(): Rect {
        val metrics = DisplayMetrics()
        val windowManager = getSystemService(Context.WINDOW_SERVICE) as? WindowManager
        // defaultDisplay/getRealMetrics sont dépréciés mais restent les seuls disponibles
        // en API 24-29 ; l'alternative (WindowMetrics) n'existe qu'à partir d'Android 11.
        runCatching { windowManager?.defaultDisplay?.getRealMetrics(metrics) }
        return Rect(
            0,
            0,
            metrics.widthPixels.coerceAtLeast(1),
            metrics.heightPixels.coerceAtLeast(1)
        )
    }

    // ------------------------------------------------------------------
    // Boucle d'injection
    // ------------------------------------------------------------------

    private fun startLoop() {
        loopJob?.cancel()
        loopJob = scope.launch {
            while (isActive) {
                val period = (preferences?.holdMs ?: InjectionPreferences.DEFAULT_HOLD_MS)
                    .coerceIn(30, 400)
                delay(period.toLong())
                try {
                    tick()
                } catch (e: Exception) {
                    LogRepository.warn(TAG, "Cycle d'injection ignoré : ${e.javaClass.simpleName} ${e.message}")
                }
            }
        }
    }

    private fun tick() {
        val prefs = preferences ?: return
        val frame = InjectionBus.frame.value

        handleVolume(frame.buttons)

        if (!prefs.enabled) {
            if (fingers.isNotEmpty()) liftAll()
            return
        }

        if (!_gameFocused.value) {
            if (fingers.isNotEmpty()) liftAll()
            return
        }

        val targets = buildTargets(frame, resolveBounds())
        if (targets.isEmpty() && fingers.isEmpty()) return
        dispatch(targets)
    }

    /**
     * Ensemble des doigts à poser : boutons reçus (+ directions déduites du stick en mode DPAD)
     * et, en mode TOUCH_STICK, le doigt analogique.
     */
    private fun buildTargets(frame: InjectionFrame, bounds: Rect): Map<String, PointF> {
        val prefs = preferences ?: return emptyMap()

        val pressed = LinkedHashSet<PspButton>()
        pressed.addAll(frame.buttons)

        if (prefs.analogMode == AnalogInjectionMode.DPAD) {
            val threshold = PpssppTouchMap.ANALOG_DPAD_THRESHOLD
            if (frame.analogX < -threshold) pressed.add(PspButton.LEFT)
            if (frame.analogX > threshold) pressed.add(PspButton.RIGHT)
            if (frame.analogY < -threshold) pressed.add(PspButton.UP)
            if (frame.analogY > threshold) pressed.add(PspButton.DOWN)
        }

        val targets = LinkedHashMap<String, PointF>()
        pressed.forEach { button ->
            // Le volume passe par l'API audio, pas par le tactile.
            if (PpssppTouchMap.VOLUME_BUTTONS.contains(button)) return@forEach
            if (!prefs.isButtonEnabled(button)) return@forEach
            // HOME et NOTE n'ont pas de cible par défaut : ils ne sont injectés qu'une fois calibrés.
            val normalized = prefs.positionOf(button) ?: return@forEach
            targets[button.name] = clamp(absolute(normalized, bounds), bounds)
        }

        if (prefs.analogMode == AnalogInjectionMode.TOUCH_STICK) {
            val deflection = hypot(frame.analogX, frame.analogY)
            if (deflection > PpssppTouchMap.ANALOG_DPAD_THRESHOLD) {
                val center = absolute(prefs.analogCenter(), bounds)
                val radius = prefs.analogRadiusFraction() * bounds.width()
                analogOrigin = center
                targets[ANALOG_KEY] = clamp(
                    PointF(center.x + frame.analogX * radius, center.y + frame.analogY * radius),
                    bounds
                )
            } else {
                analogOrigin = null
            }
        }

        return targets
    }

    /**
     * Construit et envoie UN geste : prolongations pour les appuis en cours, nouveaux traits pour
     * les appuis naissants, traits courts pour lever les doigts relâchés. Un seul geste est
     * nécessaire car Android annule tout geste en cours dès qu'un nouveau est envoyé — envoyer un
     * geste par bouton relâcherait les autres.
     */
    private fun dispatch(targets: Map<String, PointF>) {
        val prefs = preferences ?: return
        val period = prefs.holdMs.coerceIn(30, 400).toLong()
        val duration = (period * 2 + 40).coerceAtMost(MAX_STROKE_DURATION_MS)

        if (!SUPPORTS_CONTINUATION) {
            // Android 7 : pas de prolongation possible, on repose les doigts à chaque cycle.
            dispatchFresh(targets, duration)
            return
        }

        val toLift = fingers.keys.filter { !targets.containsKey(it) }
        val toKeep = fingers.keys.filter { targets.containsKey(it) }
        val toPress = targets.keys.filter { !fingers.containsKey(it) }

        val maxStrokes = runCatching { GestureDescription.getMaxStrokeCount() }.getOrDefault(10)
        val budget = maxStrokes
        var used = 0

        val builder = GestureDescription.Builder()
        val nextFingers = HashMap<String, GestureDescription.StrokeDescription>()

        try {
            // 1. Lever les doigts relâchés (si le budget manque, ils expirent d'eux-mêmes).
            toLift.forEach { key ->
                if (used >= budget) return@forEach
                val previous = fingers[key] ?: return@forEach
                val point = fingerPoints[key] ?: return@forEach
                builder.addStroke(previous.continueStroke(tapPath(point), 0, LIFT_DURATION_MS, false))
                used++
            }

            // 2. Prolonger les appuis en cours (priorité : ne pas lâcher un bouton tenu).
            toKeep.forEach { key ->
                if (used >= budget) return@forEach
                val previous = fingers[key] ?: return@forEach
                val point = targets.getValue(key)
                val stroke = previous.continueStroke(pathFor(key, point), 0, duration, true)
                builder.addStroke(stroke)
                nextFingers[key] = stroke
                used++
            }

            // 3. Poser les nouveaux doigts.
            toPress.forEach { key ->
                if (used >= budget) return@forEach
                val point = targets.getValue(key)
                val stroke = GestureDescription.StrokeDescription(pathFor(key, point), 0, duration, true)
                builder.addStroke(stroke)
                nextFingers[key] = stroke
                used++
            }

            if (used == 0) {
                fingers.clear()
                fingerPoints.clear()
                return
            }

            dispatchGesture(builder.build(), GestureCallback("geste ${toPress.size}p/${toKeep.size}m/${toLift.size}l"), null)

            fingers.clear()
            fingers.putAll(nextFingers)
            fingerPoints.keys.retainAll(nextFingers.keys)
            nextFingers.keys.forEach { key -> targets[key]?.let { fingerPoints[key] = it } }

            if (toPress.size + toKeep.size + toLift.size > maxStrokes) {
                LogRepository.warn(
                    TAG,
                    "Plus de doigts que le maximum autorisé ($maxStrokes) : les appuis excédentaires seront posés au cycle suivant"
                )
            }
        } catch (e: Exception) {
            // Repli : on repose tous les doigts demandés d'un coup. Brel relâchement d'une image
            // sur les appuis déjà tenus, mais l'entrée n'est jamais perdue.
            LogRepository.warn(
                TAG,
                "Geste combiné refusé (${e.javaClass.simpleName} ${e.message}) — repli sur des appuis simples"
            )
            fingers.clear()
            dispatchFresh(targets, duration)
        }
    }

    /** Pose tous les doigts demandés comme de nouveaux contacts (Android 7 ou repli). */
    private fun dispatchFresh(targets: Map<String, PointF>, duration: Long) {
        if (targets.isEmpty()) {
            fingers.clear()
            fingerPoints.clear()
            return
        }
        try {
            val maxStrokes = runCatching { GestureDescription.getMaxStrokeCount() }.getOrDefault(10)
            val builder = GestureDescription.Builder()
            var used = 0
            targets.forEach { (key, point) ->
                if (used >= maxStrokes) return@forEach
                builder.addStroke(GestureDescription.StrokeDescription(pathFor(key, point), 0, duration))
                used++
            }
            if (used == 0) return
            dispatchGesture(builder.build(), GestureCallback("appuis simples x$used"), null)
            fingers.clear()
            fingerPoints.clear()
            targets.forEach { (key, point) -> if (fingerPoints.size < maxStrokes) fingerPoints[key] = point }
        } catch (e: Exception) {
            LogRepository.error(TAG, "Injection impossible : ${e.javaClass.simpleName} ${e.message}")
            _lastMessage.value = "Injection refusée par le système"
        }
    }

    /** Lève tous les doigts virtuels (fin d'appui, PPSSPP quitté, service arrêté). */
    private fun liftAll() {
        if (fingers.isEmpty() || !SUPPORTS_CONTINUATION) {
            fingers.clear()
            fingerPoints.clear()
            analogOrigin = null
            return
        }
        try {
            val builder = GestureDescription.Builder()
            var used = 0
            fingers.forEach { (key, stroke) ->
                val point = fingerPoints[key] ?: return@forEach
                if (used >= 10) return@forEach
                builder.addStroke(stroke.continueStroke(tapPath(point), 0, LIFT_DURATION_MS, false))
                used++
            }
            if (used > 0) dispatchGesture(builder.build(), GestureCallback("relâchement"), null)
        } catch (e: Exception) {
            LogRepository.warn(TAG, "Relâchement des appuis impossible : ${e.javaClass.simpleName}")
        } finally {
            fingers.clear()
            fingerPoints.clear()
            analogOrigin = null
        }
    }

    /**
     * Appui de test demandé depuis l'interface : un seul tap court sur la cible de ✕.
     * Refusé hors PPSSPP pour ne pas aller cliquer dans NEMPSP lui-même.
     */
    fun selfTest(): String {
        val prefs = preferences ?: return "Service d'injection non démarré"
        if (!_gameFocused.value) {
            return "Ouvrez PPSSPP au premier plan, puis relancez le test"
        }
        val bounds = resolveBounds()
        val point = prefs.positionOf(PspButton.CROSS)?.let { clamp(absolute(it, bounds), bounds) }
            ?: return "Aucune cible pour ✕"
        return try {
            val stroke = GestureDescription.StrokeDescription(tapPath(point), 0, 60)
            dispatchGesture(GestureDescription.Builder().addStroke(stroke).build(), GestureCallback("test ✕"), null)
            val message = "Appui de test injecté sur ✕ en ${point.x.toInt()},${point.y.toInt()} " +
                "(fenêtre ${bounds.width()}x${bounds.height()})"
            _lastMessage.value = message
            LogRepository.success(TAG, message)
            message
        } catch (e: Exception) {
            val message = "Test refusé : ${e.javaClass.simpleName} ${e.message}"
            _lastMessage.value = message
            LogRepository.error(TAG, message)
            message
        }
    }

    // ------------------------------------------------------------------
    // Géométrie
    // ------------------------------------------------------------------

    private fun absolute(normalized: PointF, bounds: Rect): PointF = PointF(
        bounds.left + normalized.x * bounds.width(),
        bounds.top + normalized.y * bounds.height()
    )

    private fun clamp(point: PointF, bounds: Rect): PointF = PointF(
        point.x.coerceIn(bounds.left + 1f, bounds.right - 2f),
        point.y.coerceIn(bounds.top + 1f, bounds.bottom - 2f)
    )

    /**
     * Trajectoire d'un doigt. Pour un bouton, un segment d'un pixel suffit (PPSSPP interprète le
     * contact). Pour le stick, le trait part de la position précédente : le glisser reste continu.
     */
    private fun pathFor(key: String, target: PointF): Path {
        val start = if (key == ANALOG_KEY) {
            fingerPoints[ANALOG_KEY] ?: analogOrigin ?: target
        } else {
            target
        }
        return Path().apply {
            moveTo(start.x, start.y)
            if (abs(target.x - start.x) < 1f && abs(target.y - start.y) < 1f) {
                lineTo(target.x + 1f, target.y)
            } else {
                lineTo(target.x, target.y)
            }
        }
    }

    private fun tapPath(point: PointF): Path = Path().apply {
        moveTo(point.x, point.y)
        lineTo(point.x + 1f, point.y)
    }

    // ------------------------------------------------------------------
    // Volume
    // ------------------------------------------------------------------

    private fun handleVolume(buttons: List<PspButton>) {
        val up = buttons.contains(PspButton.VOL_UP)
        val down = buttons.contains(PspButton.VOL_DOWN)
        if (up == volumeUpHeld && down == volumeDownHeld) return

        val audio = audioManager
        if (audio != null) {
            if (up && !volumeUpHeld) {
                runCatching { audio.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, 0) }
            }
            if (down && !volumeDownHeld) {
                runCatching { audio.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER, 0) }
            }
        }
        volumeUpHeld = up
        volumeDownHeld = down
    }

    private inner class GestureCallback(private val label: String) : GestureResultCallback() {
        override fun onCompleted(gestureDescription: GestureDescription?) {
            _injectedGestures.value = _injectedGestures.value + 1
            _lastMessage.value = "Geste injecté ($label)"
        }

        override fun onCancelled(gestureDescription: GestureDescription?) {
            // Attendu : chaque nouveau geste annule le précédent lorsqu'on prolonge un appui.
        }
    }
}

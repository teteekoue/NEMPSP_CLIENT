package com.example.nempsp.injection

import android.graphics.PointF
import com.example.nempsp.model.PspButton

/**
 * Carte des cibles tactiles de PPSSPP.
 *
 * Les positions sont **normalisées** (0..1) dans la fenêtre de jeu, pas dans l'écran : sur
 * Chromebook, PPSSPP tourne souvent dans une fenêtre redimensionnable, et le service
 * d'accessibilité reconvertit ces fractions en pixels à l'aide des bornes réelles de la fenêtre
 * active. Une calibration enregistrée par l'utilisateur remplace toujours la valeur par défaut.
 *
 * Les valeurs par défaut correspondent à la disposition tactile d'usine de PPSSPP en paysage.
 * Elles restent une approximation : c'est précisément à ça que sert la calibration, un bouton
 * mal placé se corrige en le touchant une fois à l'écran.
 */
object PpssppTouchMap {

    /** Positions par défaut, en fractions de la fenêtre de jeu (x = largeur, y = hauteur). */
    val DEFAULT_POSITIONS: Map<PspButton, PointF> = mapOf(
        // Croix directionnelle (bas-gauche)
        PspButton.UP to PointF(0.100f, 0.630f),
        PspButton.DOWN to PointF(0.100f, 0.810f),
        PspButton.LEFT to PointF(0.045f, 0.720f),
        PspButton.RIGHT to PointF(0.155f, 0.720f),

        // Boutons d'action (bas-droite, disposition PSP : △ haut, ○ droite, ✕ bas, □ gauche)
        PspButton.TRIANGLE to PointF(0.900f, 0.630f),
        PspButton.CIRCLE to PointF(0.955f, 0.720f),
        PspButton.CROSS to PointF(0.900f, 0.810f),
        PspButton.SQUARE to PointF(0.845f, 0.720f),

        // Gâchettes (bandeaux en haut des coins)
        PspButton.L to PointF(0.075f, 0.055f),
        PspButton.R to PointF(0.925f, 0.055f),

        // Système
        PspButton.SELECT to PointF(0.790f, 0.930f),
        PspButton.START to PointF(0.870f, 0.930f)
    )

    /**
     * Boutons gérés en dehors du tactile : le volume passe par l'API audio du récepteur,
     * ce qui évite d'avoir à viser un hypothétique bouton volume dans PPSSPP.
     */
    val VOLUME_BUTTONS: Set<PspButton> = setOf(PspButton.VOL_UP, PspButton.VOL_DOWN)

    /**
     * Sans équivalent dans PPSSPP : HOME (ouvrir l'émulateur) et NOTE. Non injectés par défaut,
     * mais une calibration explicite les rend utilisables (utile pour viser un élément d'interface).
     */
    val UNSUPPORTED_BY_DEFAULT: Set<PspButton> = setOf(PspButton.HOME, PspButton.NOTE)

    /** Centre par défaut du stick analogique tactile de PPSSPP (s'il est activé). */
    val DEFAULT_ANALOG_CENTER: PointF = PointF(0.235f, 0.700f)

    /** Rayon de course du stick, en fraction de la largeur de la fenêtre. */
    const val DEFAULT_ANALOG_RADIUS_FRACTION: Float = 0.055f

    /**
     * Seuil de déflection au-delà duquel le stick est converti en appui directionnel
     * (mode [com.example.nempsp.model.AnalogInjectionMode.DPAD]).
     */
    const val ANALOG_DPAD_THRESHOLD: Float = 0.45f

    /** Un bouton a-t-il une cible tactile (par défaut ou après calibration) ? */
    fun isTouchable(button: PspButton): Boolean =
        button !in VOLUME_BUTTONS && DEFAULT_POSITIONS.containsKey(button)

    /** Boutons proposés à la calibration : tout ce qui a une cible tactile, plus HOME/NOTE. */
    val CALIBRABLE_BUTTONS: List<PspButton> =
        (DEFAULT_POSITIONS.keys + UNSUPPORTED_BY_DEFAULT).toList()
}

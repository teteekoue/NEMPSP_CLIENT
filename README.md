# 🎮 NEMPSP — Guide Utilisateur Complet

Bienvenue dans le guide d'utilisation de **NEMPSP**, l'application tout-en-un qui transforme votre
smartphone en manette PSP dédiée pour **PPSSPP** (sur Chromebook, tablette ou autre appareil
Android).

Un seul APK, deux rôles : **Mode Manette** sur le téléphone, **Mode Récepteur** sur l'appareil de
jeu. Le récepteur convertit les touches reçues en **vrais appuis tactiles dans PPSSPP** : il n'y a
**aucune configuration à faire dans l'émulateur**.

---

## 📌 Qu'est-ce que NEMPSP ?

Jouer sur un grand écran avec les commandes tactiles de l'émulateur masque une partie de l'image et
manque de précision. Brancher une manette physique n'est pas toujours possible.

**NEMPSP transforme votre smartphone en manette PSP tactile haute précision**, et fournit sur
l'appareil de jeu un **récepteur autonome** qui capte vos actions, les affiche en direct et les
injecte dans PPSSPP.

### Les points forts
- **Application 2-en-1** : le même APK s'installe sur les deux appareils.
- **Liaison vérifiée** : la manette ne déclare « Connecté » qu'après une vraie réponse du récepteur,
  et affiche une latence **mesurée** (aller-retour), pas estimée.
- **Trois transports** : WiFi (UDP), câble USB (adb reverse) et Bluetooth (RFCOMM/SPP).
- **Disposition PSP fidèle** : croix directionnelle, △ ○ ✕ □, stick analogique, gâchettes L/R, barre
  système (SELECT, START, HOME, volume).
- **Écran adaptatif** : la manette se redimensionne seule pour tenir sur les petits écrans, et tous
  les réglages sont regroupés dans le cadre central NEMPSP.
- **Injection dans PPSSPP** : les touches reçues deviennent des appuis tactiles, sans réglage dans
  l'émulateur.
- **Fonctionnement en arrière-plan** : notification permanente, WifiLock et WakeLock — le récepteur
  continue d'écouter pendant que PPSSPP est en plein écran.

---

## 🔄 Les deux modes

Au lancement, une fenêtre demande le rôle de l'appareil :

```
┌───────────────────────────────┬───────────────────────────────┐
│  📱 Mode Manette (Client)     │  💻 Mode Récepteur (Serveur)  │
│  Votre smartphone devient la  │  Votre Chromebook/tablette    │
│  manette tactile de jeu.      │  reçoit les commandes et les  │
│                               │  injecte dans PPSSPP.         │
└───────────────────────────────┴───────────────────────────────┘
```

> 💡 Cochez *« Se souvenir de ce choix »* pour ouvrir directement votre mode préféré. Pour changer de
> rôle : icône **Changer de rôle** dans le cadre central NEMPSP.

---

## 📱 1. Le Mode Manette (Client)

### L'écran
1. **À gauche** : gâchette **L**, **croix directionnelle** (4 directions + diagonales), **stick
   analogique**.
2. **Au centre** : le **cadre NEMPSP** — logo, état de la liaison, latence, paquets/seconde,
   diagnostic, et la **barre d'outils** (voir ci-dessous). En dessous, la barre système PSP
   (HOME, VOL−, VOL+, SELECT, START).
3. **À droite** : gâchette **R** et boutons d'action **△ ○ ✕ □**.

> 📐 **Petits écrans** : l'échelle de toute la manette est calculée d'après la place réellement
> disponible (hauteur **et** largeur), les colonnes latérales s'ajustent au contenu, et la barre
> d'outils passe sur deux lignes si le cadre est étroit. Rien ne déborde, rien n'est collé au bord de
> l'écran.

### Le cadre central NEMPSP (barre d'outils)
Tous les réglages sont **au centre**, plus sur les bords :

| Icône | Rôle |
|---|---|
| **Connexion** (antenne) | WiFi / Bluetooth / USB, adresse IP, port, scan du réseau, diagnostic |
| **Personnalisation** (manette) | taille des touches, espacements, opacité, vibration, son |
| **Journal** | chaque appui, chaque paquet, chaque erreur — en direct |
| **Guide serveur** | rappel des étapes côté récepteur |
| **Changer de rôle** | repasser en Mode Récepteur |
| **Mode Test** | vérifie les appuis localement, sans réseau |

Le cadre affiche aussi : le mode actif, le statut (**Connecté / En attente / Déconnecté / Échec**),
la latence réelle en ms, les paquets envoyés par seconde, et surtout un **diagnostic en clair**
(exemple : *« Aucun récepteur n'écoute sur 192.168.1.77:8989 »*).

### Le stick analogique
Le pouce suit le doigt **dès le premier contact** (pas d'attente d'un seuil de glissement), la course
est bornée au puits, la zone morte est recalée puis la sensibilité appliquée, et le pouce revient au
centre avec un ressort dès que le doigt se lève (l'axe est remis à 0, jamais de touche bloquée).

---

## 💻 2. Le Mode Récepteur (Serveur)

### Ce que l'écran affiche désormais (état réel, jamais inventé)
- **Badge d'état** : `EN LIGNE · 8989` seulement si au moins un socket est **réellement ouvert** ;
  `INCOMPLET` si le service tourne mais qu'aucune écoute n'a pu démarrer ; `ARRÊTÉ` sinon.
- **Erreur de démarrage** en rouge : la cause exacte (port déjà utilisé, Bluetooth refusé, …).
  C'est cette ligne qui explique un « Erreur envoi paquet UDP » côté manette.
- **Pastilles par transport** : UDP, TCP et Bluetooth avec leur état d'écoute individuel.
- **Toutes les adresses IPv4** de l'appareil, copiables d'un toucher, la meilleure marquée
  *« à utiliser »*. Les adresses non joignables en WiFi (interface loopback, pont du conteneur Android
  d'un Chromebook `arc*`/`vnic*`, réseaux cellulaires) sont écartées automatiquement.
- **Télémétrie** : paquets reçus, fréquence, clients actifs, dernière source.
- **Moniteur des touches** : les 16 boutons s'allument en direct ; toucher un bouton simule une
  injection (pratique pour tester PPSSPP sans la manette).
- **Carte « Injection dans PPSSPP »** : état du service, activation, mode analogique, calibration,
  test et compteur de gestes injectés (voir plus bas).

> Sur un écran étroit, les deux colonnes s'empilent et deviennent déroulantes ; les boutons passent en
> ligne flexible.

### Arrière-plan
Dès le démarrage du récepteur, une notification permanente est publiée, un **WifiLock** (faible
latence) et un **WakeLock** sont pris : Android ne coupe ni le WiFi ni le CPU pendant la partie.

---

## 🚀 Connexion pas-à-pas

### Option A — WiFi (la plus simple)
1. **Récepteur** : NEMPSP → *Mode Récepteur* → **Démarrer**.
   Vérifiez le badge `EN LIGNE · 8989` et l'absence d'erreur rouge. Notez l'IP marquée *« à utiliser »*.
2. **Manette** : NEMPSP → *Mode Manette* → icône **Connexion** du cadre central → onglet **WiFi**.
   Saisissez l'IP (ou touchez **Scanner le réseau** puis l'IP détectée) → **Connecter**.
3. **Vérification** : le statut passe à *« Connecté au récepteur 192.168.1.45:8989 (aller-retour 3ms) »*
   et les touches s'allument sur l'écran du récepteur.

> La manette n'envoie **aucun** paquet tant que le récepteur n'a pas répondu à son PING : le statut
> « Connecté » signifie donc toujours « ça marche », et un échec affiche toujours pourquoi.

### Option B — Câble USB (latence la plus faible)
1. Branchez le téléphone à l'appareil de jeu, activez le **débogage USB** (options développeur).
2. Sur l'appareil de jeu, lancez la commande affichée par NEMPSP (copiable en un toucher) :
   `adb reverse tcp:8989 tcp:8989`
3. Dans la manette : onglet **USB** → **Connecter USB**. Le trafic passe par `127.0.0.1`.

### Option C — Bluetooth (sans WiFi)
1. Associez les deux appareils dans les réglages Bluetooth Android.
2. Récepteur : le mode Récepteur ouvre automatiquement une écoute RFCOMM « NEMPSP_GAMEPAD_SERVER ».
3. Manette : onglet **Bluetooth** → **Actualiser** → choisissez l'appareil → **Connecter**.

---

## 🎮 Jouer dans PPSSPP : aucune configuration dans PPSSPP

Android interdit à une application d'envoyer des événements clavier/manette à une **autre**
application. La seule API publique capable de produire de vrais appuis dans PPSSPP est un
**service d'accessibilité** : il envoie des gestes tactiles, exactement comme un doigt. PPSSPP voit
donc ses propres commandes tactiles être touchées — d'où l'absence totale de réglage dans
l'émulateur.

### Activation (une seule fois)
1. Sur l'appareil de jeu, ouvrez NEMPSP → **Mode Récepteur** → **Démarrer**.
2. Dans la carte **Injection dans PPSSPP**, touchez **Activer**.
3. Android ouvre les réglages d'accessibilité : choisissez
   **« NEMPSP — Manette pour PPSSPP »**, puis **Utiliser le service**.
4. La carte passe à **ACTIVE** dès que PPSSPP est au premier plan.

> 🔒 Ce service ne lit **aucun** contenu d'écran (il n'a que le droit d'envoyer des gestes). Il
> n'injecte rien quand PPSSPP n'est pas au premier plan, et lève immédiatement tous les appuis si
> vous quittez le jeu. Il se désactive depuis les réglages d'accessibilité ou depuis NEMPSP.

### Utilisation
1. Démarrez le récepteur, connectez la manette.
2. Touchez **PPSSPP** (bouton orange) : NEMPSP lance l'émulateur et reste actif en arrière-plan.
3. Jouez. La carte affiche le compteur de gestes injectés — s'il augmente, l'injection fonctionne.
4. Bouton **Tester** : injecte un appui sur ✕ dans PPSSPP pour vérifier le ciblage.

### Si un bouton ne tombe pas au bon endroit : la calibration
Par défaut, NEMPSP vise la **disposition tactile d'usine de PPSSPP en paysage**. Si vous avez déplacé
ou redimensionné les commandes tactiles, calibrez :

1. Carte **Injection dans PPSSPP** → **Calibrer**.
2. Un schéma 16:9 de la fenêtre de jeu s'affiche, avec les cibles actuelles.
3. Lisez l'instruction (*« Touchez l'endroit où PPSSPP affiche ✕ CROSS »*) et touchez le schéma à cet
   endroit. La cible suivante est proposée automatiquement.
4. **Terminé**.

Les positions sont mémorisées en **proportions de la fenêtre de jeu** : elles restent valables si la
fenêtre PPSSPP est déplacée ou redimensionnée (cas courant sur Chromebook, où vous pouvez garder
NEMPSP et PPSSPP côte à côte pendant la calibration). *Réinitialiser* remet une cible, *Tout
réinitialiser* revient à la disposition d'usine.

### Stick analogique et volume
- **Mode « Croix directionnelle »** (par défaut) : la déflection du stick est convertie en appuis
  Haut/Bas/Gauche/Droite. Aucune option à activer dans PPSSPP.
- **Mode « Stick analogique tactile »** : un doigt virtuel reste posé sur le stick tactile de PPSSPP et
  glisse — analogique réel. Activez alors *Touch analog stick* dans PPSSPP et calibrez son centre.
- **VOL+ / VOL−** : réglés via le volume système de l'appareil de jeu, pas par le tactile.
- **HOME** : sans équivalent PPSSPP, non injecté tant qu'il n'est pas calibré.

---

## 🎨 Personnalisation de la manette

Icône **Personnalisation** du cadre central :
- taille de la croix directionnelle, des boutons d'action, du stick et des gâchettes (60 % à 160 %) ;
- espacements et position des blocs ;
- opacité globale ;
- zone morte et sensibilité du stick ;
- retour vibratoire (léger / moyen / fort / désactivé) et sons de clic ;
- cadence d'envoi automatique.

**Enregistrer** conserve tout entre les sessions.

---

## 🛡️ Autorisations et confidentialité

| Autorisation | Pourquoi |
|---|---|
| Réseau / état du WiFi / multicast | envoyer et recevoir les paquets de commandes, scanner le réseau |
| Modification de l'état WiFi (WifiLock) | empêcher la mise en veille du WiFi pendant la partie |
| Bluetooth (connexion, scan) | liaison RFCOMM avec l'appareil de jeu |
| Vibration | retour haptique des appuis |
| Service de premier plan + notifications | maintenir le récepteur actif pendant le jeu |
| WakeLock | empêcher la mise en veille du CPU |
| **Accessibilité** (activation manuelle) | **injecter les appuis tactiles dans PPSSPP** |

*NEMPSP ne collecte aucune donnée, ne contient aucune publicité et fonctionne entièrement en local :
rien ne quitte votre réseau.*

---

## ❓ Dépannage

#### « Erreur envoi paquet UDP » / la manette ne se connecte pas
Le diagnostic affiché dans le cadre central indique la cause. Dans l'ordre :
1. **Le récepteur tourne-t-il vraiment ?** Badge `EN LIGNE · 8989` sur l'appareil de jeu. S'il affiche
   `INCOMPLET` ou une erreur rouge, corrigez-la d'abord (port déjà pris par une autre application,
   Bluetooth refusé…).
2. **Même réseau WiFi ?** Les deux appareils doivent être sur la même box / le même point d'accès.
3. **La bonne IP ?** Utilisez l'adresse marquée *« à utiliser »* sur l'écran du récepteur (un
   Chromebook expose plusieurs IPs, dont celle du conteneur Android, **non joignable** en WiFi).
   Touchez-la pour la copier, ou lancez **Scanner le réseau** côté manette.
4. **« Isolation client » / « AP isolation »** activée sur la box ou le point d'accès invité ? Elle
   bloque les échanges entre appareils du même réseau : désactivez-la.
5. **Pare-feu / VPN / réseau invité** sur l'appareil de jeu : le port 8989 doit être libre en UDP, TCP
   et Bluetooth.
6. **Mode Test** (cadre central) : vérifie que les appuis sont bien détectés localement, hors réseau.

#### Les touches s'allument sur le récepteur mais rien ne se passe dans PPSSPP
1. La carte **Injection dans PPSSPP** est-elle sur **ACTIVE** (et non *INACTIVE* ou *PRÊTE*) ? Sinon
   touchez **Activer** et validez le service d'accessibilité.
2. L'interrupteur **Injecter les touches** est-il activé ?
3. Le compteur **gestes injectés** augmente-t-il quand vous appuyez ? S'il reste à 0, le service n'est
   pas lié : réactivez-le depuis les réglages d'accessibilité.
4. Les commandes tactiles de PPSSPP sont-elles affichées ? (*Paramètres → Commandes → Contrôles
   tactiles à l'écran*). L'injection appuie sur ces commandes : si elles sont masquées, il n'y a rien
   à toucher.
5. Utilisez **Tester** : un appui sur ✕ doit être visible dans PPSSPP. Sinon, **Calibrez**.

#### Un bouton agit au mauvais endroit dans PPSSPP
Ouvrez **Calibrer** et touchez le schéma à l'endroit où PPSSPP affiche ce bouton.

#### Puis-je changer le rôle par défaut d'un appareil ?
Oui : icône **Changer de rôle** du cadre central, puis décochez/recochez *« Se souvenir de ce choix »*.

---

## 🔧 Protocole (pour les curieux)

- **Trame manette** : 9 octets — `N`,`M`, séquence (16 bits LE), masque de boutons (16 bits LE),
  axe X, axe Y (0…255, centre 128), somme de contrôle XOR.
- **Trame de contrôle** : 8 octets — `N`,`P`, type, charge utile (32 bits LE), XOR.
  Types : `HELLO`(1) / `WELCOME`(2) / `PING`(3) / `PONG`(4).
  Le `PING` transporte l'horloge monotone de l'émetteur : le `PONG` permet de mesurer un
  **aller-retour réel**.
- **Handshake** : la manette envoie `HELLO` puis un `PING` toutes les 350 ms tant qu'elle n'a pas de
  réponse (1 s une fois connectée, pour mesurer la latence en continu). Elle ne passe à `CONNECTED` —
  et ne commence à envoyer des trames manette — qu'au premier `PONG` reçu. Sans réponse pendant 4 s
  elle affiche l'échec avec la raison, et considère la liaison perdue après 5 s.
- **Découverte** : broadcast UDP `NEMPSP_DISCOVER` sur le port du récepteur, qui répond
  `NEMPSP_SERVER:…`.
- **UDP, TCP et Bluetooth** répondent tous aux trames de contrôle, avec un découpeur de flux commun
  (resynchronisation automatique sur les canaux orientés connexion).

---

## 🏗️ Construire l'APK

```bash
./gradlew assembleDebug          # APK universel debug
```

Un workflow GitHub Actions (`.github/workflows/build-apk.yml`) construit l'APK universel à chaque
poussée et le publie comme artefact de l'exécution :
**Actions → dernière exécution → Artifacts → `NEMPSP-universal-v1.0.apk`**.

Les erreurs de compilation Kotlin sont remontées en **annotations lisibles** sur l'exécution grâce à
un *problem matcher* (`ci/kotlin-problem-matcher.json`, enregistré par `app/build.gradle.kts`) :
onglet **Annotations** de la tâche en échec.

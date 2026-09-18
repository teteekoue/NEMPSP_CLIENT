# 🎮 NEMPSP - Guide Utilisateur Complet

Bienvenue dans le guide d'utilisation de **NEMPSP**, l'application tout-en-un qui connecte votre smartphone comme manette de jeu dédiée à votre émulateur **PPSSPP** (sur Chromebook, PC, tablette ou autre appareil Android).

Ce document explique en détail le fonctionnement de l'application, la gestion de ses deux modes complémentaires, ainsi que la configuration pas-à-pas pour jouer dans les meilleures conditions.

---

## 📌 Qu'est-ce que NEMPSP ?

**NEMPSP** résout un problème classique du jeu sur émulateur PSP : le confort des commandes.

Jouer sur un grand écran (comme un Chromebook ou une tablette) avec les touches virtuelles à l'écran masque la zone de jeu et manque d'ergonomie. Brancher une manette physique n'est pas toujours possible ou pratique en déplacement.

**NEMPSP transforme votre smartphone Android en une véritable manette PSP tactile haute précision**, tout en fournissant sur l'appareil de jeu un **récepteur autonome** capable de capter vos actions et de les transmettre au jeu en temps réel.

### Les points forts :
- **Application 2-en-1** : un seul fichier APK à installer sur vos deux appareils.
- **Zéro latence ressentie** : transmission ultra-rapide optimisée (WiFi, câble USB ou Bluetooth).
- **Disposition PSP fidèle** : croix directionnelle (D-Pad), touches d'action emblématiques (△, ○, ✕, □), stick analogique fluide, gâchettes d'épaule (L et R) et barre système (SELECT, START, HOME).
- **Personnalisation complète** : taille des touches, espacements, opacité et retours vibratoires ajustables selon vos mains.
- **Fonctionnement en arrière-plan** : le mode récepteur continue de fonctionner pendant que vous jouez en plein écran dans PPSSPP.

---

## 🔄 Les Deux Modes de l'Application

Au lancement de l'application, une fenêtre vous demande de choisir le rôle de l'appareil :

```
             ┌──────────────────────────────────────────────┐
             │            Sélectionnez votre Rôle                   │
             ├──────────────────────┬───────────────────────┤
             │  📱 Mode Manette         │  💻 Mode Récepteur        │
             │     (Client)             │     (Serveur)             │
             │                          │                           │
             │ Votre smartphone         │ Votre Chromebook, PC      │
             │ devient la manette       │ ou tablette reçoit        │
             │ tactile de jeu.          │ les commandes en jeu.     │
             └──────────────────────┴───────────────────────┘
```

> 💡 **Astuce** : Vous pouvez cocher l'option *« Se souvenir de ce choix »* pour ouvrir directement votre mode préféré. Pour changer de rôle par la suite, appuyez simplement sur le bouton **Changer de rôle** dans la barre supérieure.

---

## 📱 1. Le Mode Manette (Client)

Ce mode s'utilise sur votre **smartphone**.

### Description de l'interface
L'écran affiche l'agencement authentique d'une console portable PSP :
1. **À gauche** :
   - **Gâchette L** en haut à gauche.
   - **Croix directionnelle (D-Pad)** à réponse immédiate (supporte les 4 directions et les diagonales).
   - **Stick analogique circulaire** avec zone morte configurable et recentrage automatique élastique.
2. **Au centre** :
   - **Moniteur d'état** indiquant le mode de connexion, la latence et les paquets envoyés par seconde.
   - **Barre système PSP** en bas : touches *HOME*, *VOL -*, *VOL +*, *SELECT* et *START*.
3. **À droite** :
   - **Gâchette R** en haut à droite.
   - **Boutons d'action géométriques** : Triangle (vert), Rond (rouge), Croix (bleu), Carré (rose).

### Barre d'outils supérieure
En haut de l'écran, vous trouverez les raccourcis d'accès rapide :
- **Statut de connexion** (Pastille verte = Connecté, Orange = En attente, Rouge = Déconnecté).
- **Icône Paramètres de connexion** : pour choisir entre WiFi, USB et Bluetooth, régler l'adresse IP et tester la liaison.
- **Icône Personnalisation (Manette)** : pour redimensionner les touches, déplacer les blocs et modifier la vibration.
- **Icône Journal en direct** : pour visualiser chaque appui de touche et vérifier que tout répond.
- **Icône Guide** : conseils rapides intégrés.
- **Icône Rôle** : pour revenir au choix des modes.

---

## 💻 2. Le Mode Récepteur (Serveur)

Ce mode s'utilise sur l'appareil où tourne votre jeu (**Chromebook, PC, TV ou tablette**).

### Description de l'interface
L'écran serveur affiche un tableau de bord complet :
- **État du service** : indique si le récepteur est actif et en écoute.
- **Informations réseau** : affiche l'adresse IP locale de votre Chromebook pour la saisir facilement sur la manette.
- **Manette virtuelle de contrôle** : reproduit visuellement et en direct chaque bouton pressé sur le smartphone pour tester la réception avant de lancer le jeu.
- **Journal d'activité** : liste les paquets reçus, le mode actif (WiFi/USB/Bluetooth) et les temps de réponse.

### Fonctionnement en arrière-plan
Dès que vous activez le serveur, une notification persistante garantit qu'Android ne ferme pas l'application lorsque vous basculez sur PPSSPP. Vous pouvez mettre NEMPSP en arrière-plan et lancer votre partie en plein écran sans coupure.

---

## 🚀 Guide de Connexion Pas-à-Pas

NEMPSP propose **trois manières** de relier vos deux appareils. Choisissez celle qui correspond à votre installation :

---

### Option A : Connexion sans fil via WiFi (La plus simple)

Idéale pour jouer sans aucun câble si les deux appareils sont connectés à la même box Internet ou au même réseau WiFi :

1. **Sur le Chromebook (Récepteur)** :
   - Lancez NEMPSP et sélectionnez **Mode Récepteur**.
   - Notez l'adresse IP affichée à l'écran (exemple : `192.168.1.45`).
   - Vérifiez que le statut indique « Écoute UDP 0.0.0.0:8989 ACTIVE » (port 8989 par défaut).

2. **Sur le Smartphone (Manette)** :
   - Lancez NEMPSP et sélectionnez **Mode Manette**.
   - Appuyez sur l'icône **Connexion** (antenne en haut).
   - Sélectionnez l'onglet **WiFi (UDP)**.
   - Entrez l'adresse IP notée sur le Chromebook.
   - Cliquez sur **Connecter**.

3. **Vérification** :
   - Appuyez sur les boutons de votre smartphone : les touches correspondantes s'illuminent instantanément sur l'écran du Chromebook.

---

### Option B : Connexion filaire par câble USB (Latence la plus faible)

Idéale pour les jeux d'action ou de combat exigeant une réactivité absolue :

1. Reliez votre smartphone au Chromebook avec un câble USB (activez le débogage USB dans les options pour développeurs de votre téléphone).
2. Dans les paramètres de connexion de NEMPSP sur le smartphone, choisissez l'onglet **Câble USB (ADB)**.
3. La connexion s'établit localement via le port USB à une vitesse maximale sans dépendre de la qualité du WiFi.

---

### Option C : Connexion sans fil via Bluetooth

Pratique lorsque vous êtes en déplacement sans réseau WiFi disponible :

1. Activez le Bluetooth sur les deux appareils et associez-les dans les paramètres Bluetooth de vos systèmes.
2. Ouvrez NEMPSP sur le smartphone, ouvrez les paramètres de connexion et sélectionnez l'onglet **Bluetooth**.
3. Choisissez votre Chromebook dans la liste des périphériques associés et cliquez sur **Connexion**.

---

## 🎮 Configuration dans l'Émulateur PPSSPP

Une fois que les deux appareils communiquent, voici comment assigner vos commandes dans PPSSPP :

1. Lancez **PPSSPP** sur votre Chromebook ou appareil de jeu.
2. Allez dans **Paramètres** > **Commandes** > **Assignation des commandes**.
3. Pour chaque commande PSP (Croix, Rond, Carré, Triangle, Haut, Bas, Gâchettes...) :
   - Cliquez sur le bouton à assigner dans PPSSPP.
   - Appuyez sur la touche correspondante sur votre smartphone.
   - L'émulateur enregistre l'action immédiatement.
4. Lancez votre jeu PSP favori et profitez d'une prise en main authentique !

---

## 🎨 Personnalisation Ergonomique de la Manette

Chaque joueur a des mains de taille différente. NEMPSP intègre un outil complet d'ajustement :

1. Dans le **Mode Manette**, appuyez sur l'icône **Personnalisation** (icône de manette).
2. Vous pouvez régler :
   - **Taille de la croix directionnelle** (de 60% à 160%).
   - **Taille des boutons d'action** (de 60% à 160%).
   - **Taille du stick analogique** et sensibilité de la zone morte.
   - **Taille des gâchettes L et R**.
   - **Opacité globale** (pour rendre les touches plus ou moins visibles).
   - **Intensité du retour vibratoire (haptique)** : Léger, Moyen, Fort ou Désactivé.
   - **Effets sonores** discrets au clic.
3. Cliquez sur **Enregistrer** pour conserver vos réglages en mémoire permanente.

---

## 🛡️ Autorisations Requises & Confidentialité

L'application demande uniquement les autorisations indispensables à son fonctionnement technique :
- **Réseau et WiFi** : pour envoyer et recevoir les paquets de commandes entre les deux appareils.
- **Bluetooth** : pour rechercher et communiquer avec votre appareil de jeu sans passer par Internet.
- **Vibration** : pour restituer la sensation physique d'un clic mécanique sous vos doigts.
- **Service d'arrière-plan & Notifications** : pour maintenir le récepteur actif lorsque vous jouez en plein écran dans PPSSPP.

*NEMPSP ne collecte aucune donnée personnelle, ne contient aucune publicité et fonctionne entièrement en local.*

---

## ❓ Foire Aux Questions (FAQ)

#### Les boutons restent-ils cliqués si je glisse mon doigt ?
Non. Le gestionnaire de gestes tactiles de NEMPSP libère automatiquement et instantanément toute touche dès que le contact avec l'écran s'interrompt, évitant tout blocage de commande en pleine partie.

#### Que faire si la manette ne se connecte pas en WiFi ?
1. Vérifiez que votre smartphone et votre Chromebook sont connectés exactement à la même box ou au même point d'accès WiFi.
2. Assurez-vous que l'adresse IP saisie dans l'application manette correspond bien à celle affichée sur l'écran récepteur du Chromebook.
3. Utilisez le bouton **Mode Test / Simulation** dans la barre supérieure pour vérifier que vos touches réagissent bien localement.

#### Puis-je changer le rôle d'un appareil enregistré par défaut ?
Oui. À tout moment, appuyez sur l'icône de permutation de rôle située dans la barre supérieure pour ouvrir le menu et basculer instantanément d'un mode à l'autre.

package com.example.nempsp.model

/**
 * Façon dont la déflection du stick analogique est injectée dans PPSSPP.
 *
 * - [DPAD] : la déflection est convertie en appuis sur la croix directionnelle tactile.
 *   C'est le mode par défaut car il fonctionne **sans rien configurer dans PPSSPP** (la croix
 *   tactile est active par défaut, alors que le stick analogique tactile est optionnel).
 * - [TOUCH_STICK] : un doigt virtuel est posé au centre du stick analogique tactile de PPSSPP
 *   puis déplacé. Nécessite d'avoir activé « Touch analog stick » dans PPSSPP et de calibrer
 *   sa position.
 */
enum class AnalogInjectionMode(val label: String, val description: String) {
    DPAD(
        "Croix directionnelle",
        "Le stick est converti en appuis Haut/Bas/Gauche/Droite. Aucune configuration dans PPSSPP."
    ),
    TOUCH_STICK(
        "Stick analogique tactile",
        "Un doigt virtuel reste posé sur le stick tactile de PPSSPP. Activez « Touch analog stick » dans PPSSPP."
    )
}

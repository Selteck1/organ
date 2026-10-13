package ru.castle.game

fun GameView.mapSceneOrNull(): MapScene? =
    try { mapScene() } catch (e: Exception) { null }

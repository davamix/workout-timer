package io.github.davamix.workouttimer.di

import android.content.Context

/**
 * Creates and holds the app's long-lived objects (plan D-3). Each phase adds the dependencies it provides here:
 * storage in P3 and audio in P5.
 */
class AppContainer(
    val context: Context,
)

package org.nooklin.photos

import android.content.Context
import androidx.annotation.StringRes
import java.lang.ref.WeakReference

// UI text outside @Composable scope too. Uses the current activity's resources so a language change applies at once, falls back to the application.
object AppStrings {
    @Volatile var app: Context? = null
    @Volatile var activity: WeakReference<Context>? = null
}

fun appString(@StringRes id: Int, vararg args: Any?): String {
    val ctx = AppStrings.activity?.get() ?: AppStrings.app ?: error("AppStrings not initialised")
    return if (args.isEmpty()) ctx.getString(id) else ctx.getString(id, *args)
}

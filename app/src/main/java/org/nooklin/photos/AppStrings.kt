package org.nooklin.photos

import android.content.Context
import androidx.annotation.StringRes
import java.lang.ref.WeakReference

// UI text outside @Composable scope too.
object AppStrings {
    @Volatile var app: Context? = null
    @Volatile var activity: WeakReference<Context>? = null
}

fun appString(@StringRes id: Int, vararg args: Any?): String {
    val ctx = AppStrings.activity?.get() ?: AppStrings.app ?: error("AppStrings not initialised")
    return if (args.isEmpty()) ctx.getString(id) else ctx.getString(id, *args)
}

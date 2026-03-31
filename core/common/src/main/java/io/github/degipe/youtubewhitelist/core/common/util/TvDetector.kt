package io.github.degipe.youtubewhitelist.core.common.util

import android.content.Context
import android.content.pm.PackageManager

object TvDetector {
    fun isTV(context: Context): Boolean =
        context.packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK)
}

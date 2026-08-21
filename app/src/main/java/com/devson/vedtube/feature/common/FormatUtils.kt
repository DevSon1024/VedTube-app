package com.devson.vedtube.feature.common

import java.util.Locale

object FormatUtils {

    fun formatDurationSeconds(seconds: Long): String {
        if (seconds <= 0) return "0:00"
        val s = seconds % 60
        val m = (seconds / 60) % 60
        val h = seconds / 3600
        return if (h > 0) {
            String.format(Locale.US, "%d:%02d:%02d", h, m, s)
        } else {
            String.format(Locale.US, "%d:%02d", m, s)
        }
    }

    fun formatDurationMillis(millis: Long): String {
        return formatDurationSeconds(millis / 1000)
    }

    fun formatViewCount(views: Long): String {
        return when {
            views < 0 -> ""
            views < 1_000 -> "$views views"
            views < 1_000_000 -> String.format(Locale.US, "%.1fK views", views / 1_000.0).replace(".0K", "K")
            views < 1_000_000_000 -> String.format(Locale.US, "%.1fM views", views / 1_000_000.0).replace(".0M", "M")
            else -> String.format(Locale.US, "%.1fB views", views / 1_000_000_000.0).replace(".0B", "B")
        }
    }

    fun formatSubscriberCount(count: Long?): String {
        if (count == null || count < 0) return ""
        return when {
            count < 1_000 -> "$count subscribers"
            count < 1_000_000 -> String.format(Locale.US, "%.1fK subscribers", count / 1_000.0).replace(".0K", "K")
            else -> String.format(Locale.US, "%.1fM subscribers", count / 1_000_000.0).replace(".0M", "M")
        }
    }
}

package com.skynet.monitoring.ui.navigation

/** Definisi route navigasi. */
object Routes {
    const val LOGIN = "login"
    const val TASKS = "tasks"
    const val TASK_DETAIL = "tasks/{id}"
    const val WORKING = "tasks/{id}/working"
    const val NOTIFICATIONS = "notifications"
    const val PROFILE = "profile"
    const val HISTORY = "history"

    /** Argumen path bersama untuk detail & working. */
    const val ARG_ID = "id"

    fun taskDetail(id: Int) = "tasks/$id"
    fun working(id: Int) = "tasks/$id/working"
}

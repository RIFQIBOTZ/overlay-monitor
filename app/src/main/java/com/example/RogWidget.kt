package com.example

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews

class RogWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        appWidgetIds.forEach { appWidgetId ->
            updateWidget(context, appWidgetManager, appWidgetId)
        }
    }

    companion object {
        fun updateWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val prefs = context.getSharedPreferences("widget_data", Context.MODE_PRIVATE)
            val cpu   = prefs.getString("cpu",  "--") ?: "--"
            val ram   = prefs.getString("ram",  "--") ?: "--"
            val bat   = prefs.getString("bat",  "--") ?: "--"
            val temp  = prefs.getString("temp", "--") ?: "--"

            val views = RemoteViews(context.packageName, R.layout.widget_layout)
            views.setTextViewText(R.id.widget_cpu,  "CPU $cpu")
            views.setTextViewText(R.id.widget_ram,  "RAM $ram")
            views.setTextViewText(R.id.widget_bat,  "BAT $bat")
            views.setTextViewText(R.id.widget_temp, "TEMP $temp")

            // Tap widget buka app
            val intent = Intent(context, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
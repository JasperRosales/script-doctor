package com.scriptdoctor

import android.annotation.SuppressLint
import android.content.Context

@SuppressLint("StaticFieldLeak") // Holds the Application context only.
object AndroidAppContext {
    lateinit var context: Context
        private set

    fun init(app: Context) {
        context = app.applicationContext
    }
}

package cl.duocuc.pagoflex.app.data

import android.content.Context
import androidx.core.content.edit

class RecordarCorreo(context: Context) {
    private val prefs = context.getSharedPreferences("sesion_pagoflex", Context.MODE_PRIVATE)

    companion object {
        private const val CLAVE_ULTIMO_CORREO = "ultimo_correo"
    }

    fun guardarCorreo(correo: String) = prefs.edit { putString(CLAVE_ULTIMO_CORREO, correo) }

    fun leerCorreo(): String = prefs.getString(CLAVE_ULTIMO_CORREO, "") ?: ""
}

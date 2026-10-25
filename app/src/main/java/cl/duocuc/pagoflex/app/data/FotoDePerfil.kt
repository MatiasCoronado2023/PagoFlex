package cl.duocuc.pagoflex.app.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File

private const val LADO_MAXIMO = 512

class FotoDePerfil(private val context: Context) {
    private fun archivo(idUsuario: Long) = File(context.filesDir, "foto_$idUsuario.jpg")

    fun leer(idUsuario: Long): Bitmap? {
        val f = archivo(idUsuario)
        return if (f.exists()) BitmapFactory.decodeFile(f.absolutePath) else null
    }

    fun guardar(idUsuario: Long, foto: Bitmap): Bitmap {
        val reducida = reducir(foto)
        archivo(idUsuario).outputStream().use { reducida.compress(Bitmap.CompressFormat.JPEG, 85, it) }
        return reducida
    }

    fun guardar(idUsuario: Long, uri: Uri): Bitmap? {
        val medidas = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, medidas) }
        if (medidas.outWidth <= 0 || medidas.outHeight <= 0) return null
        var muestra = 1
        while (maxOf(medidas.outWidth, medidas.outHeight) / (muestra * 2) >= LADO_MAXIMO) muestra *= 2
        val opciones = BitmapFactory.Options().apply { inSampleSize = muestra }
        val foto = context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opciones) }
            ?: return null
        return guardar(idUsuario, foto)
    }

    fun borrar(idUsuario: Long) {
        archivo(idUsuario).delete()
    }

    private fun reducir(foto: Bitmap): Bitmap {
        val lado = maxOf(foto.width, foto.height)
        if (lado <= LADO_MAXIMO) return foto
        return Bitmap.createScaledBitmap(foto, foto.width * LADO_MAXIMO / lado, foto.height * LADO_MAXIMO / lado, true)
    }
}

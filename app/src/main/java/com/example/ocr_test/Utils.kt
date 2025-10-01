package com.example.ocr_test

import android.content.Context
import android.util.Base64
import java.io.File
import java.io.FileInputStream
import org.json.JSONObject

fun drawableToFile(context: Context, drawableId: Int, fileName: String): File {
    val file = File(context.cacheDir, fileName)
    val inputStream = context.resources.openRawResource(drawableId)
    val outputStream = file.outputStream()

    inputStream.use { input ->
        outputStream.use { output ->
            input.copyTo(output)
        }
    }
    return file
}

fun fileToBase64(file: File): String {
    val bytes = FileInputStream(file).readBytes()
    return Base64.encodeToString(bytes, Base64.NO_WRAP)
}

fun parseInferText(json: String): String {
    val jsonObj = JSONObject(json)
    val images = jsonObj.getJSONArray("images")
    if (images.length() == 0) return ""

    val fields = images.getJSONObject(0).getJSONArray("fields")

    val sb = StringBuilder()

    var prevY = -1.0
    var prevX = -1.0
    var prevText = ""

    for (i in 0 until fields.length()) {
        val field = fields.getJSONObject(i)
        val text = field.getString("inferText")

        val vertices = field.getJSONObject("boundingPoly").getJSONArray("vertices")
        val ys = mutableListOf<Double>()
        val xs = mutableListOf<Double>()
        for (j in 0 until vertices.length()) {
            ys.add(vertices.getJSONObject(j).getDouble("y"))
            xs.add(vertices.getJSONObject(j).getDouble("x"))
        }
        val avgY = ys.average()
        val avgX = xs.average()
        if (prevText.endsWith(".") || prevText.endsWith("?") ||
            prevText.endsWith("!") || prevText.endsWith("\"") ||
            prevText.endsWith("”")) {

            if (prevY > 0 && (Math.abs(avgY - prevY) > 300) || (Math.abs(avgX - prevX) > 800) ) {
                println("Y: ${Math.abs(avgY - prevY)}")
                println("X: ${Math.abs(avgX - prevX)}")
                sb.append("\n")
            }
            sb.append("\n")
        }

        sb.append(text).append(" ")

        prevY = avgY
        prevX = avgX
        prevText = text
    }

    return sb.toString().trim()
}
package com.example.dtls

import android.content.Context
import android.graphics.Bitmap
import android.util.Base64
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.io.BufferedReader
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

/*
* GestureDetection using Robloflow API
*/
class GestureDetectionOnline private constructor(private val context: Context){

    public var threshold: Float = 0.50f
    public var models: Array<String> = arrayOf(
        "deteccion-y-traduccion-de-lenguaje-de-senas-chilena/2", // 6k
        "deteccion-y-traduccion-de-lenguaje-de-senas-chilena/9", // 3k
        "deteccion-y-traduccion-de-lenguaje-de-senas-chilena/10",// 2k
        "deteccion-y-traduccion-de-lenguaje-de-senas-chilena/12", // 6K CU NN LA
        "deteccion-y-traduccion-de-lenguaje-de-senas-chilena/15", // 6K CU NN HA
    )
    public var model:Int = 4
    private var apiKey:String = "hO4rKeR6jM4wx0Shoexg"

    // each RoboFlow predictions flows this object format
    @Serializable
    data class Prediction(
        val x: Float,
        val y: Float,
        val width: Float,
        val height: Float,
        @SerialName("class")
        val className: String,
        val confidence: Float
    )

    @Serializable
    data class ImageData(
        val width: Float,
        val height: Float
    )

    @Serializable
    data class PredictionResponse(
        @SerialName("predictions")
        val predictions: List<Prediction>,
        @SerialName("image")
        val image: ImageData
    )

    companion object{
        private var instance: GestureDetectionOnline? = null

        fun getInstance(context: Context): GestureDetectionOnline{
            return instance?: synchronized(this){
                instance?: GestureDetectionOnline(context).also { instance = it }
            }
        }
    }

    fun getPredictions(bitmap: Bitmap): List<Prediction>{
        // Encoding in Base64
        val base64Bitmap = bitmapToBase64(bitmap)
        // constructing the url
        val uploadUrl = "https://detect.roboflow.com/" + models[model] + "?api_key=" + apiKey
        // HTTP request
        var connection: HttpURLConnection? = null
        try {
            // set up connection to url
            val url = URL(uploadUrl)
            connection = url.openConnection() as HttpURLConnection
            connection.requestMethod  = "POST"
            connection.setRequestProperty(
                "Content-Type",
                "application/x-www-form-urlencoded"
            )
            connection.setRequestProperty(
                "Content-Length",
                base64Bitmap.length.toString()
            )
            connection.setRequestProperty(
                "Content-Lenguaje",
                "en-Us"
            )
            connection.useCaches = false
            connection.doOutput = true

            // send request
            val wr = DataOutputStream(connection.outputStream)
            wr.writeBytes(base64Bitmap)
            wr.close()

            // get response
            val stream =connection.inputStream

            val response = connection.inputStream.use { stream ->
                val reader = BufferedReader(InputStreamReader(stream))
                reader.readText()
            }

            // Deserialization off the json onto PredictionResponse
            val json = Json{ignoreUnknownKeys = true}
            val predictionResponse = json.decodeFromString<PredictionResponse>(response)

            return predictionResponse.predictions
        }catch (e: Exception){
            e.printStackTrace()
        } finally {
            connection?.disconnect()
        }

        return emptyList()
    }



    fun bitmapToBase64(bitmap: Bitmap):String{
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG,100,byteArrayOutputStream)
        val byteArray = byteArrayOutputStream.toByteArray()
        return Base64.encodeToString(byteArray,Base64.DEFAULT)
    }

}
package com.example.dtls

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.RectF
import android.util.Log
import org.pytorch.IValue
import org.pytorch.Module
import org.pytorch.Tensor
import org.pytorch.torchvision.TensorImageUtils
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class GestureDetection private constructor(private val context: Context){
    public var models: Array<String> = arrayOf(
        "optimized_b4nms.pth",
        "optimized_b4nms.pt",
        "optimized_2k.pth",
        "optimized_2k_int.ptl",
        "optimized_6k_CU_NN_HA_class.pth",
        "optimized_6k_CU_NN_HA_int.ptl"
        )

    public val model = 4
    public var threshold: Float = 0.50f
    private val module: Module = Module.load(assetFilePath(context,models[model]))

    data class DetectResult(
        val boundingBox: RectF,
        val classId: Int,
        val score: Float,
    )

    companion object{
        private var instace: GestureDetection? = null

        fun getInstance(context: Context): GestureDetection{
            return instace?: GestureDetection(context).also { instace= it }
        }
    }

    public fun getTranslation(bitmap: Bitmap): List<DetectResult>? {
        var resizedBitmap= resizeBitmap(bitmap)
        val inputTensor = TensorImageUtils.bitmapToFloat32Tensor(
            resizedBitmap,
            TensorImageUtils.TORCHVISION_NORM_MEAN_RGB,
            TensorImageUtils.TORCHVISION_NORM_STD_RGB
        )

        var x :IValue
        var boxes: IValue
        var score: IValue
        try {
            var (mX,mBoxes,mScores)  = module.forward(IValue.from(inputTensor)).toTuple()
            x = mX
            boxes = mBoxes
            score = mScores

        }catch (e:Exception){
            Log.println(Log.ERROR,"In getTranslation","Prediction Failed"+ e.toString())
            return null
        }

        if(x.isNull){
            Log.println(Log.ERROR,"In getTranslation" ,"x is null")
        }else{
            Log.println(Log.ERROR,"In getTranslation" ,x.toString())
        }

        // this a self implemented (NMS) no maximum suppress
        var detection = nms(x.toTensor(), 0.50f)
        if(detection.isEmpty()){
            Log.println(Log.ERROR,"In getTranslation" ,"detection")
        }else{
            Log.println(Log.ERROR,"In getTranslation" ,detection.toString())
            Log.println(Log.ERROR,"In getTranslation" ,x.toTensor().toString())

        }

         return detection
    }



    fun assetFilePath(context: Context, assetName: String): String? {
        val file = File(context.filesDir, assetName)

        try {
            context.assets.open(assetName).use { `is` ->
                FileOutputStream(file).use { os ->
                    val buffer = ByteArray(4 * 1024)
                    while (true) {
                        val length = `is`.read(buffer)
                        if (length <= 0)
                            break
                        os.write(buffer, 0, length)
                    }
                    os.flush()
                    os.close()
                }
                return file.absolutePath
            }
        } catch (e: IOException) {
            Log.e("pytorchandroid", "Error process asset $assetName to file path")
        }

        return null
    }


    fun getHighestScore(detectResult: List<DetectResult>): DetectResult? {
        if(detectResult.isEmpty()){
            return null
        }

        return detectResult.maxByOrNull { it.score }
    }
    fun resizeBitmap(bitmap: Bitmap): Bitmap {
        val desiredWith = 640
        val desiredHeight = 640

        val scaleX = desiredWith.toFloat() /bitmap.width
        val scaleY = desiredHeight.toFloat() / bitmap.height

        val matrix = Matrix()
        matrix.postScale(scaleX,scaleY)

        return Bitmap.createBitmap(bitmap,0,0,bitmap.width,bitmap.height,matrix,false)
    }


    fun nms(x: Tensor, threshold: Float): List<DetectResult> {
        // x: [0:4] - box, [4] - score, [5] - class
        val data = x.dataAsFloatArray
        val numElem = x.shape()[0].toInt()
        val innerShape = x.shape()[1].toInt()
        val selected_indices = (0 until numElem).toMutableList()

        val scores =  data.sliceArray( (0 until numElem)
            .flatMap { r->(r*innerShape)+4 until (r*innerShape)+5 } )

        val boxes = data.sliceArray( (0 until numElem)
            .flatMap { r->(r*innerShape) until (r*innerShape)+4 } )

        val classes = data.sliceArray( (0 until numElem)
            .flatMap { r->(r*innerShape)+5 until (r*innerShape)+6 } )

        for (i in 0 until numElem) {
            val current_class = classes[i].toInt()
            for (j in i+1 until numElem) {
                val box_i = boxes.sliceArray(i*4 until (i*4)+4)
                val box_j = boxes.sliceArray(j*4 until (j*4)+4)
                val iou = calculate_iou(box_i, box_j)
                // TODO: el treshold no se esta respetando
                if (iou > threshold && classes[j].toInt() == current_class) {
                    if (scores[j] > scores[i]) {
                        selected_indices.remove(i)
                        break
                    } else {
                        selected_indices.remove(j)
                    }
                }
            }
        }

        val result = mutableListOf<DetectResult>()
        for (i in 0 until numElem) {
            if (selected_indices.contains(i)) {
                val box = boxes.slice((i*4) until (i*4)+4)
                val detection = DetectResult(boundingBox =
                RectF(box[0], box[1], box[2], box[3]), score = scores[i],
                    classId = classes[i].toInt())
                result.add(detection)
            }
        }

        return result
    }

    fun calculate_iou(box1: FloatArray, box2: FloatArray): Float {
        val x1 = maxOf(box1[0], box2[0])
        val y1 = maxOf(box1[1], box2[1])
        val x2 = minOf(box1[2], box2[2])
        val y2 = minOf(box1[3], box2[3])

        val intersection = maxOf(0f, x2 - x1) * maxOf(0f, y2 - y1)
        val area1 = (box1[2] - box1[0]) * (box1[3] - box1[1])
        val area2 = (box2[2] - box2[0]) * (box2[3] - box2[1])
        val union = area1 + area2 - intersection

        return intersection / union
    }

}
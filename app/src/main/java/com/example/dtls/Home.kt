package com.example.dtls

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.RectF
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import com.google.android.material.button.MaterialButton
import com.google.common.util.concurrent.ListenableFuture
import org.pytorch.IValue
import org.pytorch.Module
import org.pytorch.Tensor
import org.pytorch.torchvision.TensorImageUtils
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.Executors


class Home : Fragment() {

    private val executor = Executors.newSingleThreadExecutor()
    private lateinit var cameraProviderFuture : ListenableFuture<ProcessCameraProvider>
    private lateinit var previewView : PreviewView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val rootView = inflater.inflate(R.layout.fragment_home, container, false)

        /*if(ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.CAMERA
            )!= PackageManager.PERMISSION_GRANTED){
            val dialogFragment = PermissionDialogFragment()
            dialogFragment.show(childFragmentManager,"Permisos Dialog")
        }*/


        previewView = rootView.findViewById<PreviewView>(R.id.previewView)


        val reloadButton = rootView.findViewById<MaterialButton>(R.id.reloadButton)


        reloadButton.setOnClickListener{
            testRedNeuronal(rootView)
        }
       /* var assetManager = requireContext().assets

        var bitmap = BitmapFactory.decodeStream(assetManager.open("a1.jpg"))
        // optimized_b4nms
        val moduleFilePath = assetFilePath(requireContext(), "optimized_b4nms.pth")
        val module = Module.load(moduleFilePath)

        bitmap = resizeBitmap(bitmap)
        val inputTensor = TensorImageUtils.bitmapToFloat32Tensor(
            bitmap,
            TensorImageUtils.TORCHVISION_NORM_MEAN_RGB, TensorImageUtils.TORCHVISION_NORM_STD_RGB
        )
        val textView: TextView = rootView.findViewById(R.id.testText)
        textView.text = inputTensor.toString()+"\n"
        // the model have 3 outputs, so make it into Tuple
        val (x, boxes, scores) = module.forward(IValue.from(inputTensor)).toTuple()
        // This is a self implemented NMS...
        // (which if torchvision provide this, we no longer need to implemenet ourselves)
        var detResult = nms(x.toTensor(), 0.45f) // the 0.45 is IoU threshold
        var highestScore = getHighestScore(detResult)
        // detResult[0].boundingBox
        var boundingBox = highestScore?.boundingBox
        var classId = highestScore?.classId
        var score= highestScore?.score
        var text = "Bounding Box: $boundingBox\n ClassID: $classId\n Score: $score\n"
        textView.text = text*/
        return rootView
    }

    fun testRedNeuronal(rootView :View) {
        var assetManager = requireContext().assets

        var bitmap = BitmapFactory.decodeStream(assetManager.open("a1.jpg"))
        // optimized_b4nms
        val moduleFilePath = assetFilePath(requireContext(), "optimized_b4nms.pth")
        val module = Module.load(moduleFilePath)

        bitmap = resizeBitmap(bitmap)
        val inputTensor = TensorImageUtils.bitmapToFloat32Tensor(
            bitmap,
            TensorImageUtils.TORCHVISION_NORM_MEAN_RGB, TensorImageUtils.TORCHVISION_NORM_STD_RGB
        )
        val textView: TextView = rootView.findViewById(R.id.testText)
        textView.text = inputTensor.toString()+"\n"
        // the model have 3 outputs, so make it into Tuple
        val (x, boxes, scores) = module.forward(IValue.from(inputTensor)).toTuple()
        // This is a self implemented NMS...
        // (which if torchvision provide this, we no longer need to implemenet ourselves)
        var detResult = nms(x.toTensor(), 0.45f) // the 0.45 is IoU threshold
        var highestScore = getHighestScore(detResult)
        // detResult[0].boundingBox
        var boundingBox = highestScore?.boundingBox
        var classId = highestScore?.classId
        var score= highestScore?.score
        var text = "Bounding Box: $boundingBox\n ClassID: $classId\n Score: $score\n"
        textView.text = text
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                requireActivity(),
                REQUIRED_PERMISSIONS,
                REQUEST_CODE_PERMISSIONS
            )
        }
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(requireContext(), it) == PackageManager.PERMISSION_GRANTED
    }

    private fun startCamera() {
        cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())

        cameraProviderFuture.addListener(Runnable {
            val cameraProvider = cameraProviderFuture.get()
            bindPreview(cameraProvider)
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    fun bindPreview(cameraProvider : ProcessCameraProvider) {
        var preview : Preview = Preview.Builder()
            .build()

        var cameraSelector : CameraSelector = CameraSelector.Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_BACK)
            .build()

        preview.setSurfaceProvider(previewView.surfaceProvider)

        var camera = cameraProvider.bindToLifecycle(this as LifecycleOwner, cameraSelector, preview)
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

    data class DetectResult(
        val boundingBox: RectF,
        val classId: Int,
        val score: Float,
    )

    fun nms(x: Tensor, threshold: Float): List<DetectResult> {
        // x: [0:4] - box, [4] - score, [5] - class
        val data = x.dataAsFloatArray
        val numElem = x.shape()[0].toInt()
        val innerShape = x.shape()[1].toInt()
        val selected_indices = (0 until numElem).toMutableList()

        val scores =  data.sliceArray( (0 until numElem).flatMap { r->(r*innerShape)+4 until (r*innerShape)+5 } )
        val boxes = data.sliceArray( (0 until numElem).flatMap { r->(r*innerShape) until (r*innerShape)+4 } )
        val classes = data.sliceArray( (0 until numElem).flatMap { r->(r*innerShape)+5 until (r*innerShape)+6 } )

        for (i in 0 until numElem) {
            val current_class = classes[i].toInt()
            for (j in i+1 until numElem) {
                val box_i = boxes.sliceArray(i*4 until (i*4)+4)
                val box_j = boxes.sliceArray(j*4 until (j*4)+4)
                val iou = calculate_iou(box_i, box_j)
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
                val detection = DetectResult(boundingBox = RectF(box[0], box[1], box[2], box[3]), score = scores[i], classId = classes[i].toInt())
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

    companion object {
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }
}
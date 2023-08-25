package com.example.dtls

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
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
import org.pytorch.torchvision.TensorImageUtils
import java.util.concurrent.Executors


class Home : Fragment() {

    private val executor = Executors.newSingleThreadExecutor()
    private lateinit var cameraProviderFuture : ListenableFuture<ProcessCameraProvider>
    private lateinit var translatedTextView: EditText
    private lateinit var previewView : PreviewView
    private lateinit var gestureDetection: GestureDetection
    private var translate: Boolean =false

    val spanishAlphabet = "abcdefghijklmnñopqrstuvwxyz"
    val alphabetMap =spanishAlphabet
        .withIndex()
        .associate { (index, letter) -> index to letter.toString() }


    // debug textview
    private lateinit var debugClassId: TextView
    private lateinit var debugScore: TextView
    private lateinit var debugBoundigBox: TextView


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
        translatedTextView= rootView.findViewById<EditText>(R.id.translatedText)
        debugScore = rootView.findViewById<TextView>(R.id.textScore)
        debugClassId = rootView.findViewById<TextView>(R.id.textClassId)
        debugBoundigBox = rootView.findViewById<TextView>(R.id.textBoundingBox)



        val reloadButton = rootView.findViewById<MaterialButton>(R.id.reloadButton)
        val button = rootView.findViewById<MaterialButton>(R.id.translateButton)


        button.setOnClickListener{
            translateCurrentGesture()
        }

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

    fun makeToast(string: String){
        Toast.makeText(requireContext(),string,Toast.LENGTH_LONG)
    }
    fun translateCurrentGesture(){

        Log.println(Log.ERROR,"Translate Current Gesture", "Entro")

        var detection = getImage()?.let { gestureDetection.getTranslation(it) }
        Log.println(Log.ERROR,"Translate Current Gesture", "Entro2")

        if (detection==null){
//            Toast.makeText(requireContext(),"Error al obtener el gesto", Toast.LENGTH_LONG)
            Log.println(Log.ERROR,"Translate Current Gesture", "Detection is null")
            return
        }

        debugScore.text = detection.score.toString()
        debugClassId.text = detection.classId.toString()
        debugBoundigBox.text = detection.boundingBox.toString()


//        if (detection?.classId!! <0 || detection?.classId!! >27){
////            Toast.makeText(requireContext(),"Error al obtener el gesto",Toast.LENGTH_LONG)
//            Log.println(Log.ERROR,"Translate Current Gesture", "class id out of bounds")
//            return
//        }

        var letter = alphabetMap[detection.classId]

        translatedTextView.append(letter)
    }


    fun getImage(): Bitmap? {
        var previewBitmap: Bitmap? =null
        requireActivity().runOnUiThread {
            previewBitmap= previewView.bitmap!!
        }
        return previewBitmap
    }

    fun testRedNeuronal(rootView :View) {
        var assetManager = requireContext().assets

        var bitmap = BitmapFactory.decodeStream(assetManager.open("a1.jpg"))
        // optimized_b4nms
        val moduleFilePath = gestureDetection.assetFilePath(requireContext(), "optimized_b4nms.pth")
        val module = Module.load(moduleFilePath)

        bitmap = gestureDetection.resizeBitmap(bitmap)
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
        var detResult =gestureDetection.nms(x.toTensor(), 0.45f) // the 0.45 is IoU threshold
        var highestScore =gestureDetection.getHighestScore(detResult)
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
            gestureDetection = GestureDetection.getInstance(requireContext())
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



    companion object {
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }
}
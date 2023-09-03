package com.example.dtls

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.os.Bundle
import android.os.Environment
import android.text.Editable
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.toRect
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import com.google.android.material.button.MaterialButton
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.pytorch.IValue
import org.pytorch.Module
import org.pytorch.torchvision.TensorImageUtils
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.util.concurrent.Executors


class Home : Fragment() {

    private val executor = Executors.newSingleThreadExecutor()
    private lateinit var cameraProviderFuture : ListenableFuture<ProcessCameraProvider>
    private lateinit var translatedTextView: EditText
    private lateinit var previewView : PreviewView
    private lateinit var overlayView: View
    private lateinit var progressBar:ProgressBar

    private lateinit var gestureDetection: GestureDetection
    private lateinit var gestureDetectionOnline: GestureDetectionOnline

    private var loading: Boolean = false

    private val timeUntilWhiteSpace = 5000
    private var timeSinceLastTranslation: Long =0
    private var currentTime: Long = 0
    private var translationThread: Thread? = null
    @Volatile
    private var isTranslating: Boolean = false

    private var bitmapImage: Bitmap? = null
    private var bitmapThread: Thread? =null

    // openCV variables



    val spanishAlphabet = "abcdefghijklmnopqrstuvwxyz"
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
        overlayView = rootView.findViewById<View>(R.id.overlayView)
        progressBar = rootView.findViewById<ProgressBar>(R.id.progressCircular)

        val reloadButton = rootView.findViewById<MaterialButton>(R.id.reloadButton)
        val translateButton = rootView.findViewById<MaterialButton>(R.id.translateButton)
        val deleteButton = rootView.findViewById<MaterialButton>(R.id.deleteButton)
        val spaceButton = rootView.findViewById<MaterialButton>(R.id.spaceButton)

        progressBar.visibility = View.GONE

        translateButton.setOnClickListener{
            progressBar.visibility = View.VISIBLE
            trasnlateCurrenteGestureOnline()
        }

        deleteButton.setOnClickListener{
            var string = translatedTextView.text

            if(string.isEmpty()){
                return@setOnClickListener
            }

           translatedTextView.text = string.delete(string.length-1,string.length)
        }

        spaceButton.setOnClickListener{
            if(translatedTextView.text.isEmpty()){
                return@setOnClickListener
            }
            if (translatedTextView.text.last().equals(" ")){
                return@setOnClickListener
            }
            translatedTextView.text.append(" ")
        }

        reloadButton.setOnClickListener{
            testRedNeuronal(rootView)
            // to minimize errors
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


        // thread in charge of the translation
        translationThread = Thread{
            while (true){
                if(!isTranslating){
                    continue
                }
                Log.println(
                    Log.DEBUG,
                    "Trans Thread",
                    "Is translating" +isTranslating.toString())
                currentTime = System.currentTimeMillis()
                val isLetter =  translateCurrentGesture()
                if(isLetter){
                    timeSinceLastTranslation = System.currentTimeMillis() - currentTime

                    // if the last char is a " " do not add the " "
                    val lastChar = translatedTextView.text.last()
                    if(lastChar.equals(" ")){
                        continue
                    }
                    // if the time since the last valid translation is less than
                    // the constant add a " "
                    if(timeSinceLastTranslation> timeUntilWhiteSpace){
                        translatedTextView.text.append(" ")
                    }
                }
                Thread.sleep(1000)
            }
        }

//        translationThread?.start()



        return rootView
    }

    override fun onDestroyView() {
        isTranslating = false
//        translationThread?.join()
//        bitmapThread?.join()

        super.onDestroyView()
    }

    fun drawRectangleOverlay(rectF: RectF){
        val overlayPaint = Paint().apply {
            color = Color.RED
            style = Paint.Style.STROKE
            strokeWidth = 4f
        }

        if (rectF.isEmpty || rectF == null){
            return
        }

        val rect = rectF.toRect()


        overlayView.requestRectangleOnScreen(rect,true)
        var canvas = previewView.bitmap?.let { Canvas(it) }
        canvas?.clipRect(rectF)

        previewView.draw(canvas)


    }

    fun String.toEditable(): Editable = Editable.Factory.getInstance().newEditable(this)

    private fun imageProxyToBitmap(imageProxy: ImageProxy):Bitmap?{
        val planeProxy = imageProxy.planes[0]
        val buffer: ByteBuffer= planeProxy.buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get()
        return BitmapFactory.decodeByteArray(bytes,0,bytes.size)
    }

    fun makeToast(string: String){
        Toast.makeText(requireContext(),string,Toast.LENGTH_SHORT)
    }

    fun trasnlateCurrenteGestureOnline(){
        var bitmap = getImage()
        GlobalScope.launch {

            if (bitmap==null){
                Log.println(Log.ERROR,"Translate Current Gesture", "bitmap is null")
                requireActivity().runOnUiThread {
                    progressBar.visibility = View.GONE
                }
                return@launch
            }

            var predictions= withContext(Dispatchers.IO){
                gestureDetectionOnline.getPredictions(bitmap)
            }

            if (predictions.isEmpty()){
                requireActivity().runOnUiThread {
                    Log.d("Prediction","No se detecto ningun gesto")
                    progressBar.visibility = View.GONE
                }
                return@launch
            }

            for (prediction in predictions){
                Log.d("Prediction",prediction.toString())
            }



            // debug
            requireActivity().runOnUiThread {
                debugScore.text = predictions[0].confidence.toString()
                debugClassId.text = predictions[0].className
                debugBoundigBox.text =
                    "x: ${predictions[0].x}\n"+"y: ${predictions[0].y}\n"+
                            "width: ${predictions[0].width}\n" + "height: ${predictions[0].height}"
            }

            if(predictions[0].confidence < gestureDetectionOnline.threshold){
                Log.println(
                    Log.ERROR,
                    "Translate Current Gesture",
                    "Score: "+ predictions[0].confidence.toString()
                )
                requireActivity().runOnUiThread {
                    progressBar.visibility = View.GONE
                }
                return@launch
            }

            requireActivity().runOnUiThread {
                translatedTextView.append(predictions[0].className)
                progressBar.visibility = View.GONE
            }
        }
    }
    fun translateCurrentGesture():Boolean{

        var bitmap = getImage()

        if (bitmap==null){
            Log.println(Log.ERROR,"Translate Current Gesture", "bitmap is null")
            return false
        }else{
            Log.println(Log.ERROR,"Translate Current Gesture","Bitmap is not Null")
//            saveBitmap(bitmap)
        }

//        saveBitmap(bitmap)

        var detection = gestureDetection.getTranslation(bitmap)

        if (detection==null){
            Log.println(Log.ERROR,"Translate Current Gesture", "Detection is null")
            return false
        }

        // debug
        debugScore.text = detection.score.toString()
        debugClassId.text = detection.classId.toString() +" = "+ alphabetMap[detection.classId]
        debugBoundigBox.text = detection.boundingBox.toString()

        if(detection.score < gestureDetection.threshold){
            Log.println(
                Log.ERROR,
                "Translate Current Gesture",
                "Score: "+ detection.score.toString()
            )
            return false
        }

//            drawRectangleOverlay(detection.boundingBox)


        var letter = alphabetMap[detection.classId]

        translatedTextView.append(letter)
        return true
    }

    fun checkGrammar(string: String): String {
        return "a"
    }

    private var count: Int = 0
    fun saveBitmap(bitmap: Bitmap){
        var myDir = Environment.getExternalStoragePublicDirectory(
            Environment.DIRECTORY_PICTURES)

        try {
            myDir.mkdir()


            var fileName = "$count.jpg"
            var file =File(myDir,fileName)

            var resizedBitmap = gestureDetection.resizeBitmap(bitmap)

            count++;
            var out = FileOutputStream(file)
            resizedBitmap.compress(Bitmap.CompressFormat.JPEG,100,out)
            out.flush()
            out.close()
        }catch (exc: Exception){
            Log.println(Log.ERROR,"save Bitmap", exc.toString())
        }
    }

    fun getImage(): Bitmap? {
        var previewBitmap: Bitmap? =null
        requireActivity().runOnUiThread {
            previewBitmap= previewView.bitmap
            bitmapImage = previewBitmap
        }
        return previewBitmap
    }

    fun testRedNeuronal(rootView :View) {
        var assetManager = requireContext().assets

        var bitmap = BitmapFactory.decodeStream(assetManager.open("a1.jpg"))
        val moduleFilePath = gestureDetection.assetFilePath(
            requireContext(),
            gestureDetection.models[gestureDetection.model]
        )
        // optimized_b4nms
        val module = Module.load(moduleFilePath)

        bitmap = gestureDetection.resizeBitmap(bitmap)
        val inputTensor = TensorImageUtils.bitmapToFloat32Tensor(
            bitmap,
            TensorImageUtils.TORCHVISION_NORM_MEAN_RGB, TensorImageUtils.TORCHVISION_NORM_STD_RGB
        )
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

        debugBoundigBox.text = boundingBox.toString()
        debugClassId.text = classId.toString()
        debugScore.text = score.toString()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (allPermissionsGranted()) {
            startCamera()
            gestureDetection = GestureDetection.getInstance(requireContext())
            gestureDetectionOnline = GestureDetectionOnline.getInstance(requireContext())
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

            val preview: Preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }


            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            cameraProvider.unbindAll()
//            preview.setSurfaceProvider(previewView.surfaceProvider)

            cameraProvider.bindToLifecycle(this , cameraSelector, preview)

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


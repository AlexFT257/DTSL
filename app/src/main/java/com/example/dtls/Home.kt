package com.example.dtls

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.pytorch.IValue
import org.pytorch.Module
import org.pytorch.torchvision.TensorImageUtils
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.Executors


class Home : Fragment() {

    private val executor = Executors.newSingleThreadExecutor()
    private lateinit var cameraProviderFuture : ListenableFuture<ProcessCameraProvider>
    private lateinit var translatedTextView: EditText
    private lateinit var previewView : PreviewView
    private lateinit var overlayView: View
    private lateinit var progressBar:ProgressBar

    private lateinit var optionContainer: LinearLayout
    private lateinit var optionButtons: MutableList<MaterialButton>
    private lateinit var suggestionTextView: TextView

    private lateinit var gestureDetection: GestureDetection
    private lateinit var gestureDetectionOnline: GestureDetectionOnline


    private val timeUntilWhiteSpace = 5000
    private var timeSinceLastTranslation: Long =0
    private var currentTime: Long = 0
    private var translationThread: Thread? = null
    @Volatile
    private var isTranslating: Boolean = false

    private var bitmapImage: Bitmap? = null
    private var bitmapThread: Thread? =null

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
        optionContainer = rootView.findViewById(R.id.optionContainer)
        suggestionTextView = rootView.findViewById(R.id.suggestionTextView)



        val reloadButton = rootView.findViewById<MaterialButton>(R.id.reloadButton)
        val translateButton = rootView.findViewById<MaterialButton>(R.id.translateButton)
        val deleteButton = rootView.findViewById<MaterialButton>(R.id.deleteButton)
        val spaceButton = rootView.findViewById<MaterialButton>(R.id.spaceButton)

        progressBar.visibility = View.GONE

        optionButtons = mutableListOf<MaterialButton>()
        suggestionTextView.visibility = View.GONE

        translateButton.setOnClickListener{
            progressBar.visibility = View.VISIBLE

            if(isInternetAvailable(requireContext())){
                trasnlateCurrenteGestureOnline()
            } else{
                translateCurrentGesture()
            }
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
        }

        // thread in charge of the translation
        /*translationThread = Thread{
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
        }*/



        return rootView
    }

    override fun onDestroyView() {
        isTranslating = false
//        translationThread?.join()
//        bitmapThread?.join()

        super.onDestroyView()
    }

    fun isInternetAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        val networkCapabilities = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)

        return networkCapabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
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

                    // reset the view and content if nothing was found
                    progressBar.visibility = View.GONE
                    suggestionTextView.visibility =View.GONE
                    optionContainer.visibility = View.GONE

                    // delete previous buttons
                    optionContainer.removeAllViews()
                    optionButtons.clear()
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

            Log.println(
                Log.ERROR,
                "Translate Current Gesture",
                "Score: "+ predictions[0].confidence.toString()
            )
            if(predictions[0].confidence < gestureDetectionOnline.threshold){
                requireActivity().runOnUiThread {
                    progressBar.visibility = View.GONE
                    suggestionTextView.visibility =View.VISIBLE
                    optionContainer.visibility = View.VISIBLE

                    // delete previous buttons
                    optionContainer.removeAllViews()
                    optionButtons.clear()

                    val maxButtonsToShow = 3
                    for ((index,prediction) in predictions.withIndex()){
                        if (index>=maxButtonsToShow){
                            break
                        }

                        val optionButton = MaterialButton(requireContext())
                        optionButton.text = "${prediction.className}"

                        optionButton.setOnClickListener {
                            translatedTextView.append(prediction.className)

                            for (button in optionButtons){
                                optionContainer.removeView(button)
                            }
                            optionButtons.clear()
                            optionContainer.visibility = View.GONE
                            suggestionTextView.visibility = View.GONE
                        }

                        // add the button to the list and container
                        optionButtons.add(optionButton)
                        optionContainer.addView(optionButton)
                    }
                }
                return@launch
            }

            requireActivity().runOnUiThread {
                translatedTextView.append(predictions[0].className)

                // reset the view and content if nothing was found
                progressBar.visibility = View.GONE
                suggestionTextView.visibility =View.GONE
                optionContainer.visibility = View.GONE

                // delete previous buttons
                optionContainer.removeAllViews()
                optionButtons.clear()
            }
        }
    }

    fun translateCurrentGesture(){
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
                gestureDetection.getTranslation(bitmap)
            }



            if (predictions.isNullOrEmpty()){
                requireActivity().runOnUiThread {
                    Log.d("Prediction","No se detecto ningun gesto")

                    // reset the view and content if nothing was found
                    progressBar.visibility = View.GONE
                    suggestionTextView.visibility =View.GONE
                    optionContainer.visibility = View.GONE

                    // delete previous buttons
                    optionContainer.removeAllViews()
                    optionButtons.clear()
                }
                return@launch
            }

            for (prediction in predictions){
                Log.d("Prediction",prediction.toString())
            }

            // debug
            requireActivity().runOnUiThread {
                debugScore.text = predictions[0].score.toString()
                debugClassId.text = predictions[0].classId.toString()
                debugBoundigBox.text =
                    "x: ${predictions[0].boundingBox.centerX()}\n"+"y: ${predictions[0].boundingBox.centerY()}\n"+
                            "width: ${predictions[0].boundingBox.width()}\n" + "height: ${predictions[0].boundingBox.height()}"
            }

            Log.println(
                Log.ERROR,
                "Translate Current Gesture",
                "Score: "+ predictions[0].score.toString()
            )
            if(predictions[0].score < gestureDetection.threshold){
                requireActivity().runOnUiThread {
                    progressBar.visibility = View.GONE
                    suggestionTextView.visibility =View.VISIBLE
                    optionContainer.visibility = View.VISIBLE

                    // delete previous buttons
                    optionContainer.removeAllViews()
                    optionButtons.clear()

                    val maxButtonsToShow = 3
                    for ((index,prediction) in predictions.withIndex()){
                        if (index>=maxButtonsToShow){
                            break
                        }

                        val optionButton = MaterialButton(requireContext())
                        optionButton.text = "${alphabetMap[prediction.classId]}"

                        optionButton.setOnClickListener {
                            translatedTextView.append(alphabetMap[prediction.classId])

                            for (button in optionButtons){
                                optionContainer.removeView(button)
                            }
                            optionButtons.clear()
                            optionContainer.visibility = View.GONE
                            suggestionTextView.visibility = View.GONE
                        }

                        // add the button to the list and container
                        optionButtons.add(optionButton)
                        optionContainer.addView(optionButton)
                    }
                }
                return@launch
            }

            requireActivity().runOnUiThread {
                translatedTextView.append(alphabetMap[predictions[0].classId])

                // reset the view and content if nothing was found
                progressBar.visibility = View.GONE
                suggestionTextView.visibility =View.GONE
                optionContainer.visibility = View.GONE

                // delete previous buttons
                optionContainer.removeAllViews()
                optionButtons.clear()
            }
        }
    }

    /*fun translateCurrentGesture():Boolean{

        var bitmap = getImage()

        if (bitmap==null){
            Log.println(Log.ERROR,"Translate Current Gesture", "bitmap is null")
            requireActivity().runOnUiThread {
                progressBar.visibility = View.GONE
            }
            return false
        }

        var detection = gestureDetection.getTranslation(bitmap)

        if (detection.isNullOrEmpty()){
            Log.println(Log.ERROR,"Translate Current Gesture", "Detection is null")
            requireActivity().runOnUiThread {
                progressBar.visibility = View.GONE
            }
            return false
        }

        // debug
        requireActivity().runOnUiThread {
            debugScore.text = detection[0].score.toString()
            debugClassId.text = detection[0].classId.toString() +" = "+ alphabetMap[detection.classId]
            debugBoundigBox.text = detection[0].boundingBox.toString()
        }

        if(detection[0].score < gestureDetection.threshold){
            Log.println(
                Log.ERROR,
                "Translate Current Gesture",
                "Score: "+ detection.score[0].toString()
            )
            requireActivity().runOnUiThread {
                progressBar.visibility = View.GONE
            }
            return false
        }

//            drawRectangleOverlay(detection.boundingBox)


        var letter = alphabetMap[detection.classId]

        translatedTextView.append(letter)
        requireActivity().runOnUiThread {
            progressBar.visibility = View.GONE
        }
        return true
    }*/


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

            cameraProvider.bindToLifecycle(this , cameraSelector, preview)

        }, ContextCompat.getMainExecutor(requireContext()))
    }

    companion object {
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }
}


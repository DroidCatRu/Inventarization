package ru.droidcat.inventarization

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.*
import android.media.Image
import android.os.Bundle
import android.util.Log
import android.util.SparseArray
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.camera.core.*
import androidx.camera.core.Camera
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.util.isNotEmpty
import androidx.core.view.setPadding
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.google.android.gms.vision.Frame
import com.google.android.gms.vision.barcode.Barcode
import com.google.android.gms.vision.barcode.BarcodeDetector
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.WriterException
import com.google.zxing.common.BitMatrix
import com.journeyapps.barcodescanner.BarcodeEncoder
import kotlinx.android.synthetic.main.add_item_view.*
import ru.droidcat.inventarization.database.Item
import ru.droidcat.inventarization.databinding.AddItemViewBinding
import ru.droidcat.inventarization.view_model.ItemsViewModel
import java.io.ByteArrayOutputStream
import java.lang.IllegalStateException
import java.nio.ByteBuffer
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.roundToInt

class AddItem: Fragment(), ActivityCompat.OnRequestPermissionsResultCallback {
    private var preview: Preview? = null
    private var imageAnalyzer: ImageAnalysis? = null
    private var camera: Camera? = null
    private lateinit var viewFinder: PreviewView
    private var detector: BarcodeDetector? = null
    private var cameraProvider: ProcessCameraProvider? = null

    private var cameraExecutor: ExecutorService? = null

    private lateinit var binding: AddItemViewBinding
    private lateinit var itemsViewModel: ItemsViewModel

    private var codeBuffer: ArrayList<String> = ArrayList()
    private var curCodeInBuf = 0

    private var item = Item("", "", 0, "")

    private var resumeAfterPermissions = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = AddItemViewBinding.inflate(inflater, container, false)
        itemsViewModel = ViewModelProvider(this).get(ItemsViewModel::class.java)

        viewFinder = binding.root.findViewById(R.id.view_finder)

        binding.root.findViewById<ConstraintLayout>(R.id.overlay).setOnClickListener { startScan() }
        binding.root.findViewById<ImageView>(R.id.code_viewer).setOnClickListener { startScan() }
        viewFinder.setOnClickListener {
            stopCamera()
            overlay.visibility = View.VISIBLE
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        item_count_textinputlayout.editText?.setText("0")
        item_id_textinputedittext.doAfterTextChanged {
            item_id_textinputlayout.error = null
            item.item_id = item_id_textinputlayout.editText?.text.toString()
        }
        item_name_textinputedittext.doAfterTextChanged {
            item_name_textinputlayout.error = null
            item.item_name = item_name_textinputlayout.editText?.text.toString()
        }
        item_count_textinputedittext.doAfterTextChanged {
            item_count_textinputlayout.error = null
            if(item_count_textinputlayout.editText?.text.toString().isNotEmpty()) {
                item.item_count = item_count_textinputlayout.editText?.text.toString().toLong()
            }
            else {
                item.item_count = 0
            }
        }
        add_item_button.setOnClickListener {
            var allData = true
            if(item.item_name.isEmpty()) {
                allData = false
                item_name_textinputlayout.error = "Item name is empty"
            }
            if(item.item_id.isEmpty()) {
                allData = false
                item_id_textinputlayout.error = "Item id is empty"
            }
            if(item_count_textinputlayout.editText?.text.toString().isEmpty()) {
                allData = false
                item_count_textinputlayout.error = "Item quantity is empty"
            }
            if(allData) {
                itemsViewModel.insert(item)
                findNavController().popBackStack()
            }
        }
    }

    private fun startScan() {
        detector =
            BarcodeDetector.Builder(context)
                .setBarcodeFormats(
                    Barcode.AZTEC or
                            Barcode.CODABAR or
                            Barcode.CODE_39 or
                            Barcode.CODE_93 or
                            Barcode.CODE_128 or
                            Barcode.QR_CODE or
                            Barcode.DATA_MATRIX or
                            Barcode.EAN_8 or
                            Barcode.EAN_13 or
                            Barcode.ITF or
                            Barcode.PDF417 or
                            Barcode.UPC_A or
                            Barcode.UPC_E
                )
                .build()

        // Request camera permissions
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            requestPermissions(REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }

        cameraExecutor = Executors.newSingleThreadExecutor()

        overlay.visibility = View.GONE
        code_viewer.visibility = View.GONE
    }

    override fun onResume() {
        super.onResume()
        if(!resumeAfterPermissions) {
            overlay.visibility = View.VISIBLE
            resumeAfterPermissions = false
        }
    }

    override fun onStop() {
        super.onStop()
        cameraProvider?.unbindAll()
        stopCamera()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        cameraProvider?.unbindAll()
        stopCamera()
    }

    private fun stopCamera() {
        // Shut down our background executor
        cameraExecutor?.shutdown()
        viewFinder.post {
            viewFinder.visibility = View.GONE
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults:
        IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                resumeAfterPermissions = true
                startScan()
            }
            else {
                Toast.makeText(context,
                    "Permissions not granted by the user.",
                    Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun startCamera() {

        viewFinder.post {
            viewFinder.visibility = View.VISIBLE
        }
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())

        cameraProviderFuture.addListener(Runnable {
            cameraProvider = cameraProviderFuture.get()
            // Used to bind the lifecycle of cameras to the lifecycle owner
            bindCameraUseCases()

        }, ContextCompat.getMainExecutor(requireContext()))
        startPostponedEnterTransition()
    }

    private fun bindCameraUseCases() {
        val cameraProvider = cameraProvider
            ?: throw IllegalStateException("Camera initialization failed.")

        // Preview
        preview = Preview.Builder()
            .build()

        val width = card.width
        val height = card.height

        imageAnalyzer = ImageAnalysis.Builder()
            .build()
            .also {
                it.setAnalyzer(cameraExecutor!!,
                    BarcodeAnalyzer (barcodeListener, detector!!, width, height))
            }

        // Select back camera
        val cameraSelector = CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK).build()

        // Unbind use cases before rebinding
        cameraProvider.unbindAll()
        try {
            // Bind use cases to camera
            camera = cameraProvider.bindToLifecycle(
                this, cameraSelector, preview, imageAnalyzer)
            preview?.setSurfaceProvider(viewFinder.createSurfaceProvider())
        } catch(exc: Exception) {
            Log.e(TAG, "Use case binding failed", exc)
        }
    }

    private val barcodeListener = BarcodeListener { code, format ->

        var codeVerified = true

        if(codeBuffer.size < 3) {
            codeBuffer.add(code)
            codeVerified = false
        }
        else {
            codeBuffer[curCodeInBuf % 3] = code
            for(codeInBuf in codeBuffer) {
                if(code != codeInBuf) {
                    codeVerified = false
                    break
                }
            }
        }

        curCodeInBuf++

        if(code.isNotEmpty() && codeVerified) {
            stopCamera()
            val coderFormat =
            when(format) {
                Barcode.AZTEC -> BarcodeFormat.AZTEC
                Barcode.CODABAR -> BarcodeFormat.CODABAR
                Barcode.CODE_39 -> BarcodeFormat.CODE_39
                Barcode.CODE_93 -> BarcodeFormat.CODE_93
                Barcode.CODE_128 -> BarcodeFormat.CODE_128
                Barcode.QR_CODE -> BarcodeFormat.QR_CODE
                Barcode.DATA_MATRIX -> BarcodeFormat.DATA_MATRIX
                Barcode.EAN_8 -> BarcodeFormat.EAN_8
                Barcode.EAN_13 -> BarcodeFormat.EAN_13
                Barcode.ITF -> BarcodeFormat.ITF
                Barcode.PDF417 -> BarcodeFormat.PDF_417
                Barcode.UPC_A -> BarcodeFormat.UPC_A
                Barcode.UPC_E -> BarcodeFormat.UPC_E
                else -> BarcodeFormat.CODE_128
            }

            val multiFormatWriter = MultiFormatWriter()
            try {
                val bitMatrix: BitMatrix = multiFormatWriter.encode(
                    code,
                    coderFormat,
                    600,
                    300
                )
                val barcodeEncoder = BarcodeEncoder()
                val bitmap: Bitmap = barcodeEncoder.createBitmap(bitMatrix)
                code_viewer.post {
                    code_viewer.visibility = View.VISIBLE
                    if(coderFormat != BarcodeFormat.QR_CODE) {
                        val density = resources.displayMetrics.density
                        code_viewer.setPadding((32 * density).roundToInt())
                    }
                    code_viewer.setImageBitmap(bitmap)
                }
            }
            catch(e: WriterException) {
                e.printStackTrace()
            }
            item.item_id = code

            view?.post {
                binding.item = item
                binding.executePendingBindings()
            }
        }
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            requireContext(), it) == PackageManager.PERMISSION_GRANTED
    }

    companion object {
        private const val TAG = "Camera view"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }

}

class BarcodeListener(val listener: (code: String, format: Int) -> Unit) {
    fun onCode(code: String, format: Int) = listener(code, format)
}

private class BarcodeAnalyzer(
    private val barlistener: BarcodeListener,
    private val detector: BarcodeDetector,
    private val width: Int,
    private val height: Int) : ImageAnalysis.Analyzer {


    @SuppressLint("UnsafeExperimentalUsageError")
    override fun analyze(image: ImageProxy) {

        if(!detector.isOperational) {
            Log.d("Detector", "Detector is not ready")
        }
        else {

            val data = nv21toJPEG(yuv420toNV21(image.image!!)!!, image.width, image.height)
            val bmp = BitmapFactory.decodeByteArray(data, 0, data!!.size)

            val matrix = Matrix()
            matrix.postRotate(90F)
            val bitmapPicture = Bitmap.createBitmap(bmp, 0, 0, bmp.width,
            bmp.height, matrix, true)

            if(bitmapPicture != null) {
                val aspectRatio = height.toFloat() / width.toFloat()
                val yOffset =
                    ((bitmapPicture.height.toFloat() - (bitmapPicture.width.toFloat() * aspectRatio)) / 2).roundToInt()
                val yHeight = (bitmapPicture.width.toFloat() * aspectRatio).roundToInt()
                val croppedBitmap =
                    Bitmap.createBitmap(bitmapPicture, 0, yOffset, bitmapPicture.width, yHeight)
                val frame = Frame.Builder().setBitmap(croppedBitmap).build()
                val barcodes: SparseArray<Barcode> = detector.detect(frame)

                if (barcodes.isNotEmpty()) {
                    val format = barcodes.valueAt(0).format
                    val code = barcodes.valueAt(0).displayValue
                    barlistener.onCode(code, format)
                }
            }
        }

        image.close()
    }

    private fun nv21toJPEG(
        nv21: ByteArray,
        width: Int,
        height: Int
    ): ByteArray? {
        val out = ByteArrayOutputStream()
        val yuv = YuvImage(nv21, ImageFormat.NV21, width, height, null)
        yuv.compressToJpeg(Rect(0, 0, width, height), 100, out)
        return out.toByteArray()
    }

    private fun yuv420toNV21(image: Image): ByteArray? {
        val crop: Rect = image.cropRect
        val format: Int = image.format
        val width = crop.width()
        val height = crop.height()
        val planes: Array<Image.Plane> = image.planes
        val data =
            ByteArray(width * height * ImageFormat.getBitsPerPixel(format) / 8)
        val rowData = ByteArray(planes[0].rowStride)
        var channelOffset = 0
        var outputStride = 1
        for (i in planes.indices) {
            when (i) {
                0 -> {
                    channelOffset = 0
                    outputStride = 1
                }
                1 -> {
                    channelOffset = width * height + 1
                    outputStride = 2
                }
                2 -> {
                    channelOffset = width * height
                    outputStride = 2
                }
            }
            val buffer: ByteBuffer = planes[i].buffer
            val rowStride: Int = planes[i].rowStride
            val pixelStride: Int = planes[i].pixelStride
            val shift = if (i == 0) 0 else 1
            val w = width shr shift
            val h = height shr shift
            buffer.position(rowStride * (crop.top shr shift) + pixelStride * (crop.left shr shift))
            for (row in 0 until h) {
                var length: Int
                if (pixelStride == 1 && outputStride == 1) {
                    length = w
                    buffer[data, channelOffset, length]
                    channelOffset += length
                } else {
                    length = (w - 1) * pixelStride + 1
                    buffer[rowData, 0, length]
                    for (col in 0 until w) {
                        data[channelOffset] = rowData[col * pixelStride]
                        channelOffset += outputStride
                    }
                }
                if (row < h - 1) {
                    buffer.position(buffer.position() + rowStride - length)
                }
            }
        }
        return data
    }
}
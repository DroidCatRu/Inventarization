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
import android.widget.Toast
import androidx.camera.core.*
import androidx.camera.core.Camera
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.util.isNotEmpty
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.google.android.gms.vision.Frame
import com.google.android.gms.vision.barcode.Barcode
import com.google.android.gms.vision.barcode.BarcodeDetector
import kotlinx.android.synthetic.main.add_item_view.*
import ru.droidcat.inventarization.database.Item
import ru.droidcat.inventarization.databinding.AddItemViewBinding
import ru.droidcat.inventarization.databinding.ItemViewBinding
import ru.droidcat.inventarization.view_model.ItemsViewModel
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.roundToInt


typealias BarcodeListener = (code: String) -> Unit

class AddItem: Fragment(), ActivityCompat.OnRequestPermissionsResultCallback {
    private var preview: Preview? = null
    private var imageAnalyzer: ImageAnalysis? = null
    private var camera: Camera? = null
    private lateinit var viewFinder: PreviewView
    private var detector: BarcodeDetector? = null

    private lateinit var cameraExecutor: ExecutorService

    private lateinit var binding: AddItemViewBinding
    private lateinit var itemsViewModel: ItemsViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = AddItemViewBinding.inflate(inflater, container, false)
        itemsViewModel = ViewModelProvider(this).get(ItemsViewModel::class.java)
        viewFinder = binding.root.findViewById(R.id.view_finder)

        detector =
            BarcodeDetector.Builder(context)
                .setBarcodeFormats(Barcode.ALL_FORMATS)
                .build()

        // Request camera permissions
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            requestPermissions(REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }

        cameraExecutor = Executors.newSingleThreadExecutor()
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        // Make sure that all permissions are still present, since the
        // user could have removed them while the app was in paused state.
        if(allPermissionsGranted()) {
            startCamera()
        } else {
            requestPermissions(REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }
        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    override fun onStop() {
        super.onStop()
        stopCamera()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        stopCamera()
    }

    private fun stopCamera() {
        // Shut down our background executor
        cameraExecutor.shutdown()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults:
        IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                Toast.makeText(context,
                    "Permissions not granted by the user.",
                    Toast.LENGTH_SHORT).show()
                findNavController().navigate(R.id.itemsList)
            }
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())

        cameraProviderFuture.addListener(Runnable {
            // Used to bind the lifecycle of cameras to the lifecycle owner
            val cameraProvider = cameraProviderFuture.get()

            // Preview
            preview = Preview.Builder()
                .build()

            imageAnalyzer = ImageAnalysis.Builder()
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor, BarcodeAnalyzer ({ code ->
                        Log.d(TAG, "Barcode: $code")
                        if(code.isNotEmpty()) {
                            overlay.visibility = View.INVISIBLE
                            stopCamera()
                            val item = Item(code, code, 1, "")
                            binding.item = item
                            binding.executePendingBindings()
                            itemsViewModel.insert(item)
                        }
                    }, detector!!))
                }

            // Select back camera
            val cameraSelector = CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK).build()

            try {
                // Unbind use cases before rebinding
                cameraProvider!!.unbindAll()

                // Bind use cases to camera
                camera = cameraProvider!!.bindToLifecycle(
                    this, cameraSelector, preview, imageAnalyzer)
                preview?.setSurfaceProvider(viewFinder.createSurfaceProvider(camera?.cameraInfo))
            } catch(exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(requireContext()))
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

private class BarcodeAnalyzer(private val listener: BarcodeListener, private val detector: BarcodeDetector) : ImageAnalysis.Analyzer {

    private fun ByteBuffer.toByteArray(): ByteArray {
        rewind()    // Rewind the buffer to zero
        val data = ByteArray(remaining())
        get(data, 0, data.size)   // Copy the buffer into a byte array
        return data // Return the byte array
    }

    @SuppressLint("UnsafeExperimentalUsageError")
    override fun analyze(image: ImageProxy) {

        if(!detector.isOperational) {
            Log.d("Detector", "Detector is not ready")
        }
        else {

            val data = nv21toJPEG(yuv420toNV21(image.image!!)!!, image.width, image.height, 100)
            val bmp = BitmapFactory.decodeByteArray(data, 0, data!!.size)

            Log.d("Image", "${image.width} * ${image.height}")
            Log.d("Bitmap", "${bmp.width} * ${bmp.height}")

            val matrix = Matrix()
            matrix.postRotate(90F)
            val bitmapPicture = Bitmap.createBitmap(bmp, 0, 0, bmp.width,
            bmp.height, matrix, true)

            if(bitmapPicture != null) {
                val yOffset =
                    ((bitmapPicture.height.toFloat() - (bitmapPicture.width.toFloat() / 2)) / 2).roundToInt()
                val yHeight = (bitmapPicture.width.toFloat() / 2).roundToInt()
                val croppedBitmap =
                    Bitmap.createBitmap(bitmapPicture, 0, yOffset, bitmapPicture.width, yHeight)
                val frame = Frame.Builder().setBitmap(croppedBitmap).build()
                val barcodes: SparseArray<Barcode> = detector.detect(frame)

                Log.d("Cropped bitmap", "Y offset: $yOffset Height: $yHeight")
                Log.d("Cropped bitmap", "${croppedBitmap.width} * ${croppedBitmap.height}")
                if (barcodes.isNotEmpty()) {
                    listener(barcodes.valueAt(0).displayValue)
                }
            }
        }

        image.close()
    }

    private fun nv21toJPEG(
        nv21: ByteArray,
        width: Int,
        height: Int,
        quality: Int
    ): ByteArray? {
        val out = ByteArrayOutputStream()
        val yuv = YuvImage(nv21, ImageFormat.NV21, width, height, null)
        yuv.compressToJpeg(Rect(0, 0, width, height), quality, out)
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
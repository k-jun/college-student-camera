package com.example.student_camera.camera

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Size
import android.view.LayoutInflater
import android.view.TextureView
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.findNavController
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.student_camera.R
import com.example.student_camera.database.AppDatabase
import com.example.student_camera.databinding.FragmentCameraBinding
import com.example.student_camera.time_schedule.TimeScheduleViewModel
import com.example.student_camera.time_schedule.TimeScheduleViewModelFactory
import java.io.File
import java.util.concurrent.Executors


private const val REQUEST_CODE_PERMISSIONS = 10
private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA, Manifest.permission.INTERNET)

class CameraFragment : Fragment() {
    private val executor = Executors.newSingleThreadExecutor()
    private lateinit var viewFinder: TextureView
    private lateinit var binding: FragmentCameraBinding
    private lateinit var viewModel: CameraViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_camera, container, false)
        viewFinder = binding.viewFinder

        // Action barを隠す(https://stackoverflow.com/questions/21504088/how-to-hide-action-bar-for-fragment)
        (activity as AppCompatActivity).supportActionBar!!.hide()

        // ViewModelを設定
        val application = requireNotNull(this.activity).application
        val dataSourcePhoto = AppDatabase.getPhotoInstance(application).photoDatabaseDao()
        val dataSourceTimeSchedule =
            AppDatabase.getPhotoInstance(application).timeScheduleDatabaseDao()
        val viewModelFactory = CameraViewModelFactory(dataSourcePhoto, dataSourceTimeSchedule, application)
        viewModel = ViewModelProviders.of(this, viewModelFactory).get(CameraViewModel::class.java)

        val viewModelFactoryTs = TimeScheduleViewModelFactory(dataSourceTimeSchedule, application)
        activity?.run {
            ViewModelProviders.of(this, viewModelFactoryTs).get(TimeScheduleViewModel::class.java)
        } ?: throw Exception("Invalid Activity")

        // Request camera permissions
        if (allPermissionsGranted()) {
            viewFinder.post { startCamera() }
        } else {
            requestPermissions(REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }

        viewModel.lastPhoto.observe(this, Observer { newPhoto ->
            val ctx = context
            if (ctx != null) {
                Glide.with(ctx)
                    .load(Uri.parse(newPhoto.uri))
                    .apply(
                        RequestOptions()
                            .placeholder(R.drawable.loading_animation)
                            .error(R.drawable.ic_broken_image)
                    )
                    .into(binding.lastImage)
            }
        })

        // Every time the provided texture view changes, recompute layout
        viewFinder.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            updateTransform()
        }

        binding.icSetting.setOnClickListener { view: View ->
            CameraX.unbindAll()
            view.findNavController()
                .navigate(CameraFragmentDirections.actionCameraFragmentToSettingFragment())
        }

        binding.icAllPhotos.setOnClickListener { view: View ->
            CameraX.unbindAll()
            view.findNavController()
                .navigate(CameraFragmentDirections.actionCameraFragmentToAllPhotoFragment())
        }

        return binding.root
    }

    private fun startCamera() {
        // TODO いい感じにする
        val ctx = context ?: return

        // Create configuration object for the viewfinder use case
        val previewConfig = PreviewConfig.Builder().apply {
            setTargetResolution(Size(320, 240))
        }.build()
        val preview = Preview(previewConfig)

        preview.setOnPreviewOutputUpdateListener {
            val parent = viewFinder.parent as ViewGroup
            parent.removeView(viewFinder)
            parent.addView(viewFinder, 0)

            viewFinder.surfaceTexture = it.surfaceTexture
            updateTransform()
        }

        val imageCaptureConfig = ImageCaptureConfig.Builder()
            .apply {
                // We don't set a resolution for image capture; instead, we
                // select a capture mode which will infer the appropriate
                // resolution based on aspect ration and requested mode
                setCaptureMode(ImageCapture.CaptureMode.MIN_LATENCY)
            }.build()

        // Build the image capture use case and attach button click listener
        val imageCapture = ImageCapture(imageCaptureConfig)
        binding.captureButton.setOnClickListener {
            val uri = ctx.externalMediaDirs.first()
            val fileName = System.currentTimeMillis().toString() + ".jpg"
            val file = File(uri, fileName)

            imageCapture.takePicture(file, executor,
                object : ImageCapture.OnImageSavedListener {
                    override fun onError(
                        imageCaptureError: ImageCapture.ImageCaptureError,
                        message: String,
                        exc: Throwable?
                    ) {
                        val msg = "Photo capture failed: $message"
                        viewFinder.post {
                            Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show()
                        }

                    }

                    override fun onImageSaved(file: File) {
                        val uri = uri.toURI().toString() + fileName
                        viewModel.insert(uri)
                    }
                })
        }

        CameraX.bindToLifecycle(this, preview, imageCapture)
    }

    private fun updateTransform() {
        // TODO: Implement camera viewfinder transformations
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                viewFinder.post { startCamera() }
            } else {
                Toast.makeText(
                    context,
                    "Permissions not granted by the user.",
                    Toast.LENGTH_SHORT
                ).show()

                // アプリを終了
                getActivity()!!.finishAndRemoveTask()
            }
        }
    }

    /**
     * Check if all permission specified in the manifest have been granted
     */
    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        val ctx = context ?: return false
        // TODO いい感じにする
        ContextCompat.checkSelfPermission(ctx, it) == PackageManager.PERMISSION_GRANTED
    }
}

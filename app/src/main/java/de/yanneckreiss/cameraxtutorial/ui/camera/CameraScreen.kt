package de.yanneckreiss.cameraxtutorial.ui.camera

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.Color
import android.util.Log
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.LinearLayout
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.ExtendedFloatingActionButton
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import de.yanneckreiss.cameraxtutorial.core.utils.rotateBitmap

@Composable
fun CameraScreen(
    viewModel: CameraViewModel = viewModel()
) {

    val cameraState: CameraState by viewModel.state.collectAsStateWithLifecycle()

    CameraContent(
        onPhotoCaptured = viewModel::onPhotoCaptured
    )

    cameraState.capturedImage?.let { capturedImage: Bitmap ->
        CapturedImageBitmapDialog(
            capturedImage = capturedImage,
            onDismissRequest = viewModel::onCapturedPhotoConsumed
        )
    }
}

@Composable
private fun CapturedImageBitmapDialog(
    capturedImage: Bitmap,
    onDismissRequest: () -> Unit
) {

    val capturedImageBitmap: ImageBitmap = remember { capturedImage.asImageBitmap() }

    Dialog(
        onDismissRequest = onDismissRequest
    ) {
        Image(
            bitmap = capturedImageBitmap,
            contentDescription = "Captured photo"
        )
    }
}

@SuppressLint("UnusedMaterialScaffoldPaddingParameter")
@Composable
private fun CameraContent(
    onPhotoCaptured: (Bitmap) -> Unit
) {

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraController = remember { LifecycleCameraController(context) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        floatingActionButton = {
            ExtendedFloatingActionButton(
                text = { Text(text = "Take photo") },
                onClick = {
                    val mainExecutor = ContextCompat.getMainExecutor(context)

                    cameraController.takePicture(mainExecutor, object : ImageCapture.OnImageCapturedCallback() {
                        override fun onCaptureSuccess(image: ImageProxy) {

                            // Note: There is a known issue with the CameraX library, that JPEG images are currently not supported
                            // for Bitmap conversion via `.toBitmap()`. A Fix is on the way: https://android-review.googlesource.com/c/platform/frameworks/support/+/2594225
                            // Probably will released with the next 1.3.0-alpha/beta version of CameraX https://developer.android.com/jetpack/androidx/releases/camera#version_13_2
                            //
                            // tl;dr: The following therefore only will work in the next version of CameraX
                            // In the meantime you can use the ImageCapture.OnImageSavedCallback overload of the takePicture method
                            val correctedBitmap: Bitmap = image
                                .toBitmap()
                                .rotateBitmap(image.imageInfo.rotationDegrees)

                            onPhotoCaptured(correctedBitmap)

                            image.close()
                        }

                        override fun onError(exception: ImageCaptureException) {
                            Log.e("CameraContent", "Error capturing image", exception)
                        }
                    })
                }
            )
        }
    ) { paddingValues: PaddingValues ->
        AndroidView(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            factory = { context ->
                PreviewView(context).apply {
                    layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT)
                    setBackgroundColor(Color.BLACK)
                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                    scaleType = PreviewView.ScaleType.FILL_START
                }.also { previewView ->
                    previewView.controller = cameraController
                    cameraController.bindToLifecycle(lifecycleOwner)
                }
            }
        )
    }
}

@Preview
@Composable
private fun Preview_CameraContent() {
    CameraContent(
        onPhotoCaptured = {}
    )
}

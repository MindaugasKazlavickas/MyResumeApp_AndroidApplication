package com.example.penktas_kamera

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.speech.RecognizerIntent
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavHostController

@Composable
fun StarterPage(navController: NavHostController) {
    val context = LocalContext.current
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            val imageUri = saveImageToStorage(context, bitmap)
            navController.navigate("cameraPage?imageUri=${Uri.encode(imageUri.toString())}")
        } else {
            Toast.makeText(context, "Failed to capture photo.", Toast.LENGTH_SHORT).show()
        }
    }

    requestCameraPermission(context)


    val voiceCommandLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val spokenText = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
        if (spokenText?.get(0)?.lowercase() in listOf("fotografuoti", "kamera", "photograph", "camera")) {
            cameraLauncher.launch(null)
        } else {
            Toast.makeText(context, "Command not recognized.", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(Unit) {
        voiceCommandLauncher.launch(
            Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Say 'fotografuoti' or 'kamera'")
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "lt")
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "lt")
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Button(
            onClick = { openCamera(context, cameraLauncher) },
            modifier = Modifier.size(150.dp)
        ) {
            Text("CLICK ME")
        }
    }
}

@Composable
fun requestCameraPermission(context: Context) {
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Toast.makeText(context, "Camera permission granted.", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Camera permission denied.", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }
}

fun openCamera(context: Context, cameraLauncher: ActivityResultLauncher<Void?>) {
    cameraLauncher.launch(null)
}

fun saveImageToStorage(context: Context, bitmap: Bitmap): String {
    val contentResolver = context.contentResolver
    val imageCollection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
    val values = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, "photo_${System.currentTimeMillis()}.jpg")
        put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
    }

    Log.d("ImageStorage", "Inserting image with values: $values")
    val uri = contentResolver.insert(imageCollection, values)
    if (uri == null) {
        Log.e("ImageStorage", "Failed to insert image into MediaStore.")
        return ""
    }
    Log.d("ImageStorage", "Image inserted successfully, URI: $uri")

    try {
        contentResolver.openOutputStream(uri)?.use { outputStream ->
            Log.d("ImageStorage", "Opened output stream for URI: $uri")
            val success = bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
            if (success) {
                Log.d("ImageStorage", "Image successfully compressed and saved.")
            } else {
                Log.e("ImageStorage", "Failed to compress and save the image.")
            }
        } ?: run {
            Log.e("ImageStorage", "Failed to open output stream for URI: $uri")
            return ""
        }
    } catch (e: Exception) {
        Log.e("ImageStorage", "Error while writing the image: ${e.message}", e)
        return ""
    }

    Log.d("ImageStorage", "Image saved successfully at URI: $uri")
    return uri.toString()
}

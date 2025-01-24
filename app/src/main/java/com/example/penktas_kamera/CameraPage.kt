package com.example.penktas_kamera

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.navigation.NavHostController
import java.io.File
import java.io.FileOutputStream

@Composable
fun CameraPage(navController: NavHostController, imageUri: String?) {
    val context = LocalContext.current

    if (imageUri.isNullOrEmpty()) {
        Log.e("CameraPage", "Received empty URI. Unable to load the image.")
        Text("Error: Image not found.")
        return
    }

    val uri = Uri.parse(imageUri)
    Log.d("CameraPage", "Attempting to load image from URI: $uri")

    val originalBitmap = remember(uri) {
        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BitmapFactory.decodeStream(inputStream)
            }
        } catch (e: Exception) {
            Log.e("CameraPage", "Error loading image from URI: $uri", e)
            null
        }
    }

    var processedBitmap by remember { mutableStateOf(originalBitmap) }

    LaunchedEffect(originalBitmap) {
        originalBitmap?.let { bitmap ->
            addRedNoseOverlay(bitmap, context) { updatedBitmap ->
                processedBitmap = updatedBitmap
            }
        }
    }

    val bitmapWithText = remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(processedBitmap) {
        processedBitmap?.let { bitmap ->
            addTextAndHatToBitmap(
                bitmap,
                "Mindaugas Kazlavickas MKDf 21/2",
                context
            ) { processedBitmapWithHat ->
                bitmapWithText.value = processedBitmapWithHat
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        bitmapWithText.value?.let { bitmap ->
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Processed Image",
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(bitmap.width.toFloat() / bitmap.height.toFloat())
            )
        } ?: Text("Processing image...")

        Spacer(modifier = Modifier.height(16.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Button(onClick = { navController.popBackStack() }) {
                Text("Go back")
            }

            Button(onClick = {
                bitmapWithText.value?.let { bitmap ->
                    val updatedUri = saveNewImageToStorage(context, bitmap)
                    sendEmail(
                        context = context,
                        recipientEmail = "mindaugaskazlavickas@gmail.com",
                        subject = "PenktasNamuDarbas",
                        body = "Prisegta mano nuotrauka su visais efektais.",
                        imageUri = updatedUri
                    )
                }
            }) {
                Text("Send")
            }
        }
    }
}

private fun sendEmail(
    context: Context,
    recipientEmail: String,
    subject: String,
    body: String,
    imageUri: Uri
) {
    if (recipientEmail.isBlank()) {
        Toast.makeText(context, "Recipient email is blank.", Toast.LENGTH_SHORT).show()
        return
    }

    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "image/*"
        putExtra(Intent.EXTRA_EMAIL, arrayOf(recipientEmail))
        putExtra(Intent.EXTRA_SUBJECT, subject)
        putExtra(Intent.EXTRA_TEXT, body)
        putExtra(Intent.EXTRA_STREAM, imageUri)
    }

    val packageManager = context.packageManager
    val activities = packageManager.queryIntentActivities(intent, 0)
    if (activities.isNotEmpty()) {
        context.startActivity(Intent.createChooser(intent, "Send Email"))
    } else {
        Toast.makeText(context, "No email apps found. Please install an email app.", Toast.LENGTH_LONG).show()
    }
}

private fun saveNewImageToStorage(context: Context, bitmap: Bitmap): Uri {
    val file = File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "image_${System.currentTimeMillis()}.jpg")
    FileOutputStream(file).use { outputStream ->
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
    }
    return FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
}
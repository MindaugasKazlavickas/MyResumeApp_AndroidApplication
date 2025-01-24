package com.example.penktas_kamera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.util.Log
import android.widget.Toast
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.face.FaceLandmark
import kotlin.math.atan2

fun addRedNoseOverlay(bitmap: Bitmap, context: Context, onComplete: (Bitmap) -> Unit) {
    val image = InputImage.fromBitmap(bitmap, 0)

    val options = FaceDetectorOptions.Builder()
        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
        .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
        .build()

    val detector = FaceDetection.getClient(options)

    detector.process(image)
        .addOnSuccessListener { faces ->
            val resultBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
            val canvas = Canvas(resultBitmap)
            val paint = Paint().apply {
                color = Color.RED
                style = Paint.Style.FILL
            }

            for (face in faces) {
                val nose = face.getLandmark(FaceLandmark.NOSE_BASE)
                nose?.let {
                    val noseX = it.position.x
                    val noseY = it.position.y
                    canvas.drawCircle(noseX, noseY, 3f, paint)
                }
            }
            onComplete(resultBitmap)
        }
        .addOnFailureListener {
            Toast.makeText(context, "Face detection failed.", Toast.LENGTH_SHORT).show()
        }
}

fun addTextAndHatToBitmap(bitmap: Bitmap, text: String, context: Context, onComplete: (Bitmap) -> Unit) {
    val mutableBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
    val canvas = Canvas(mutableBitmap)

    val hatBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.hat)

    val paint = Paint().apply {
        color = android.graphics.Color.WHITE
        isAntiAlias = true
    }

    val maxTextWidth = canvas.width * 0.98f
    var textSize = 60f
    paint.textSize = textSize

    while (paint.measureText(text) > maxTextWidth) {
        textSize -= 2f
        paint.textSize = textSize
    }

    val xPos = (canvas.width / 2) - (paint.measureText(text) / 2)
    val yPos = (canvas.height * 0.92f).toFloat()

    canvas.drawText(text, xPos, yPos, paint)

    val image = InputImage.fromBitmap(bitmap, 0)

    val options = FaceDetectorOptions.Builder()
        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
        .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
        .build()

    val detector = FaceDetection.getClient(options)

    detector.process(image)
        .addOnSuccessListener { faces ->
            for (face in faces) {
                val leftEye = face.getLandmark(FaceLandmark.LEFT_EYE)
                val rightEye = face.getLandmark(FaceLandmark.RIGHT_EYE)

                if (leftEye != null && rightEye != null) {
                    val eyeMidPointX = (leftEye.position.x + rightEye.position.x) / 2
                    val eyeMidPointY = (leftEye.position.y + rightEye.position.y) / 2
                    val hatWidth = hatBitmap.width
                    val hatHeight = hatBitmap.height
                    val hatScaleFactor = canvas.width.toFloat() / hatBitmap.width.toFloat() * 0.7
                    val scaledHatBitmap = Bitmap.createScaledBitmap(hatBitmap, (hatWidth * hatScaleFactor).toInt(), (hatHeight * hatScaleFactor).toInt(), false)

                    val deltaX = rightEye.position.x - leftEye.position.x
                    val deltaY = rightEye.position.y - leftEye.position.y
                    val angleRadians = atan2(deltaY, deltaX)
                    val angleDegrees = Math.toDegrees(angleRadians.toDouble())

                    Log.d("HatRotation", "Calculated angle (degrees): $angleDegrees")

                    val rotationMatrix = Matrix()
                    rotationMatrix.postRotate(angleDegrees.toFloat())

                    val rotatedHatBitmap = Bitmap.createBitmap(scaledHatBitmap, 0, 0, scaledHatBitmap.width, scaledHatBitmap.height, rotationMatrix, true)

                    val rotatedHatWidth = rotatedHatBitmap.width
                    val rotatedHatHeight = rotatedHatBitmap.height

                    var hatXPos = 0.0
                    var hatYPos = 0.0
                    if (angleDegrees < 25 && angleDegrees > -25){
                        hatYPos = (eyeMidPointY - (rotatedHatHeight * 1.5f)).toDouble()
                        hatXPos = (eyeMidPointX - (rotatedHatWidth /2)).toDouble()
                    }
                    else{
                        if(angleDegrees > 25){
                            hatYPos = (eyeMidPointY - (rotatedHatHeight * 0.75)).toDouble()
                            hatXPos = (eyeMidPointX - (rotatedHatWidth *0.17)).toDouble()
                        }
                        else{
                            hatYPos = (eyeMidPointY - (rotatedHatHeight * 0.85)).toDouble()
                            hatXPos = (eyeMidPointX - (rotatedHatWidth /1.2)).toDouble()
                        }
                    }
                    Log.d("HatRotation", "$angleDegrees  $hatXPos $eyeMidPointX $rotatedHatWidth")
                    canvas.drawBitmap(rotatedHatBitmap, hatXPos.toFloat(), hatYPos.toFloat(), null)
                }
            }
            onComplete(mutableBitmap)
        }
        .addOnFailureListener {
            Toast.makeText(context, "Face detection failed.", Toast.LENGTH_SHORT).show()
        }
}

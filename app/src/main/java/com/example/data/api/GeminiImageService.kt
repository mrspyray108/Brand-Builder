package com.example.data.api

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.util.Base64
import android.util.Log
import com.example.BuildConfig
import com.example.data.model.AdvertisingMedium
import com.example.data.model.ProductDraft
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

class GeminiImageService(private val context: Context) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    // Mandatory Model as specified: "Nano-Banana" model -> 'gemini-2.5-flash-image'
    val modelName = "gemini-2.5-flash-image"

    /**
     * Resolves API Key from BuildConfig or custom user override
     */
    fun getEffectiveApiKey(customApiKey: String?): String {
        if (!customApiKey.isNullOrBlank()) {
            return customApiKey.trim()
        }
        val buildKey = BuildConfig.GEMINI_API_KEY
        if (buildKey.isNotBlank() && buildKey != "MY_GEMINI_API_KEY") {
            return buildKey
        }
        return ""
    }

    /**
     * Generates a Master Hero Shot for the product.
     * This shot becomes the consistency reference image for all subsequent medium shots.
     */
    suspend fun generateHeroShot(
        product: ProductDraft,
        apiKey: String
    ): Result<GeneratedImageResult> = withContext(Dispatchers.IO) {
        val heroPrompt = buildHeroPrompt(product)
        generateImageFromPrompt(
            prompt = heroPrompt,
            aspectRatio = "1:1",
            resolution = product.resolution,
            referenceImageBase64 = null,
            apiKey = apiKey,
            filePrefix = "hero_${sanitize(product.name)}"
        )
    }

    /**
     * Generates an advertising medium shot (Billboard, Newspaper, Social Post, etc.)
     * strictly maintaining product consistency with the hero shot and enforcing NO PEOPLE.
     */
    suspend fun generateMediumShot(
        medium: AdvertisingMedium,
        product: ProductDraft,
        heroReferenceBase64: String?,
        apiKey: String
    ): Result<GeneratedImageResult> = withContext(Dispatchers.IO) {
        val prompt = buildMediumPrompt(medium, product, !heroReferenceBase64.isNullOrBlank())
        generateImageFromPrompt(
            prompt = prompt,
            aspectRatio = medium.aspectRatio,
            resolution = product.resolution,
            referenceImageBase64 = heroReferenceBase64,
            apiKey = apiKey,
            filePrefix = "${medium.key.lowercase()}_${sanitize(product.name)}"
        )
    }

    private suspend fun generateImageFromPrompt(
        prompt: String,
        aspectRatio: String,
        resolution: String,
        referenceImageBase64: String?,
        apiKey: String,
        filePrefix: String
    ): Result<GeneratedImageResult> = withContext(Dispatchers.IO) {
        val effectiveKey = getEffectiveApiKey(apiKey)

        if (effectiveKey.isBlank()) {
            // If no API key configured, generate a high-fidelity stylized placeholder image
            // so the app remains fully functional and testable without crashing.
            Log.w("GeminiImageService", "No active Gemini API key found; rendering stylized preview.")
            val mockFile = generateStylizedMockImage(filePrefix, prompt, aspectRatio)
            return@withContext Result.success(
                GeneratedImageResult(
                    filePath = mockFile.absolutePath,
                    base64 = fileToBase64(mockFile),
                    promptUsed = prompt,
                    isSimulation = true
                )
            )
        }

        try {
            val url = "https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent?key=$effectiveKey"

            // Construct payload with Nano-Banana / gemini-2.5-flash-image imageConfig
            val payload = JSONObject()

            // 1. Contents
            val contentsArray = JSONArray()
            val contentObj = JSONObject()
            val partsArray = JSONArray()

            // Prompt part
            val textPart = JSONObject()
            textPart.put("text", prompt)
            partsArray.put(textPart)

            // Reference Image part for strict visual consistency
            if (!referenceImageBase64.isNullOrBlank()) {
                val imagePart = JSONObject()
                val inlineData = JSONObject()
                inlineData.put("mimeType", "image/jpeg")
                inlineData.put("data", referenceImageBase64)
                imagePart.put("inlineData", inlineData)
                partsArray.put(imagePart)
            }

            contentObj.put("parts", partsArray)
            contentsArray.put(contentObj)
            payload.put("contents", contentsArray)

            // 2. Generation Config with high-resolution settings
            val genConfig = JSONObject()
            genConfig.put("responseModalities", JSONArray().put("IMAGE"))

            val imageConfig = JSONObject()
            imageConfig.put("aspectRatio", normalizeAspectRatio(aspectRatio))
            imageConfig.put("imageSize", if (resolution.equals("1K", true)) "1K" else "2K")
            genConfig.put("imageConfig", imageConfig)

            payload.put("generationConfig", genConfig)

            val body = payload.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            val request = Request.Builder()
                .url(url)
                .post(body)
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                val errorMsg = try {
                    val errJson = JSONObject(responseBody)
                    errJson.optJSONObject("error")?.optString("message") ?: "HTTP ${response.code}: ${response.message}"
                } catch (_: Exception) {
                    "HTTP ${response.code}: ${response.message}"
                }
                Log.e("GeminiImageService", "API Error: $errorMsg")
                return@withContext Result.failure(Exception(errorMsg))
            }

            val responseJson = JSONObject(responseBody)
            val candidates = responseJson.optJSONArray("candidates")
            if (candidates == null || candidates.length() == 0) {
                return@withContext Result.failure(Exception("No image candidates returned by Gemini model."))
            }

            val candidate = candidates.getJSONObject(0)
            val content = candidate.optJSONObject("content")
            val parts = content?.optJSONArray("parts")

            var imageBase64Data: String? = null
            if (parts != null) {
                for (i in 0 until parts.length()) {
                    val part = parts.getJSONObject(i)
                    val inlineData = part.optJSONObject("inlineData")
                    if (inlineData != null) {
                        imageBase64Data = inlineData.optString("data")
                        if (!imageBase64Data.isNullOrBlank()) {
                            break
                        }
                    }
                }
            }

            if (imageBase64Data.isNullOrBlank()) {
                // If candidate only contained text rejection or safety notice
                val textOutput = parts?.optJSONObject(0)?.optString("text") ?: "No image generated"
                return@withContext Result.failure(Exception("Model returned message instead of image: $textOutput"))
            }

            // Save Base64 to disk
            val imageBytes = Base64.decode(imageBase64Data, Base64.DEFAULT)
            val savedFile = saveImageBytesToDisk(imageBytes, filePrefix)

            Result.success(
                GeneratedImageResult(
                    filePath = savedFile.absolutePath,
                    base64 = imageBase64Data,
                    promptUsed = prompt,
                    isSimulation = false
                )
            )
        } catch (e: Exception) {
            Log.e("GeminiImageService", "Generation failed", e)
            Result.failure(e)
        }
    }

    private fun normalizeAspectRatio(aspectRatio: String): String {
        return when (aspectRatio) {
            "16:9", "9:16", "4:3", "3:4", "1:1", "2:3", "3:2" -> aspectRatio
            else -> "1:1"
        }
    }

    private fun buildHeroPrompt(product: ProductDraft): String {
        return buildString {
            append("Ultra high-resolution commercial studio master packshot photography of ")
            append(product.name).append(", a ").append(product.category).append(". ")
            append(product.buildConsistencyAnchor()).append(". ")
            append("The product is centered as a magnificent hero item on a minimalist sculptured architectural pedestal. ")
            append("Clean studio three-point commercial lighting, crisp metallic highlights, subtle natural reflections, 8k packshot sharpness, immaculate label legibility. ")
            append("CRITICAL RESTRICTION: ABSOLUTELY NO PEOPLE, NO HUMANS, NO HANDS, NO FACES, NO BODY PARTS ANYWHERE IN THE FRAME. ")
            append("Pure standalone product packshot showcasing the product design with absolute clarity.")
        }
    }

    private fun buildMediumPrompt(
        medium: AdvertisingMedium,
        product: ProductDraft,
        hasReferenceImage: Boolean
    ): String {
        return buildString {
            if (hasReferenceImage) {
                append("CRITICAL CONSISTENCY DIRECTIVE: The attached reference image contains the EXACT product design, silhouette, materials, colors, and branding for '")
                append(product.name).append("'. You MUST reproduce this EXACT same product packaging with 100% fidelity in this new advertising setting. Do NOT alter the product's colors, materials, or logo. ")
            }
            append(medium.formatPrompt(
                productName = product.name,
                category = product.category,
                anchor = product.buildConsistencyAnchor()
            ))
            append(" ")
            append("STRICT NEGATIVE CONSTRAINT: NO PEOPLE, NO HUMANS, NO HANDS, NO FACES, NO HUMAN SILHOUETTES, NO PASSING PEDESTRIANS, NO DRIVERS. The composition must be completely devoid of any humans. Pure commercial advertising scene.")
        }
    }

    private fun saveImageBytesToDisk(bytes: ByteArray, prefix: String): File {
        val dir = File(context.filesDir, "brand_shots")
        if (!dir.exists()) dir.mkdirs()
        val file = File(dir, "${prefix}_${System.currentTimeMillis()}.jpg")
        FileOutputStream(file).use { it.write(bytes) }
        return file
    }

    private fun fileToBase64(file: File): String {
        val bytes = file.readBytes()
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    fun convertFileToBase64(filePath: String): String? {
        val file = File(filePath)
        if (!file.exists()) return null
        return Base64.encodeToString(file.readBytes(), Base64.NO_WRAP)
    }

    /**
     * Fallback high-res stylized mock graphic generated on Android Canvas if API key is unconfigured.
     */
    private fun generateStylizedMockImage(prefix: String, prompt: String, aspectRatio: String): File {
        val (width, height) = when (aspectRatio) {
            "16:9" -> 1600 to 900
            "9:16" -> 900 to 1600
            "3:4" -> 900 to 1200
            "4:3" -> 1200 to 900
            else -> 1200 to 1200
        }

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Sleek studio background
        val bgPaint = Paint().apply {
            shader = LinearGradient(
                0f, 0f, width.toFloat(), height.toFloat(),
                Color.parseColor("#0F172A"),
                Color.parseColor("#1E1B4B"),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        // Accent ambient glow
        val glowPaint = Paint().apply {
            shader = LinearGradient(
                width * 0.5f, height * 0.2f, width * 0.5f, height * 0.8f,
                Color.parseColor("#38BDF8"),
                Color.TRANSPARENT,
                Shader.TileMode.CLAMP
            )
            alpha = 70
        }
        canvas.drawCircle(width * 0.5f, height * 0.45f, width * 0.35f, glowPaint)

        // Pedestal
        val pedestalPaint = Paint().apply {
            color = Color.parseColor("#1E293B")
            isAntiAlias = true
        }
        val pedRect = RectF(width * 0.25f, height * 0.65f, width * 0.75f, height * 0.75f)
        canvas.drawRoundRect(pedRect, 16f, 16f, pedestalPaint)

        // Product silhouette cylinder / flacon
        val prodPaint = Paint().apply {
            shader = LinearGradient(
                width * 0.4f, height * 0.3f, width * 0.6f, height * 0.65f,
                Color.parseColor("#F59E0B"),
                Color.parseColor("#B45309"),
                Shader.TileMode.CLAMP
            )
            isAntiAlias = true
        }
        val prodRect = RectF(width * 0.38f, height * 0.32f, width * 0.62f, height * 0.65f)
        canvas.drawRoundRect(prodRect, 32f, 32f, prodPaint)

        // Dropper / Cap
        val capPaint = Paint().apply {
            color = Color.parseColor("#FDE68A")
            isAntiAlias = true
        }
        val capRect = RectF(width * 0.44f, height * 0.22f, width * 0.56f, height * 0.32f)
        canvas.drawRoundRect(capRect, 12f, 12f, capPaint)

        // Title text
        val textPaint = Paint().apply {
            color = Color.WHITE
            textSize = (width * 0.038f).coerceAtLeast(36f)
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        canvas.drawText("BRAND BUILDER STUDIO", width * 0.5f, height * 0.84f, textPaint)

        val subTextPaint = Paint().apply {
            color = Color.parseColor("#94A3B8")
            textSize = (width * 0.024f).coerceAtLeast(24f)
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        canvas.drawText("Model: gemini-2.5-flash-image (Nano-Banana)", width * 0.5f, height * 0.88f, subTextPaint)
        canvas.drawText("High-Res • 100% No People • Visual Anchor Locked", width * 0.5f, height * 0.92f, subTextPaint)

        val dir = File(context.filesDir, "brand_shots")
        if (!dir.exists()) dir.mkdirs()
        val file = File(dir, "${prefix}_mock_${System.currentTimeMillis()}.jpg")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
        }
        return file
    }

    private fun sanitize(str: String): String {
        return str.replace(Regex("[^a-zA-Z0-9]"), "_").take(16)
    }
}

data class GeneratedImageResult(
    val filePath: String,
    val base64: String,
    val promptUsed: String,
    val isSimulation: Boolean = false
)

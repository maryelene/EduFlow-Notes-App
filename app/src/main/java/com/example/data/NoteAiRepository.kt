package com.example.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.util.Base64
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.UUID
import kotlin.math.max
import kotlin.math.roundToInt

private const val TAG = "NoteAiRepository"

data class ExtractedNoteDraft(
    val suggestedTitle: String,
    val photoTagSuggestion: String,
    val extractedText: String,
    val highlights: List<String>,
    val definitions: List<KeyDefinition>
)

class NoteAiRepository {

    private val extractionSchema = Schema.obj(
        properties = mapOf(
            "suggestedTitle" to Schema.string(
                description = "A concise 2 to 6 word student-friendly title for this study note"
            ),
            "photoTagSuggestion" to Schema.string(
                description = "Short tag describing the page or topic, e.g. 'Bio Ch.4 p.12' or 'Lecture Notes'"
            ),
            "extractedText" to Schema.string(
                description = "Complete, clean, editable plain-text transcription of the printed text or handwriting in the photo, preserving paragraphs and bullet points"
            ),
            "highlights" to Schema.array(
                items = Schema.string(),
                description = "Up to 6 key sentences or core takeaway phrases directly from the extracted text"
            ),
            "definitions" to Schema.array(
                items = Schema.obj(
                    properties = mapOf(
                        "term" to Schema.string(description = "Key vocabulary term or concept name"),
                        "definition" to Schema.string(description = "Clear, concise student-friendly definition of the term")
                    )
                ),
                description = "Up to 8 important key terms and definitions found in the note"
            )
        )
    )

    private val definitionsSchema = Schema.obj(
        properties = mapOf(
            "highlights" to Schema.array(
                items = Schema.string(),
                description = "Up to 5 most important takeaway sentences from the text"
            ),
            "definitions" to Schema.array(
                items = Schema.obj(
                    properties = mapOf(
                        "term" to Schema.string(description = "Key concept or vocabulary word"),
                        "definition" to Schema.string(description = "Clear student-friendly definition")
                    )
                ),
                description = "Up to 8 key terms and their definitions found in the note"
            )
        )
    )

    private val extractionModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = extractionSchema
            }
        )
    }

    private val definitionsModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = definitionsSchema
            }
        )
    }

    suspend fun extractFromPhoto(bitmap: Bitmap): Result<ExtractedNoteDraft> {
        return try {
            val scaledBitmap = withContext(Dispatchers.Default) {
                scaleBitmapMaxDimension(bitmap, maxDim = 1400)
            }
            val response = withTimeout(60_000L) {
                extractionModel.generateContent(
                    content {
                        image(scaledBitmap)
                        text(
                            "You are helping a young student organize study notes. " +
                                "Carefully transcribe all readable printed text or clean handwriting from this photo into clean, editable plain text. " +
                                "Fix obvious line-break splits inside sentences while keeping lists and headings neat. " +
                                "Also identify key takeaway phrases to highlight and extract key vocabulary terms with simple, clear definitions."
                        )
                    }
                )
            }

            val jsonString = response.text
                ?: return Result.failure(IllegalStateException("Gemini returned an empty response."))

            val json = JSONObject(jsonString)
            val title = json.optString("suggestedTitle", "Study Note").trim().ifEmpty { "Study Note" }
            val photoTag = json.optString("photoTagSuggestion", "Page Capture").trim()
            val text = json.optString("extractedText", "").trim()

            val highlightsArr = json.optJSONArray("highlights")
            val highlights = buildList {
                if (highlightsArr != null) {
                    for (i in 0 until highlightsArr.length()) {
                        val item = highlightsArr.optString(i).trim()
                        if (item.isNotEmpty()) add(item)
                    }
                }
            }.take(12)

            val defsArr = json.optJSONArray("definitions")
            val colorCycle = listOf(
                SubjectColorTag.ROSE.name,
                SubjectColorTag.BERRY.name,
                SubjectColorTag.BLUSH.name,
                SubjectColorTag.CORAL.name
            )
            val definitions = buildList {
                if (defsArr != null) {
                    for (i in 0 until defsArr.length()) {
                        val obj = defsArr.optJSONObject(i) ?: continue
                        val term = obj.optString("term").trim()
                        val def = obj.optString("definition").trim()
                        if (term.isNotEmpty() && def.isNotEmpty()) {
                            add(
                                KeyDefinition(
                                    id = UUID.randomUUID().toString().take(8),
                                    term = term,
                                    definition = def,
                                    highlightColor = colorCycle[i % colorCycle.size]
                                )
                            )
                        }
                    }
                }
            }.take(15)

            Result.success(
                ExtractedNoteDraft(
                    suggestedTitle = title,
                    photoTagSuggestion = photoTag,
                    extractedText = text,
                    highlights = highlights,
                    definitions = definitions
                )
            )
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Photo extraction timed out", e)
            Result.failure(Exception("Text extraction timed out after 60 seconds. Please try a clearer or smaller photo."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Photo extraction failed", e)
            Result.failure(Exception(e.localizedMessage ?: "Could not extract text from this photo."))
        }
    }

    suspend fun spotDefinitionsFromText(text: String): Result<Pair<List<String>, List<KeyDefinition>>> {
        if (text.isBlank()) {
            return Result.failure(IllegalArgumentException("Add or extract some note text first."))
        }
        return try {
            val response = withTimeout(45_000L) {
                definitionsModel.generateContent(
                    "Analyze these student study notes and extract the most important key terms with short, clear definitions, plus key sentences worth highlighting:\n\n${text.take(12000)}"
                )
            }
            val jsonString = response.text
                ?: return Result.failure(IllegalStateException("Gemini returned an empty response."))

            val json = JSONObject(jsonString)
            val highlightsArr = json.optJSONArray("highlights")
            val highlights = buildList {
                if (highlightsArr != null) {
                    for (i in 0 until highlightsArr.length()) {
                        val item = highlightsArr.optString(i).trim()
                        if (item.isNotEmpty()) add(item)
                    }
                }
            }.take(10)

            val defsArr = json.optJSONArray("definitions")
            val colorCycle = listOf(
                SubjectColorTag.ROSE.name,
                SubjectColorTag.BERRY.name,
                SubjectColorTag.BLUSH.name,
                SubjectColorTag.CORAL.name
            )
            val definitions = buildList {
                if (defsArr != null) {
                    for (i in 0 until defsArr.length()) {
                        val obj = defsArr.optJSONObject(i) ?: continue
                        val term = obj.optString("term").trim()
                        val def = obj.optString("definition").trim()
                        if (term.isNotEmpty() && def.isNotEmpty()) {
                            add(
                                KeyDefinition(
                                    id = UUID.randomUUID().toString().take(8),
                                    term = term,
                                    definition = def,
                                    highlightColor = colorCycle[i % colorCycle.size]
                                )
                            )
                        }
                    }
                }
            }.take(12)

            Result.success(highlights to definitions)
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Definition spotting timed out", e)
            Result.failure(Exception("Definition spotting timed out. Please try again."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Definition spotting failed", e)
            Result.failure(Exception(e.localizedMessage ?: "Failed to spot definitions."))
        }
    }

    suspend fun decodeUriToBitmap(context: Context, uri: Uri): Bitmap? = withContext(Dispatchers.IO) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val source = ImageDecoder.createSource(context.contentResolver, uri)
                ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                    val maxDim = max(info.size.width, info.size.height)
                    if (maxDim > 1600) {
                        val ratio = 1600f / maxDim.toFloat()
                        decoder.setTargetSize(
                            (info.size.width * ratio).roundToInt().coerceAtLeast(1),
                            (info.size.height * ratio).roundToInt().coerceAtLeast(1)
                        )
                    }
                    decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                }
            } else {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    BitmapFactory.decodeStream(stream)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to decode URI to Bitmap", e)
            null
        }
    }

    suspend fun createCompactThumbnailBase64(bitmap: Bitmap): String = withContext(Dispatchers.Default) {
        val thumb = scaleBitmapMaxDimension(bitmap, maxDim = 360)
        var quality = 72
        var encoded = ""
        while (quality >= 35) {
            val out = ByteArrayOutputStream()
            thumb.compress(Bitmap.CompressFormat.JPEG, quality, out)
            val bytes = out.toByteArray()
            val candidate = Base64.encodeToString(bytes, Base64.NO_WRAP)
            if (candidate.length <= 95_000) {
                encoded = candidate
                break
            }
            quality -= 12
        }
        encoded
    }

    private fun scaleBitmapMaxDimension(bitmap: Bitmap, maxDim: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val currentMax = max(width, height)
        if (currentMax <= maxDim || width <= 0 || height <= 0) return bitmap
        val ratio = maxDim.toFloat() / currentMax.toFloat()
        val newW = (width * ratio).roundToInt().coerceAtLeast(1)
        val newH = (height * ratio).roundToInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, newW, newH, true)
    }
}

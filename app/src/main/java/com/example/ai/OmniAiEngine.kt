package com.example.ai

import android.graphics.Bitmap
import android.util.Log
import com.example.model.AspectRatioOption
import com.example.model.AudioTrackOption
import com.example.model.BackgroundPreset
import com.example.model.CameraMovement
import com.example.model.MotionTemplate
import com.example.model.MuseGeneratedClip
import com.example.model.TransitionEffect
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.AspectRatio
import com.google.firebase.ai.type.ImageConfig
import com.google.firebase.ai.type.ImagePart
import com.google.firebase.ai.type.ImageSize
import com.google.firebase.ai.type.ResponseModality
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

data class DirectorCommandResult(
    val reply: String,
    val followUps: List<String>,
    val motionPreset: MotionTemplate? = null,
    val motionSpeed: Float? = null,
    val motionIntensity: Float? = null,
    val trimDeadAir: Boolean = false,
    val addSuspenseTransition: Boolean = false,
    val splitAtPlayhead: Boolean = false,
    val newHeadline: String? = null,
    val newCta: String? = null,
    val newBgPreset: BackgroundPreset? = null,
    val newAspectRatio: AspectRatioOption? = null,
    val newAudioTrack: AudioTrackOption? = null,
    val transitionEffect: TransitionEffect? = null,
    val transitionDurationSec: Float? = null,
    val applyTransitionToAll: Boolean = false
)

data class MuseShotPlan(
    val title: String,
    val sceneDirection: String,
    val visualPrompt: String,
    val suggestedHook: String,
    val durationSec: Float,
    val refinedAudioCue: String
)

class OmniAiEngine {

    companion object {
        private const val TAG = "OmniAiEngine"
        private const val TEXT_MODEL = "gemini-3.8-flash"
        private const val IMAGE_MODEL = "gemini-nano-banana-2.1"
    }

    private val directorSchema = Schema.obj(
        properties = mapOf(
            "reply" to Schema.string(description = "Conversational AI Director response explaining exact timeline/canvas changes made and asking 1 sharp clarifying question."),
            "followUps" to Schema.array(
                items = Schema.string(),
                description = "2 to 3 short tap-ready follow-up commands or answers to the clarifying question."
            ),
            "motionPreset" to Schema.enumeration(
                values = MotionTemplate.entries.map { it.name },
                description = "Optional motion graphics preset to apply."
            ),
            "motionSpeed" to Schema.double(description = "Optional animation velocity from 0.5 to 2.5."),
            "motionIntensity" to Schema.double(description = "Optional motion shake amplitude from 0.2 to 2.0."),
            "trimDeadAir" to Schema.boolean(description = "True to tighten clip durations and cut out dead air / pauses."),
            "addSuspenseTransition" to Schema.boolean(description = "True to insert a suspenseful FX transition at the current playhead."),
            "splitAtPlayhead" to Schema.boolean(description = "True to split the primary timeline clip at the current playhead."),
            "newHeadline" to Schema.string(description = "Optional punchy ad headline / video hook."),
            "newCta" to Schema.string(description = "Optional short CTA pill text."),
            "newBgPreset" to Schema.enumeration(
                values = BackgroundPreset.entries.filter { it != BackgroundPreset.CUSTOM_PROMPT }.map { it.name },
                description = "Optional studio background preset."
            ),
            "newAspectRatio" to Schema.enumeration(
                values = AspectRatioOption.entries.map { it.name },
                description = "Optional canvas aspect ratio."
            ),
            "newAudioTrack" to Schema.enumeration(
                values = AudioTrackOption.entries.map { it.name },
                description = "Optional BGM audio track mood."
            ),
            "transitionEffect" to Schema.enumeration(
                values = TransitionEffect.entries.map { it.name },
                description = "Optional clip-to-clip transition effect (FADE, SLIDE_LEFT, SLIDE_UP, ZOOM_IN, WHIP_PAN, FLASH_CUT, or NONE to remove)."
            ),
            "transitionDurationSec" to Schema.double(
                description = "Optional transition duration in seconds from 0.3 to 1.5."
            ),
            "applyTransitionToAll" to Schema.boolean(
                description = "True to apply the transitionEffect across all multi-layered video/text clips in the timeline."
            )
        ),
        optionalProperties = listOf(
            "motionPreset",
            "motionSpeed",
            "motionIntensity",
            "newHeadline",
            "newCta",
            "newBgPreset",
            "newAspectRatio",
            "newAudioTrack",
            "transitionEffect",
            "transitionDurationSec"
        )
    )

    private val directorModel by lazy {
        Firebase.ai.generativeModel(
            modelName = TEXT_MODEL,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = directorSchema
            }
        )
    }

    private val musePlannerSchema = Schema.obj(
        properties = mapOf(
            "shots" to Schema.array(
                items = Schema.obj(
                    properties = mapOf(
                        "title" to Schema.string(description = "Short 3-5 word cinematic shot title"),
                        "sceneDirection" to Schema.string(description = "Concise cinematography & lighting notes"),
                        "visualPrompt" to Schema.string(description = "Rich photographic prompt for generating the visual frame"),
                        "suggestedHook" to Schema.string(description = "Short 3-6 word viral on-screen hook caption"),
                        "durationSec" to Schema.double(description = "Recommended clip duration between 2.5 and 6.0 seconds"),
                        "refinedAudioCue" to Schema.string(description = "Audio SFX & ambience description")
                    )
                ),
                description = "Storyboard shots to generate"
            )
        )
    )

    private val musePlannerModel by lazy {
        Firebase.ai.generativeModel(
            modelName = TEXT_MODEL,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = musePlannerSchema
            }
        )
    }

    suspend fun runDirectorChat(
        userPrompt: String,
        currentContextSummary: String,
        attachedBitmap: Bitmap? = null,
        isProTier: Boolean = false
    ): Result<DirectorCommandResult> {
        return try {
            val systemContext = """
                You are OmniStudio Director AI, an elite mobile video editor and TikTok/Reels creative director for ENJEBEL TECH.
                Tier Mode: ${if (isProTier) "PRO STUDIO (Deep pacing & precision cuts)" else "STANDARD"}.
                Current Studio State: $currentContextSummary
                
                Rules:
                - Interpret the creator's request and map it to concrete timeline/canvas edits using the JSON fields.
                - If they say "Make this punchier for TikTok": set motionPreset=DYNAMIC_ZOOM_PUNCH, motionSpeed=1.6, motionIntensity=1.5, trimDeadAir=true, newAspectRatio=RATIO_9_16, transitionEffect=ZOOM_IN, and suggest a punchy newHeadline.
                - If they say "Cut out dead air": set trimDeadAir=true and splitAtPlayhead=false (or true if splitting silence).
                - If they say "Add a suspenseful transition": set addSuspenseTransition=true, transitionEffect=FLASH_CUT (or ZOOM_IN), transitionDurationSec=0.45, and motionPreset=FLASH_SALE_GLITCH or CINEMATIC_REVEAL.
                - If they ask for fade, dissolve, slide, wipe, zoom, whip pan, or flash transitions between clips: set transitionEffect (FADE, SLIDE_LEFT, SLIDE_UP, ZOOM_IN, WHIP_PAN, FLASH_CUT, or NONE), transitionDurationSec (0.3-1.5), and set applyTransitionToAll=true if they say "all clips" or "between clips".
                - Always explain what you modified in 'reply' and ask 1 helpful clarifying question, with 2-3 short 'followUps'.
            """.trimIndent()

            val response = withTimeout(60_000L) {
                directorModel.generateContent(
                    content {
                        if (attachedBitmap != null) {
                            image(attachedBitmap)
                        }
                        text("$systemContext\n\nCreator Request: $userPrompt")
                    }
                )
            }

            val rawJson = response.text ?: throw IllegalStateException("Director AI returned an empty response.")
            val json = JSONObject(rawJson)

            val followUpsArr = json.optJSONArray("followUps")
            val followUps = if (followUpsArr != null) {
                (0 until followUpsArr.length()).mapNotNull { followUpsArr.optString(it).takeIf { s -> s.isNotBlank() } }
            } else {
                emptyList()
            }

            val motionPreset = json.optString("motionPreset", "").let { name ->
                MotionTemplate.entries.find { it.name == name }
            }
            val newBgPreset = json.optString("newBgPreset", "").let { name ->
                BackgroundPreset.entries.find { it.name == name }
            }
            val newAspectRatio = json.optString("newAspectRatio", "").let { name ->
                AspectRatioOption.entries.find { it.name == name }
            }
            val newAudioTrack = json.optString("newAudioTrack", "").let { name ->
                AudioTrackOption.entries.find { it.name == name }
            }
            val transitionEffect = json.optString("transitionEffect", "").let { name ->
                TransitionEffect.entries.find { it.name == name }
            }
            val transitionDurationSec = if (json.has("transitionDurationSec") && !json.isNull("transitionDurationSec")) {
                json.getDouble("transitionDurationSec").toFloat().coerceIn(0.3f, 1.5f)
            } else null

            val speedVal = if (json.has("motionSpeed") && !json.isNull("motionSpeed")) {
                json.getDouble("motionSpeed").toFloat().coerceIn(0.5f, 2.5f)
            } else null

            val intensityVal = if (json.has("motionIntensity") && !json.isNull("motionIntensity")) {
                json.getDouble("motionIntensity").toFloat().coerceIn(0.2f, 2.0f)
            } else null

            val headlineVal = if (json.has("newHeadline") && !json.isNull("newHeadline")) {
                json.getString("newHeadline").takeIf { it.isNotBlank() }
            } else null

            val ctaVal = if (json.has("newCta") && !json.isNull("newCta")) {
                json.getString("newCta").takeIf { it.isNotBlank() }
            } else null

            Result.success(
                DirectorCommandResult(
                    reply = json.optString("reply", "Applied your edits to the OmniStudio timeline!"),
                    followUps = followUps,
                    motionPreset = motionPreset,
                    motionSpeed = speedVal,
                    motionIntensity = intensityVal,
                    trimDeadAir = json.optBoolean("trimDeadAir", false),
                    addSuspenseTransition = json.optBoolean("addSuspenseTransition", false),
                    splitAtPlayhead = json.optBoolean("splitAtPlayhead", false),
                    newHeadline = headlineVal,
                    newCta = ctaVal,
                    newBgPreset = newBgPreset,
                    newAspectRatio = newAspectRatio,
                    newAudioTrack = newAudioTrack,
                    transitionEffect = transitionEffect,
                    transitionDurationSec = transitionDurationSec,
                    applyTransitionToAll = json.optBoolean("applyTransitionToAll", false)
                )
            )
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Director Chat timed out", e)
            Result.failure(IllegalStateException("Director AI timed out after 60 seconds. Please try again."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Director Chat failed", e)
            Result.failure(e)
        }
    }

    suspend fun generateMuseVideoClips(
        prompt: String,
        cameraMovement: CameraMovement,
        panAmount: Float = cameraMovement.defaultPan,
        zoomFactor: Float = cameraMovement.defaultZoom,
        tiltAngle: Float = cameraMovement.defaultTilt,
        aspectRatio: AspectRatioOption,
        audioCue: String,
        batchCount: Int,
        referenceBitmap: Bitmap? = null,
        referenceUri: String? = null
    ): Result<List<MuseGeneratedClip>> {
        return try {
            val safeBatch = batchCount.coerceIn(1, 3)
            val rigSummary = "Pan=${(panAmount * 100).toInt()}%, Zoom=${String.format(java.util.Locale.US, "%.2fx", zoomFactor)}, Tilt=${(tiltAngle * 100).toInt()}%"
            val planPrompt = """
                You are Muse Engine, an AI Video Generator for faceless creators inside OmniStudio.
                User Prompt: "$prompt"
                Camera Movement: ${cameraMovement.label} (${cameraMovement.subtitle}) | Rig Controls: $rigSummary
                Aspect Ratio: ${aspectRatio.label}
                Audio Cue: "${audioCue.ifBlank { "Atmospheric bass swell + subtle ambient foley" }}"
                Mode: ${if (referenceBitmap != null) "Image-to-Video animation extending the uploaded frame" else "Text-to-Video from scratch"}
                Generate $safeBatch distinct cinematic shot(s) in JSON.
            """.trimIndent()

            val planResponse = withTimeout(60_000L) {
                musePlannerModel.generateContent(
                    content {
                        if (referenceBitmap != null) {
                            image(referenceBitmap)
                        }
                        text(planPrompt)
                    }
                )
            }

            val rawPlan = planResponse.text ?: throw IllegalStateException("Muse Engine returned an empty storyboard.")
            val planJson = JSONObject(rawPlan)
            val shotsArray = planJson.getJSONArray("shots")

            val plans = (0 until shotsArray.length()).map { idx ->
                val obj = shotsArray.getJSONObject(idx)
                MuseShotPlan(
                    title = obj.optString("title", "Muse Shot ${idx + 1}"),
                    sceneDirection = obj.optString("sceneDirection", cameraMovement.subtitle),
                    visualPrompt = obj.optString("visualPrompt", prompt),
                    suggestedHook = obj.optString("suggestedHook", "WATCH THIS"),
                    durationSec = obj.optDouble("durationSec", 4.0).toFloat().coerceIn(2.5f, 6.0f),
                    refinedAudioCue = obj.optString("refinedAudioCue", audioCue.ifBlank { "Cinematic bass pulse" })
                )
            }

            val sdkAspectRatio = when (aspectRatio) {
                AspectRatioOption.RATIO_9_16 -> AspectRatio.PORTRAIT_9x16
                AspectRatioOption.RATIO_16_9 -> AspectRatio.LANDSCAPE_16x9
                AspectRatioOption.RATIO_1_1 -> AspectRatio.SQUARE_1x1
                AspectRatioOption.RATIO_4_5 -> AspectRatio.PORTRAIT_3x4
            }

            val imageModel = Firebase.ai.generativeModel(
                modelName = IMAGE_MODEL,
                generationConfig = generationConfig {
                    responseModalities = listOf(ResponseModality.IMAGE)
                    imageConfig = ImageConfig.builder()
                        .setAspectRatio(sdkAspectRatio)
                        .setImageSize(ImageSize.SIZE_1K)
                        .build()
                }
            )

            val generatedClips = mutableListOf<MuseGeneratedClip>()
            for ((index, shot) in plans.withIndex()) {
                val framePrompt = "Cinematic still frame for ${aspectRatio.label} video, ${cameraMovement.label} framing ($rigSummary), ultra high detail, dramatic lighting: ${shot.visualPrompt}"
                val imageResponse = withTimeout(60_000L) {
                    if (referenceBitmap != null) {
                        imageModel.generateContent(
                            content {
                                image(referenceBitmap)
                                text("Reimagine and animate this subject with ${cameraMovement.label} ($rigSummary): $framePrompt")
                            }
                        )
                    } else {
                        imageModel.generateContent(framePrompt)
                    }
                }

                val bitmap = imageResponse.candidates.firstOrNull()
                    ?.content?.parts?.filterIsInstance<ImagePart>()
                    ?.firstOrNull()?.image
                    ?: throw IllegalStateException("Muse Engine did not return an image frame for shot ${index + 1}.")

                generatedClips.add(
                    MuseGeneratedClip(
                        id = "muse_${System.currentTimeMillis()}_$index",
                        title = shot.title,
                        prompt = prompt,
                        cameraMovement = cameraMovement,
                        panAmount = panAmount,
                        zoomFactor = zoomFactor,
                        tiltAngle = tiltAngle,
                        aspectRatio = aspectRatio,
                        audioCue = shot.refinedAudioCue,
                        sceneDirection = shot.sceneDirection,
                        suggestedHook = shot.suggestedHook,
                        durationSec = shot.durationSec,
                        keyframeBitmap = bitmap,
                        referenceUri = referenceUri,
                        isImportedToTimeline = false
                    )
                )
            }

            Result.success(generatedClips)
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Muse Engine timed out", e)
            Result.failure(IllegalStateException("Muse Engine timed out after 60 seconds. Please try again."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Muse Engine generation failed", e)
            Result.failure(e)
        }
    }
}

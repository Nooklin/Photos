package org.nooklin.photos.ml

import android.content.Context
import android.util.Log
import android.util.Size
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.nooklin.photos.BuildConfig
import org.nooklin.photos.data.AnalysisRepository
import org.nooklin.photos.data.MediaItem
import org.nooklin.photos.data.PersonRepository
import org.nooklin.photos.data.local.MlTag

internal const val FACE_SCAN_LOG_TAG = "NooklinFaceScan"
internal const val OBJECT_SCAN_LOG_TAG = "NooklinObjectScan"

// Debug builds only: the logs hold content URIs and results
internal fun scanLogD(tag: String, message: String) {
    if (BuildConfig.DEBUG) Log.d(tag, message)
}
internal fun scanLogW(tag: String, message: String, throwable: Throwable? = null) {
    if (BuildConfig.DEBUG) Log.w(tag, message, throwable)
}

// Runs every enabled detector per photo. PEOPLE runs both BlazeFace models and clusters faces,
// PETS/CARS/MOTORCYCLES share one object detection pass.
// Known limit: needsAnalysis() gates the whole photo, so a newly enabled category skips scanned photos.
class PhotoAnalysisScanner(
    private val context: Context,
    private val analysisRepository: AnalysisRepository,
    private val personRepository: PersonRepository
) {
    data class ScanProgress(val processed: Int, val total: Int, val done: Boolean = false)

    suspend fun scan(
        items: List<MediaItem>,
        enabledTags: Set<MlTag>,
        onProgress: (ScanProgress) -> Unit
    ) = withContext(Dispatchers.Default) {
        val peopleEnabled = MlTag.PEOPLE in enabledTags
        val objectTagsEnabled = enabledTags.any { it == MlTag.PETS || it == MlTag.CARS || it == MlTag.MOTORCYCLES }
        val duplicatesEnabled = MlTag.DUPLICATES in enabledTags
        val toScan = items.filter { !it.isVideo }

        if ((!peopleEnabled && !objectTagsEnabled && !duplicatesEnabled) || toScan.isEmpty()) {
            onProgress(ScanProgress(0, 0, done = true))
            return@withContext
        }

        val statusDates = analysisRepository.statusDates()
        personRepository.resetScanCache()
        var shortRangeDetector: FaceDetector? = null
        var fullRangeDetector: FaceDetector? = null
        var faceEmbedder: FaceEmbedder? = null
        var faceLandmarkVerifier: FaceLandmarkVerifier? = null
        var objectDetector: ObjectDetector? = null
        try {
            if (peopleEnabled) {
                shortRangeDetector = FaceDetector(context, FaceModel.SHORT_RANGE)
                fullRangeDetector = FaceDetector(context, FaceModel.FULL_RANGE)
                faceEmbedder = FaceEmbedder(context)
                faceLandmarkVerifier = FaceLandmarkVerifier(context)
            }
            if (objectTagsEnabled) {
                objectDetector = ObjectDetector(context)
            }

            toScan.forEachIndexed { index, item ->
                onProgress(ScanProgress(index, toScan.size))

                if (statusDates[item.id] == item.dateModified) {
                    // Every 50th skip is logged, shows needsAnalysis() as the reason when nothing new is found
                    if (index % 50 == 0) {
                        scanLogD(OBJECT_SCAN_LOG_TAG, "Skipping ${item.uri}, already analyzed (needsAnalysis=false)")
                    }
                    return@forEachIndexed
                }

                // 320px thumbnail shared by both pipelines, each downscales further
                val bitmap = try {
                    // Blocking decode on IO, keeps Default free for inference
                    withContext(Dispatchers.IO) {
                        context.contentResolver.loadThumbnail(item.uri, Size(320, 320), null)
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    null
                }

                val tags = mutableListOf<Pair<String, Float>>()

                if (bitmap != null) {
                    if (peopleEnabled && shortRangeDetector != null && fullRangeDetector != null && faceEmbedder != null && faceLandmarkVerifier != null) {
                        val shortFaces = try {
                            shortRangeDetector.detectFaces(bitmap)
                        } catch (e: Exception) {
                            // Logged: a swallowed exception looks the same as "found nothing"
                            scanLogW(FACE_SCAN_LOG_TAG, "SHORT_RANGE failed for ${item.uri}", e)
                            emptyList()
                        }
                        val fullFaces = try {
                            fullRangeDetector.detectFaces(bitmap)
                        } catch (e: Exception) {
                            scanLogW(FACE_SCAN_LOG_TAG, "FULL_RANGE failed for ${item.uri}", e)
                            emptyList()
                        }
                        val mergedFaces = FaceDetector.mergeFaceBoxes(shortFaces, fullFaces)

                        // Threshold 0.001: MediaPipe's 0.5 rejected real faces, this only drops extreme non-face scores (2e-9)
                        val faces = mergedFaces.filter { faceLandmarkVerifier.isConfirmedFace(bitmap, it) }
                        val verifierRejected = mergedFaces.size - faces.size

                        if (faces.isNotEmpty() || shortFaces.isNotEmpty() || fullFaces.isNotEmpty()) {
                            scanLogD(
                                FACE_SCAN_LOG_TAG,
                                "${item.uri}: short=${shortFaces.size} full=${fullFaces.size} merged=${mergedFaces.size} " +
                                    "verified=${faces.size} (landmark-rejected=$verifierRejected)"
                            )
                        }

                        // Clear before writing, a failed re-analysis must not mix old and new faces
                        personRepository.clearFacesForMedia(item.id)

                        faces.forEachIndexed { faceIndex, box ->
                            val embedding = try {
                                faceEmbedder.embed(bitmap, box)
                            } catch (e: Exception) {
                                scanLogW(FACE_SCAN_LOG_TAG, "Embedding failed for ${item.uri} face $faceIndex", e)
                                null
                            }
                            if (embedding != null) {
                                val personId = personRepository.findOrCreatePerson(embedding)
                                scanLogD(FACE_SCAN_LOG_TAG, "${item.uri} face $faceIndex -> personId=$personId")
                                personRepository.saveFace(item.id, faceIndex, personId, embedding, box.score, box)
                            }
                        }

                        if (faces.isNotEmpty()) tags.add(MlTag.PEOPLE.id to 1f)
                    }

                    if (objectTagsEnabled && objectDetector != null) {
                        val objects = try {
                            objectDetector.detect(bitmap)
                        } catch (e: Exception) {
                            scanLogW(OBJECT_SCAN_LOG_TAG, "ObjectDetector failed for ${item.uri}", e)
                            emptyList()
                        }
                        if (objects.isNotEmpty()) {
                            scanLogD(
                                OBJECT_SCAN_LOG_TAG,
                                "${item.uri}: " + objects.joinToString { "class=${it.cocoClassIndex} score=${"%.2f".format(it.score)}" }
                            )
                        }
                        // Highest score per tag wins
                        objects.forEach { obj ->
                            val tag = CocoClass.tagFor(obj.cocoClassIndex) ?: return@forEach
                            if (tag !in enabledTags) return@forEach
                            val existingIndex = tags.indexOfFirst { it.first == tag.id }
                            if (existingIndex == -1) {
                                tags.add(tag.id to obj.score)
                            } else if (obj.score > tags[existingIndex].second) {
                                tags[existingIndex] = tag.id to obj.score
                            }
                        }
                    }
                }

                // Runs when only Duplicates is on, reuses the decoded bitmap
                if (bitmap != null && duplicatesEnabled) {
                    try {
                        analysisRepository.saveHash(item.id, computeDHash(bitmap))
                    } catch (e: Exception) {
                        scanLogW(OBJECT_SCAN_LOG_TAG, "Hashing failed for ${item.uri}", e)
                    }
                }

                // Both pipelines are done with the bitmap, release it now
                bitmap?.recycle()

                // Only record decoded photos: saving a failure as "found nothing" would skip it forever
                if (bitmap != null) {
                    analysisRepository.saveAnalysis(item.id, item.dateModified, tags)
                }
            }
        } finally {
            personRepository.resetScanCache()
            shortRangeDetector?.close()
            fullRangeDetector?.close()
            faceEmbedder?.close()
            faceLandmarkVerifier?.close()
            objectDetector?.close()
        }

        if (peopleEnabled) {
            try {
                personRepository.combineSimilarPeople()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // A failed combine pass must not fail the scan
            }
        }

        onProgress(ScanProgress(toScan.size, toScan.size, done = true))
    }
}

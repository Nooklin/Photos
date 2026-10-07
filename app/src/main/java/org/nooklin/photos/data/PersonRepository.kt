package org.nooklin.photos.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.nooklin.photos.data.local.FaceEmbeddingEntity
import org.nooklin.photos.data.local.NooklinDatabase
import org.nooklin.photos.data.local.PersonEntity
import org.nooklin.photos.ml.FaceBox
import org.nooklin.photos.ml.FaceClusterer

class PersonRepository(context: Context) {
    private val dao = NooklinDatabase.getInstance(context).personDao()

    val allPeople = dao.observeAllPeople()
    val allFaces = dao.observeAllFaceSummaries()

    private class CachedFace(val mediaId: Long, val personId: Long, val embedding: FloatArray)

    // Parsed once per scan, not once per face
    @Volatile
    private var scanCache: MutableList<CachedFace>? = null

    fun resetScanCache() {
        scanCache = null
    }

    suspend fun facesForMedia(mediaId: Long) = withContext(Dispatchers.IO) {
        dao.facesForMedia(mediaId)
    }

    suspend fun clearFacesForMedia(mediaId: Long) = withContext(Dispatchers.IO) {
        dao.clearFacesForMedia(mediaId)
        scanCache?.removeAll { it.mediaId == mediaId }
    }

    // Nearest neighbor over all faces, a centroid drifts away from real faces
    suspend fun findOrCreatePerson(embedding: FloatArray): Long = withContext(Dispatchers.IO) {
        val existing = scanCache ?: dao.allFaceEmbeddingsSnapshot()
            .map { CachedFace(it.mediaId, it.personId, FaceClusterer.deserialize(it.embedding)) }
            .toMutableList()
            .also { scanCache = it }
        var bestPersonId: Long? = null
        var bestDistance = Float.MAX_VALUE
        for (row in existing) {
            val d = FaceClusterer.distance(embedding, row.embedding)
            if (d < bestDistance) {
                bestDistance = d
                bestPersonId = row.personId
            }
        }
        val matched = bestPersonId
        // Person may have been merged away during the scan
        if (matched != null && bestDistance < FaceClusterer.MATCH_THRESHOLD && dao.personById(matched) != null) {
            matched
        } else {
            dao.insertPerson(PersonEntity(name = null, createdAt = System.currentTimeMillis()))
        }
    }

    suspend fun saveFace(
        mediaId: Long,
        faceIndex: Int,
        personId: Long,
        embedding: FloatArray,
        confidence: Float,
        box: FaceBox
    ) = withContext(Dispatchers.IO) {
        dao.insertFaceEmbedding(
            FaceEmbeddingEntity(
                mediaId = mediaId,
                faceIndex = faceIndex,
                personId = personId,
                embedding = FaceClusterer.serialize(embedding),
                confidence = confidence,
                boxXMin = box.xMin,
                boxYMin = box.yMin,
                boxXMax = box.xMax,
                boxYMax = box.yMax
            )
        )
        scanCache?.add(CachedFace(mediaId, personId, embedding))
    }

    // Same name merges into the existing person, no duplicate cards
    suspend fun rename(personId: Long, name: String?) = withContext(Dispatchers.IO) {
        val trimmed = name?.trim()?.ifBlank { null }
        val existing = trimmed?.let { dao.findByName(it) }
        if (existing != null && existing.id != personId) {
            dao.mergePeople(intoPersonId = existing.id, fromPersonId = personId)
        } else {
            dao.renamePerson(personId, trimmed)
        }
    }

    suspend fun merge(intoPersonId: Long, fromPersonId: Long) = withContext(Dispatchers.IO) {
        dao.mergePeople(intoPersonId, fromPersonId)
    }

    suspend fun splitOut(faceIds: List<Long>): Long = withContext(Dispatchers.IO) {
        if (faceIds.isEmpty()) return@withContext -1L
        val newPersonId = dao.insertPerson(PersonEntity(name = null, createdAt = System.currentTimeMillis()))
        faceIds.forEach { dao.reassignFace(it, newPersonId) }
        newPersonId
    }

    // Compares people by their average face and merges close ones, different names never merge
    suspend fun combineSimilarPeople(
        threshold: Float = FaceClusterer.COMBINE_THRESHOLD,
        apply: Boolean = true
    ): Int = withContext(Dispatchers.IO) {
        class Cluster(val id: Long, var name: String?, val centroid: FloatArray, var faces: Int)
        val people = dao.peopleSnapshot()
        val sums = HashMap<Long, FloatArray>()
        val counts = HashMap<Long, Int>()
        for (row in dao.allFaceEmbeddingsSnapshot()) {
            val embedding = FaceClusterer.deserialize(row.embedding)
            val sum = sums.getOrPut(row.personId) { FloatArray(embedding.size) }
            if (sum.size != embedding.size) continue
            for (i in embedding.indices) sum[i] += embedding[i]
            counts[row.personId] = (counts[row.personId] ?: 0) + 1
        }
        val clusters = people.mapNotNull { person ->
            val count = counts[person.id] ?: return@mapNotNull null
            val sum = sums[person.id] ?: return@mapNotNull null
            Cluster(person.id, person.name, FloatArray(sum.size) { sum[it] / count }, count)
        }.toMutableList()

        var combined = 0
        while (true) {
            var best: Pair<Int, Int>? = null
            var bestDistance = threshold
            for (i in clusters.indices) {
                for (j in i + 1 until clusters.size) {
                    val a = clusters[i]
                    val b = clusters[j]
                    val bothNamed = !a.name.isNullOrBlank() && !b.name.isNullOrBlank()
                    if (bothNamed && !a.name.equals(b.name, ignoreCase = true)) continue
                    val distance = FaceClusterer.distance(a.centroid, b.centroid)
                    if (distance < bestDistance) {
                        bestDistance = distance
                        best = i to j
                    }
                }
            }
            val (i, j) = best ?: break
            val a = clusters[i]
            val b = clusters[j]
            // Keep the named one, else the one with more faces
            val keepFirst = when {
                !a.name.isNullOrBlank() && b.name.isNullOrBlank() -> true
                a.name.isNullOrBlank() && !b.name.isNullOrBlank() -> false
                else -> a.faces >= b.faces
            }
            val keep = if (keepFirst) a else b
            val drop = if (keepFirst) b else a
            if (apply) dao.mergePeople(keep.id, drop.id)
            val total = keep.faces + drop.faces
            for (k in keep.centroid.indices) {
                keep.centroid[k] = (keep.centroid[k] * keep.faces + drop.centroid[k] * drop.faces) / total
            }
            keep.faces = total
            clusters.remove(drop)
            combined++
        }
        combined
    }

    suspend fun clearAll() = withContext(Dispatchers.IO) { dao.clearAll() }

    // Empty people stay behind, cleaning those up is separate
    suspend fun pruneOrphanedFaces(validMediaIds: Set<Long>) = withContext(Dispatchers.IO) {
        val referenced = dao.distinctFaceMediaIds()
        val orphaned = referenced.filterNot { it in validMediaIds }
        if (orphaned.isNotEmpty()) dao.deleteFacesForMediaIds(orphaned)
    }
}

package org.nooklin.photos

import android.Manifest
import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.nooklin.photos.data.MediaStoreRepository
import org.nooklin.photos.data.ThumbnailStore
import org.nooklin.photos.ui.common.ThumbnailWarmup

// Fills the thumbnail store while the app is closed, when the battery is not low. The system stops it after a while and starts it again.
class ThumbnailJobService : JobService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var job: Job? = null

    override fun onStartJob(params: JobParameters): Boolean {
        job = scope.launch {
            val items = MediaStoreRepository(applicationContext).queryMedia()
            ThumbnailWarmup.fillStore(applicationContext, items)
            ThumbnailStore.cleanUp(applicationContext, items)
            jobFinished(params, false)
        }
        return true
    }

    override fun onStopJob(params: JobParameters): Boolean {
        job?.cancel()
        return true
    }

    companion object {
        private const val JOB_ID = 4101

        fun schedule(context: Context) {
            val granted = context.checkSelfPermission(Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
            val scheduler = context.getSystemService(JobScheduler::class.java) ?: return
            // scheduling again would stop a run that is going
            if (scheduler.getPendingJob(JOB_ID) != null) return
            scheduler.schedule(
                JobInfo.Builder(JOB_ID, ComponentName(context, ThumbnailJobService::class.java))
                    .setRequiresBatteryNotLow(true)
                    .build()
            )
        }
    }
}

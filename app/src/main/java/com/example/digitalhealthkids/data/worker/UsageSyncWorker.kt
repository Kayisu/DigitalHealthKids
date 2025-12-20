package com.example.digitalhealthkids.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.digitalhealthkids.core.network.usage.UsageApi
import com.example.digitalhealthkids.core.network.usage.UsageReportRequestDto
// 🔥 DEĞİŞEN IMPORT BURASI:
import com.example.digitalhealthkids.core.network.usage.readUsageEventsForRange
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.util.Log
import com.example.digitalhealthkids.core.network.usage.UsageEventDto
import java.util.Calendar

@HiltWorker
class UsageSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val usageApi: UsageApi
) : CoroutineWorker(appContext, workerParams) {

    private fun aggregateByDayAndPackage(events: List<UsageEventDto>): List<UsageEventDto> {
        if (events.isEmpty()) return events
        val cal = Calendar.getInstance()
        val agg = mutableMapOf<Pair<Long, String>, Triple<Long, Long, Int>>()

        events.forEach { ev ->
            cal.timeInMillis = ev.timestampStart
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            val dayStart = cal.timeInMillis
            val key = dayStart to ev.packageName
            val existing = agg[key]
            if (existing == null) {
                agg[key] = Triple(ev.timestampStart, ev.timestampEnd, ev.durationSeconds)
            } else {
                val minStart = minOf(existing.first, ev.timestampStart)
                val maxEnd = maxOf(existing.second, ev.timestampEnd)
                val totalDur = existing.third + ev.durationSeconds
                agg[key] = Triple(minStart, maxEnd, totalDur)
            }
        }

        return agg.map { (key, triple) ->
            val (_, pkg) = key
            UsageEventDto(
                packageName = pkg,
                appName = events.firstOrNull { it.packageName == pkg }?.appName ?: pkg,
                timestampStart = triple.first,
                timestampEnd = triple.second,
                durationSeconds = triple.third
            )
        }
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val prefs = applicationContext.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

            // User ID ve Device ID yoksa çalışamayız
            val userId = prefs.getString("user_id", null)
            val deviceId = prefs.getString("device_id", null)

            if (userId == null || deviceId == null) {
                Log.e("UsageSync", "Missing ids userId=$userId deviceId=$deviceId")
                return@withContext Result.failure()
            }

            // Şimdilik sadece bugünü gönder (0 = bugün). Geçmiş backfill'i manuel çalıştırırız.
            val rawEvents = readUsageEventsForRange(applicationContext, 0)
            Log.i("UsageSync", "Raw events read count=${rawEvents.size} (daysBack=0)")

            // Gün + paket bazında özetleyerek payload'ı küçült (aynı paket/gün tekrarlarını birleştir).
            val events = aggregateByDayAndPackage(rawEvents)
            Log.i("UsageSync", "Aggregated events count=${events.size}")

            if (events.isNotEmpty()) {
                val minStart = events.minOf { it.timestampStart }
                val maxEnd = events.maxOf { it.timestampEnd }
                val spanHours = ((maxEnd - minStart) / 1000.0 / 3600.0)
                Log.i(
                    "UsageSync",
                    "Preparing report: count=${events.size}, spanHours=%.2f, start=${minStart}, end=${maxEnd}".format(spanHours)
                )

                val chunkSize = 25
                val chunks = events.chunked(chunkSize)
                Log.i("UsageSync", "Reporting in ${chunks.size} chunk(s) with chunkSize=$chunkSize")

                for ((idx, chunk) in chunks.withIndex()) {
                    Log.i(
                        "UsageSync",
                        "Chunk ${idx + 1}/${chunks.size} size=${chunk.size} start=${chunk.first().timestampStart} end=${chunk.last().timestampEnd}"
                    )
                    val started = System.currentTimeMillis()
                    try {
                        val resp = usageApi.reportUsage(
                            UsageReportRequestDto(userId, deviceId, chunk)
                        )
                        val elapsed = System.currentTimeMillis() - started
                        Log.i("UsageSync", "Chunk ${idx + 1}/${chunks.size} inserted=${resp.inserted} elapsedMs=${elapsed}")
                    } catch (e: Exception) {
                        val elapsed = System.currentTimeMillis() - started
                        Log.e("UsageSync", "Chunk ${idx + 1}/${chunks.size} failed after ${elapsed}ms", e)
                        throw e
                    }
                }

                prefs.edit().putLong("last_sync_time", System.currentTimeMillis()).apply()
                return@withContext Result.success()
            }

            Log.d("UsageSync", "No events to report")
            Result.success()
        } catch (e: Exception) {
            Log.e("UsageSync", "Reporting failed", e)
            Result.retry()
        }
    }
}
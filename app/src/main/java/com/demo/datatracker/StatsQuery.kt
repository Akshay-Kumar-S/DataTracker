package com.demo.datatracker

import android.app.usage.NetworkStats
import android.app.usage.NetworkStatsManager
import android.content.Context
import android.os.Build
import android.os.RemoteException
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import java.util.*
import java.util.concurrent.TimeUnit

object StatsQuery {
    private val TAG = "dataTracker"

    /**
     * Find aggregation time
     * This method will find the aggregation time of bucket
     * @return
     */
    private fun findAggregationTime(): Long {
        Log.d(TAG, "findAggregationTime: ")
        val nsm = App.getInstance()
            .getSystemService(AppCompatActivity.NETWORK_STATS_SERVICE) as NetworkStatsManager
        var aggregationTime: Long = -1

        for (networkType in 0..1) {
            val networkStats = nsm.queryDetailsForUid(
                networkType,
                Util.getSimSubscriberId(),
                System.currentTimeMillis() - TimeUnit.DAYS.toMillis(105),
                System.currentTimeMillis() + TimeUnit.HOURS.toMillis(2),
                Util.getUid("com.android.vending") //TODO update package name as your wish
            )

            val bucket = NetworkStats.Bucket()
            if (networkStats.hasNextBucket()) {
                Log.d(TAG, "findAggregationTime: " + Util.getDateDefault(bucket.startTimeStamp))
                networkStats.getNextBucket(bucket)
                aggregationTime = bucket.startTimeStamp
                networkStats.close()
                break
            }
        }
        return aggregationTime
    }

    /**
     * Get bucket time
     * This method will return
     * Example: Aggregation time: 9:30
     *  Case1: (current running bucket)
     *      current time: 10:30
     *      calendar.timeInMillis = current time
     *      TimePeriod.StartTime = 9:30 and TimePeriod.endTime = 11:30
     *  Case 2: (completed bucket)
     *      current time: 10:30
     *      calendar.timeInMillis = 8:15
     *      TimePeriod.StartTime = 7:30 and TimePeriod.endTime = 9:30
     * @param time will be equal to System.currentTimeMillis() or any past times.
     * @return bucket start time and endTime
     */
    fun getBucketTime(time: Long = System.currentTimeMillis()): TimePeriod {
        Log.d(TAG, "getLastAggregatedTime: ")
        val aggregationTime = findAggregationTime()
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = time

        if (aggregationTime != -1L) {
            val aCalendar = Calendar.getInstance()
            aCalendar.timeInMillis = aggregationTime

            calendar.set(Calendar.MINUTE, aCalendar.get(Calendar.MINUTE))
            calendar.set(Calendar.SECOND, aCalendar.get(Calendar.SECOND))
            calendar.set(Calendar.MILLISECOND, aCalendar.get(Calendar.MILLISECOND))

            val aHourEven = isEven(aCalendar.get(Calendar.HOUR_OF_DAY))
            val cHourEven = isEven(calendar.get(Calendar.HOUR_OF_DAY))

            if ((aHourEven && cHourEven) || (!aHourEven && !cHourEven)) {
                if (calendar.timeInMillis > time) {
                    calendar.timeInMillis -= TimeUnit.HOURS.toMillis(2)
                }
            } else {
                calendar.timeInMillis -= TimeUnit.HOURS.toMillis(1)
            }
        } else {
            Log.d(TAG, "getLastAggregatedTime: else ")
        //not handling this case for now.
        }
        return TimePeriod(calendar.timeInMillis, calendar.timeInMillis + TimeUnit.HOURS.toMillis(2))
    }

    private fun isEven(value: Int): Boolean {
        return value % 2 == 0
    }

    fun findAppDataUsage(
        ctx: Context, queryConfig: QueryConfig, uid: Int
    ): MutableMap<Int, DataUsage> {
        Log.e(TAG, "findAppDataUsage: ")
        val networkStatsManager =
            ctx.getSystemService(AppCompatActivity.NETWORK_STATS_SERVICE) as NetworkStatsManager
        val networkStats: NetworkStats
        val appUsageMap = mutableMapOf<Int, DataUsage>()
        try {
            networkStats = networkStatsManager.querySummary(
                queryConfig.networkType,
                Util.getSimSubscriberId(),
                queryConfig.timePeriod.startTime,
                queryConfig.timePeriod.endTime
            )
            val bucket = NetworkStats.Bucket()
            while (networkStats.hasNextBucket()) {
                networkStats.getNextBucket(bucket)
                logBucket(bucket, uid)  //For testing, logging only one app usage.
                if (appUsageMap.containsKey(bucket.uid)) {
                    appUsageMap[bucket.uid]!!.txBytes += bucket.txBytes
                    appUsageMap[bucket.uid]!!.rxBytes += bucket.rxBytes
                    appUsageMap[bucket.uid]!!.txPackets += bucket.txPackets
                    appUsageMap[bucket.uid]!!.rxPackets += bucket.rxPackets
                } else {
                    appUsageMap[bucket.uid] = DataUsage(
                        bucket.txBytes,
                        bucket.rxBytes,
                        bucket.txPackets,
                        bucket.rxPackets
                    )
                }
            }
            networkStats.close()
        } catch (e: RemoteException) {
            Log.d(TAG, "getUsage: RemoteException")
        }
        return appUsageMap
    }

    fun queryUsageForUid(ctx: Context, queryConfig: QueryConfig, uid: Int) {
        Log.e(TAG, "findAppDataUsage: ")
        val networkStatsManager =
            ctx.getSystemService(AppCompatActivity.NETWORK_STATS_SERVICE) as NetworkStatsManager
        val networkStats: NetworkStats
        try {
            networkStats = networkStatsManager.queryDetailsForUid(
                queryConfig.networkType,
                Util.getSimSubscriberId(),
                queryConfig.timePeriod.startTime,
                queryConfig.timePeriod.endTime,
                uid
            )
            val bucket = NetworkStats.Bucket()
            while (networkStats.hasNextBucket()) {
                networkStats.getNextBucket(bucket)
                logBucket(bucket, uid)
            }
            networkStats.close()
        } catch (e: RemoteException) {
            Log.d(TAG, "getUsage: RemoteException")
        }
    }

    fun findDeviceDataUsage(ctx: Context, queryConfig: QueryConfig): DataUsage {
        Log.d(TAG, "findDeviceDataUsage: ")
        val networkStatsManager =
            ctx.getSystemService(AppCompatActivity.NETWORK_STATS_SERVICE) as NetworkStatsManager
        val bucket: NetworkStats.Bucket = networkStatsManager.querySummaryForDevice(
            queryConfig.networkType,
            Util.getSimSubscriberId(),
            queryConfig.timePeriod.startTime,
            queryConfig.timePeriod.endTime
        )
        return DataUsage(bucket.txBytes, bucket.rxBytes, bucket.txPackets, bucket.rxPackets)
    }

    private fun logBucket(bucket: NetworkStats.Bucket, uid: Int) {
        if (bucket.uid == uid) {
            Log.d(
                TAG,
                "uid: ${bucket.uid}, " +
                        "st: ${Util.getDateDefault(bucket.startTimeStamp)}, " +
                        "et: ${Util.getDateDefault(bucket.endTimeStamp)},  " +
                        "tx Bytes: ${bucket.txBytes},  " +
                        "rx Bytes: ${bucket.rxBytes},  " +
                        "tx Packet: ${bucket.txPackets},  " +
                        "rx Packet: ${bucket.rxPackets},  "
            )
        }
    }
}
package com.demo.datatracker

import android.net.NetworkCapabilities
import android.util.Log
import java.util.concurrent.TimeUnit

object DataUsageTest {
    private val TAG = "dataTracker"

    /**
     * Case1:
     * Suppose, last aggregation time is 1:30. then,
     * For my querySummary() with start time 2:00 and end time 2:15 (current time),
     *  the response will be data usage from 1:30 to 2:15.
     * For my querySummary() with start time 2:15 and end time 2:30 (current time),
     *  the response will be data usage from 1:30 to 2:30.
     *
     * Note: Current bucket startTime = PrevBucket endTime
     *  Run this method 2 times with 2 different start and end time, similar to above example.
     *  @see Util.getStartTime
     *  @see Util.getEndTime use this two methods to give different start and end time
     *
     *  compare logs of findAppDataUsage with queryUsageForUid.
     *  the buckets txBytes, rxBytes, txPackets and rxPackets returned by findAppDataUsage will be equal to the
     *  txBytes, rxBytes, txPackets and rxPackets returned by queryUsageForUid.
     */
    fun case1() {
        Log.e(TAG, "***************Case1***************")
        val queryConfig =
            QueryConfig(NetworkCapabilities.TRANSPORT_CELLULAR) //TODO update networkType here
        val currentBucketTime = StatsQuery.getBucketTime()
        Log.d(
            TAG,
            "current bucket startTime: ${Util.getDateDefault(currentBucketTime.startTime)} endTime: ${
                Util.getDateDefault(currentBucketTime.endTime)
            }"
        )
        //TODO update start time and end time.
        //Condition: currentBucket startTime >= startTime > endTime <= currentBucket endTime
        queryConfig.timePeriod = TimePeriod(Util.getStartTime(), Util.getEndTime())
        val uid =
            Util.getUid("com.android.vending")    //TODO update package name as your wish. Try different apps.
        StatsQuery.findAppDataUsage(App.getInstance(), queryConfig, uid)
        queryConfig.timePeriod = TimePeriod(currentBucketTime.startTime, currentBucketTime.endTime)
        StatsQuery.queryUsageForUid(App.getInstance(), queryConfig, uid)
    }

    /**
     * Case2:
     * Suppose, last aggregation time is 3:30 then,
     * Query1: For my querySummary() with start time 2:00 and end time 2:15,
     *  the response will be data usage from 1:30 to 3:30 / 8.
     * Query2: For my querySummary() with start time 2:15 and end time 2:30,
     *  the response will be data usage from 1:30 to 3:30 / 8.
     * ie; response of Query1 = response of Query2
     *
     * Run this method 2 times with 2 different start and end time, similar to above example.
     *  @see Util.getStartTime
     *  @see Util.getEndTime use this two methods to give different start and end time
     *
     *  compare logs of findAppDataUsage with queryUsageForUid.
     *  the buckets txBytes, rxBytes, txPackets and rxPackets returned by findAppDataUsage will be equal to the
     *  txBytes, rxBytes, txPackets and rxPackets returned by queryUsageForUid.
     */
    fun case2() {
        Log.e(TAG, "***************Case2***************")
        val queryConfig =
            QueryConfig(NetworkCapabilities.TRANSPORT_CELLULAR) //TODO update networkType here
        val bucketTime = getPrevBucketTime()
        Log.d(
            TAG,
            "last aggregation time is startTime: ${Util.getDateDefault(bucketTime.startTime)} endTime: ${
                Util.getDateDefault(bucketTime.endTime)
            }"
        )
        //TODO update start time and end time.
        //Condition bucketTime >= startTime > endTime <= bucketTime endTime.
        queryConfig.timePeriod = TimePeriod(Util.getStartTime(), Util.getEndTime())
        val uid =
            Util.getUid("com.android.vending")    //TODO update package name as your wish. Try different apps.
        StatsQuery.findAppDataUsage(App.getInstance(), queryConfig, uid)
        queryConfig.timePeriod = TimePeriod(bucketTime.startTime, bucketTime.endTime)
        StatsQuery.queryUsageForUid(App.getInstance(), queryConfig, uid)
    }

    /**
     * Get prev bucket time
     * This method is used find the just prev bucket. You can change it to your logic or pass any
     * calendar instance with time < currentBucket startTime
     */
    private fun getPrevBucketTime(): TimePeriod {
        val currentBucketTime = StatsQuery.getBucketTime()
        val timeBeforeCurrentBucket = currentBucketTime.startTime - TimeUnit.MINUTES.toMillis(15)
        return StatsQuery.getBucketTime(timeBeforeCurrentBucket)
    }


    /**
     * Case3
     * Suppose, last aggregation time is 3:30, then,
     * The sum of app-wise data usage from 3:30 and end time 4:30 is not equal to querySummaryForDevice
     * response with start time 3:30 and end time 4:30.
     *
     *  You can use any start and end time, but working on current bucket start and end time is easy to understand.
     *  @see Util.getStartTime
     *  @see Util.getEndTime use this two methods to give different start and end time
     */
    fun case3() {
        Log.e(TAG, "***************Case3***************")
        val queryConfig =
            QueryConfig(NetworkCapabilities.TRANSPORT_CELLULAR) //TODO update networkType here
        val currentBucketTime = StatsQuery.getBucketTime()
        Log.d(
            TAG, "current bucket " +
                    "startTime: ${Util.getDateDefault(currentBucketTime.startTime)} " +
                    "endTime: ${Util.getDateDefault(currentBucketTime.endTime)}"
        )
        //TODO update start time and end time.
        //Condition: currentBucket startTime >= startTime > endTime <= currentBucket endTime
        //You can use any start and end time, but working on current bucket start and end time is easy to understand.
        queryConfig.timePeriod = TimePeriod(Util.getStartTime(), Util.getEndTime())
        val appsDataUsage = StatsQuery.findAppDataUsage(App.getInstance(), queryConfig, 0)
        val deviceDataUsage = StatsQuery.findDeviceDataUsage(App.getInstance(), queryConfig)
        Log.d(TAG, "Total apps data usage")
        Log.d(
            TAG, "txBytes: ${appsDataUsage.values.sumOf { it.txBytes }} " +
                    "rxBytes: ${appsDataUsage.values.sumOf { it.rxBytes }} " +
                    "txPackets: ${appsDataUsage.values.sumOf { it.txPackets }} " +
                    "rxPackets: ${appsDataUsage.values.sumOf { it.rxPackets }}"
        )
        Log.d(TAG, "Total device data usage")
        Log.d(
            TAG, "txBytes: ${deviceDataUsage.txBytes} " +
                    "rxBytes: ${deviceDataUsage.rxBytes} " +
                    "txPackets: ${deviceDataUsage.txPackets} " +
                    "rxPackets: ${deviceDataUsage.rxPackets}"
        )
    }
}
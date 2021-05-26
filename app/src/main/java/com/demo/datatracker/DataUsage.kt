package com.demo.datatracker

data class DataUsage(
    var txBytes: Long = 0,
    var rxBytes: Long = 0,
    var txPackets: Long = 0,
    var rxPackets: Long = 0
)

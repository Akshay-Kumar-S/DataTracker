package com.demo.datatracker

data class QueryConfig(val networkType: Int) {
    lateinit var timePeriod: TimePeriod
}

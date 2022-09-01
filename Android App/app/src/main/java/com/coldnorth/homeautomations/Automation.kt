package com.coldnorth.homeautomations

class Automation(
    val id:Int,
    val name:String,
    val targetDeviceid:String,
    val targetState:Int,
    val conditions:MutableList<Condition>,
    val startDelay:Long,
    val daySchedule: MutableList<Int>,
    val start_time:String?,
    val end_time:String?
) {
}
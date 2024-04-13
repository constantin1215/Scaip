package entity

import java.time.LocalDateTime
import java.util.*
import kotlin.properties.Delegates

enum class CallType {
    INSTANT,
    SCHEDULED
}

class Call() {
    val id : String = UUID.randomUUID().toString()
    lateinit var leaderId : String
    lateinit var type: CallType
    var groupId : String? = null
    var timestamp by Delegates.notNull<Long>()
    lateinit var channel : String
    var scheduledTime : LocalDateTime?= null

    constructor(
        leaderId : String,
        type : CallType,
        channel : String
    ) : this() {
        this.leaderId = leaderId
        this.type = type
        this.timestamp = System.currentTimeMillis()
        this.channel = channel
    }

    constructor(
        leaderId : String,
        type : CallType,
        channel : String,
        scheduledTime: LocalDateTime
    ) : this() {
        this.leaderId = leaderId
        this.type = type
        this.timestamp = System.currentTimeMillis()
        this.channel = channel
        this.scheduledTime = scheduledTime
    }

    override fun toString(): String {
        return "Call(id='$id', leaderId='$leaderId', type=$type, groupId=$groupId, timestamp=$timestamp, channel='$channel', scheduledTime=$scheduledTime)"
    }
}
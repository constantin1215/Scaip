package entity

import java.time.LocalDateTime
import java.util.*
import kotlin.properties.Delegates

enum class CallType {
    INSTANT,
    SCHEDULED
}
class Call() {
    lateinit var id : String
    lateinit var leaderId : String
    lateinit var type: CallType
    var timestamp by Delegates.notNull<Long>()
    var scheduledTime : LocalDateTime?= null

    constructor(
        id : String,
        leaderId : String,
        type : CallType,
        timestamp: Long
    ) : this() {
        this.id = id
        this.leaderId = leaderId
        this.type = type
        this.timestamp = timestamp
    }

    constructor(
        id : String,
        leaderId : String,
        type : CallType,
        scheduledTime: LocalDateTime,
        timestamp: Long
    ) : this() {
        this.id = id
        this.leaderId = leaderId
        this.type = type
        this.timestamp = timestamp
        this.scheduledTime = scheduledTime
    }

    override fun toString(): String {
        return "Call(id='$id', leaderId='$leaderId', type=$type, timestamp=$timestamp, scheduledTime=$scheduledTime)"
    }
}
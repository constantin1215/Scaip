package entity

import java.time.LocalDateTime
import java.util.*
import kotlin.properties.Delegates

enum class CallType {
    INSTANT,
    SCHEDULED
}

enum class CallStatus {
    ONGOING,
    FINISHED,
    SCHEDULED
}

class Call() {
    lateinit var id : String
    lateinit var leaderId : String
    lateinit var type: CallType
    var timestamp by Delegates.notNull<Long>()
    var scheduledTime : Long?= null
    lateinit var status : CallStatus
    var title : String? = null

    constructor(
        id : String,
        leaderId : String,
        type : CallType,
        timestamp: Long,
        status: CallStatus
    ) : this() {
        this.id = id
        this.leaderId = leaderId
        this.type = type
        this.timestamp = timestamp
        this.status = status
    }

    constructor(
        id : String,
        leaderId : String,
        type : CallType,
        scheduledTime: Long,
        timestamp: Long,
        title: String,
        status: CallStatus
    ) : this() {
        this.id = id
        this.leaderId = leaderId
        this.type = type
        this.timestamp = timestamp
        this.scheduledTime = scheduledTime
        this.title = title
        this.status = status
    }

    override fun toString(): String {
        return "Call(id='$id', leaderId='$leaderId', type=$type, timestamp=$timestamp, scheduledTime=$scheduledTime)"
    }
}
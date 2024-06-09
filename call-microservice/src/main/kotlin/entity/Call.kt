package entity

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
    var id : String = UUID.randomUUID().toString()
    lateinit var leaderId : String
    lateinit var type: CallType
    var groupId : String? = null
    var timestamp by Delegates.notNull<Long>()
    lateinit var channel : String
    var scheduledTime : Long?= null
    lateinit var status : CallStatus
    var title : String? = null
    lateinit var joinedVideo : MutableSet<String>
    lateinit var joinedAudio : MutableSet<String>

    constructor(
        leaderId : String,
        type : CallType,
        channel : String
    ) : this() {
        this.leaderId = leaderId
        this.type = type
        this.timestamp = System.currentTimeMillis()
        this.channel = channel
        this.status = CallStatus.ONGOING
        this.joinedVideo = mutableSetOf()
        this.joinedAudio = mutableSetOf()
    }

    constructor(
        leaderId : String,
        type : CallType,
        channel : String,
        scheduledTime: Long,
        title: String
    ) : this() {
        this.leaderId = leaderId
        this.type = type
        this.timestamp = System.currentTimeMillis()
        this.channel = channel
        this.scheduledTime = scheduledTime
        this.status = CallStatus.SCHEDULED
        this.title = title
        this.joinedVideo = mutableSetOf()
        this.joinedAudio = mutableSetOf()
    }

    constructor(
        id: String,
        groupId: String
    ) : this() {
        this.id = id
        this.groupId = groupId

        this.leaderId = ""
        this.type = CallType.INSTANT
        this.timestamp = 0
        this.channel = ""
        this.status = CallStatus.FINISHED
        this.joinedVideo = mutableSetOf()
        this.joinedAudio = mutableSetOf()
    }

    override fun toString(): String {
        return "Call(id='$id', leaderId='$leaderId', type=$type, groupId=$groupId, timestamp=$timestamp, channel='$channel', scheduledTime=$scheduledTime, status=$status)"
    }
}
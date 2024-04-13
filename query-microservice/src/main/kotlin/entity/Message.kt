package entity

import java.util.*
import kotlin.properties.Delegates

class Message() {
    lateinit var id : String
    lateinit var userId : String
    lateinit var content : String
    lateinit var groupId : String
    var timestamp by Delegates.notNull<Long>()

    constructor(
        id : String,
        userId : String,
        content : String,
        groupId : String,
        timestamp : Long
    ) : this() {
        this.id = id
        this.userId = userId
        this.content = content
        this.groupId = groupId
        this.timestamp = timestamp
    }

    override fun toString(): String {
        return "Message(id='$id', userId='$userId', content='$content', groupId='$groupId', timestamp=$timestamp)"
    }

}
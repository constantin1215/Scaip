package entity

import java.util.UUID
import kotlin.properties.Delegates

class Message() {
    val id : String = UUID.randomUUID().toString()
    lateinit var userId : String
    lateinit var content : String
    var groupId : String? = null
    var timestamp by Delegates.notNull<Long>()

    constructor(
        userId : String,
        content : String
    ) : this() {
        this.userId = userId
        this.content = content
        this.timestamp = System.currentTimeMillis()
    }
    override fun toString(): String {
        return "Message(id='$id', userId='$userId', content='$content', timestamp=$timestamp)"
    }
}
package entity

import java.util.UUID
import kotlin.properties.Delegates

class Message() {
    val id : String = UUID.randomUUID().toString()
    lateinit var user : User
    lateinit var content : String
    var groupId : String? = null
    var timestamp by Delegates.notNull<Long>()

    constructor(
        user : User,
        content : String
    ) : this() {
        this.user = user
        this.content = content
        this.timestamp = System.currentTimeMillis()
    }
    override fun toString(): String {
        return "Message(id='$id', user='$user', content='$content', timestamp=$timestamp)"
    }
}
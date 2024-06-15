package entity

import io.vertx.ext.auth.User
import org.bson.Document
import java.util.*
import kotlin.properties.Delegates

class Message() {
    lateinit var id : String
    lateinit var user : Document
    lateinit var content : String
    lateinit var groupId : String
    var timestamp by Delegates.notNull<Long>()

    constructor(
        id : String,
        user : Document,
        content : String,
        groupId : String,
        timestamp : Long
    ) : this() {
        this.id = id
        this.user = user
        this.content = content
        this.groupId = groupId
        this.timestamp = timestamp
    }

    override fun toString(): String {
        return "Message(id='$id', userId='$user', content='$content', groupId='$groupId', timestamp=$timestamp)"
    }

}
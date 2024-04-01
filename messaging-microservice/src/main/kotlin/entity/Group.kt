package entity

import io.quarkus.mongodb.panache.common.MongoEntity
import org.bson.codecs.pojo.annotations.BsonId

@MongoEntity(collection = "Group", database = "messages")
class Group() {
    @BsonId
    lateinit var id : String
    lateinit var owner: String
    lateinit var members : MutableSet<String>
    var lastMessage: Message? = null
    lateinit var messages : MutableList<Message>


    constructor(
        id : String,
        owner: String,
        members : MutableSet<String>,
        messages : MutableList<Message>
    ) : this() {
        this.id = id
        this.owner = owner
        this.members = members
        this.messages = messages
    }

    override fun toString(): String {
        return "Group(id='$id', owner='$owner', members=$members)"
    }
}
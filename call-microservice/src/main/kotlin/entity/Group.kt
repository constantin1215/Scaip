package entity

import io.quarkus.mongodb.panache.common.MongoEntity
import org.bson.codecs.pojo.annotations.BsonId
import java.security.MessageDigest

@MongoEntity(collection = "Group", database = "calls")
class Group() {
    @BsonId
    lateinit var id : String
    lateinit var owner: String
    lateinit var members : MutableSet<String>
    lateinit var calls : MutableList<Call>

    constructor(
        id : String,
        owner: String,
        members : MutableSet<String>,
        calls : MutableList<Call>
    ) : this() {
        this.id = id
        this.owner = owner
        this.members = members
        this.calls = calls
    }

    override fun toString(): String {
        return "Group(id='$id', owner='$owner', members=$members)"
    }
}
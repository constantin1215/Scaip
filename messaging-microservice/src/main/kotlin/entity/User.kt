package entity

import io.quarkus.mongodb.panache.common.MongoEntity
import org.bson.codecs.pojo.annotations.BsonId

@MongoEntity(collection = "User", database = "messages")
class User() {
    @BsonId
    lateinit var id : String
    lateinit var username : String

    constructor(
        id : String,
        username : String
    ) : this() {
        this.id = id
        this.username = username
    }

    override fun toString(): String {
        return "User(id='$id', username='$username')"
    }
}
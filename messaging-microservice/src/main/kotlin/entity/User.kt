package entity

import io.quarkus.mongodb.panache.common.MongoEntity
import org.bson.codecs.pojo.annotations.BsonId

@MongoEntity(collection = "User", database = "messages")
class User() {
    @BsonId
    lateinit var id : String

    constructor(
        id : String,
    ) : this() {
        this.id = id
    }

    override fun toString(): String {
        return "User(id='$id')"
    }
}
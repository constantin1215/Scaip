package entity

import entity.pojo.Group
import io.quarkus.mongodb.panache.common.MongoEntity
import org.bson.codecs.pojo.annotations.BsonId
import org.bson.types.ObjectId

@MongoEntity(collection = "UsersData", database = "query")
class User() {
    @BsonId
    lateinit var id : String
    lateinit var username : String
    lateinit var email : String
    lateinit var firstName : String
    lateinit var lastName : String
    lateinit var pictureId : String
    lateinit var groups : Set<Group>
    lateinit var calls : Set<String>
    lateinit var notifications : Set<String>
    constructor(id : String,
                username : String,
                email : String,
                firstName : String,
                lastName : String,
                pictureId : String,
                groups : Set<Group>,
                calls : Set<String>,
                notifications : Set<String>
    ) : this() {
        this.id = id
        this.username = username
        this.email = email
        this.firstName = firstName
        this.lastName = lastName
        this.pictureId = pictureId
        this.groups = groups
        this.calls = calls
        this.notifications = notifications
    }
}
package entity

import io.quarkus.mongodb.panache.common.MongoEntity
import org.bson.codecs.pojo.annotations.BsonId

@MongoEntity(collection = "UsersData", database = "query")
class User() {
    @BsonId
    lateinit var id : String
    lateinit var username : String
    lateinit var email : String
    lateinit var firstName : String
    lateinit var lastName : String
    var pictureId : String? = null
    lateinit var groups : MutableSet<GroupSummary>
    lateinit var notifications : MutableSet<String>
    constructor(
        id : String,
        username : String,
        email : String,
        firstName : String,
        lastName : String,
        groups : MutableSet<GroupSummary>,
        notifications : MutableSet<String>
    ) : this() {
        this.id = id
        this.username = username
        this.email = email
        this.firstName = firstName
        this.lastName = lastName
        this.groups = groups
        this.notifications = notifications
    }
}
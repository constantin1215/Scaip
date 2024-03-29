package entity

import io.quarkus.mongodb.panache.common.ProjectionFor

@ProjectionFor(User::class)
class SearchedUser() {
    lateinit var id : String
    lateinit var username : String
    lateinit var email : String
    lateinit var firstName : String
    lateinit var lastName : String
    lateinit var pictureId : String

    constructor(
        id : String,
        username : String,
        email : String,
        firstName : String,
        lastName : String,
        pictureId : String
    ) : this() {
        this.id = id
        this.username = username
        this.email = email
        this.firstName = firstName
        this.lastName = lastName
        this.pictureId = pictureId
    }
}
package entity

import entity.pojo.Group
import io.quarkus.mongodb.panache.common.ProjectionFor

@ProjectionFor(User::class)
class SearchedUser() {
    lateinit var username : String
    lateinit var email : String
    lateinit var firstName : String
    lateinit var lastName : String

    constructor(username : String,
                email : String,
                firstName : String,
                lastName : String,
    ) : this() {
        this.username = username
        this.email = email
        this.firstName = firstName
        this.lastName = lastName
    }
}
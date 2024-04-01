package entity

import io.quarkus.mongodb.panache.common.ProjectionFor

@ProjectionFor(User::class)
class UserSummary() {
    lateinit var id : String
    lateinit var username : String
    lateinit var firstName : String
    lateinit var lastName : String
    var pictureId : String? = null

    constructor(
        id : String,
        username : String,
        firstName : String,
        lastName : String,
        pictureId : String
    ) : this() {
        this.id = id
        this.username = username
        this.firstName = firstName
        this.lastName = lastName
        this.pictureId = pictureId
    }

    constructor(user: User) : this() {
        this.id = user.id
        this.username = user.username
        this.firstName = user.firstName
        this.lastName = user.lastName
        this.pictureId = user.pictureId
    }

    override fun toString(): String {
        return "UserSummary(id='$id', username='$username', firstName='$firstName', lastName='$lastName', pictureId='$pictureId')"
    }


}
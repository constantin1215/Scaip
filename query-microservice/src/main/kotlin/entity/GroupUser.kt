package entity

import io.quarkus.mongodb.panache.common.ProjectionFor

@ProjectionFor(User::class)
class GroupUser() {
    lateinit var id : String
    lateinit var username : String
    lateinit var firstName : String
    lateinit var lastName : String
    lateinit var pictureId : String

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

    override fun toString(): String {
        return "GroupUser(id='$id', username='$username', firstName='$firstName', lastName='$lastName', pictureId='$pictureId')"
    }


}
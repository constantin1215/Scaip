package entity

import entity.pojo.Group

class UserProfile() {
    lateinit var username : String
    lateinit var email : String
    lateinit var firstName : String
    lateinit var lastName : String
    lateinit var pictureId : String
    lateinit var groups : Set<Group>
    lateinit var calls : Set<String>
    lateinit var notifications : Set<String>
    constructor(user: User) : this() {
        this.username = user.username
        this.email = user.email
        this.firstName = user.firstName
        this.lastName = user.lastName
        this.pictureId = user.pictureId
        this.groups = user.groups
        this.calls = user.calls
        this.notifications = user.notifications
    }
}
package entity

import jakarta.persistence.*

class GroupInfo() {
    lateinit var id : String
    lateinit var title : String
    lateinit var description : String

    constructor(group: Group) : this() {
        this.id = group.id
        this.title = group.title
        this.description = group.description
    }
}
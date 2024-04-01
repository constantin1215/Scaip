package entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.ManyToMany
import jakarta.persistence.OneToMany
import jakarta.persistence.Table

@Entity
@Table(name = "\"User\"")
class User() {
    @Id
    lateinit var id : String
    @ManyToMany(mappedBy = "members")
    @Transient
    val groups : MutableSet<Group> = mutableSetOf()
    @OneToMany(mappedBy = "owner")
    @Transient
    val ownedGroups : MutableSet<Group> = mutableSetOf()

    constructor(id : String) : this() {
        this.id = id
    }

    override fun toString(): String {
        return "User(id='$id')"
    }

}
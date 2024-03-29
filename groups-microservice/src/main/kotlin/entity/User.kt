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
    @Column(nullable = false, unique = true, length = 30)
    lateinit var username : String
    @Column(nullable = false, length = 100)
    lateinit var firstName : String
    @Column(nullable = false, length = 100)
    lateinit var lastName : String
    @ManyToMany(mappedBy = "members")
    @Transient
    val groups : MutableSet<Group> = mutableSetOf()
    @OneToMany(mappedBy = "owner")
    @Transient
    val ownedGroups : MutableSet<Group> = mutableSetOf()

    constructor(id : String, username: String, firstName: String, lastName: String) : this() {
        this.id = id
        this.username = username
        this.firstName = firstName
        this.lastName = lastName
    }

    override fun toString(): String {
        return "User(id='$id', username='$username', firstName='$firstName', lastName='$lastName')"
    }

}
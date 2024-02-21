package entity

import jakarta.persistence.*

@Entity
@Table(name = "\"User\"")
class User() {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    lateinit var id : String
    @Column(nullable = false, unique = true, length = 30)
    lateinit var username : String
    @Column(nullable = false, length = 16)
    lateinit var password : String
    @Column(nullable = false, unique = true, length = 200)
    lateinit var email : String
    @Column(nullable = false, length = 100)
    lateinit var firstName : String
    @Column(nullable = false, length = 100)
    lateinit var lastName : String

    constructor(username: String, password: String, email: String, firstName: String, lastName: String) : this() {
        this.username = username
        this.password = password
        this.email = email
        this.firstName = firstName
        this.lastName = lastName
    }
}
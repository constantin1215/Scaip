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
    @Column(nullable = false)
    lateinit var password : String

    constructor(id : String, username: String, password: String) : this() {
        this.id = id
        this.username = username
        this.password = password
    }
}
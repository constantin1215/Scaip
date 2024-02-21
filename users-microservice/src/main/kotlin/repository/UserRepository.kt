package repository

import entity.User
import io.quarkus.hibernate.orm.panache.kotlin.PanacheRepository
import jakarta.enterprise.context.ApplicationScoped

@ApplicationScoped
class UserRepository : PanacheRepository<User> {
    fun findByUsername(username: String ) = find("username", username).firstResult()
    fun existsByEmail(email : String) = count("email", email).toInt() == 1
    fun existsByUsername(username : String) = count("username", username).toInt() == 1
}
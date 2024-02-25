package repository

import entity.User
import io.quarkus.hibernate.orm.panache.kotlin.PanacheRepository
import jakarta.enterprise.context.ApplicationScoped

@ApplicationScoped
class UserRepository : PanacheRepository<User> {
    fun findByUsername(username: String ) = find("username", username).firstResult()
    fun findById(id : String) = find("id", id).firstResult()
}
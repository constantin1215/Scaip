package repository

import entity.User
import io.quarkus.mongodb.panache.kotlin.PanacheMongoRepository
import jakarta.enterprise.context.ApplicationScoped

@ApplicationScoped
class UserRepository : PanacheMongoRepository<User> {
    fun findById(id : String) = find("_id", id).firstResult()
}
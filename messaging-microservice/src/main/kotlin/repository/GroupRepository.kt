package repository

import entity.Group
import io.quarkus.mongodb.panache.kotlin.PanacheMongoRepository
import jakarta.enterprise.context.ApplicationScoped

@ApplicationScoped
class GroupRepository : PanacheMongoRepository<Group> {
    fun findById(id : String) = find("_id", id).firstResult()
}
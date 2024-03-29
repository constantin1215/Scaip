package repository

import entity.Group
import entity.GroupUser
import entity.SearchedUser
import entity.User
import io.quarkus.mongodb.panache.kotlin.PanacheMongoRepository
import io.quarkus.panache.common.Parameters
import jakarta.enterprise.context.ApplicationScoped

@ApplicationScoped
class GroupRepository : PanacheMongoRepository<Group> {
    fun findById(id : String) = find("_id", id).firstResult()
}
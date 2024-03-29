package repository

import entity.Group
import entity.User
import io.quarkus.hibernate.orm.panache.kotlin.PanacheRepository
import jakarta.enterprise.context.ApplicationScoped

@ApplicationScoped
class GroupRepository : PanacheRepository<Group> {
    fun findById(id : String) = find("id", id).firstResult()
    fun findByOwner(owner : User) = find("owner", owner).firstResult()
}
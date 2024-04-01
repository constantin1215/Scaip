package repository

import entity.UserSummary
import entity.User
import io.quarkus.mongodb.panache.kotlin.PanacheMongoRepository
import io.quarkus.panache.common.Parameters
import jakarta.enterprise.context.ApplicationScoped

@ApplicationScoped
class UserRepository : PanacheMongoRepository<User> {
    fun findByQuery(query : String) = find("{ \$text: { \$search: :query }  }", Parameters.with("query", query))
        .project(UserSummary::class.java).list()
    fun findById(id : String) = find("_id", id).firstResult()
    fun findGroupMemberById(id : String) = find("_id", id).project(UserSummary::class.java).firstResult()
}
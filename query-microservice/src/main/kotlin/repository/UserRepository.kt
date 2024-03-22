package repository

import entity.SearchedUser
import entity.User
import entity.UserProfile
import io.quarkus.mongodb.panache.kotlin.PanacheMongoRepository
import io.quarkus.panache.common.Parameters
import jakarta.enterprise.context.ApplicationScoped

@ApplicationScoped
class UserRepository : PanacheMongoRepository<User> {
    fun findByQuery(query : String) = find("{ \$text: { \$search: :query }  }", Parameters.with("query", query))
        .project(SearchedUser::class.java).list()
    fun findById(id : String) = find("_id", id).firstResult()
}
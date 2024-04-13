package repository

import com.mongodb.client.model.UpdateOptions
import entity.Message
import entity.UserSummary
import entity.User
import io.quarkus.mongodb.panache.kotlin.PanacheMongoRepository
import io.quarkus.panache.common.Parameters
import jakarta.enterprise.context.ApplicationScoped
import org.bson.Document
import org.bson.conversions.Bson

@ApplicationScoped
class UserRepository : PanacheMongoRepository<User> {
    fun findByQuery(query : String) = find("{ \$text: { \$search: :query }  }", Parameters.with("query", query))
        .project(UserSummary::class.java).list()
    fun findById(id : String) = find("_id", id).firstResult()
    fun findGroupMemberById(id : String) = find("_id", id).project(UserSummary::class.java).firstResult()
    fun updateUserGroupsLastMessage(groupId : String, message: Message) {
        val filter : Bson = Document("groups._id", groupId)
        val update : Bson = Document("\$set", Document("groups.\$[elem].lastMessage", message))
        val arrayFilter = Document("elem._id", groupId)

        mongoCollection().updateMany(filter, update, UpdateOptions().arrayFilters(listOf(arrayFilter)))
    }
}
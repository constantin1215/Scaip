import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import entity.Group
import entity.GroupEvent
import entity.GroupInfo
import entity.User
import exceptions.*
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.transaction.Transactional
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.eclipse.microprofile.reactive.messaging.Incoming
import org.jboss.logging.Logger
import repository.GroupRepository
import repository.OutboxRepository
import repository.UserRepository
import kotlin.math.log
import kotlin.math.min

@ApplicationScoped
class GroupsMicroservice {

    enum class Event {
        CREATE_GROUP,
        CREATE_GROUP_SUCCESS,
        CREATE_GROUP_FAIL,
        ADD_MEMBERS,
        ADD_MEMBERS_SUCCESS,
        ADD_MEMBERS_FAIL,
        REMOVE_MEMBERS,
        REMOVE_MEMBERS_SUCCESS,
        REMOVE_MEMBERS_FAIL,
        UPDATE_GROUP,
        UPDATE_GROUP_SUCCESS,
        UPDATE_GROUP_FAIL,
        REGISTRATION_SUCCESS,
        UPDATE_USER_SUCCESS,
        UNAUTHORIZED
    }

    private val gson = Gson()
    private val type = object : TypeToken<Map<String, Any>>() {}.type
    private val logger : Logger = Logger.getLogger(this.javaClass)

    @Inject
    lateinit var userRepository: UserRepository

    @Inject
    lateinit var groupRepository: GroupRepository

    @Inject
    lateinit var outboxRepository: OutboxRepository

    @Incoming("group_topic")
    @Transactional
    fun consume(msg: ConsumerRecord<String, String>) {
        val headers = msg.headers().associate { it.key() to it.value().toString(Charsets.UTF_8) }.toMutableMap()
        val data = gson.fromJson(msg.value(), type) as MutableMap<String, Any>
        logger.info("Received msg: $headers and $data")

        try {
            when(Event.valueOf(headers["EVENT"] as String)) {
                Event.CREATE_GROUP -> {
                    logger.info("Creating group.")
                    handleNewGroup(data, headers)
                }
                Event.UPDATE_GROUP -> {
                    logger.info("Updating group info.")
                    handleGroupUpdate(data, headers)
                }
                Event.ADD_MEMBERS -> {
                    logger.info("Adding members to group.")
                    handleNewMembers(data, headers)
                }
                Event.REMOVE_MEMBERS -> {
                    logger.info("Removing members from group.")
                    handleMemberRemoval(data, headers)
                }
                Event.REGISTRATION_SUCCESS -> {
                    logger.info("Adding new user.")
                    handleNewUser(data)
                }
                Event.UPDATE_USER_SUCCESS -> {
                    logger.info("Updating user.")
                    handleUserUpdate(data, headers)
                }
                else -> {
                    logger.info("TO DO() handle other events")
                }
            }
        } catch (ex : EntityAlreadyInCollection) {
            logger.warn("Entity with ID: ${ex.entityId} already in collection")
        } catch (ex : NecessaryDataMissing) {
            logger.warn("Event with missing data received")
            outboxRepository.persist(GroupEvent(
                headers["EVENT"] as String,
                getFailedEvent(headers["EVENT"] as String).toString(),
                msg.value(),
                "Missing or malformed fields.",
                headers["SESSION_ID"] as String
            ))
        } catch (ex : UserNotFound) {
            logger.warn("User with ID: ${ex.userId} not found while handling EVENT: ${ex.event}")
            outboxRepository.persist(GroupEvent(
                headers["EVENT"] as String,
                getFailedEvent(headers["EVENT"] as String).toString(),
                msg.value(),
                "User with ID: ${ex.userId} not found.",
                headers["SESSION_ID"] as String
            ))
        } catch (ex : GroupNotFound) {
            logger.warn("Group with ID: ${ex.groupId} not found while handling EVENT: ${ex.event}")
            outboxRepository.persist(GroupEvent(
                headers["EVENT"] as String,
                getFailedEvent(headers["EVENT"] as String).toString(),
                msg.value(),
                "Group with ID: ${ex.groupId} not found.",
                headers["SESSION_ID"] as String
            ))
        } catch (ex : Unauthorized) {
            logger.warn("User ${data["userId"]} tried to perform an unauthorized action ${ex.event} on group ${data["groupId"]}")
            outboxRepository.persist(GroupEvent(
                headers["EVENT"] as String,
                Event.UNAUTHORIZED.toString(),
                msg.value(),
                ex.message ?: "",
                headers["SESSION_ID"] as String
            ))
        } catch (ex : IllegalArgumentException) {
            logger.warn("Unknown event possibly detected!")
        } catch (ex : Exception) {
            logger.warn(ex.message ?: "Error: ${ex.javaClass} with no message")
        }
    }

    private fun handleMemberRemoval(
        data: MutableMap<String, Any>,
        headers: MutableMap<String, String>
    ) {
        val group = groupRepository.findById(data["groupId"] as String)
            ?: throw GroupNotFound(data["groupId"] as String, headers["EVENT"] as String)

        if (group.owner.id != data["userId"])
            throw Unauthorized("Only the owner of the group can remove members.", headers["EVENT"] as String)

        val members = extractMembers(data, headers)

        val membersId = members.map { it.id }
        group.members.removeIf { it.id in membersId }

        groupRepository.persist(group)
        outboxRepository.persist(
            GroupEvent(
                headers["EVENT"] as String,
                Event.REMOVE_MEMBERS_SUCCESS.toString(),
                gson.toJson(members),
                headers["SESSION_ID"] as String
            )
        )

        logger.info("Members removed successfully.")
    }

    private fun handleNewMembers(
        data: MutableMap<String, Any>,
        headers: MutableMap<String, String>
    ) {
        val group = groupRepository.findById(data["groupId"] as String)
            ?: throw GroupNotFound(data["groupId"] as String, headers["EVENT"] as String)

        if (group.owner.id != data["userId"])
            throw Unauthorized("Only the owner of the group can add new members.", headers["EVENT"] as String)

        val members = extractMembers(data, headers)

        for (member in members)
            group.members.add(member)

        groupRepository.persist(group)

        outboxRepository.persist(
            GroupEvent(
                headers["EVENT"] as String,
                Event.ADD_MEMBERS_SUCCESS.toString(),
                gson.toJson(members),
                headers["SESSION_ID"] as String
            )
        )

        logger.info("New members added successfully.")
    }

    private fun handleGroupUpdate(
        data: MutableMap<String, Any>,
        headers: MutableMap<String, String>
    ) {
        val group = groupRepository.findById(data["groupId"] as String)
            ?: throw GroupNotFound(data["groupId"] as String, headers["EVENT"] as String)

        if (group.owner.id != data["userId"])
            throw Unauthorized("You are not allowed to update group information.", headers["EVENT"] as String)

        group.title = (data["title"] as String).substring(0, min(30, (data["title"] as String).length))
        group.description = data["description"] as String

        groupRepository.persist(group)

        outboxRepository.persist(
            GroupEvent(
                headers["EVENT"] as String,
                Event.UPDATE_GROUP_SUCCESS.toString(),
                gson.toJson(GroupInfo(group)),
                headers["SESSION_ID"] as String
            )
        )

        logger.info("Group data updated successfully.")
    }

    private fun handleNewGroup(
        data: MutableMap<String, Any>,
        headers: MutableMap<String, String>
    ) {
        val initialMembers = extractMembers(data, headers)

        val owner = userRepository.findById(data["userId"] as String) ?: throw UserNotFound(
            data["userId"] as String,
            headers["EVENT"] as String
        )

        initialMembers.add(owner)

        groupRepository.persist(
            Group(
                owner,
                data["title"] as String,
                data["description"] as String,
                initialMembers
            )
        )

        outboxRepository.persist(
            GroupEvent(
                headers["EVENT"] as String,
                Event.CREATE_GROUP_SUCCESS.toString(),
                gson.toJson(groupRepository.findByOwner(owner)),
                headers["SESSION_ID"] as String
            )
        )

        logger.info("Group created successfully!")
    }

    private fun extractMembers(
        data: MutableMap<String, Any>,
        headers: MutableMap<String, String>
    ) = (data["members"] as ArrayList<Any>).filter { it != null }.map {
        val user = gson.fromJson(
            it.toString()
                .replace("=", "\":\"")
                .replace("{", "{\"")
                .replace("}", "\"}")
                .replace(", ", "\", \""),
            User::class.java
        )
        if (userRepository.findById(user.id) == null)
            throw UserNotFound(user.id, headers["EVENT"] as String)
        user
    }.toMutableSet()

    private fun handleUserUpdate(
        data: MutableMap<String, Any>,
        headers: MutableMap<String, String>
    ) {
        if (!isNecessaryData(data))
            throw NecessaryDataMissing()

        val user = userRepository.findById(data["id"] as String) ?: throw UserNotFound(
            data["id"] as String,
            headers["EVENT"] as String
        )

        user.username = data["username"] as String
        user.firstName = data["firstName"] as String
        user.lastName = data["lastName"] as String

        userRepository.persist(user)

        logger.info("User successfully updated.")
    }

    private fun handleNewUser(data: MutableMap<String, Any>) {
        if (!isNecessaryData(data))
            throw NecessaryDataMissing()

        if (userRepository.findById(data["id"] as String) != null)
            throw EntityAlreadyInCollection(data["id"] as String)

        userRepository.persist(
            User(
                data["id"] as String,
                data["username"] as String,
                data["firstName"] as String,
                data["lastName"] as String,
            )
        )

        logger.info("User in database ${userRepository.findById(data["id"] as String)}")
    }

    private fun isNecessaryData(data: MutableMap<String, Any>): Boolean {
        return data["id"] != null &&
                data["username"] != null &&
                data["firstName"] != null &&
                data["lastName"] != null
    }

    private fun getFailedEvent(event : String) : Event {
        return when(Event.valueOf(event)) {
            Event.CREATE_GROUP -> Event.CREATE_GROUP_FAIL
            Event.UPDATE_GROUP -> Event.UPDATE_GROUP_FAIL
            Event.ADD_MEMBERS -> Event.ADD_MEMBERS_FAIL
            Event.REMOVE_MEMBERS -> Event.REMOVE_MEMBERS_FAIL
            else -> throw RuntimeException("Unknown event")
        }
    }
}
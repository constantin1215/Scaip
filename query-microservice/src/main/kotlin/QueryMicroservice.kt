import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import entity.*
import exceptions.*
import io.smallrye.reactive.messaging.kafka.api.OutgoingKafkaRecordMetadata
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.transaction.Transactional
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.common.header.internals.RecordHeaders
import org.eclipse.microprofile.reactive.messaging.Channel
import org.eclipse.microprofile.reactive.messaging.Emitter
import org.eclipse.microprofile.reactive.messaging.Incoming
import org.eclipse.microprofile.reactive.messaging.Message
import org.jboss.logging.Logger
import repository.GroupRepository
import repository.UserRepository
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import kotlin.math.log
import kotlin.time.Duration.Companion.seconds

@ApplicationScoped
class QueryMicroservice {
    enum class Event {
        REGISTRATION_SUCCESS,
        FETCH_USERS_BY_QUERY,
        FETCH_PROFILE,
        UPDATE_USER_SUCCESS,
        GROUP_CREATION_SUCCESS,
        CREATE_GROUP_SUCCESS,
        UPDATE_GROUP_SUCCESS,
        ADD_MEMBERS_SUCCESS,
        REMOVE_MEMBERS_SUCCESS,
        NEW_MESSAGE_SUCCESS,
        NEW_CALL_SUCCESS,
        FETCH_GROUP_MEMBERS,
        FETCH_GROUP
    }

    private val gson = Gson()
    private val type = object : TypeToken<Map<String, Any>>() {}.type
    private val logger : Logger = Logger.getLogger(this.javaClass)

    @Inject
    lateinit var userRepository: UserRepository

    @Inject
    lateinit var groupRepository: GroupRepository

    @Inject
    @Channel("gateway_topic")
    lateinit var gatewayEmitter : Emitter<String>

    @Incoming("query_topic")
    @Transactional
    fun consume(msg: ConsumerRecord<String, String>) {
        var value = msg.value()
        if (msg.value().startsWith("[")) {
            value = "$value}"
            value = "{\"members\":$value"
        }
        logger.info(value)

        val data = gson.fromJson(value, type) as MutableMap<String, Any>
        val headers = msg.headers().associate { it.key() to it.value().toString(Charsets.UTF_8) }.toMutableMap()
        logger.info("Received msg: $headers and $data")
        try {
            when(Event.valueOf(headers["EVENT"] as String)) {
                Event.REGISTRATION_SUCCESS -> {
                    logger.info("Adding new user.")
                    handleNewUser(data)
                }
                Event.UPDATE_USER_SUCCESS -> {
                    logger.info("Updating existing user.")
                    handleUpdateUserProfile(data, headers)
                }
                Event.FETCH_USERS_BY_QUERY -> {
                    logger.info("Fetching users.")
                    handleQueryUsers(data, headers)
                }
                Event.FETCH_PROFILE -> {
                    logger.info("Fetching profile.")
                    handleProfileFetching(data, headers)
                }
                Event.CREATE_GROUP_SUCCESS -> {
                    logger.info("Assigning new group to users.")
                    handleNewGroup(data, headers)
                }
                Event.UPDATE_GROUP_SUCCESS -> {
                    logger.info("Updating info for group.")
                    handleGroupUpdate(data, headers)
                }
                Event.ADD_MEMBERS_SUCCESS -> {
                    logger.info("Assign users to existing group.")
                    handleNewMembersInGroup(data, headers)
                }
                Event.REMOVE_MEMBERS_SUCCESS -> {
                    logger.info("Removing users from existing group.")
                    handleMemberRemovalFromGroup(data, headers)
                }
                Event.NEW_MESSAGE_SUCCESS -> {
                    logger.info("Updating last message of group.")
                    handleNewMessage(data, headers)
                }
                Event.NEW_CALL_SUCCESS -> {
                    logger.info("Adding new call to group.")
                    handleNewCall(data, headers)
                }
                Event.FETCH_GROUP_MEMBERS -> {
                    logger.info("Fetching group members.")
                    handleMembersFetching(data, headers)
                }
                Event.FETCH_GROUP -> {
                    logger.info("Fetch group info.")
                    handleGroupFetching(data, headers)
                }
                else -> { println("TO DO() handle ${headers["EVENT"] as String}") }
            }
        } catch (ex : EntityAlreadyInCollection) {
            logger.warn("Entity with ID: ${ex.entityId} already in collection")
        } catch (ex : NecessaryDataMissing) {
            logger.warn("Event with missing data received")
        } catch (ex : UserProfileNotFound) {
            logger.warn("User with ID: ${ex.userId} not found while handling EVENT: ${ex.event}")
        } catch (ex : GroupNotFound) {
            logger.warn("User with ID: ${ex.groupId} not found while handling EVENT: ${ex.event}")
        } catch (ex : IllegalArgumentException) {
            logger.warn("Unknown event possibly detected!")
        }
    }

    private fun handleGroupFetching(data: MutableMap<String, Any>, headers: MutableMap<String, String>) {
        if((data["groupId"] as String?) == null)
            return

        val result = groupRepository.fetchGroupDetails(data["groupId"] as String)

        val newMsg = Message
            .of(result)
            .addMetadata(
                OutgoingKafkaRecordMetadata.builder<String>()
                    .withHeaders(createHeaders(headers)).build()
            )

        gatewayEmitter.send(newMsg)

        logger.info("Group fetched successfully.")
    }

    private fun handleMembersFetching(data: MutableMap<String, Any>, headers: MutableMap<String, String>) {
        if((data["groupId"] as String?) == null)
            return

        val result = groupRepository.fetchMembers(data["groupId"] as String)

        val newMsg = Message
            .of(result)
            .addMetadata(
                OutgoingKafkaRecordMetadata.builder<String>()
                    .withHeaders(createHeaders(headers)).build()
            )

        gatewayEmitter.send(newMsg)

        logger.info("Group members fetched successfully.")
    }

    private fun handleNewCall(data: MutableMap<String, Any>, headers: MutableMap<String, String>) {
        val group = groupRepository.findById((data["groupId"] as String?) ?: "-1")
            ?: throw GroupNotFound(data["groupId"] as String? ?: "-1", headers["EVENT"] as String)

        val type = CallType.valueOf(data["type"] as String)

        var call : Call
        when(type) {
            CallType.INSTANT -> {
                call = Call(
                        data["_id"] as String,
                        data["leaderId"] as String,
                        type,
                        (data["timestamp"] as MutableMap<String, String>)["\$numberLong"]!!.toLong()
                    )
            }
            CallType.SCHEDULED -> {
                val seconds = ((data["scheduledTime"] as MutableMap<String, Any>)["\$date"]!! as Double).toLong()
                val date = ZonedDateTime.ofInstant(Instant.ofEpochMilli(seconds), ZoneId.systemDefault()).toLocalDateTime()

                call = Call(
                    data["_id"] as String,
                    data["leaderId"] as String,
                    type,
                    date,
                    (data["timestamp"] as MutableMap<String, String>)["\$numberLong"]!!.toLong(),
                )
            }
        }

        group.calls.add(call)

        groupRepository.update(group)

        logger.info("New call added to group successfully.")
    }

    private fun handleNewMessage(data: MutableMap<String, Any>, headers: MutableMap<String, String>) {
        if((data["groupId"] as String?) == null)
            return

        val group = groupRepository.findById(data["groupId"] as String)
            ?: throw GroupNotFound(data["groupId"] as String, headers["EVENT"] as String)

        val timestamp = (data["timestamp"] as MutableMap<String, String>)["\$numberLong"]!!.toLong()

        val message = entity.Message(
            data["_id"] as String,
            data["userId"] as String,
            data["content"] as String,
            data["groupId"] as String,
            timestamp
        )

        groupRepository.updateGroupLastMessage(data["groupId"] as String, message)

        userRepository.updateUserGroupsLastMessage(data["groupId"] as String, message)

        logger.info("Last message in group updated.")
    }

    private fun handleMemberRemovalFromGroup(data: MutableMap<String, Any>, headers: MutableMap<String, String>) {
        val group = groupRepository.findById(data["groupId"] as String)
            ?: throw GroupNotFound(data["groupId"] as String, headers["EVENT"] as String)

        val membersId = (data["members"] as ArrayList<Map<String, String>>)
            .map {
                val user = userRepository.findById(it["id"]!!)
                    ?: throw UserProfileNotFound(it["id"]!!, headers["EVENT"] as String)
                user.groups.removeIf { userGroup -> userGroup.id == group.id }
                userRepository.update(user)
                user.id
            }.toSet()

        group.members.removeIf { it.id in membersId }

        groupRepository.update(group)

        logger.info("Member removed successfully from group.")
    }

    private fun handleNewMembersInGroup(data: MutableMap<String, Any>, headers: MutableMap<String, String>) {
        val group = groupRepository.findById(data["groupId"] as String)
            ?: throw GroupNotFound(data["groupId"] as String, headers["EVENT"] as String)

        val members = (data["members"] as ArrayList<Map<String, String>>)
            .map {
                val user = userRepository.findById(it["id"]!!)
                    ?: throw UserProfileNotFound(it["id"]!!, headers["EVENT"] as String)
                if (user.groups.find { userGroup -> userGroup.id == group.id } == null) {
                    user.groups.add(GroupSummary(group))
                    userRepository.update(user)
                }
                UserSummary(user)
            }.toSet()

        members.forEach {
            if (group.members.find { member -> member.id == it.id } == null)
                group.members.add(it)
        }

        groupRepository.update(group)

        logger.info("Member added successfully to group.")
    }

    private fun handleGroupUpdate(data: MutableMap<String, Any>, headers: MutableMap<String, String>) {
        val group = groupRepository.findById(data["id"] as String)
            ?: throw GroupNotFound(data["id"] as String, headers["EVENT"] as String)

        group.title = data["title"] as String
        group.description = data["description"] as String

        groupRepository.update(group)

        logger.info("Group info updated successfully.")
    }

    private fun handleNewGroup(data: MutableMap<String, Any>, headers: MutableMap<String, String>) {
        if (groupRepository.findById(data["id"] as String) != null)
            throw EntityAlreadyInCollection(data["id"] as String)

        val ownerId = (data["owner"] as Map<String, String>)["id"]
        val owner = userRepository.findGroupMemberById(ownerId!!)
            ?: throw UserProfileNotFound(ownerId, headers["EVENT"] as String)

        val members = (data["members"] as ArrayList<Map<String, String>>)
            .map {
                val user = userRepository.findById(it["id"]!!)
                    ?: throw UserProfileNotFound(it["id"]!!, headers["EVENT"] as String)
                user
            }.toSet()

        val newGroup = Group(
            data["id"] as String,
            owner,
            data["title"] as String,
            data["description"] as String,
            members.map { UserSummary(it) }.toMutableSet(),
            mutableListOf()
        )

        groupRepository.persist(newGroup)

        members.forEach {
            it.groups.add(GroupSummary(newGroup))
            userRepository.update(it)
        }

        logger.info("New group added successfully.")
    }

    private fun handleProfileFetching(
        data: MutableMap<String, Any>,
        headers: MutableMap<String, String>
    ) {
        val result = userRepository.findById(data["userId"] as String) ?: mapOf("message" to "Profile not found!")

        val newMsg = Message
            .of(gson.toJson(result))
            .addMetadata(
                OutgoingKafkaRecordMetadata.builder<String>()
                    .withHeaders(createHeaders(headers)).build()
            )

        gatewayEmitter.send(newMsg)

        logger.info("Profile queried successfully.")
    }

    private fun handleQueryUsers(
        data: MutableMap<String, Any>,
        headers: MutableMap<String, String>
    ) {
        val users = userRepository.findByQuery(data["query"] as String)

        val newMsg = Message
            .of(gson.toJson(users))
            .addMetadata(
                OutgoingKafkaRecordMetadata.builder<String>()
                    .withHeaders(createHeaders(headers)).build()
            )

        gatewayEmitter.send(newMsg)

        logger.info("Users queried successfully.")
    }

    private fun createHeaders(headers: MutableMap<String, String>): RecordHeaders {
        val newHeaders = RecordHeaders()
        headers.forEach { newHeaders.add(it.key, it.value.encodeToByteArray()) }
        return newHeaders
    }

    private fun handleUpdateUserProfile(
        data: MutableMap<String, Any>,
        headers: MutableMap<String, String>
    ) {
        if (!isNecessaryData(data))
            throw NecessaryDataMissing()

        val user = userRepository.findById(data["id"] as String) ?: throw UserProfileNotFound(
            data["id"] as String,
            headers["EVENT"] as String
        )

        user.username = data["username"] as String
        user.email = data["email"] as String
        user.firstName = data["firstName"] as String
        user.lastName = data["lastName"] as String

        userRepository.update(user)

        logger.info("New user updated successfully.")
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
                data["email"] as String,
                data["firstName"] as String,
                data["lastName"] as String,
                mutableSetOf(),
                mutableSetOf()
            )
        )

        logger.info("New user added successfully.")
    }

    private fun isNecessaryData(data: MutableMap<String, Any>): Boolean {
        return data["id"] != null &&
                data["username"] != null &&
                data["email"] != null &&
                data["firstName"] != null &&
                data["lastName"] != null
    }
}
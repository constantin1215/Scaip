import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import entity.Group
import entity.Message
import entity.MessageEvent
import entity.User
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
import org.jboss.logging.Logger
import repository.GroupRepository
import repository.OutboxRepository
import repository.UserRepository

@ApplicationScoped
class MessagingMicroservice {
    enum class Event {
        REGISTRATION_SUCCESS,
        CREATE_GROUP_SUCCESS,
        UPDATE_GROUP_SUCCESS,
        ADD_MEMBERS_SUCCESS,
        REMOVE_MEMBERS_SUCCESS,
        NEW_MESSAGE,
        NEW_MESSAGE_SUCCESS,
        NEW_MESSAGE_FAIL,
        FETCH_MESSAGES,
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

    @Inject
    @Channel("gateway_topic")
    lateinit var gatewayEmitter : Emitter<String>

    @Incoming("message_topic")
    @Transactional
    fun consume(msg: ConsumerRecord<String, String>) {
        try {
            val data = gson.fromJson(msg.value(), type) as MutableMap<String, Any>
            val headers = msg.headers().associate { it.key() to it.value().toString(Charsets.UTF_8) }.toMutableMap()
            logger.info("Received msg: $headers and $data")
                when(Event.valueOf(headers["EVENT"] as String)) {
                    Event.REGISTRATION_SUCCESS -> {
                        logger.info("Adding new user.")
                        handleNewUser(data)
                    }
                    Event.CREATE_GROUP_SUCCESS -> {
                        logger.info("Adding new group.")
                        handleNewGroup(data, headers)
                    }
                    Event.ADD_MEMBERS_SUCCESS -> {
                        logger.info("Adding new members to group.")
                        handleAddNewMembers(data, headers)
                    }
                    Event.REMOVE_MEMBERS_SUCCESS -> {
                        logger.info("Removing members from group.")
                        handleMemberRemoval(data, headers)
                    }
                    Event.NEW_MESSAGE -> {
                        logger.info("Adding to message to conversation.")
                        handleNewMessage(data, headers)
                    }
                    Event.FETCH_MESSAGES -> {
                        logger.info("Fetching messages from conversation.")
                        handleMessageFetching(data, headers)
                    }
                    else -> { println("TO DO() handle ${headers["EVENT"] as String}") }
                }
        } catch (ex : EntityAlreadyInCollection) {
            logger.warn("Entity with ID: ${ex.entityId} already in collection")
        } catch (ex : NecessaryDataMissing) {
            logger.warn("Event with missing data received")
        } catch (ex : UserNotFound) {
            logger.warn("User with ID: ${ex.userId} not found while handling EVENT: ${ex.event}")
        } catch (ex : GroupNotFound) {
            logger.warn("Group with ID: ${ex.groupId} not found while handling EVENT: ${ex.event}")
        } catch (ex : Exception) {
            logger.warn("An exception has occured! ${ex.message}")
        }
    }

    private fun handleMessageFetching(data: MutableMap<String, Any>, headers: MutableMap<String, String>) {

        val timestamp = (data["timestamp"] as String).toLong()

        val messagesJson = groupRepository.fetchMessagesFromGroup(data["groupId"] as String, timestamp)

        val newMsg = org.eclipse.microprofile.reactive.messaging.Message
            .of(messagesJson)
            .addMetadata(
                OutgoingKafkaRecordMetadata.builder<String>()
                    .withHeaders(createHeaders(headers)).build()
            )

        gatewayEmitter.send(newMsg)

        logger.info("Messages fetched successfully.")
    }

    private fun createHeaders(headers: MutableMap<String, String>): RecordHeaders {
        val newHeaders = RecordHeaders()
        headers.forEach { newHeaders.add(it.key, it.value.encodeToByteArray()) }
        return newHeaders
    }

    private fun handleNewMessage(data: MutableMap<String, Any>, headers: MutableMap<String, String>) {
        val group = groupRepository.findById(data["groupId"] as String)
            ?: throw GroupNotFound(data["groupId"] as String, headers["EVENT"] as String)

        val userId = data["userId"] as String
        if (userId !in group.members)
            throw Unauthorized("The user $userId is not in this group", headers["EVENT"] as String)

        val user = userRepository.findById(userId) ?: throw UserNotFound(userId, headers["EVENT"] as String)

        val newMessage = Message(user, data["content"] as String)
        group.lastMessage = newMessage
        group.messages.add(newMessage)

        groupRepository.update(group)

        newMessage.groupId = group.id
        outboxRepository.persist(
            MessageEvent(
                headers["EVENT"] as String,
                Event.NEW_MESSAGE_SUCCESS.toString(),
                newMessage,
                headers["SESSION_ID"] as String
            )
        )

        logger.info("Message successfully added to conversation.")
    }

    private fun handleMemberRemoval(data: MutableMap<String, Any>, headers: MutableMap<String, String>) {
        val (group, membersId) = findGroupAndExtractMembers(data, headers)

        group.members.removeIf { it in membersId }
        groupRepository.update(group)

        logger.info("Members removed successfully from group.")
    }

    private fun handleAddNewMembers(data: MutableMap<String, Any>, headers: MutableMap<String, String>) {
        val (group, membersId) = findGroupAndExtractMembers(data, headers)

        group.members.addAll(membersId)
        groupRepository.update(group)

        logger.info("Members added successfully to group.")
    }

    private fun handleNewGroup(data: MutableMap<String, Any>, headers: MutableMap<String, String>) {
        if (groupRepository.findById(data["id"] as String) != null)
            throw EntityAlreadyInCollection(data["id"] as String)

        val ownerId = (data["owner"] as Map<String, String>)["id"]!!

        if (userRepository.findById(ownerId) == null)
            throw UserNotFound(ownerId, headers["EVENT"] as String)

        val membersId = (data["members"] as ArrayList<Map<String, String>>)
            .map {
                val user = userRepository.findById(it["id"] as String)
                    ?: throw UserNotFound(it["id"] as String, headers["EVENT"] as String)
                user.id
            }
            .toMutableSet()

        val group = Group(
            data["id"] as String,
            ownerId,
            membersId,
            mutableListOf()
        )

        groupRepository.persist(group)

        logger.info("Group added successfully.")
    }

    private fun handleNewUser(data: MutableMap<String, Any>) {
        if (!isNecessaryData(data))
            throw NecessaryDataMissing()

        if (userRepository.findById(data["id"] as String) != null)
            throw EntityAlreadyInCollection(data["id"] as String)

        userRepository.persist(
            User(data["id"] as String, data["username"] as String)
        )

        logger.info("User added successfully.")
    }

    private fun findGroupAndExtractMembers(
        data: MutableMap<String, Any>,
        headers: MutableMap<String, String>
    ): Pair<Group, Set<String>> {
        val group = groupRepository.findById(data["groupId"] as String)
            ?: throw GroupNotFound(data["groupId"] as String, headers["EVENT"] as String)

        val membersId = (data["members"] as ArrayList<Map<String, String>>)
            .map {
                val user = userRepository.findById(it["id"]!!)
                    ?: throw UserNotFound(it["id"]!!, headers["EVENT"] as String)
                user.id
            }.toSet()
        return Pair(group, membersId)
    }
    private fun isNecessaryData(data: MutableMap<String, Any>): Boolean {
        return data["id"] != null && data["username"] != null
    }
}
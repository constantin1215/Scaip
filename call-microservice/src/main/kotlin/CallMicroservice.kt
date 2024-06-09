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
import repository.OutboxRepository
import repository.UserRepository
import java.security.MessageDigest
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import kotlin.math.log

@ApplicationScoped
class CallMicroservice {

    enum class Event {
        REGISTRATION_SUCCESS,
        CREATE_GROUP_SUCCESS,
        UPDATE_GROUP_SUCCESS,
        ADD_MEMBERS_SUCCESS,
        REMOVE_MEMBERS_SUCCESS,
        NEW_CALL,
        NEW_CALL_SUCCESS,
        NEW_CALL_FAIL,
        JOIN_CALL,
        UNAUTHORIZED,
        CALL_FINISHED,
        JOINED_VIDEO,
        LEFT_VIDEO,
        JOINED_AUDIO,
        LEFT_AUDIO
    }

    private val gson = Gson()
    private val type = object : TypeToken<Map<String, Any>>() {}.type
    private val logger : Logger = Logger.getLogger(this.javaClass)
    private val md = MessageDigest.getInstance("SHA-256")

    @Inject
    lateinit var userRepository: UserRepository

    @Inject
    lateinit var groupRepository: GroupRepository

    @Inject
    lateinit var outboxRepository: OutboxRepository

    @Inject
    @Channel("gateway_topic")
    lateinit var gatewayEmitter : Emitter<String>

    @Incoming("call_topic")
    @Transactional
    fun consume(msg: ConsumerRecord<String, String>) {
        var data : MutableMap<String, Any>
        try {
            data = gson.fromJson(msg.value(), type) as MutableMap<String, Any>
        } catch (ex : Exception) {
            return
        }
        val headers = msg.headers().associate { it.key() to it.value().toString(Charsets.UTF_8) }.toMutableMap()
        logger.info("Received msg: $headers and $data")

        try {
            when (Event.valueOf(headers["EVENT"] as String)) {
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
                Event.NEW_CALL -> {
                    logger.info("Initiating new call.")
                    handleNewCall(data, headers)
                }
                Event.JOIN_CALL -> {
                    logger.info("User wants to join call.")
                    handleJoinCall(data, headers)
                }
                Event.JOINED_VIDEO,
                Event.LEFT_VIDEO,
                Event.JOINED_AUDIO,
                Event.LEFT_AUDIO -> {
                    logger.info("Handling call member count change.")
                    handleMemberCountChange(data, headers)
                }
                else -> {
                    println("TO DO() handle ${headers["EVENT"] as String}")
                }
            }
        } catch (ex : EntityAlreadyInCollection) {
            logger.warn("Entity with ID: ${ex.entityId} already in collection")
        } catch (ex : NecessaryDataMissing) {
            logger.warn("Event with missing data received")
        } catch (ex : UserNotFound) {
            logger.warn("User with ID: ${ex.userId} not found while handling EVENT: ${ex.event}")
        } catch (ex : GroupNotFound) {
            logger.warn("Group with ID: ${ex.groupId} not found while handling EVENT: ${ex.event}")
        } catch (ex : Unauthorized) {
            logger.warn(ex.message)
        }
    }

    private fun handleMemberCountChange(data: MutableMap<String, Any>, headers: MutableMap<String, String>) {
        if (data["userId"] as String? == null || data["callId"] as String? == null) {
            logger.warn("Missing fields!")
            return
        }

        val userId = data["userId"] as String
        val callId = data["callId"] as String

        when (Event.valueOf(headers["EVENT"] as String)) {
            Event.JOINED_VIDEO -> {
                logger.info("User joined call with video.")
                groupRepository.addToJoinedVideo(callId, userId)
            }
            Event.LEFT_VIDEO -> {
                logger.info("User left call with video.")
                groupRepository.removeFromJoinedVideo(callId, userId)
            }
            Event.JOINED_AUDIO -> {
                logger.info("User joined call with audio.")
                groupRepository.addToJoinedAudio(callId, userId)
            }
            Event.LEFT_AUDIO -> {
                logger.info("User left call with audio.")
                groupRepository.removeFromJoinedAudio(callId, userId)
            }
            else -> {}
        }

        if (!groupRepository.isCallEmpty(callId)) {
            logger.info("Empty call!")
            groupRepository.finishCall(callId)

            val groupId = (gson.fromJson(groupRepository.getGroupIdByCallId(callId), type) as MutableMap<String, Any>)["_id"].toString()

            outboxRepository.persist(
                CallEvent(
                    headers["EVENT"] as String,
                    Event.CALL_FINISHED.toString(),
                    Call(callId, groupId),
                    ""
                )
            )
        }
    }

    fun ByteArray.toHex(): String = joinToString(separator = "") { eachByte -> "%02x".format(eachByte) }

    private fun handleJoinCall(data: MutableMap<String, Any>, headers: MutableMap<String, String>) {
        val group = groupRepository.findById(data["groupId"] as String)
            ?: throw GroupNotFound(data["groupId"] as String, headers["EVENT"] as String)

        val userId = data["userId"] as String
        if (userId !in group.members)
            throw Unauthorized("The user $userId is not in this group", headers["EVENT"] as String)

        val channelJson = groupRepository.fetchCallChannel(group.id, data["callId"] as String)

        val newMsg = Message
            .of(channelJson)
            .addMetadata(
                OutgoingKafkaRecordMetadata.builder<String>()
                    .withHeaders(createHeaders(headers)).build()
            )

        gatewayEmitter.send(newMsg)
    }

    private fun createHeaders(headers: MutableMap<String, String>): RecordHeaders {
        val newHeaders = RecordHeaders()
        headers.forEach { newHeaders.add(it.key, it.value.encodeToByteArray()) }
        return newHeaders
    }

    private fun handleNewCall(data: MutableMap<String, Any>, headers: MutableMap<String, String>) {
        val group = groupRepository.findById(data["groupId"] as String)
            ?: throw GroupNotFound(data["groupId"] as String, headers["EVENT"] as String)

        val userId = data["userId"] as String
        if (userId !in group.members)
            throw Unauthorized("The user $userId is not in this group", headers["EVENT"] as String)

        md.update("${group.id}${Instant.now().toEpochMilli()}".toByteArray())
        val channel = md.digest().toHex()

        val callType = CallType.valueOf(data["type"] as String)

        var newCall : Call
        when (callType) {
            CallType.INSTANT -> {
                newCall = Call(
                    userId,
                    callType,
                    channel
                )
            }
            CallType.SCHEDULED -> {
                val dateTime = LocalDateTime.parse(data["date"] as String, DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")).toEpochSecond(ZoneOffset.UTC)
                val title = data["title"] as String
                newCall = Call(
                    userId,
                    callType,
                    channel,
                    dateTime,
                    title
                )
            }
        }

        group.calls.add(newCall)

        groupRepository.update(group)

        newCall.channel = ""
        newCall.groupId = group.id

        outboxRepository.persist(
            CallEvent(
                headers["EVENT"] as String,
                Event.NEW_CALL_SUCCESS.toString(),
                newCall,
                headers["SESSION_ID"] as String
            )
        )

        logger.info("Call created successfully. $newCall")
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
            User(data["id"] as String)
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
        return data["id"] != null
    }
}
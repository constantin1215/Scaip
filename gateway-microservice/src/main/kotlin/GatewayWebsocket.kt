import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import exceptions.MissingFieldException
import exceptions.SessionExpired
import exceptions.UnauthorizedAction
import io.quarkus.runtime.Shutdown
import io.quarkus.runtime.Startup
import io.smallrye.common.annotation.RunOnVirtualThread
import io.smallrye.reactive.messaging.kafka.api.OutgoingKafkaRecordMetadata
import io.vertx.core.Vertx
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.websocket.*
import jakarta.websocket.server.ServerEndpoint
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.common.header.internals.RecordHeaders
import org.eclipse.microprofile.reactive.messaging.Channel
import org.eclipse.microprofile.reactive.messaging.Emitter
import org.eclipse.microprofile.reactive.messaging.Incoming
import org.eclipse.microprofile.reactive.messaging.Message
import org.jboss.logging.Logger
import service.StorageService
import java.util.concurrent.ConcurrentHashMap
import kotlin.concurrent.thread

@ApplicationScoped
@ServerEndpoint("/gateway")
class GatewayWebsocket {
    private val gson = Gson()
    private val type = object : TypeToken<Map<String, Any>>() {}.type
    private val logger : Logger = Logger.getLogger(this.javaClass)
    private var sessions : ConcurrentHashMap<String, Session> = ConcurrentHashMap()

    companion object {
        var guestCount = 0
    }

    enum class Event {
        REGISTRATION_SUCCESS,
        REGISTRATION_FAIL,
        UPDATE_USER_SUCCESS,
        UPDATE_USER_FAIL,
        LOG_IN_SUCCESS,
        LOG_IN_FAIL,
        UNAUTHORIZED,
        FETCH_USERS_BY_QUERY,
        FETCH_PROFILE,
        FETCH_GROUP_MEMBERS,
        FETCH_GROUP,
        CREATE_GROUP_SUCCESS,
        CREATE_GROUP_FAIL,
        ADD_MEMBERS_SUCCESS,
        ADD_MEMBERS_FAIL,
        REMOVE_MEMBERS_SUCCESS,
        REMOVE_MEMBERS_FAIL,
        UPDATE_GROUP_SUCCESS,
        UPDATE_GROUP_FAIL,
        NEW_MESSAGE,
        NEW_MESSAGE_SUCCESS,
        NEW_MESSAGE_FAIL,
        FETCH_MESSAGES,
        NEW_CALL,
        NEW_CALL_SUCCESS,
        NEW_CALL_FAIL,
        JOIN_CALL,
        FETCH_CALLS,
        CALL_FINISHED
    }

    @Inject
    lateinit var storageService : StorageService

    @Inject
    @Channel("auth_topic")
    lateinit var emitter : Emitter<String>

    @Startup
    fun startup() {
        logger.info("Cleaning up Redis.")
        storageService.deleteKeys("SESSION:*")
        storageService.deleteKeys("USER:*")
    }

    @OnOpen
    fun onOpen(session: Session?) {
        logger.info("connection> ${session!!.id}")
        session.asyncRemote.sendText("guest" + ++guestCount)
        sessions[session.id] = session
        recordSession(session)
    }

    private fun recordSession(session: Session) {
        Vertx.vertx().executeBlocking { ->
            storageService.setString("SESSION:${session.id}", "guest$guestCount", 120)
        }
    }

    @OnClose
    fun onClose(session: Session?) {
        logger.info("exit> ${session!!.id}")
        sessions.remove(session.id)

        removeSession(session)
    }

    private fun removeSession(session: Session) {
        Vertx.vertx().executeBlocking { ->
            val value = storageService.getString("SESSION:${session.id}")
            if (storageService.keyExists("USER:$value"))
                storageService.deleteKey("USER:$value")
            storageService.deleteKey("SESSION:${session.id}")
        }
    }

    @OnError
    fun onError(session: Session?, throwable: Throwable) {
        logger.info("error> ${session!!.id}: ${throwable.message}")

        when(throwable) {
            is MissingFieldException -> session.asyncRemote.sendText("A mandatory field is missing")
            is UnauthorizedAction -> session.asyncRemote.sendText(throwable.message)
            else -> logger.error(throwable.printStackTrace())
        }
    }

    @OnMessage
    fun onMessage(session: Session?, message: String?) {
        //checkExpiredSession(session)
        if(message.isNullOrBlank()) {
            logger.warn("Received empty or null message.")
            return
        }

        try {
            val data = gson.fromJson(message, type) as MutableMap<String, Any>

            if(data["EVENT"] == null)
                throw MissingFieldException("The EVENT field is missing!")

            logger.info("event> ${session!!.id}: ${data["EVENT"]}")

            val headers = RecordHeaders()

            if(data["JWT"] != null)
                headers.add("JWT", data["JWT"].toString().encodeToByteArray())

            headers.add("EVENT", data["EVENT"].toString().encodeToByteArray())
            headers.add("SESSION_ID", session.id.encodeToByteArray())
            headers.add("TRACE", "GATEWAY-".encodeToByteArray())

            data.remove("EVENT")

            val msg = Message
                .of(gson.toJson(data))
                .addMetadata(OutgoingKafkaRecordMetadata.builder<String>()
                    .withHeaders(headers).build())

            emitter.send(msg)
        } catch (ex : Exception) {
            logger.warn("Exception encoutered. ${ex.message}")
        }
    }

    private fun checkExpiredSession(session: Session?) {
        Vertx.vertx().executeBlocking { ->
            if (!storageService.keyExists("SESSION:${session!!.id}")) {
                sessions[session.id]!!.asyncRemote.sendText(gson.toJson(mapOf("message" to "Session expired please reconnect!")))
                sessions[session.id]!!.close()
            }
        }
    }

    @Incoming("gateway_topic")
    fun consume(msg: ConsumerRecord<String, String>) {
        try {
            var value = msg.value()
            if (msg.value().startsWith("[")) {
                value = "$value}"
                value = "{\"members\":$value"
            }
            logger.info(value)
            val data = gson.fromJson(value, type) as MutableMap<String, Any>
            val headers = msg.headers().associate { it.key() to it.value().toString(Charsets.UTF_8) }.toMutableMap()
            logger.info("Received msg: $headers and ${msg.value()}")

            val session = headers["SESSION_ID"] as String
            handleEvent(headers, session, data, msg)
        } catch (ex: Exception) {
            logger.info("An exception occured! ${ex.message}")
        }
    }

    private fun handleEvent(
        headers: MutableMap<String, String>,
        session: String,
        data: MutableMap<String, Any>,
        msg: ConsumerRecord<String, String>
    ) {
        Vertx.vertx().executeBlocking { ->
            data["EVENT"] = headers["EVENT"] as String
            when (Event.valueOf(headers["EVENT"] as String)) {
                Event.REGISTRATION_FAIL,
                Event.LOG_IN_FAIL,
                Event.UPDATE_USER_FAIL,
                Event.ADD_MEMBERS_FAIL,
                Event.REMOVE_MEMBERS_FAIL,
                Event.CREATE_GROUP_FAIL,
                Event.UPDATE_GROUP_FAIL -> {
                    logger.info("Notifying online user of failed action.(${headers["EVENT"] as String})")
                }

                Event.REGISTRATION_SUCCESS -> {
                    logger.info("Successful registration from session $session")

                    if (sessions[session] != null)
                        sessions[session]!!.asyncRemote.sendText(gson.toJson(mapOf("EVENT" to data["EVENT"], "message" to "Successfully registered! Please log in!")))

                    return@executeBlocking
                }

                Event.UPDATE_USER_SUCCESS -> {
                    logger.info("Successful profile update from session $session")
                }

                Event.LOG_IN_SUCCESS -> {
                    logger.info("Successful log in from session $session")
                }

                Event.CREATE_GROUP_SUCCESS -> {
                    logger.info("Successful creation of group event received.")
                    storageService.addGroup(data["id"] as String)
                    val members = (data["members"] as ArrayList<Map<String, String>>).map { it["id"]!! }.toSet()
                    storageService.addToSet("GROUP:${data["id"] as String}", members)
                    if (storageService.groupExists(data["id"] as String)) {
                        logger.info("Notifying users of new group!")
                        multicastToGroup(data, data["id"] as String)
                        return@executeBlocking
                    }
                }

                Event.UPDATE_GROUP_SUCCESS -> {
                    logger.info("Successful group update from session $session")
                }

                Event.ADD_MEMBERS_SUCCESS -> {
                    logger.info("Users successfully added to group by user with session $session")
                    val members = (data["members"] as ArrayList<Map<String, String>>).map { it["id"]!! }.toSet()
                    storageService.addToSet("GROUP:${data["groupId"] as String}", members)
                    if (storageService.groupExists(data["groupId"] as String)) {
                        logger.info("Notifying users of new users in ${data["groupId"]}")
                        multicastToGroup(data, data["groupId"] as String)
                        return@executeBlocking
                    }
                }

                Event.REMOVE_MEMBERS_SUCCESS -> {
                    logger.info("Users successfully removed from group by user with session $session")
                    val members = (data["members"] as ArrayList<Map<String, String>>).map { it["id"]!! }.toSet()
                    if (storageService.groupExists(data["groupId"] as String)) {
                        logger.info("Notifying users of kicked user in ${data["groupId"]}")
                        multicastToGroup(data, data["groupId"] as String)
                    }
                    storageService.removeFromSet("GROUP:${data["groupId"] as String}", members)
                    return@executeBlocking
                }

                Event.UNAUTHORIZED -> {
                    logger.info("An action that requires authorization has failed for an user with session $session.")
                }

                Event.FETCH_USERS_BY_QUERY -> {
                    logger.info("A query result has arrived for an user with session $session.")
                }

                Event.FETCH_PROFILE -> {
                    logger.info("An user with session $session retrieved it's profile.")
                    storageService.setString("SESSION:$session", data["id"] as String, 7200)
                    storageService.setString("USER:${data["id"] as String}", session, 7200)
                }

                Event.NEW_MESSAGE_SUCCESS,
                Event.NEW_CALL_SUCCESS,
                Event.CALL_FINISHED -> {
                    logger.info("A new message/call/update has been received.")
                    if (storageService.groupExists(data["groupId"] as String)) {
                        logger.info("New message/call/update in ${data["groupId"]}")
                        multicastToGroup(data, data["groupId"] as String)
                        return@executeBlocking
                    }
                }

                Event.FETCH_MESSAGES -> {
                    logger.info("An user fetched a conversation.")
                }

                Event.JOIN_CALL -> {
                    logger.info("An user tries to join a call.")
                }

                Event.FETCH_GROUP_MEMBERS -> {
                    logger.info("An user fetched the members of a group.")
                }

                Event.FETCH_GROUP -> {
                    logger.info("An user fetched a group.")
                }

                Event.FETCH_CALLS -> {
                    logger.info("An user fetched a group calls.")
                }

                else -> {
                    logger.info("TO DO() handle other events ${headers["EVENT"] as String}")
                    //session.asyncRemote.sendText("TO DO() handle other events")

                }
            }
            if (sessions[session] != null)
                sessions[session]!!.asyncRemote.sendText(gson.toJson(data))
        }
    }

    private fun multicastToGroup(data: MutableMap<String, Any>, groupId: String) {
        val members = storageService.getSet("GROUP:$groupId")
        for (member in members) {
            val memberSession = storageService.getString("USER:$member")
            logger.info("Member session: $memberSession")
            if (memberSession != null) {
                logger.info("Sending message to $memberSession")
                sessions[memberSession]!!.asyncRemote.sendText(gson.toJson(data))
            }
        }
    }
}

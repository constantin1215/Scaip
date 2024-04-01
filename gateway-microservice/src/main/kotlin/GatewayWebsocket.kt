import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import exceptions.MissingFieldException
import exceptions.UnauthorizedAction
import io.smallrye.reactive.messaging.kafka.api.OutgoingKafkaRecordMetadata
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.websocket.*
import jakarta.websocket.server.ServerEndpoint
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
    }

    @Inject
    lateinit var storageService : StorageService

    @Inject
    @Channel("auth_topic")
    lateinit var emitter : Emitter<String>

    @OnOpen
    fun onOpen(session: Session?) {
        logger.info("connection> ${session!!.id}")
        session.asyncRemote.sendText("guest" + ++guestCount)
        sessions[session.id] = session
        storageService.setString("SESSION:${session.id}", "guest$guestCount", 120)
    }

    @OnClose
    fun onClose(session: Session?) {
        logger.info("exit> ${session!!.id}")
        sessions.remove(session.id)
        val value = storageService.getString("SESSION:${session.id}")
        if (storageService.keyExists("USER:$value"))
            storageService.deleteKey("USER:$value")
        storageService.deleteKey("SESSION:${session.id}")
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
        if(message.isNullOrBlank()) {
            logger.warn("Received empty or null message.")
            return
        }

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
    }

    @Incoming("gateway_topic")
    fun consume(msg: ConsumerRecord<String, String>) {
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

        try {
            thread {
                if (!storageService.keyExists("SESSION:$session")) {
                    sessions[session]!!.asyncRemote.sendText(gson.toJson(mapOf("message" to "Session expired please reconnect!")))
                    sessions[session]!!.close()
                    sessions.remove(session)
                    return@thread
                }
                handleEvent(headers, session, data, msg)
            }
        } catch (ex : Exception) {
            logger.info("Encountered undefined event")
        }
    }

    private fun handleEvent(
        headers: MutableMap<String, String>,
        session: String,
        data: MutableMap<String, Any>,
        msg: ConsumerRecord<String, String>
    ) {
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
            }

            Event.UPDATE_GROUP_SUCCESS -> {
                logger.info("Successful group update from session $session")
            }

            Event.ADD_MEMBERS_SUCCESS -> {
                logger.info("Users successfully added to group by user with session $session")
                val members = (data["members"] as ArrayList<Map<String, String>>).map { it["id"]!! }.toSet()
                storageService.addToSet("GROUP:${data["groupId"] as String}", members)
            }

            Event.REMOVE_MEMBERS_SUCCESS -> {
                logger.info("Users successfully removed from group by user with session $session")
                val members = (data["members"] as ArrayList<Map<String, String>>).map { it["id"]!! }.toSet()
                storageService.removeFromSet("GROUP:${data["groupId"] as String}", members)
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

            else -> {
                logger.info("TO DO() handle other events")
                //session.asyncRemote.sendText("TO DO() handle other events")

            }
        }
        sessions[session]!!.asyncRemote.sendText(msg.value())
    }
}

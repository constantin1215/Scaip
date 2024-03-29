package org.licenta

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
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
import org.licenta.exceptions.MissingFieldException
import org.licenta.exceptions.UnauthorizedAction


@ApplicationScoped
@ServerEndpoint("/gateway")
class GatewayWebsocket {
    private val gson = Gson()
    private val type = object : TypeToken<Map<String, Any>>() {}.type
    private val logger : Logger = Logger.getLogger(this.javaClass)
    private var sessions : Set<Session> = setOf()

    companion object {
        var guests = 0
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
    }

    @Inject
    @Channel("auth_topic")
    lateinit var emitter : Emitter<String>

    @OnOpen
    fun onOpen(session: Session?) {
        logger.info("connection> ${session!!.id}")
        session.asyncRemote.sendText("guest" + ++guests)
        sessions = session.openSessions
    }

    @OnClose
    fun onClose(session: Session?) {
        logger.info("exit> ${session!!.id}")
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
        if(!message.isNullOrBlank()) {
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
        else
            logger.warn("Received empty or null message.")
    }

    @Incoming("gateway_topic")
    fun consume(msg: ConsumerRecord<String, String>) {
        val headers = msg.headers().associate { it.key() to it.value().toString(Charsets.UTF_8) }.toMutableMap()
        logger.info("Received msg: $headers and ${msg.value()}")

        val session = sessions.find { it.id.equals(headers["SESSION_ID"]) }

        try {
            if (session != null) {
                when(Event.valueOf(headers["EVENT"] as String)) {
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
                        logger.info("Successful registration from session ${session.id}")
                        session.asyncRemote.sendText(gson.toJson("message" to "Registration successful! Please log in."))
                        return
                    }
                    Event.UPDATE_USER_SUCCESS -> {
                        logger.info("Successful profile update from session ${session.id}")
                    }
                    Event.LOG_IN_SUCCESS -> {
                        logger.info("Successful log in from session ${session.id}")
                    }
                    Event.CREATE_GROUP_SUCCESS -> {
                        logger.info("Successful creation of group from session ${session.id}")
                    }
                    Event.UPDATE_GROUP_SUCCESS -> {
                        logger.info("Successful group update from session ${session.id}")
                    }
                    Event.ADD_MEMBERS_SUCCESS -> {
                        logger.info("Users successfully added to group by user with session ${session.id}")
                    }
                    Event.REMOVE_MEMBERS_SUCCESS -> {
                        logger.info("Users successfully removed from group by user with session ${session.id}")
                    }
                    Event.UNAUTHORIZED -> {
                        logger.info("An action that requires authorization has failed.")
                    }
                    Event.FETCH_USERS_BY_QUERY -> {
                        logger.info("A query result has arrived for an user.")
                    }
                    Event.FETCH_PROFILE -> {
                        logger.info("An user retrieved it's profile.")
                    }
                    else -> {
                        logger.info("TO DO() handle other events")
                        //session.asyncRemote.sendText("TO DO() handle other events")
                    }
                }
                session.asyncRemote.sendText(msg.value())
            } else {
                logger.info("Session not found! TODO implement this")
                //TODO Cache response for later or ignore
            }
        } catch (ex : RuntimeException) {
            logger.info("Encountered undefined event")
        }
    }
}

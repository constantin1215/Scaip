package org.licenta

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.quarkus.runtime.Startup
import io.smallrye.reactive.messaging.kafka.OutgoingKafkaRecord
import io.smallrye.reactive.messaging.kafka.api.IncomingKafkaRecordMetadata
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
import org.licenta.actions.GuestActions
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

    enum class Events {
        REGISTRATION_SUCCESS,
        REGISTRATION_FAIL,
        UPDATE_USER_SUCCESS,
        UPDATE_USER_FAIL,
        LOG_IN_SUCCESS,
        LOG_IN_FAIL,
        UNAUTHORIZED
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
                when(Events.valueOf(headers["EVENT"] as String)) {
                    Events.REGISTRATION_FAIL, Events.UPDATE_USER_FAIL -> {
                        session.asyncRemote.sendText(msg.value())
                    }
                    Events.REGISTRATION_SUCCESS -> {
                        logger.info("Successful registration from session ${session.id}")
                        session.asyncRemote.sendText("Registration successful! Please log in.")
                    }
                    Events.UPDATE_USER_SUCCESS -> {
                        logger.info("Successful profile update from session ${session.id}")
                        val data = gson.fromJson(msg.value(), type) as MutableMap<String, Any>
                        session.asyncRemote.sendText(gson.toJson(data))
                    }
                    Events.LOG_IN_SUCCESS -> {
                        logger.info("Successful log in from session ${session.id}")
                        session.asyncRemote.sendText(msg.value())
                    }
                    Events.LOG_IN_FAIL -> {
                        logger.info("Failed log in from session ${session.id}")
                        session.asyncRemote.sendText(msg.value())
                    }
                    Events.UNAUTHORIZED -> {
                        logger.info("An action that requires authorization has failed.")
                        session.asyncRemote.sendText(msg.value())
                    }
                    else -> {
                        logger.info("TO DO() handle other events")
                        session.asyncRemote.sendText("TO DO() handle other events")
                    }
                }
            } else {
                logger.info("Session not found! TODO implement this")
                //TODO Cache response for later or ignore
            }
        } catch (ex : RuntimeException) {
            logger.info("Encountered undefined event")
        }
    }
}

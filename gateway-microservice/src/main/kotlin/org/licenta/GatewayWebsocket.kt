package org.licenta

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
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
import org.licenta.actions.GuestActions
import org.licenta.exceptions.MissingFieldException
import org.licenta.exceptions.UnauthorizedAction


@ApplicationScoped
@ServerEndpoint("/gateway")
class GatewayWebsocket {

    val gson = Gson()
    val type = object : TypeToken<Map<String, Any>>() {}.type

    companion object {
        var guests = 0
    }

    @Inject
    @Channel("auth_topic")
    lateinit var emitter : Emitter<String>

    @OnOpen
    fun onOpen(session: Session?) {
        println("connection> ${session!!.id}")


        session.asyncRemote.sendText("guest" + ++guests)
    }

    @OnClose
    fun onClose(session: Session?) {
        println("exit> ${session!!.id}")
    }

    @OnError
    fun onError(session: Session?, throwable: Throwable) {
        println("error> ${session!!.id}: ${throwable.message}")

        if (throwable is MissingFieldException)
            session.asyncRemote.sendText("A mandatory field is missing")
        else if (throwable is UnauthorizedAction)
            session.asyncRemote.sendText(throwable.message)
        else
            println(throwable.printStackTrace())
    }

    @OnMessage
    fun onMessage(session: Session?, message: String) {
        println("event> ${session!!.id}: $message")
        val data = gson.fromJson(message, type) as MutableMap<String, Any>

        if(data["EVENT"] == null)
            throw MissingFieldException("The EVENT field is missing!")

        val headers = RecordHeaders()

        if(data["JWT"] != null)
            headers.add("JWT", data["JWT"].toString().encodeToByteArray())
        else if (data["EVENT"] != GuestActions.LOG_IN.toString() && data["EVENT"] != GuestActions.REGISTER.toString())
            throw UnauthorizedAction("Cannot perform action without JWT.");

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
        println("Received msg from system: ${msg.key()} and ${msg.value()}")
    }
}

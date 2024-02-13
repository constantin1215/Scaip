package org.licenta

import io.smallrye.reactive.messaging.kafka.Record
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.websocket.*
import jakarta.websocket.server.PathParam
import jakarta.websocket.server.ServerEndpoint
import org.eclipse.microprofile.reactive.messaging.*
import java.util.concurrent.ConcurrentHashMap



@ApplicationScoped
@ServerEndpoint("/gateway/{name}")
class StartWebSocket {

    @Inject
    @Channel("auth_topic")
    lateinit var emitter : Emitter<String>

    val sessions: MutableMap<String, Session?> = ConcurrentHashMap()

    @OnOpen
    fun onOpen(session: Session?, @PathParam("name") name: String) {
        println("onOpen> $name ${session!!.id}")
        sessions[name] = session
    }

    @OnClose
    fun onClose(session: Session?, @PathParam("name") name: String) {
        println("onClose> $name")
    }

    @OnError
    fun onError(session: Session?, @PathParam("name") name: String, throwable: Throwable) {
        println("onError> $name: $throwable")
    }

    @OnMessage
    fun onMessage(message: String, @PathParam("name") name: String) {
        println("onMessage> $name: $message")

        emitter.send(Message.of(message))
    }

    @Incoming("dispatch_topic")
    fun consume(msg: Record<String, String>) {
        println("Received msg from system: ${msg.key()} and ${msg.value()}")
    }
}

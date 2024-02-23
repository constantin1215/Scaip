import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.smallrye.reactive.messaging.kafka.api.OutgoingKafkaRecordMetadata
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.common.header.internals.RecordHeaders
import org.eclipse.microprofile.reactive.messaging.Channel
import org.eclipse.microprofile.reactive.messaging.Emitter
import org.eclipse.microprofile.reactive.messaging.Incoming
import org.eclipse.microprofile.reactive.messaging.Message
import org.jboss.logging.Logger

@ApplicationScoped
class DispatchMicroservice {
    enum class Event {
        REGISTER,
        UPDATE_USER,
        REGISTRATION_SUCCESS,
        REGISTRATION_FAIL,
        UPDATE_USER_SUCCESS,
        UPDATE_USER_FAIL
    }

    private val gson = Gson()
    private val type = object : TypeToken<Map<String, Any>>() {}.type
    private val logger : Logger = Logger.getLogger(this.javaClass)

    @Inject
    @Channel("users_topic")
    lateinit var usersEmitter : Emitter<String>

    @Inject
    @Channel("gateway_topic")
    lateinit var gatewayEmitter : Emitter<String>

    @Incoming("dispatch_topic")
    fun consume(msg: ConsumerRecord<String, String>) {
        try {
            val data = gson.fromJson(msg.value(), type) as MutableMap<String, Any>
            val headers = msg.headers().associate { it.key() to it.value().toString(Charsets.UTF_8) }.toMutableMap()
            logger.info("Received msg: $headers and $data")

            headers["TRACE"] = headers["TRACE"] as String + "DISPATCH-"

            val newHeaders = RecordHeaders()
            headers.forEach { newHeaders.add(it.key, it.value.encodeToByteArray()) }

            val newMsg = Message
                .of(gson.toJson(data))
                .addMetadata(
                    OutgoingKafkaRecordMetadata.builder<String>()
                        .withHeaders(newHeaders).build())

            when(Event.valueOf(headers["EVENT"] as String)) {
                Event.REGISTER, Event.UPDATE_USER -> usersEmitter.send(newMsg)
                else -> println("TO DO()")
            }
        } catch (ex : Throwable) {
            println(ex.message)
            println(msg.value())
            println(msg.key())
        }
    }

    @Incoming("users-microservice.public.Outbox")
    fun consumeUserOutbox(msg: ConsumerRecord<String, String>) {
        val data = gson.fromJson(msg.value(), type) as MutableMap<String, Any>
        logger.info("Consume event from users-microservice Outbox\n $data")

        val parsedData = data["after"].toString()
            .split(", ")
            .associate {
            val (key, value) = it.split('=')
            key to value
        }

        val event = parsedData["generatedevent"]
        val details = parsedData["details"]
        //val payload = parsedData["data"]
        val sessionId = parsedData["sessionid"]!!.dropLast(1)
        logger.info(event)
        logger.info(details)
        //logger.info(payload)
        logger.info(sessionId)

        val newHeaders = RecordHeaders()
        newHeaders.add("SESSION_ID", sessionId.encodeToByteArray())
        newHeaders.add("EVENT", event!!.encodeToByteArray())

        when(Event.valueOf(event)) {
            Event.REGISTRATION_FAIL, Event.UPDATE_USER_FAIL -> {
                logger.info("Failed registration, update.")

                val newMsg = Message
                    .of(gson.toJson(mapOf("message" to details)))
                    .addMetadata(
                        OutgoingKafkaRecordMetadata.builder<String>()
                            .withHeaders(newHeaders).build())

                gatewayEmitter.send(newMsg)
            }
            Event.REGISTRATION_SUCCESS -> logger.info("Successful registration")
            Event.UPDATE_USER_SUCCESS -> logger.info("Successful user update")
            else -> println("TO DO() handle event ${data["generatedevent"]}")
        }
    }
}
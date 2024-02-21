import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.smallrye.reactive.messaging.kafka.Record
import io.smallrye.reactive.messaging.kafka.api.OutgoingKafkaRecordMetadata
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.common.header.internals.RecordHeaders
import org.eclipse.microprofile.reactive.messaging.Channel
import org.eclipse.microprofile.reactive.messaging.Emitter
import org.eclipse.microprofile.reactive.messaging.Incoming
import org.eclipse.microprofile.reactive.messaging.Message

@ApplicationScoped
class DispatchMicroservice {
    enum class Event {
        REGISTER,
        UPDATE_USER
    }

    val gson = Gson()
    val type = object : TypeToken<Map<String, Any>>() {}.type

    @Inject
    @Channel("users_topic")
    lateinit var usersEmitter : Emitter<String>

    @Incoming("dispatch_topic")
    fun consume(msg: ConsumerRecord<String, String>) {
        try {
            val data = gson.fromJson(msg.value(), type) as MutableMap<String, Any>
            val headers = msg.headers().associate { it.key() to it.value().toString(Charsets.UTF_8) }.toMutableMap()
            println("Received msg: $headers and $data")

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
            }
        } catch (ex : Throwable) {
            println(ex.message)
            println(msg.value())
            println(msg.key())
        }
    }
}
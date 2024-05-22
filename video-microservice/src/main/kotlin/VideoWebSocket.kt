import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.smallrye.reactive.messaging.kafka.api.OutgoingKafkaRecordMetadata
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.websocket.*
import jakarta.websocket.server.PathParam
import jakarta.websocket.server.ServerEndpoint
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.eclipse.microprofile.config.ConfigProvider
import org.eclipse.microprofile.reactive.messaging.Channel
import org.eclipse.microprofile.reactive.messaging.Emitter
import org.eclipse.microprofile.reactive.messaging.Message
import org.jboss.logging.Logger
import java.nio.ByteBuffer
import java.time.Duration
import java.util.*
import java.util.concurrent.ConcurrentHashMap


@ApplicationScoped
@ServerEndpoint("/video/{channel}/{userId}")
class VideoWebSocket {
    private val gson = Gson()
    private val type = object : TypeToken<Map<String, Any>>() {}.type
    var sessions: MutableMap<String, Session> = ConcurrentHashMap()
    var channels: MutableMap<String, MutableList<String>> = mutableMapOf()
    var consumers: MutableMap<String, Thread> = mutableMapOf()

    private val logger : Logger = Logger.getLogger(this.javaClass)
    @OnOpen
    fun onOpen(session: Session?, @PathParam("channel") channel: String, @PathParam("userId") userId: String) {
        logger.info("connection video from $userId to channel: $channel> ${session!!.id}")
        sessions[userId] = session

        if (channels[channel].isNullOrEmpty())
            channels[channel] = mutableListOf(userId)
        else
            channels[channel]!!.add(userId)
        logger.info(channels)

        if (!consumers.containsKey(channel)) {
            val properties = Properties()
            properties[ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG] = ConfigProvider.getConfig().getValue("kafka.bootstrap.servers", String::class.java)
            properties[ConsumerConfig.GROUP_ID_CONFIG] = "dynamic-topic-consumer"
            properties[ConsumerConfig.AUTO_OFFSET_RESET_CONFIG] = "latest"
            properties[ConsumerConfig.CLIENT_ID_CONFIG] = "DYNAMIC-CLIENT"
            properties[ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG] = "org.apache.kafka.common.serialization.StringDeserializer"
            properties[ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG] = "org.apache.kafka.common.serialization.ByteBufferDeserializer"

            val consumer = KafkaConsumer<String, ByteBuffer>(properties)
            consumer.subscribe(Collections.singletonList("video-$channel"))

            val consumerThread = Thread(object : Runnable {
                override fun run() {
                    while (true) {
                        val records: ConsumerRecords<String, ByteBuffer> = consumer.poll(Duration.ofSeconds(1))
                        records.forEach {
                            multicastBinary(it.value(), "", channel)
                        }
                    }
                }
            })

            consumerThread.start()

            consumers[channel] = consumerThread
        }

        multicastMessage(gson.toJson(mapOf("EVENT" to "JOINED_VIDEO", "userId" to userId)), session.id, channel)
        val members = sessions.keys.filter { it != userId }
        session.asyncRemote.sendText(gson.toJson(mapOf("EVENT" to "JOINED_VIDEO", "members" to members)))
    }

    @OnClose
    fun onClose(session: Session?, @PathParam("channel") channel: String, @PathParam("userId") userId: String) {
        logger.info("exit video from $userId to channel: $channel> ${session!!.id}")
        sessions.remove(userId)
        channels[channel]!!.remove(userId)

        if (channels[channel]!!.isEmpty())
            channels.remove(channel)

        logger.info(channels)
        multicastMessage(gson.toJson(mapOf("EVENT" to "LEFT_VIDEO", "userId" to userId)), session.id, channel)
    }

    @OnError
    fun onError(session: Session?, @PathParam("channel") channel: String, @PathParam("userId") userId: String, throwable: Throwable) {
        logger.info("error >")
        logger.warn(throwable.message)
        throwable.printStackTrace()
    }

    @Inject
    @Channel("channel-video")
    lateinit var emitter : Emitter<ByteBuffer>

    @OnMessage
    fun onMessage(session: Session?, bytes: ByteBuffer, @PathParam("channel") channel: String, @PathParam("userId") userId: String) {
        //logger.info("frame > $bytes")
        //multicastBinary(bytes, session!!.id, channel)
        val metadata: OutgoingKafkaRecordMetadata<*> = OutgoingKafkaRecordMetadata.builder<Any>()
            .withTopic("video-$channel")
            .build()

        emitter.send(Message.of(bytes).addMetadata(metadata))
    }

    private fun broadcastBytes(binary: ByteBuffer, self : String) {
        sessions.values.forEach { s ->
            if (s.id != self) {
                s.asyncRemote.sendBinary(binary) { result ->
                    if (result.exception != null) {
                        println("Unable to send message: " + result.exception)
                    }
                }
            }
        }
    }

    private fun broadcastMessage(message: String, self : String) {
        sessions.values.forEach { s ->
            if (s.id != self) {
                s.asyncRemote.sendText(message) { result ->
                    if (result.exception != null) {
                        println("Unable to send message: " + result.exception)
                    }
                }
            }
        }
    }

    private fun multicastMessage(message: String, self: String, channel: String) {
        channels[channel]?.let { ids ->
            ids.forEach {
                val s = sessions[it] ?: return@forEach

                if (s.id == self)
                    return@forEach

                s.asyncRemote.sendText(message) { result ->
                    if (result.exception != null) {
                        println("Unable to send message: " + result.exception)
                    }
                }
            }
        }
    }

    private fun multicastBinary(binary: ByteBuffer, self: String, channel: String) {
        channels[channel]?.let { ids ->
            ids.forEach {
                val s = sessions[it] ?: return@forEach

                if (s.id == self)
                    return@forEach

                s.asyncRemote.sendBinary(binary) { result ->
                    if (result.exception != null) {
                        println("Unable to send message: " + result.exception)
                    }
                }
            }
        }
    }
}
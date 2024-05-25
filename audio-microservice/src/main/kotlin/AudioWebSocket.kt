import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.smallrye.reactive.messaging.kafka.api.OutgoingKafkaRecordMetadata
import io.vertx.core.Vertx
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.websocket.*
import jakarta.websocket.server.PathParam
import jakarta.websocket.server.ServerEndpoint
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.eclipse.microprofile.config.ConfigProvider
import org.eclipse.microprofile.reactive.messaging.Channel
import org.eclipse.microprofile.reactive.messaging.Emitter
import org.eclipse.microprofile.reactive.messaging.Incoming
import org.eclipse.microprofile.reactive.messaging.Message
import org.jboss.logging.Logger
import service.StorageService
import java.nio.ByteBuffer
import java.time.Duration
import java.util.*
import java.util.concurrent.ConcurrentHashMap

@ApplicationScoped
@ServerEndpoint("/audio/{channel}/{userId}")
class AudioWebSocket {
    private val gson = Gson()
    private val type = object : TypeToken<Map<String, Any>>() {}.type
    var sessions: MutableMap<String, Session> = ConcurrentHashMap()
    var channels: MutableMap<String, MutableList<String>> = mutableMapOf()
    var consumers: MutableMap<String, Thread> = mutableMapOf()

    private val logger : Logger = Logger.getLogger(this.javaClass)

    enum class Event {
        CREATE_GROUP_SUCCESS,
        ADD_MEMBERS_SUCCESS,
        REMOVE_MEMBERS_SUCCESS,
        NEW_CALL_SUCCESS
    }

    @Inject
    lateinit var storageService : StorageService

    @Inject
    @Channel("channel-audio")
    lateinit var emitter : Emitter<ByteBuffer>

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
            consumer.subscribe(Collections.singletonList("audio-$channel"))

            val consumerThread = Thread {
                try {
                    while (true) {
                        val records: ConsumerRecords<String, ByteBuffer> = consumer.poll(Duration.ofSeconds(1))
                        records.forEach {
                            multicastBinary(it.value(), "", channel)
                        }
                    }
                } catch (err: Exception) {
                    logger.info("Audio streaming stopped for channel $channel!")
                }
            }

            consumerThread.start()

            consumers[channel] = consumerThread
        }

        multicastMessage(gson.toJson(mapOf("EVENT" to "JOINED_AUDIO", "userId" to userId)), session.id, channel)
        val members = sessions.keys.filter { it != userId }
        session.asyncRemote.sendText(gson.toJson(mapOf("EVENT" to "JOINED_AUDIO", "members" to members)))
    }

    @OnClose
    fun onClose(session: Session?, @PathParam("channel") channel: String, @PathParam("userId") userId: String) {
        logger.info("exit video from $userId to channel: $channel> ${session!!.id}")
        sessions.remove(userId)

        channels[channel]!!.remove(userId)

        if (channels[channel]!!.isEmpty()) {
            channels.remove(channel)
//            consumers[channel]!!.interrupt()
//            consumers.remove(channel)
        }

        logger.info(channels)
        multicastMessage(gson.toJson(mapOf("EVENT" to "LEFT_AUDIO", "userId" to userId)), session.id, channel)
    }

    @OnError
    fun onError(session: Session?, @PathParam("channel") channel: String, @PathParam("userId") userId: String, throwable: Throwable) {
        logger.info("error >")
        logger.warn(throwable.message)
        throwable.printStackTrace()
    }

    @OnMessage
    fun onMessage(session: Session?, bytes: ByteBuffer, @PathParam("channel") channel: String, @PathParam("userId") userId: String) {
        //logger.info("frame > $bytes")
        //multicastBinary(bytes, session!!.id, channel)

        val metadata: OutgoingKafkaRecordMetadata<*> = OutgoingKafkaRecordMetadata.builder<Any>()
            .withTopic("audio-$channel")
            .build()

        emitter.send(Message.of(bytes).addMetadata(metadata))
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

    @Incoming("audio_call_topic")
    fun consume(msg: ConsumerRecord<String, String>) {
        val data = gson.fromJson(msg.value(), type) as MutableMap<String, Any>
        val headers = msg.headers().associate { it.key() to it.value().toString(Charsets.UTF_8) }.toMutableMap()
        logger.info("Received msg: $headers and ${msg.value()}")

        handleEvent(headers, data)
    }

    private fun handleEvent(headers: MutableMap<String, String>, data: MutableMap<String, Any>) {
        Vertx.vertx().executeBlocking { ->
            when (Event.valueOf(headers["EVENT"] as String)) {
                Event.CREATE_GROUP_SUCCESS -> {
                    logger.info("Successful creation of group event received.")
                    storageService.addGroup(data["id"] as String)
                    val members = (data["members"] as ArrayList<Map<String, String>>).map { it["id"]!! }.toSet()
                    storageService.addToSet("GROUP:${data["id"] as String}", members)
                }
                Event.ADD_MEMBERS_SUCCESS -> {
                    logger.info("Users successfully added to group")
                    val members = (data["members"] as ArrayList<Map<String, String>>).map { it["id"]!! }.toSet()
                    storageService.addToSet("GROUP:${data["groupId"] as String}", members)
                }
                Event.REMOVE_MEMBERS_SUCCESS -> {
                    logger.info("Users successfully removed from group")
                    val members = (data["members"] as ArrayList<Map<String, String>>).map { it["id"]!! }.toSet()
                    storageService.removeFromSet("GROUP:${data["groupId"] as String}", members)
                }
                Event.NEW_CALL_SUCCESS -> {
                    logger.info("Handling new call")
                    logger.info(data)
                    logger.info(headers)
                }
            }
        }
    }
}
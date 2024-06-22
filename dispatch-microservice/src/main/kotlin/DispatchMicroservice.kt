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
        UPDATE_USER_FAIL,
        FETCH_USERS_BY_QUERY,
        FETCH_PROFILE,
        CREATE_GROUP,
        CREATE_GROUP_SUCCESS,
        CREATE_GROUP_FAIL,
        ADD_MEMBERS,
        ADD_MEMBERS_SUCCESS,
        ADD_MEMBERS_FAIL,
        REMOVE_MEMBERS,
        REMOVE_MEMBERS_SUCCESS,
        REMOVE_MEMBERS_FAIL,
        UPDATE_GROUP,
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
        JOIN_CALL_FAIL,
        FETCH_GROUP_MEMBERS,
        FETCH_GROUP,
        FETCH_CALLS,
        CALL_FINISHED
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

    @Inject
    @Channel("auth_topic")
    lateinit var authEmitter : Emitter<String>

    @Inject
    @Channel("query_topic")
    lateinit var queryEmitter : Emitter<String>

    @Inject
    @Channel("group_topic")
    lateinit var groupEmitter : Emitter<String>

    @Inject
    @Channel("message_topic")
    lateinit var messageEmitter : Emitter<String>

    @Inject
    @Channel("call_topic")
    lateinit var callEmitter : Emitter<String>

    @Inject
    @Channel("video_call_topic")
    lateinit var videoEmitter : Emitter<String>

    @Inject
    @Channel("audio_call_topic")
    lateinit var audioEmitter : Emitter<String>

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
                Event.REGISTER,
                Event.UPDATE_USER -> usersEmitter.send(newMsg)

                Event.FETCH_USERS_BY_QUERY,
                Event.FETCH_GROUP_MEMBERS,
                Event.FETCH_GROUP,
                Event.FETCH_CALLS,
                Event.FETCH_PROFILE -> queryEmitter.send(newMsg)

                Event.CREATE_GROUP,
                Event.UPDATE_GROUP,
                Event.ADD_MEMBERS,
                Event.REMOVE_MEMBERS -> groupEmitter.send(newMsg)

                Event.NEW_MESSAGE,
                Event.FETCH_MESSAGES -> messageEmitter.send(newMsg)

                Event.NEW_CALL,
                Event.JOIN_CALL -> callEmitter.send(newMsg)

                else -> println("TO DO()")
            }
        } catch (ex: Exception) {
            logger.warn("An exception has occured while consuming from dispatch_topic! ${ex.message}")
        } catch (ex : Throwable) {
            println(ex.message)
            println(msg.value())
            println(msg.key())
        }
    }

    @Incoming("users-microservice.public.Outbox")
    fun consumeUserOutbox(msg: ConsumerRecord<String, String>) {
        try {
            val data = gson.fromJson(msg.value(), type) as MutableMap<String, Any>
            logger.info("Consume event from users-microservice Outbox\n $data")

            val parsedData = parseData(data)

            val event = parsedData["generatedevent"]
            val details = parsedData["details"]
            val payload = parsedData["data"]
            val sessionId = parsedData["sessionid"]!!

            val newHeaders = createHeaders(sessionId, event!!)

            when (Event.valueOf(event)) {
                Event.REGISTRATION_FAIL, Event.UPDATE_USER_FAIL -> {
                    logger.info("Failed event response from users-microservice.")
                    notifyOfFailedEvent(details, newHeaders)
                }

                Event.REGISTRATION_SUCCESS -> {
                    logger.info("Successful registration event from users-microservice.")
                    notifyOfSuccessfulEvent(
                        payload,
                        newHeaders,
                        authMs = true,
                        gatewayMs = true,
                        messageMs = true,
                        queryMs = true,
                        groupMs = true,
                        callMs = true
                    )
                }

                Event.UPDATE_USER_SUCCESS -> {
                    logger.info("Successful update event from users-microservice.")
                    notifyOfSuccessfulEvent(
                        payload,
                        newHeaders,
                        authMs = true,
                        gatewayMs = true,
                        queryMs = true
                    )
                }

                else -> println("TO DO() handle event $event")
            }
        } catch (ex: Exception) {
            logger.warn("An exception has occured while consuming from Users Outbox! ${ex.message}")
        }
    }

    @Incoming("groups-microservice.public.Outbox")
    fun consumeGroupOutbox(msg: ConsumerRecord<String, String>) {
        try {
            val data = gson.fromJson(msg.value(), type) as MutableMap<String, Any>
            logger.info("Consume event from groups-microservice Outbox\n $data")

            val parsedData = parseData(data)

            val event = parsedData["generatedevent"]
            val details = parsedData["details"]
            val payload = parsedData["data"]
            val sessionId = parsedData["sessionid"]

            val newHeaders = createHeaders(sessionId!!, event!!)

            when (Event.valueOf(event)) {
                Event.CREATE_GROUP_FAIL,
                Event.UPDATE_GROUP_FAIL,
                Event.ADD_MEMBERS_FAIL,
                Event.REMOVE_MEMBERS_FAIL -> {
                    logger.info("Failed action response from groups-microservice.")
                    notifyOfFailedEvent(details, newHeaders)
                }

                Event.CREATE_GROUP_SUCCESS,
                Event.UPDATE_GROUP_SUCCESS,
                Event.ADD_MEMBERS_SUCCESS,
                Event.REMOVE_MEMBERS_SUCCESS -> {
                    notifyOfSuccessfulEvent(
                        payload,
                        newHeaders,
                        gatewayMs = true,
                        queryMs = true,
                        messageMs = true,
                        callMs = true,
                        videoMs = true,
                        audioMs = true
                    )
                }

                else -> println("TO DO() handle event $event")
            }
        } catch (ex : Exception) {
            logger.warn("An exception has occured while consuming from Groups Outbox! ${ex.message}")
        }
    }

    @Incoming("messages-microservice.messages.Outbox")
    fun consumeMessagesOutbox(msg: ConsumerRecord<String, String>) {
        try {

            logger.info("Consume event from groups-microservice Outbox")

            val (event, details, payload, sessionId) = extractData(msg);
            val newHeaders = createHeaders(sessionId, event)

            when (Event.valueOf(event)) {
                Event.NEW_MESSAGE_FAIL -> notifyOfFailedEvent(details, newHeaders)
                Event.NEW_MESSAGE_SUCCESS -> notifyOfSuccessfulEvent(
                    payload,
                    newHeaders,
                    gatewayMs = true,
                    queryMs = true
                )

                else -> println("TO DO() handle event $event")
            }
        } catch (ex : Exception) {
            logger.warn("An exception has occured while consuming from Messaging Outbox! ${ex.message}")
        }
    }

    @Incoming("calls-microservice.calls.Outbox")
    fun consumeCallsOutbox(msg: ConsumerRecord<String, String>) {
        try {
            logger.info("Consume event from groups-microservice Outbox")

            val (event, details, payload, sessionId) = extractData(msg);

            val newHeaders = createHeaders(sessionId, event)

            when (Event.valueOf(event)) {
                Event.NEW_CALL_FAIL,
                Event.JOIN_CALL_FAIL -> notifyOfFailedEvent(details, newHeaders)

                Event.CALL_FINISHED,
                Event.NEW_CALL_SUCCESS -> notifyOfSuccessfulEvent(
                    payload,
                    newHeaders,
                    gatewayMs = true,
                    queryMs = true,
                    videoMs = true,
                    audioMs = true
                )

                else -> println("TO DO() handle event $event")
            }
        } catch (ex : Exception) {
            logger.warn("An exception has occured while consuming from Calls Outbox! ${ex.message}")
        }
    }

    private fun createHeaders(
        sessionId: String,
        event: String
    ): RecordHeaders {
        val newHeaders = RecordHeaders()
        newHeaders.add("SESSION_ID", sessionId.encodeToByteArray())
        newHeaders.add("EVENT", event.encodeToByteArray())
        return newHeaders
    }

    fun extractData(msg: ConsumerRecord<String, String>) : Array<String> {
        var data = gson.fromJson(msg.value(), type) as MutableMap<String, Any>

        data = gson.fromJson(data["after"] as String, type) as MutableMap<String, Any>

        val event = data["generatedEvent"] as String
        val details = data["details"] as String? ?: ""
        val payload = gson.toJson(data["data"])
        val sessionId = data["sessionId"] as String

        logger.info(event)
        logger.info(details)
        logger.info(payload)
        logger.info(sessionId)

        return arrayOf(event, details, payload, sessionId)
    }

    private fun notifyOfFailedEvent(details: String?, newHeaders: RecordHeaders) {
        val newMsg = Message
            .of(gson.toJson(mapOf("message" to details)))
            .addMetadata(
                OutgoingKafkaRecordMetadata.builder<String>()
                    .withHeaders(newHeaders).build()
            )

        gatewayEmitter.send(newMsg)
    }

    private fun parseData(data: MutableMap<String, Any>): Map<String, String> {

        //TODO DELETE THIS SHIT
        val parsedData = data["after"].toString()
            .split(", ")
            .associate {
                val (key, value) = it.split('=')
                key to value
            }
        return parsedData
    }

    private fun notifyOfSuccessfulEvent(
        payload: String?,
        newHeaders: RecordHeaders,
        authMs : Boolean = false,
        gatewayMs : Boolean = false,
        queryMs : Boolean = false,
        groupMs : Boolean = false,
        messageMs : Boolean = false,
        callMs : Boolean = false,
        videoMs : Boolean = false,
        audioMs : Boolean = false
    ) {
        val newMsg = Message
            .of(payload)
            .addMetadata(
                OutgoingKafkaRecordMetadata.builder<String>()
                    .withHeaders(newHeaders).build()
            )

        newMsg.withPayload(payload)

        if (authMs)
            authEmitter.send(newMsg)

        if (gatewayMs)
            gatewayEmitter.send(newMsg)

        if (queryMs)
            queryEmitter.send(newMsg)

        if (groupMs)
            groupEmitter.send(newMsg)

        if (messageMs)
            messageEmitter.send(newMsg)

        if (callMs)
            callEmitter.send(newMsg)

        if (videoMs)
            videoEmitter.send(newMsg)

        if (audioMs)
            audioEmitter.send(newMsg)

        logger.info("Dispatched to Auth: $authMs Gateway: $gatewayMs Query: $queryMs Group: $groupMs Message: $messageMs Call: $callMs Video: $videoMs Audio: $audioMs")
    }
}
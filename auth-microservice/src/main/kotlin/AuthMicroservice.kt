import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import entity.User
import exceptions.JWTNotProvided
import exceptions.UserNotFound
import io.quarkus.elytron.security.common.BcryptUtil
import io.smallrye.common.annotation.Blocking
import io.smallrye.jwt.build.Jwt
import io.smallrye.reactive.messaging.kafka.api.OutgoingKafkaRecordMetadata
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.transaction.Transactional
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.common.header.internals.RecordHeaders
import org.eclipse.microprofile.config.inject.ConfigProperty
import org.eclipse.microprofile.reactive.messaging.Channel
import org.eclipse.microprofile.reactive.messaging.Emitter
import org.eclipse.microprofile.reactive.messaging.Incoming
import org.eclipse.microprofile.reactive.messaging.Message
import org.jboss.logging.Logger
import repository.UserRepository
import java.time.Instant
import kotlin.concurrent.thread

@ApplicationScoped
class AuthMicroservice {
    enum class Event {
        LOG_IN,
        LOG_IN_SUCCESS,
        LOG_IN_FAIL,
        REGISTER,
        UPDATE_USER,
        REGISTRATION_SUCCESS,
        UPDATE_USER_SUCCESS,
        UNAUTHORIZED
    }

    private val gson = Gson()
    private val type = object : TypeToken<Map<String, Any>>() {}.type
    private val logger : Logger = Logger.getLogger(this.javaClass)

    @Inject
    @Channel("dispatch_topic")
    lateinit var dispatchEmitter : Emitter<String>

    @Inject
    @Channel("gateway_topic")
    lateinit var gatewayEmitter : Emitter<String>

    @Inject
    lateinit var userRepository: UserRepository

    @ConfigProperty(name = "jwt.issuer.hash")
    lateinit var issuer: String

    @Incoming("auth_topic")
    @Transactional
    fun consume(msg: ConsumerRecord<String, String>) {
        val headers = msg.headers().associate { it.key() to it.value().toString(Charsets.UTF_8) }.toMutableMap()

        val data = gson.fromJson(msg.value(), type) as MutableMap<String, Any>
        logger.info("Received msg: $headers and $data")

        if (headers["TRACE"] != null)
            headers["TRACE"] = headers["TRACE"] as String + "AUTH-"

        val newHeaders = RecordHeaders()
        headers.forEach { newHeaders.add(it.key, it.value.encodeToByteArray()) }

        when(Event.valueOf(headers["EVENT"] as String)) {
            Event.LOG_IN -> {
                logger.info("Performing authentication!")

                thread {
                    gatewayEmitter.send(handleLogIn(data, headers, newHeaders))
                }
            }
            Event.REGISTRATION_SUCCESS -> {
                logger.info("New user")

                logger.info(data["id"])
                logger.info(data["username"])
                logger.info(data["password"])

                userRepository.persist(User(
                    data["id"] as String,
                    data["username"] as String,
                    data["password"] as String,
                ))

                logger.info("User in database ${userRepository.findById(data["id"] as String)}")
            }
            Event.UPDATE_USER_SUCCESS -> {
                println("Update user")

                val user = userRepository.findById(data["id"] as String)

                if (user == null) {
                    logger.info("User with id ${data["id"] as String} not found")
                } else {
                    user.username = data["username"] as String
                    user.password = data["password"] as String

                    userRepository.persist(user);

                    logger.info("User with id ${user.id} successfully updated!")
                }
            }
            Event.REGISTER -> {
                logger.info("Forwarding registration event to dispatch.")

                val newMsg = Message
                    .of(gson.toJson(data))
                    .addMetadata(
                        OutgoingKafkaRecordMetadata.builder<String>()
                            .withHeaders(newHeaders).build())

                dispatchEmitter.send(newMsg)
            }
            else -> {
                logger.info("Performing authorization and forwarding to dispatch")

                try {
                    if (headers["JWT"] == null)
                        throw JWTNotProvided()

                    val jwt = headers["JWT"]

                    logger.info(jwt)

                    val newMsg = Message
                        .of(gson.toJson(data))
                        .addMetadata(
                            OutgoingKafkaRecordMetadata.builder<String>()
                                .withHeaders(newHeaders).build())

                    dispatchEmitter.send(newMsg)
                } catch (ex : JWTNotProvided) {
                    newHeaders.add("EVENT", Event.UNAUTHORIZED.toString().encodeToByteArray())
                    val newMsg = Message
                        .of(gson.toJson(mapOf("message" to "You need to be authenticated to perform ${headers["EVENT"]}")))
                        .addMetadata(
                            OutgoingKafkaRecordMetadata.builder<String>()
                                .withHeaders(newHeaders).build())
                    gatewayEmitter.send(newMsg)
                }
            }
        }
    }

    @Transactional
    fun handleLogIn(
        data: MutableMap<String, Any>,
        headers: MutableMap<String, String>,
        newHeaders: RecordHeaders
    ): Message<String>? {
        try {
            val user = userRepository.findByUsername(data["username"] as String) ?: throw UserNotFound()

            if (BcryptUtil.matches(data["password"] as String, user.password)) {
                logger.info("Account found! Generating JWT...")
                val jwt = Jwt
                    .issuer(issuer)
                    .subject(user.id)
                    .issuedAt(Instant.now())
                    .expiresAt(Instant.now().plusSeconds(7200))
                    .sign()

                logger.info(jwt)
                newHeaders.add("EVENT", Event.LOG_IN_SUCCESS.toString().encodeToByteArray())

                val newMsg = Message
                    .of(gson.toJson(mapOf("JWT" to jwt)))
                    .addMetadata(
                        OutgoingKafkaRecordMetadata.builder<String>()
                            .withHeaders(newHeaders).build()
                    )

                return newMsg
            }
            else {
                logger.info("Credentials don't match.")
                newHeaders.add("EVENT", Event.LOG_IN_FAIL.toString().encodeToByteArray())
                val newMsg = Message
                    .of(gson.toJson(mapOf("message" to "Wrong password!")))
                    .addMetadata(
                        OutgoingKafkaRecordMetadata.builder<String>()
                            .withHeaders(newHeaders).build()
                    )

                return newMsg
            }
        } catch (ex: UserNotFound) {
            logger.info("User not found!")
        }
        throw RuntimeException()
    }
}
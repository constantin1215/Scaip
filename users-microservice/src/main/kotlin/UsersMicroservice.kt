import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import entity.User
import entity.UserEvent
import exceptions.EmailAlreadyExists
import exceptions.IdNotProvided
import exceptions.UserDoesNotExist
import exceptions.UsernameAlreadyExists
import io.quarkus.elytron.security.common.BcryptUtil
import io.smallrye.reactive.messaging.kafka.Record
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.transaction.Transactional
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.eclipse.microprofile.reactive.messaging.Channel
import org.eclipse.microprofile.reactive.messaging.Emitter
import org.eclipse.microprofile.reactive.messaging.Incoming
import org.jboss.logging.Logger
import repository.OutboxRepository
import repository.UserRepository

@ApplicationScoped
class UsersMicroservice {
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
    @Channel("dispatch_topic")
    lateinit var emitter : Emitter<Record<String, String>>

    @Inject
    lateinit var userRepository: UserRepository

    @Inject
    lateinit var outboxRepository: OutboxRepository

    @Incoming("users_topic")
    @Transactional
    fun consume(msg: ConsumerRecord<String, String>) {
        val data = gson.fromJson(msg.value(), type) as MutableMap<String, Any>
        val headers = msg.headers().associate { it.key() to it.value().toString(Charsets.UTF_8) }.toMutableMap()

        headers["TRACE"] = headers["TRACE"] as String + "USERS-"
        logger.info("Received msg: ${msg.key()} and ${msg.value()}")

        try {
            if (isNecessaryData(data)) {
                when (Event.valueOf(headers["EVENT"] as String)) {
                    Event.REGISTER -> {
                        println("Performing registration")
                        logger.info("Registering user")

                        if (userRepository.existsByEmail(data["email"] as String)) {
                            logger.info("Email ${data["email"] as String} already exists")
                            throw EmailAlreadyExists()
                        }

                        if (userRepository.existsByUsername(data["username"] as String)) {
                            logger.info("Username ${data["username"] as String} already exists")
                            throw UsernameAlreadyExists()
                        }

                        userRepository.persist(
                            User(
                                data["username"] as String,
                                BcryptUtil.bcryptHash(data["password"] as String),
                                data["email"] as String,
                                data["firstName"] as String,
                                data["lastName"] as String
                            )
                        )

                        outboxRepository.persist(
                            UserEvent(
                                headers["EVENT"] as String,
                                Event.REGISTRATION_SUCCESS.toString(),
                                gson.toJson(userRepository.findByUsername(data["username"] as String)),
                                headers["SESSION_ID"] as String
                            )
                        )

                        logger.info("Registered user successfully")
                    }
                    Event.UPDATE_USER -> {
                        logger.info("Performing update")
                        if (data["id"] == null) {
                            logger.info("User ID was not provided")
                            throw IdNotProvided()
                        }

                        val user = userRepository.findById(data["id"] as String) ?: throw UserDoesNotExist()

                        if (user.email != data["email"] as String && userRepository.existsByEmail(data["email"] as String)) {
                            logger.info("Provided email already exists!")
                            throw EmailAlreadyExists()
                        }

                        if (user.username != data["username"] as String && userRepository.existsByUsername(data["username"] as String)) {
                            logger.info("Provided username already exists!")
                            throw UsernameAlreadyExists()
                        }

                        user.email = data["email"] as String
                        user.username = data["username"] as String
                        user.password = BcryptUtil.bcryptHash(data["password"] as String)
                        user.firstName = data["firstName"] as String
                        user.lastName = data["lastName"] as String

                        userRepository.persist(user)

                        outboxRepository.persist(
                            UserEvent(
                                headers["EVENT"] as String,
                                Event.UPDATE_USER_SUCCESS.toString(),
                                gson.toJson(userRepository.findById(data["id"] as String)),
                                headers["SESSION_ID"] as String
                            )
                        )

                        logger.info("User ${user.id} updated successfully!")
                    }
                    else -> logger.info("WTF is this(${headers["EVENT"]})!")
                }
            } else {
                logger.info("Data does not contain the necessary fields")
                outboxRepository.persist(UserEvent(
                    headers["EVENT"] as String,
                    getFailedEvent(headers["EVENT"] as String).toString(),
                    msg.value(),
                    "Missing or malformed fields.",
                    headers["SESSION_ID"] as String
                ))
            }
        } catch (ex : UsernameAlreadyExists) {
            outboxRepository.persist(UserEvent(
                headers["EVENT"] as String,
                getFailedEvent(headers["EVENT"] as String).toString(),
                msg.value(),
                "Username already exists.",
                headers["SESSION_ID"] as String
            ))
        } catch (ex : EmailAlreadyExists) {
            outboxRepository.persist(UserEvent(
                headers["EVENT"] as String,
                getFailedEvent(headers["EVENT"] as String).toString(),
                msg.value(),
                "Email already exists.",
                headers["SESSION_ID"] as String
            ))
        } catch (ex : UserDoesNotExist) {
            logger.info("User with provided ID was not found!")
            outboxRepository.persist(UserEvent(
                headers["EVENT"] as String,
                Event.UPDATE_USER_FAIL.toString(),
                msg.value(),
                "The user with id ${data["id"]} does not exist.",
                headers["SESSION_ID"] as String
            ))
        } catch (ex : IdNotProvided) {
            outboxRepository.persist(UserEvent(
                headers["EVENT"] as String,
                Event.UPDATE_USER_FAIL.toString(),
                msg.value(),
                "The id of the user was not provided.",
                headers["SESSION_ID"] as String
            ))
        }
    }

    private fun isNecessaryData(data: MutableMap<String, Any>): Boolean {
        return data["username"] != null &&
                data["password"] != null &&
                data["email"] != null &&
                data["firstName"] != null &&
                data["lastName"] != null
    }

    private fun getFailedEvent(event : String) : Event {
        return when(Event.valueOf(event)) {
            Event.REGISTER -> Event.REGISTRATION_FAIL
            Event.UPDATE_USER -> Event.UPDATE_USER_FAIL
            else -> throw RuntimeException("Unknown event")
        }
    }
}
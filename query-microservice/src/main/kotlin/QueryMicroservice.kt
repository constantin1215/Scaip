import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import entity.User
import entity.UserProfile
import exceptions.EntityAlreadyInCollection
import exceptions.NecessaryDataMissing
import exceptions.UserProfileNotFound
import io.quarkus.runtime.Startup
import io.smallrye.reactive.messaging.kafka.api.OutgoingKafkaRecordMetadata
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.transaction.Transactional
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.common.header.internals.RecordHeaders
import org.bson.types.ObjectId
import org.eclipse.microprofile.reactive.messaging.*
import org.jboss.logging.Logger
import repository.UserRepository

@ApplicationScoped
class QueryMicroservice {
    enum class Event {
        REGISTRATION_SUCCESS,
        FETCH_USERS_BY_QUERY,
        FETCH_PROFILE,
        UPDATE_USER_SUCCESS,
        GROUP_CREATION_SUCCESS,
    }

    private val gson = Gson()
    private val type = object : TypeToken<Map<String, Any>>() {}.type
    private val logger : Logger = Logger.getLogger(this.javaClass)

    @Inject
    lateinit var userRepository: UserRepository

    @Inject
    @Channel("gateway_topic")
    lateinit var gatewayEmitter : Emitter<String>

    @Incoming("query_topic")
//    @Transactional
    fun consume(msg: ConsumerRecord<String, String>) {
        val data = gson.fromJson(msg.value(), type) as MutableMap<String, Any>
        val headers = msg.headers().associate { it.key() to it.value().toString(Charsets.UTF_8) }.toMutableMap()
        logger.info("Received msg: $headers and $data")

        try {
            when(Event.valueOf(headers["EVENT"] as String)) {
                Event.REGISTRATION_SUCCESS -> {
                    logger.info("Adding new user")
                    handleNewUser(data)
                }
                Event.UPDATE_USER_SUCCESS -> {
                    logger.info("Updating existing user")
                    handleUpdateUserProfile(data, headers)
                }
                Event.FETCH_USERS_BY_QUERY -> {
                    logger.info("Fetching users")
                    handleQueryUsers(data, headers)
                }
                Event.FETCH_PROFILE -> {
                    logger.info("Fetching profile")
                    handleProfileFetching(data, headers)
                }
                else -> { println("TO DO()") }
            }
        } catch (ex : EntityAlreadyInCollection) {
            logger.warn("Entity with ID: ${ex.entityId} already in collection")
        } catch (ex : NecessaryDataMissing) {
            logger.warn("Event with missing data received")
        } catch (ex : UserProfileNotFound) {
            logger.warn("User with ID: ${ex.userId} not found while handling EVENT: ${ex.event}")
        } catch (ex : IllegalArgumentException) {
            logger.warn("Unknown event possibly detected!")
        }
    }

    private fun handleProfileFetching(
        data: MutableMap<String, Any>,
        headers: MutableMap<String, String>
    ) {
        var result = userRepository.findById(data["userId"] as String) ?: mapOf("message" to "Profile not found!")

        if (result is User)
            result = UserProfile(result)

        val newMsg = Message
            .of(gson.toJson(result))
            .addMetadata(
                OutgoingKafkaRecordMetadata.builder<String>()
                    .withHeaders(createHeaders(headers)).build()
            )

        gatewayEmitter.send(newMsg)
    }

    private fun handleQueryUsers(
        data: MutableMap<String, Any>,
        headers: MutableMap<String, String>
    ) {
        val users = userRepository.findByQuery(data["query"] as String)

        val newMsg = Message
            .of(gson.toJson(users))
            .addMetadata(
                OutgoingKafkaRecordMetadata.builder<String>()
                    .withHeaders(createHeaders(headers)).build()
            )

        gatewayEmitter.send(newMsg)
    }

    private fun createHeaders(headers: MutableMap<String, String>): RecordHeaders {
        val newHeaders = RecordHeaders()
        headers.forEach { newHeaders.add(it.key, it.value.encodeToByteArray()) }
        return newHeaders
    }

    private fun handleUpdateUserProfile(
        data: MutableMap<String, Any>,
        headers: MutableMap<String, String>
    ) {
        if (!isNecessaryData(data))
            throw NecessaryDataMissing()

        val user = userRepository.findById(data["id"] as String) ?: throw UserProfileNotFound(
            data["id"] as String,
            headers["EVENT"] as String
        )

        user.username = data["username"] as String
        user.email = data["email"] as String
        user.firstName = data["firstName"] as String
        user.lastName = data["lastName"] as String

        userRepository.update(user)
    }

    private fun handleNewUser(data: MutableMap<String, Any>) {
        if (!isNecessaryData(data))
            throw NecessaryDataMissing()

        if (userRepository.findById(data["id"] as String) != null)
            throw EntityAlreadyInCollection(data["id"] as String)

        userRepository.persist(
            User(
                data["id"] as String,
                data["username"] as String,
                data["email"] as String,
                data["firstName"] as String,
                data["lastName"] as String,
                "",
                setOf(),
                setOf(),
                setOf()
            )
        )
    }

    private fun isNecessaryData(data: MutableMap<String, Any>): Boolean {
        return data["id"] != null &&
                data["username"] != null &&
                data["email"] != null &&
                data["firstName"] != null &&
                data["lastName"] != null
    }
}
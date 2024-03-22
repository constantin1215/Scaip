import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import entity.User
import exceptions.EntityAlreadyInCollection
import exceptions.NecessaryDataMissing
import exceptions.UserNotFound
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.transaction.Transactional
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.eclipse.microprofile.reactive.messaging.Incoming
import org.jboss.logging.Logger
import repository.UserRepository

@ApplicationScoped
class GroupsMicroservice {

    enum class Event {
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
        REGISTRATION_SUCCESS,
        UPDATE_USER_SUCCESS
    }

    private val gson = Gson()
    private val type = object : TypeToken<Map<String, Any>>() {}.type
    private val logger : Logger = Logger.getLogger(this.javaClass)

    @Inject
    lateinit var userRepository: UserRepository

    @Incoming("group_topic")
    @Transactional
    fun consume(msg: ConsumerRecord<String, String>) {
        val headers = msg.headers().associate { it.key() to it.value().toString(Charsets.UTF_8) }.toMutableMap()
        val data = gson.fromJson(msg.value(), type) as MutableMap<String, Any>
        logger.info("Received msg: $headers and $data")

        try {
            when(Event.valueOf(headers["EVENT"] as String)) {
                Event.CREATE_GROUP -> {
                    logger.info("Creating group.")
                }
                Event.UPDATE_GROUP -> {
                    logger.info("Updating group info.")
                }
                Event.ADD_MEMBERS -> {
                    logger.info("Adding members to group.")
                }
                Event.REMOVE_MEMBERS -> {
                    logger.info("Removing members from group.")
                }
                Event.REGISTRATION_SUCCESS -> {
                    logger.info("Adding new user.")
                    handleNewUser(data)
                }
                Event.UPDATE_USER_SUCCESS -> {
                    logger.info("Updating user.")
                    handleUserUpdate(data, headers)
                }
                else -> {
                    logger.info("TO DO() handle other events")
                }
            }
        } catch (ex : EntityAlreadyInCollection) {
            logger.warn("Entity with ID: ${ex.entityId} already in collection")
        } catch (ex : NecessaryDataMissing) {
            logger.warn("Event with missing data received")
        } catch (ex : UserNotFound) {
            logger.warn("User with ID: ${ex.userId} not found while handling EVENT: ${ex.event}")
        }  catch (ex : IllegalArgumentException) {
            logger.warn("Unknown event possibly detected!")
        }
    }

    private fun handleUserUpdate(
        data: MutableMap<String, Any>,
        headers: MutableMap<String, String>
    ) {
        if (!isNecessaryData(data))
            throw NecessaryDataMissing()

        val user = userRepository.findById(data["id"] as String) ?: throw UserNotFound(
            data["id"] as String,
            headers["EVENT"] as String
        )

        user.username = data["username"] as String
        user.firstName = data["firstName"] as String
        user.lastName = data["lastName"] as String

        userRepository.persist(user)

        logger.info("User successfully updated.")
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
                data["firstName"] as String,
                data["lastName"] as String,
            )
        )

        logger.info("User in database ${userRepository.findById(data["id"] as String)}")
    }

    private fun isNecessaryData(data: MutableMap<String, Any>): Boolean {
        return data["id"] != null &&
                data["username"] != null &&
                data["firstName"] != null &&
                data["lastName"] != null
    }
}
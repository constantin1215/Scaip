import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import jakarta.enterprise.context.ApplicationScoped
import jakarta.websocket.*
import jakarta.websocket.server.PathParam
import jakarta.websocket.server.ServerEndpoint
import org.jboss.logging.Logger
import java.nio.ByteBuffer
import java.util.concurrent.ConcurrentHashMap




@ApplicationScoped
@ServerEndpoint("/video/{channel}/{userId}")
class VideoWebSocket {
    private val gson = Gson()
    private val type = object : TypeToken<Map<String, Any>>() {}.type
    var sessions: MutableMap<String, Session> = ConcurrentHashMap()

    private val logger : Logger = Logger.getLogger(this.javaClass)
    @OnOpen
    fun onOpen(session: Session?, @PathParam("channel") channel: String, @PathParam("userId") userId: String) {
        logger.info("connection video from $userId to channel: $channel> ${session!!.id}")
        sessions[userId] = session
        broadcastMessage(gson.toJson(mapOf("EVENT" to "JOINED_VIDEO", "userId" to userId)), session.id)
        val members = sessions.keys.filter { it != userId }
        session.asyncRemote.sendText(gson.toJson(mapOf("EVENT" to "JOINED_VIDEO", "members" to members)))
    }

    @OnClose
    fun onClose(session: Session?, @PathParam("channel") channel: String, @PathParam("userId") userId: String) {
        logger.info("exit video from $userId to channel: $channel> ${session!!.id}")
        sessions.remove(userId)
        broadcastMessage(gson.toJson(mapOf("EVENT" to "LEFT_VIDEO", "userId" to userId)), session.id)
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
        broadcastBytes(bytes, session!!.id)
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
}
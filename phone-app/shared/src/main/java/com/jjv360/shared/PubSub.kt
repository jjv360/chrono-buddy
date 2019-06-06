package com.jjv360.shared

import com.github.salomonbrys.kotson.fromJson
import com.github.salomonbrys.kotson.get
import com.google.gson.Gson
import com.google.gson.JsonElement
import nl.komponents.kovenant.Deferred
import nl.komponents.kovenant.Promise
import nl.komponents.kovenant.deferred
import nl.komponents.kovenant.task
import okhttp3.*
import java.util.*
import java.util.logging.Logger

/**
 * Simple PubSub service via WebSocket.in. This is designed as a peer to peer connection between two devices, if more
 * than 2 devices join the same PubSub group things could get weird.
 */
class PubSub(val channel : String, val room : String) : WebSocketListener() {

    /** True if connected to the websocket */
    var connected = false

    /** The websocket */
    var websocket : WebSocket

    /** Pending responses */
    var pending = mutableMapOf<String, Deferred<JsonElement, Exception>>()

    /** Queued messages to send */
    val sendQueue = mutableListOf<String>()

    /** Registered handlers */
    val handlers = mutableMapOf<String, (data : JsonElement) -> Promise<Any, Exception>>()

    /** Our instance ID, used to prevent answering our own messages */
    private val instanceID = UUID.randomUUID().toString()

    /** Setup web socket */
    init {


        // Open socket
        // TODO: Use secure URL - some certificate issue
        val request = Request.Builder().url("ws://connect.websocket.in/$channel?room_id=$room").build()
        val client = OkHttpClient()
        websocket = client.newWebSocket(request, this)
        client.dispatcher().executorService().shutdown()
        Logger.getLogger("PubSub").info("Connecting to websocket ${request.url()}")

    }

    override fun onOpen(webSocket: WebSocket, response: Response) {
        super.onOpen(webSocket, response)

        // Notify socket opened
        Logger.getLogger("PubSub").info("WebSocket is open, sending ${sendQueue.size} items in the queue")
        connected = true

        // Send existing items in our queue
        sendQueue.forEach {
            websocket.send(it)
        }

        // Clear queue
        sendQueue.clear()

    }

    override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
        super.onClosing(webSocket, code, reason)
        connected = false
    }

    override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
        super.onClosed(webSocket, code, reason)

        // Mark closed
        Logger.getLogger("PubSub").info("WebSocket was closed")
        connected = false

        // TODO: Retry?

    }

    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
        super.onFailure(webSocket, t, response)

        // Failed!
        Logger.getLogger("PubSub").warning("WebSocket failed: " + t.localizedMessage)

    }

    /** Register an RPC handler */
    fun register(action : String, handler : (data : JsonElement) -> Promise<Any, Exception>) {

        // Store it
        handlers[action] = handler

    }

    /** Remove an RPC handler */
    fun remove(action : String) {
        handlers.remove(action)
    }

    /** Send text along the web socket */
    private fun send(txt : String) {

        // Check if websocket is connected
        if (connected) {

            // Send immediately
            Logger.getLogger("PubSub").info("Sending payload $txt")
            websocket.send(txt)

        } else {

            // Add to queue to send once websocket is connected
            Logger.getLogger("PubSub").info("Payload is buffered")
            sendQueue.add(txt)

        }

    }

    /** Send a request along the websocket */
    fun call(action : String, data : Any) : Promise<JsonElement, Exception> {

        // Create request ID
        val id = UUID.randomUUID().toString()

        // Store pending request
        val defer = deferred<JsonElement, Exception>()
        pending[id] = defer

        // Send payload
        send(Gson().toJson(mapOf(
            "request" to true,
            "id" to id,
            "name" to action,
            "data" to data,
            "from" to instanceID
        )))

        // TODO: Timeout

        // Return promise
        return defer.promise

    }

    override fun onMessage(webSocket: WebSocket, text: String) {
        super.onMessage(webSocket, text)

        // Decode JSON
        val json: JsonElement = Gson().fromJson(text)

        // Get request ID
        val id = json["id"].asString

        // Ignore messages from ourselves
        val fromID = json["from"].asString
        if (fromID == instanceID)
            return

        // Check type
        if (json.asJsonObject.get("request")?.asBoolean == true) {

            // Start promise chain
            task {

                // Incoming RPC request, check if we have a handler
                val name = json["name"].asString ?: throw Exception("No RPC name provided.")
                val handler = handlers[name] ?: throw Exception("Unknown RPC action '$name'")

                // Call handler
                val responseData = handler(json["data"]).get()

                // Send response
                send(Gson().toJson(mapOf(
                    "response" to true,
                    "status" to "ok",
                    "id" to id,
                    "data" to responseData,
                    "from" to instanceID
                )))

            } fail {

                // Send a fail response
                send(Gson().toJson(mapOf(
                    "response" to true,
                    "status" to "error",
                    "id" to id,
                    "error_text" to it.localizedMessage,
                    "from" to instanceID
                )))

            }

        } else if (json.asJsonObject.get("response")?.asBoolean == true) {

            // Find pending request
            val pendingRequest = pending[id] ?: return

            // Remove it
            pending.remove(id)

            // Check if success
            if (json["status"].asString == "ok") {

                // Success
                pendingRequest.resolve(json["data"])

            } else {

                // Failed
                val err = Exception(json["error_text"].asString ?: "An unknown error occurred.")
                pendingRequest.reject(err)

            }

        }

    }

}
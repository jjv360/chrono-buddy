package com.jjv360.chronobuddy_watch.networking

import android.app.*
import android.content.Intent
import android.os.Build
import android.os.IBinder
import com.github.salomonbrys.kotson.fromJson
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.jjv360.chronobuddy_watch.MainActivity
import com.jjv360.chronobuddy_watch.R
import com.jjv360.ipfs.IPFS
import com.jjv360.ipfs.IPFSServer
import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.request.receiveText
import io.ktor.response.respond
import io.ktor.response.respondText
import io.ktor.routing.post
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import nl.komponents.kovenant.Promise
import nl.komponents.kovenant.deferred
import nl.komponents.kovenant.task
import java.util.logging.Logger

class P2PService : Service() {

    /** Static fields */
    companion object {

        /** Current instance, allows accessing the service in-process without going through the Binder interface */
        var singleton: P2PService? = null

        /** Startup promise */
        var startupPromise = deferred<P2PService, Exception>()
        var whenReady = startupPromise.promise

        /** @event Called when a remote phone wants to pair to us */
        var onPairRequest : ((deviceName : String) -> Promise<Boolean, Exception>)? = null

    }

    /** The IPFS instance */
    var ipfs = IPFS(this)

    /** The IPFS service that we receive connections on */
    private var service : IPFSServer? = null

    /** Called when someone tried to bind our service */
    override fun onBind(intent: Intent?): IBinder? {

        // We don't support binding
        return null

    }

    /** Called when someone tries to start our service with an Intent */
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        // We want to stay alive
        return START_STICKY

    }

    override fun onCreate() {

        // Store singleton
        singleton = this

        // Start promise chain
        task {

            // TODO: How to get a random port from ktor?
            val port = 30000 + (Math.random() * 35000).toInt()

            // Create HTTP server
            val server = embeddedServer(Netty, port, "127.0.0.1") {
                routing {

                    /** Called when a remote phone wants to pair with our watch */
                    post("/v1/pair") {
                        try {
                            handlePair(call)
                        } catch (err : Exception) {
                            call.respondText(Gson().toJson(mapOf(
                                "status" to "error",
                                "error_text" to err.localizedMessage
                            )))
                        }
                    }

                }
            }

            // Start HTTP server
            server.start()

            // Create watch peer-to-peer service
            service = ipfs.createService("/x/chronobuddy-sync", port)
            Logger.getLogger("P2PService").info("Service ${service?.serviceName} started, requests coming in on port ${service?.serverSocket?.localPort}")

            // Done, inform anyone who's waiting for us to finish loading
            startupPromise.resolve(this)

        }

        // Check which version of the notification to use
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            // Create notification channel
            val channel = NotificationChannel("p2p-service-channel", "P2P Service", NotificationManager.IMPORTANCE_LOW)
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)

        }

        // Create builder
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, "p2p-service-channel")
        } else {
            Notification.Builder(this)
        }

        // Create intent to launch when the user taps the notification
        val pendingIntent = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), 0)
        builder.setContentIntent(pendingIntent)

        // Set fields which don't change
        builder.setSmallIcon(R.drawable.ic_launcher_foreground)

        // Become a foreground service by registering a user-visible notification
        startForeground(1, builder
            .setContentTitle("Connected to phone")
            .setContentText("Notifications are being received from your phone.")
            .build())

    }

    /** Handle POST /v1/pair */
    private suspend fun handlePair(call : ApplicationCall) {

        // Check if we have a listener
        if (onPairRequest == null)
            throw Exception("This device is not accepting pair requests at the moment.")

        // Decode JSON
        val json = Gson().fromJson<JsonObject>(call.receiveText())
        val deviceName = json["device_name"].asString

        // Ask listener if they want to accept
        val shouldAccept = onPairRequest!!(deviceName).get()
        if (shouldAccept) {

            // Pair successful
            call.respondText(Gson().toJson(mapOf(
                "status" to "ok"
            )))

        } else {

            // Pair rejected
            call.respondText(Gson().toJson(mapOf(
                "status" to "cancelled"
            )))

        }

    }

}
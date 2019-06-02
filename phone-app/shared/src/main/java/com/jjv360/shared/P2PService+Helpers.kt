package com.jjv360.shared

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import io.textile.pb.Model
import io.textile.pb.QueryOuterClass
import io.textile.textile.Textile
import nl.komponents.kovenant.Promise
import nl.komponents.kovenant.deferred
import nl.komponents.kovenant.task
import nl.komponents.kovenant.then

/** Starts the service. Make sure to call this in the onCreate of any activity you use the service from. */
fun P2PService.Companion.start(context : Context, launchIntent : PendingIntent) {
    System.out.println("P2PService: Requesting start")

    // Create the service intent
    val intent = Intent(context, P2PService::class.java)
    intent.putExtra("launch-intent", launchIntent)

    // Launch it
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        context.startForegroundService(intent)
    } else {
        context.startService(intent)
    }

}

/** Returns the account address of the current app. Make sure to check that it's running first. */
val P2PService.Companion.accountAddress : String
    get() = Textile.instance().account.address()

/** Check if service is up and running */
val P2PService.Companion.running : Boolean
    get() {

        // Check if initialized
        if (!hasInitedTextile)
            return false

        // Check if started
        if (!Textile.instance().summary().isInitialized)
            return false

        // All good
        return true

    }

/** Create a promise which resolves once Textile is up and running */
val P2PService.Companion.whenRunning : Promise<P2PService, Exception>
    get() {

        // Check if running now
        if (singleton != null && Textile.instance().summary().isInitialized)
            return Promise.of(singleton!!)

        // Create deferred promise
        val defer = deferred<P2PService, Exception>()

        // Register a once-off listener
        startupListeners.add { err: Exception? ->
            System.out.println("P2PService: Handling promise")

            // Resolve the promise
            if (err == null && singleton != null)
                defer.resolve(singleton!!)
            else
                defer.reject(err ?: Exception("Service did not start correctly."))

        }

        // Done
        return defer.promise

    }

/** List the registered companion apps, ie the Textile "contacts" */
val P2PService.Companion.companions : List<Model.Contact>
    get() = Textile.instance().contacts.list().itemsList

/** Register another device by it's account address and create a new "contact." on Textile */
fun P2PService.Companion.registerContact(accountAddress : String) : Promise<Unit, Exception> {

    return task {

        // Search for the contact on the network
        val query = QueryOuterClass.ContactQuery.newBuilder().setAddress(accountAddress).build()
        val options = QueryOuterClass.QueryOptions.newBuilder().setWait(30).setLimit(2).build()
        val handle = Textile.instance().contacts.search(query, options)

        // Create listener
        val defer = deferred<Model.Contact, Exception>()
        contactQueryListeners.put(handle.id) { model, err ->

            // Resolve promise
            if (model == null || err != null)
                defer.reject(err ?: Exception("An unknown error occurred."))
            else
                defer.resolve(model)

        }

        // Wait for promise
        defer.promise.get()

    } then {

        // Found the contact on the network, now register it
        Textile.instance().contacts.add(it)

    }

}
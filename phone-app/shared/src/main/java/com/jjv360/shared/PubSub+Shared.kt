package com.jjv360.shared

/** Atcive PubSub instances */
private val activePubSubs = mutableMapOf<String, PubSub>()

/** List of connection listeners */
private val onConnectionAddedListeners = mutableListOf<(PubSub) -> Unit>()

/** Fetch a pubsub connection to the specified device, or reuse an existing one */
fun PubSub.Companion.open(channel : String, deviceID : String) : PubSub {

    // Check if exists already
    var pubsub = activePubSubs.get("$channel:$deviceID")
    if (pubsub != null)
        return pubsub

    // Nope, create one now
    pubsub = PubSub(channel, deviceID)
    activePubSubs["$channel:$deviceID"] = pubsub

    // Notify listeners
    onConnectionAddedListeners.forEach { it(pubsub) }

    // DOne
    return pubsub

    // TODO: Shut these down if the service closes, or after a while of no use if no RPCs registered

}

/** Fetch all active pubsub connections */
val PubSub.Companion.active : Collection<PubSub> get() = activePubSubs.values

/** Add a listener for when a connection is added */
fun PubSub.Companion.onConnectionAdded(callback : (PubSub) -> Unit) = onConnectionAddedListeners.add(callback)
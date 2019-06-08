package com.jjv360.shared

/** Atcive PubSub instances */
private val activePubSubs = mutableMapOf<String, PubSub>()

/** Fetch a pubsub connection to the specified device, or reuse an existing one */
fun PubSub.Companion.open(channel : String, deviceID : String) : PubSub {

    // Check if exists already
    var pubsub = activePubSubs.get("$channel:$deviceID")
    if (pubsub != null)
        return pubsub

    // Nope, create one now
    pubsub = PubSub(channel, deviceID)
    activePubSubs["$channel:$deviceID"] = pubsub
    return pubsub

    // TODO: Shut these down if the service closes

}
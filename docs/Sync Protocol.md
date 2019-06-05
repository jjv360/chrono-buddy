# Sync Protocol

This document describes the peer-to-peer protocol used by this app and the companion watch app.

## Pairing

1. The watch app hosts an IPFS stream service called `/x/chronobuddy-sync`.
2. When pairing, the watch displays a QR code containing a JSON object.
    - The `action` field must be `"pair"`
    - The `mode` field must be `"ipfs"`
    - The `id` field must be the **IPFS peer ID**.
    - The `key` field must be the watch app's Base64 encoded private key. _(TODO)_
3. The phone app scans this QR code and extracts the fields
4. The phone reads `mode` = `"ipfs"` and knows to use this flow
5. The phone stores the `id` and `key` as a new watch association
6. Messages are sent to the watch via the named IPFS stream. The watch always hosts the service, and the phone always connects to it.

## Messages

The watch creates an HTTP server over IPFS on the `/x/chronobuddy-sync` service.

### Response format

``` js
{
    "status": "ok" | "error",   // Success or failure
    "error_text": "...",        // If status=error, a human-readable error description
    ... extra fields ...        // If status=ok, any response fields
}
```

### Pair Request

Sent from the phone to the watch after scanning the QR code and setting up a communication channel. Requests a pair from the watch.

``` js
POST /v1/pair

{
    "device_name": "Steve's Phone"
}
```

``` js
// On success
{
    "status": "ok"
}

// On user cancel
{
    "status": "cancelled"
}
```
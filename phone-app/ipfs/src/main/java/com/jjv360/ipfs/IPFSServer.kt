package com.jjv360.ipfs

import java.net.ServerSocket

/** Represents an IPFS service that can accept incoming connections from remote peers */
class IPFSServer(private val ipfs : IPFS, val serviceName : String, val serverSocket : ServerSocket) {



}
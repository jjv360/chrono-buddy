package com.jjv360.ipfs

import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.logging.Logger

/** Echoes output to Logger. The inputStream is consumed. */
fun Process.echoOutput() : Process {

    // Start a thread to echo output
    val outputStream = BufferedReader(InputStreamReader(this.inputStream))
    Thread({
        while (true) {
            val str = outputStream.readLine() ?: break
            Logger.getLogger("process").info(str)
        }
    }, "Process Echo - Output Stream").start()

    // Chainable
    return this

}

/** Echoes error to Logger. The errorStream is consumed. */
fun Process.echoError() : Process {

    // Start a thread to echo errors
    val errorStream = BufferedReader(InputStreamReader(this.errorStream))
    Thread({
        while (true) {
            val str = errorStream.readLine() ?: break
            Logger.getLogger("process").info(str)
        }
    }, "Process Echo - Error Stream").start()

    // Chainable
    return this

}
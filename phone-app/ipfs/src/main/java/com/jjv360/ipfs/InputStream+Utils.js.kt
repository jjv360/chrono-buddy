package com.jjv360.ipfs

import java.io.InputStream
import java.nio.charset.Charset
import java.util.logging.Logger

/** Extracts a single line of text from the output stream of the process. Returns null if stream has ended. */
fun InputStream.readLine() : String? {

    // Create line buffer
    val buffer = ByteArray(1024)
    var bytesRead = 0
    while (true) {

        // Read a byte
        val byte = this.read()

        // Stop if stream has ended
        if (byte < 0)
            break

        // Stop if this is the end of a line
        if (byte.toChar() == '\n')
            break

        // Store byte
        buffer[bytesRead] = byte.toByte()
        bytesRead += 1

        // If we're full, break
        if (bytesRead >= buffer.size-1)
            break

    }

    // Return null if empty
    if (bytesRead == 0)
        return null

    // Parse into a string
    return String(buffer, 0, bytesRead, Charset.forName("UTF-8")).trim()

}

/** Extracts text from output stream of process. */
fun InputStream.extract(regex : Regex) : MatchResult? {

    // Go through each line until found
    while (true) {

        // Read line, stop if stream has ended
        val line = this.readLine() ?: return null
        Logger.getLogger("stream").info(line)

        // Return match, try again if not matched
        return regex.find(line) ?: continue

    }

}
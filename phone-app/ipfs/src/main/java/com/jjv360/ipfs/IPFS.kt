package com.jjv360.ipfs

import android.content.Context
import android.os.Build
import java.io.File
import java.io.FileOutputStream
import java.lang.Exception
import java.net.InetAddress
import java.net.ServerSocket
import java.util.logging.Logger

class IPFS(private val context: Context) {

    /** IPFS process */
    private var daemon : Process? = null

    /** The port the HTTP API is listening on. Only available after start()ing */
    var port = 0

    /** Calls the IPFS executable with the specified command line options */
    private fun doCommand(commandline : String) : Process {

        // If IPFS binary doesn't exist, copy it to target location
        val ipfsBinary = File(context.filesDir, "ipfs.bin")
        if (!ipfsBinary.exists()) {

            // TODO: Check for binary file difference and re-copy if needed

            // Get IPFS binary name
            var filename = ""
            if (Build.CPU_ABI.startsWith("arm64"))  filename = "ipfs-arm64"
            if (Build.CPU_ABI.startsWith("arm"))  filename = "ipfs-arm"
            // TODO: i386 abi name?
            if (filename.isEmpty())
                throw Exception("We don't have an IPFS binary for the CPU architecture in this device.")

            // Copy file
            Logger.getLogger("ipfs").info("IPFS binary is being copied to app directory...")
            val inputStream = context.assets.open(filename)
            val outputStream = FileOutputStream(ipfsBinary)
            val buffer = ByteArray(1024*1024)
            while (true) {
                val bytesRead = inputStream.read(buffer)
                if (bytesRead <= 0) break
                outputStream.write(buffer, 0, bytesRead)
            }
            inputStream.close()
            outputStream.close()

            // Make sure it's executable
            ipfsBinary.setExecutable(true)

            // Done
            Logger.getLogger("ipfs").info("IPFS binary has been copied.")

        }

        // Get path to IPFS repo folder
        val repoDir = File(context.filesDir, ".ipfs_repo")

        // Setup environment vars for the IPFS node
        val envVars = arrayOf("IPFS_PATH=" + repoDir.absolutePath)

        // Create process
        val cmd = ipfsBinary.absolutePath + " " + commandline
        val process = Runtime.getRuntime().exec(cmd, envVars)

        // Done
        return process

    }

    /** Must be called before anything else. Blocks until the IPFS daemon is up and running. */
    private fun start() {

        // Stop if already started
        if (daemon != null)
            return

        // Set the port to use
        // TODO: Determine a free port?
        port = 5001
//        port = 30000 + (Math.random() * 30000).toInt()
//        doCommand("config Addresses.API /ip4/127.0.0.1/tcp/$port").waitFor()

        // Start the daemon
        daemon = doCommand("daemon --init --enable-gc --migrate").echoError()

        // Wait until daemon has started
        daemon!!.inputStream.extract(Regex("Daemon is ready")) ?: throw Exception("IPFS daemon did not start correctly.")
        daemon!!.echoOutput()

    }

    /** Creates a new P2P service on the IPFS network. Blocks. */
    fun createService(name : String) : IPFSServer {

        // Start IPFS daemon if needed
        start()

        // Set config option if needed
        // TODO: Better way of enabling this?
        val proc = doCommand("config Experimental.Libp2pStreamMounting --bool true").echoOutput().echoError().waitFor()

        // Create server socket
        val server = ServerSocket(0, 10, InetAddress.getByName("127.0.0.1"))

        // Replace spaces in the service name
        val cleanName = name.replace(" ", "_")

        // Register listening port with IPFS daemon
        doCommand("p2p listen $cleanName /ip4/127.0.0.1/tcp/${server.localPort}").echoOutput().echoError()

        // Done
        return IPFSServer(this, name, server)

    }

}
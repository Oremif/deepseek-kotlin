package org.oremif.deepseek

import kotlinx.coroutines.runBlocking
import org.oremif.deepseek.api.userBalance
import org.oremif.deepseek.client.DeepSeekClient

// Example demonstrating how to retrieve and display user balance information
fun main(): Unit = runBlocking {
    // Get API key from environment variable or use default placeholder
    val apiKey = System.getenv("DEEPSEEK_API_KEY") ?: "YOUR_API_KEY"
    // Initialize the DeepSeek client
    val client = DeepSeekClient(apiKey)

    // Fetch user balance information
    val infos = client.userBalance().balanceInfos

    // Iterate through each balance entry and display details
    infos.forEach { balance ->
        println("Currency: ${balance.currency}")
        println("Total balance: ${balance.totalBalance}")
        println("Granted balance: ${balance.grantedBalance}")
        println("Topped-up balance: ${balance.toppedUpBalance}")
    }

    // Close the client to release resources
    client.close()
}

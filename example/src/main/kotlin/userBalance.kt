package org.oremif.deepseek

import kotlinx.coroutines.runBlocking
import org.oremif.deepseek.api.userBalance
import org.oremif.deepseek.client.DeepSeekClient

fun main(): Unit = runBlocking {
    val apiKey = System.getenv("DEEPSEEK_API_KEY") ?: "YOUR_API_KEY"
    val client = DeepSeekClient(apiKey)

    val infos = client.userBalance().balanceInfos

    infos.forEach { balance ->
        println("Currency: ${balance.currency}")
        println("Total balance: ${balance.totalBalance}")
        println("Granted balance: ${balance.grantedBalance}")
        println("Topped-up balance: ${balance.toppedUpBalance}")
    }

    client.close()
}
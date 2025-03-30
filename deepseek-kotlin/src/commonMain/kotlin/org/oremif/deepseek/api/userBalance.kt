package org.oremif.deepseek.api

import io.ktor.client.call.*
import io.ktor.client.request.*
import org.oremif.deepseek.client.DeepSeekClientBase
import org.oremif.deepseek.models.UserBalance
import org.oremif.deepseek.utils.validateResponse

/**
 * Retrieves the current user's available API credits and usage information.
 *
 * This function checks your account balance, including remaining credits, usage statistics,
 * and any applicable limits on your account.
 *
 * Example:
 * ```kotlin
 * // Check available balance
 * val balance = client.userBalance()
 *
 * // Display balance information
 * println("Total credits: ${balance.totalCredits}")
 * println("Used credits: ${balance.usedCredits}")
 * println("Remaining: ${balance.remainingCredits}")
 *
 * // Check if credits are running low
 * if (balance.remainingCredits < 1000) {
 *     println("Warning: Credits running low!")
 * }
 * ```
 *
 * @return A [UserBalance] object containing information about the user's account balance
 */
public suspend fun DeepSeekClientBase.userBalance(): UserBalance {
    val response = client.get("user/balance")
    validateResponse(response)
    return response.body()
}
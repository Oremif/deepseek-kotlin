package org.oremif.deepseek.api

import io.ktor.client.call.*
import io.ktor.client.request.*
import org.oremif.deepseek.client.DeepSeekClientBase
import org.oremif.deepseek.errors.DeepSeekException
import org.oremif.deepseek.models.UserBalance
import org.oremif.deepseek.utils.validateResponse

/**
 * Retrieves the current user's available API balance.
 *
 * Returns a snapshot of whether the account has sufficient funds for further API calls
 * and a per-currency breakdown (granted vs. topped-up balance).
 *
 * Example:
 * ```kotlin
 * val balance = client.userBalance()
 * if (!balance.isAvailable) {
 *     println("Top up at https://platform.deepseek.com/top_up")
 * }
 * balance.balanceInfos.forEach { info ->
 *     println("${info.currency}: total=${info.totalBalance}, granted=${info.grantedBalance}")
 * }
 * ```
 *
 * @return A [UserBalance] object describing the user's account balance
 * @throws DeepSeekException if the API returns a non-2xx status
 */
public suspend fun DeepSeekClientBase.userBalance(): UserBalance {
    val response = client.get("user/balance")
    validateResponse(response)
    return response.body()
}
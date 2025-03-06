package org.oremif.deepseek.api

import io.ktor.client.call.*
import io.ktor.client.request.*
import org.oremif.deepseek.client.DeepSeekClientBase
import org.oremif.deepseek.models.UserBalance
import org.oremif.deepseek.utils.validateResponse

public suspend fun DeepSeekClientBase.userBalance(): UserBalance {
    val response = client.get("user/balance")
    validateResponse(response)
    return response.body()
}
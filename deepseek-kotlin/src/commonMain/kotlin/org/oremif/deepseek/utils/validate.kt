package org.oremif.deepseek.utils

import io.ktor.client.call.body
import io.ktor.client.statement.HttpResponse
import io.ktor.http.isSuccess
import org.oremif.deepseek.errors.DeepSeekError
import org.oremif.deepseek.errors.DeepSeekException

internal suspend fun validateResponse(response: HttpResponse) {
    if (!response.status.isSuccess()) {
        val headers = response.headers
        val error = response.body<DeepSeekError>()
        val description = response.status.description
        throw if (description.isEmpty()) {
            DeepSeekException.from(response.status.value, headers, error)
        } else {
            DeepSeekException.from(response.status.value, headers, error, description)
        }
    }
}
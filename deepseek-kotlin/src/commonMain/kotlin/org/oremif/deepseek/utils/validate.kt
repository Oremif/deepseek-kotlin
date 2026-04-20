package org.oremif.deepseek.utils

import io.ktor.client.call.*
import io.ktor.client.statement.*
import io.ktor.http.*
import org.oremif.deepseek.errors.DeepSeekError
import org.oremif.deepseek.errors.DeepSeekException
import org.oremif.deepseek.errors.toDeepSeekHeaders

internal suspend fun validateResponse(response: HttpResponse) {
    if (!response.status.isSuccess()) {
        val headers = response.headers.toDeepSeekHeaders()
        val error = response.body<DeepSeekError>()
        val description = response.status.description
        throw if (description.isEmpty()) {
            DeepSeekException.from(response.status.value, headers, error)
        } else {
            DeepSeekException.from(response.status.value, headers, error, description)
        }
    }
}
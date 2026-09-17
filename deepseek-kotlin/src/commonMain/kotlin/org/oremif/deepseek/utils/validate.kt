package org.oremif.deepseek.utils

import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.json.Json
import org.oremif.deepseek.errors.DeepSeekError
import org.oremif.deepseek.errors.DeepSeekException
import org.oremif.deepseek.errors.toDeepSeekHeaders

/**
 * Turns a non-2xx response into the matching [DeepSeekException].
 *
 * The error body is read as text and parsed by hand rather than through content negotiation: the
 * API sometimes labels a JSON error with a non-JSON content type — a missing `file_id` comes back
 * as `application/octet-stream` — and negotiation answers that with Ktor's own
 * `NoTransformationFoundException` instead of the SDK's exception. Parsing defensively also means a
 * body that is not JSON at all (a proxy's HTML error page, say) still surfaces as a
 * [DeepSeekException] carrying the status, just with a `null` [DeepSeekException.error].
 *
 * @param response The response to inspect; successful ones are left alone
 * @param json Serializer used to decode the error payload
 * @throws DeepSeekException if [response] carries a non-2xx status
 */
internal suspend fun validateResponse(response: HttpResponse, json: Json) {
    if (response.status.isSuccess()) return

    val headers = response.headers.toDeepSeekHeaders()
    val error = runCatching {
        json.decodeFromString<DeepSeekError>(response.bodyAsText())
    }
        .getOrNull()
    val description = response.status.description
    throw if (description.isEmpty()) {
        DeepSeekException.from(response.status.value, headers, error)
    } else {
        DeepSeekException.from(response.status.value, headers, error, description)
    }
}

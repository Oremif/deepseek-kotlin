package org.oremif.deepseek.utils

internal fun isRetryableStatus(statusCode: Int): Boolean =
    statusCode == 408 || statusCode == 425 || statusCode == 429 || statusCode in 500..599

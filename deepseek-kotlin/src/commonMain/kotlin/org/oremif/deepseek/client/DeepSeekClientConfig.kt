package org.oremif.deepseek.client

import kotlinx.serialization.json.Json

public class DeepSeekClientConfig(
    public val jsonConfig: Json = Json,
    public val chatCompletionTimeout: Long = 45_000,
    public val fimCompletionTimeout: Long = 60_000,
)
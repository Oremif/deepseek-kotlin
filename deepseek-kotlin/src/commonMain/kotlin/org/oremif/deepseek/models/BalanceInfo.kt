package org.oremif.deepseek.models

import kotlinx.serialization.Serializable

/**
 * Per-currency balance breakdown returned inside a [UserBalance].
 *
 * Monetary amounts are kept as strings to preserve the exact precision reported by the API.
 *
 * @property currency Currency that denominates the balance. Possible values: `CNY`, `USD`.
 * @property totalBalance Total available balance (granted + topped-up).
 * @property grantedBalance Non-expired granted balance portion.
 * @property toppedUpBalance Topped-up balance portion.
 */
@Serializable
public class BalanceInfo(
    public val currency: CurrencyType,
    public val totalBalance: String,
    public val grantedBalance: String,
    public val toppedUpBalance: String,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is BalanceInfo) return false

        return currency == other.currency &&
                totalBalance == other.totalBalance &&
                grantedBalance == other.grantedBalance &&
                toppedUpBalance == other.toppedUpBalance
    }

    override fun hashCode(): Int {
        var result = currency.hashCode()
        result = 31 * result + totalBalance.hashCode()
        result = 31 * result + grantedBalance.hashCode()
        result = 31 * result + toppedUpBalance.hashCode()
        return result
    }

    override fun toString(): String =
        "BalanceInfo(currency=$currency, totalBalance=$totalBalance, grantedBalance=$grantedBalance, toppedUpBalance=$toppedUpBalance)"
}
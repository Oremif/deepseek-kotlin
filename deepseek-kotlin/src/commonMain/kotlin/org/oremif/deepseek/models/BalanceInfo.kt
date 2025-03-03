package org.oremif.deepseek.models

import kotlinx.serialization.Serializable

/**
 * @property currency **Possible values: {`CNY`, `USD`}**
 *
 * The currency of the balance.
 * @property totalBalance The total available balance, including the granted balance and the topped-up balance.
 * @property grantedBalance The total not expired granted balance.
 * @property toppedUpBalance The total topped-up balance.
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
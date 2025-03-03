package org.oremif.deepseek.models

import kotlinx.serialization.Serializable

/**
 * @property isAvailable Whether the user's balance is sufficient for API calls.
 */
@Serializable
public class UserBalance(
    public val isAvailable: Boolean,
    public val balanceInfos: List<BalanceInfo>,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is UserBalance) return false
        return isAvailable == other.isAvailable && balanceInfos == other.balanceInfos
    }

    override fun hashCode(): Int {
        var result = isAvailable.hashCode()
        result = 31 * result + balanceInfos.hashCode()
        return result
    }

    override fun toString(): String =
        "UserBalance(isAvailable=$isAvailable, balanceInfos=$balanceInfos)"
}
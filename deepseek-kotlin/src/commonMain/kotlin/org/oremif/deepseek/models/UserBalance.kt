package org.oremif.deepseek.models

import kotlinx.serialization.Serializable

/**
 * Snapshot of the current user's account balance, as returned by [org.oremif.deepseek.api.userBalance].
 *
 * @property isAvailable `true` when the account has sufficient balance for further API calls.
 * @property balanceInfos Per-currency balance breakdown; typically contains one entry per
 * currency the account holds.
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
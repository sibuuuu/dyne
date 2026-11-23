package com.example.billing

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.PreferencesRepository
import com.example.data.UserPreferences
import com.example.ui.theme.LocalDyneTheme
import com.example.ui.theme.LocalDyneType
import com.example.ui.theme.ProGold
import java.time.LocalDate

/**
 * Daily free allowances backed by DataStore that reset automatically at local midnight:
 * - Background Remover: 3
 * - Photo Converter: 10
 * - PDF export: 3
 * - Audio convert: 3
 * - OCR: 3
 * - Frequency preset previews: 3
 *
 * At the limit, the Unlock sheet offers: Watch an ad (+1 use), Use 10 coins, or Go Premium.
 * Never a dead end; always a free way forward (tomorrow's reset, or an ad).
 */
enum class QuotaType(
    val key: String,
    val displayTitle: String,
    val dailyAllowance: Int,
    val associatedFeature: PremiumFeature
) {
    BACKGROUND_REMOVER(
        key = "BACKGROUND_REMOVER",
        displayTitle = "Background Remover",
        dailyAllowance = 3,
        associatedFeature = PremiumFeature.BACKGROUND_REMOVER_PRO
    ),
    PHOTO_CONVERTER(
        key = "PHOTO_CONVERTER",
        displayTitle = "Photo Converter",
        dailyAllowance = 10,
        associatedFeature = PremiumFeature.PHOTO_CONVERTER_PRO
    ),
    PDF_EXPORT(
        key = "PDF_EXPORT",
        displayTitle = "PDF Export",
        dailyAllowance = 3,
        associatedFeature = PremiumFeature.UNLIMITED_DOCUMENTS_OCR_LOCK_MERGE
    ),
    AUDIO_CONVERT(
        key = "AUDIO_CONVERT",
        displayTitle = "Audio Convert",
        dailyAllowance = 3,
        associatedFeature = PremiumFeature.AUDIO_CONVERTER_PRO
    ),
    OCR(
        key = "OCR",
        displayTitle = "OCR Text Extract",
        dailyAllowance = 3,
        associatedFeature = PremiumFeature.UNLIMITED_DOCUMENTS_OCR_LOCK_MERGE
    ),
    FREQUENCY_PRESET_PREVIEW(
        key = "FREQUENCY_PRESET_PREVIEW",
        displayTitle = "Frequency Preset Previews",
        dailyAllowance = 3,
        associatedFeature = PremiumFeature.FREQUENCY_PRO
    )
}

data class QuotaStatus(
    val quotaType: QuotaType,
    val usedToday: Int,
    val dailyAllowance: Int,
    val bonusCredits: Int
) {
    val remainingFreeToday: Int
        get() = (dailyAllowance - usedToday).coerceIn(0, dailyAllowance)

    val totalAvailableNow: Int
        get() = remainingFreeToday + bonusCredits.coerceAtLeast(0)

    val isAtLimit: Boolean
        get() = totalAvailableNow <= 0

    val chipLabel: String
        get() = buildString {
            append("$remainingFreeToday of $dailyAllowance free today")
            if (bonusCredits > 0) {
                append(" (+$bonusCredits bonus)")
            }
        }
}

class QuotaManager(
    private val preferencesRepository: PreferencesRepository
) {
    fun getStatus(
        type: QuotaType,
        prefs: UserPreferences,
        todayDate: String = LocalDate.now().toString()
    ): QuotaStatus {
        val used = prefs.getQuotaUsedCountToday(type.key, todayDate)
        val bonus = prefs.getQuotaBonusCount(type.key)
        return QuotaStatus(
            quotaType = type,
            usedToday = used,
            dailyAllowance = type.dailyAllowance,
            bonusCredits = bonus
        )
    }

    suspend fun consumeUse(
        type: QuotaType,
        todayDate: String = LocalDate.now().toString()
    ): Boolean {
        return preferencesRepository.consumeQuotaUse(
            quotaKey = type.key,
            dailyAllowance = type.dailyAllowance,
            todayDate = todayDate
        )
    }

    suspend fun grantPlusOneUse(type: QuotaType) {
        preferencesRepository.grantQuotaBonusUse(type.key, delta = 1)
    }

    companion object {
        fun statusFromPrefs(
            type: QuotaType,
            prefs: UserPreferences,
            todayDate: String = LocalDate.now().toString()
        ): QuotaStatus {
            val used = prefs.getQuotaUsedCountToday(type.key, todayDate)
            val bonus = prefs.getQuotaBonusCount(type.key)
            return QuotaStatus(
                quotaType = type,
                usedToday = used,
                dailyAllowance = type.dailyAllowance,
                bonusCredits = bonus
            )
        }
    }
}

/**
 * Visible daily allowance chip ("2 of 3 free today") shown on tools with daily free quotas.
 * Tapping when at limit (or tapping "+1") opens the Unlock sheet offering:
 * Watch an ad (+1 use), Use 10 coins, or Go Premium.
 */
@Composable
fun QuotaAllowanceChip(
    quotaType: QuotaType,
    prefs: UserPreferences? = null,
    isPro: Boolean = false,
    isProUnlocked: Boolean = false,
    onRequestMoreUses: (() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val theme = LocalDyneTheme.current
    val type = LocalDyneType.current
    val featureGate = LocalFeatureGate.current
    val effectivePrefs = prefs ?: featureGate?.latestPrefs?.collectAsState()?.value ?: UserPreferences()
    val effectiveIsPro = isPro || isProUnlocked
    val effectiveClick = onClick ?: onRequestMoreUses

    if (effectiveIsPro) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            modifier = modifier
                .clip(RoundedCornerShape(50))
                .background(ProGold.copy(alpha = 0.14f))
                .border(1.dp, ProGold.copy(alpha = 0.45f), RoundedCornerShape(50))
                .padding(horizontal = 10.dp, vertical = 5.dp)
                .testTag("quota_chip_${quotaType.key.lowercase()}")
        ) {
            Icon(
                imageVector = Icons.Default.WorkspacePremium,
                contentDescription = null,
                tint = ProGold,
                modifier = Modifier.size(13.dp)
            )
            Text(
                text = "Unlimited • Premium",
                style = type.caption.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
                color = ProGold
            )
        }
        return
    }

    val status = QuotaManager.statusFromPrefs(quotaType, effectivePrefs)
    val atLimit = status.isAtLimit
    val chipColor = when {
        atLimit -> ProGold
        status.remainingFreeToday == 1 -> Color(0xFFFFB300)
        else -> theme.accent
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(chipColor.copy(alpha = 0.13f))
            .border(1.dp, chipColor.copy(alpha = 0.45f), RoundedCornerShape(50))
            .then(
                if (effectiveClick != null) {
                    Modifier.clickable { effectiveClick() }
                } else {
                    Modifier
                }
            )
            .padding(horizontal = 10.dp, vertical = 5.dp)
            .testTag("quota_chip_${quotaType.key.lowercase()}")
    ) {
        Icon(
            imageVector = if (atLimit) Icons.Default.AddCircleOutline else Icons.Default.CheckCircle,
            contentDescription = null,
            tint = chipColor,
            modifier = Modifier.size(13.dp)
        )
        Text(
            text = status.chipLabel,
            style = type.caption.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
            color = chipColor
        )
        if (atLimit && effectiveClick != null) {
            Spacer(modifier = Modifier.width(2.dp))
            Text(
                text = "• +1 Use",
                style = type.caption.copy(fontSize = 11.sp, fontWeight = FontWeight.ExtraBold),
                color = ProGold
            )
        }
    }
}

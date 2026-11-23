package com.example.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.billing.FeatureGate
import com.example.billing.PremiumFeature
import com.example.data.DeviceCapabilities
import com.example.data.UserPreferences
import com.example.domain.*
import com.example.ui.components.FeedbackHelper
import com.example.ui.components.GlassSheet
import com.example.ui.components.dyneClickable
import com.example.ui.components.proBorder
import com.example.ui.settings.SettingsViewModel
import com.example.ui.theme.BentoTextBlack
import com.example.ui.theme.DesignTokens
import com.example.ui.theme.LocalDyneTheme
import com.example.ui.theme.LocalDyneType
import com.example.ui.theme.ProGold
import com.example.ui.theme.ProGoldDeep
import com.example.ui.theme.ProGoldGradient
import kotlinx.coroutines.launch

enum class AllFeaturesFilter(val label: String) {
    ALL("All"),
    FREE("Free"),
    PREMIUM("Premium"),
    FAVORITES("Favorites"),
    AVAILABLE("Available on this phone")
}

enum class AllFeaturesViewMode(val label: String) {
    AZ_LIST("A–Z List"),
    CATEGORY_GRID("Category Grid")
}

/**
 * Full-screen "All Features" directory accessible from Home and from Paywall.
 * Shows live counter ("N features, M Premium"), A–Z list and Category Grid,
 * filter chips (All, Free, Premium, Favorites, Available on this phone),
 * search by name or alias, Free/Premium badges, and hardware-unsupported reasons.
 */
@Composable
fun AllFeaturesScreen(
    capabilities: DeviceCapabilities,
    preferences: UserPreferences,
    featureGate: FeatureGate?,
    isPro: Boolean,
    initialFilter: AllFeaturesFilter = AllFeaturesFilter.ALL,
    onBack: () -> Unit,
    onOpenFeature: (Feature) -> Unit,
    onToggleFavorite: (Feature) -> Unit
) {
    BackHandler(onBack = onBack)
    val theme = LocalDyneTheme.current
    val type = LocalDyneType.current

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf(initialFilter) }
    var viewMode by remember { mutableStateOf(AllFeaturesViewMode.AZ_LIST) }

    val favoriteIdsSet = remember(preferences.favoriteToolIdsCsv, isPro) {
        preferences.effectiveFavoriteIds(isPro).mapNotNull {
            FeatureRegistry.resolveFeatureOrPrimaryForTool(it)?.id ?: it
        }.toSet()
    }

    val filteredFeatures = remember(searchQuery, selectedFilter, favoriteIdsSet, capabilities) {
        val searched = FeatureRegistry.searchFeatures(
            query = searchQuery,
            capabilities = capabilities,
            includeUnsupported = true
        )
        searched.filter { feature ->
            when (selectedFilter) {
                AllFeaturesFilter.ALL -> true
                AllFeaturesFilter.FREE -> !feature.isPremium
                AllFeaturesFilter.PREMIUM -> feature.isPremium
                AllFeaturesFilter.FAVORITES -> favoriteIdsSet.contains(feature.id) || favoriteIdsSet.contains(feature.toolId)
                AllFeaturesFilter.AVAILABLE -> FeatureRegistry.isSupported(feature, capabilities)
            }
        }.sortedBy { it.title.lowercase() }
    }

    val visibleFilters = remember(isPro) {
        if (isPro) {
            listOf(AllFeaturesFilter.ALL, AllFeaturesFilter.FAVORITES, AllFeaturesFilter.AVAILABLE)
        } else {
            AllFeaturesFilter.entries
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(theme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Header Bar
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(theme.surface)
                        .border(1.dp, Color.White.copy(alpha = 0.1f), CircleShape)
                        .dyneClickable(testTag = "all_features_back", onClick = onBack)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = "Back",
                        tint = theme.textPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column {
                    Text(
                        text = "All Features",
                        style = type.title.copy(fontWeight = FontWeight.ExtraBold),
                        color = theme.textPrimary
                    )
                    Text(
                        text = if (isPro) "${FeatureRegistry.totalCount} features" else FeatureRegistry.liveCounterText,
                        style = type.caption.copy(fontWeight = FontWeight.SemiBold),
                        color = if (isPro) theme.textSecondary else ProGold
                    )
                }
            }

            // View Mode Toggle (A-Z List vs Category Grid)
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(theme.surface)
                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(14.dp))
                    .padding(3.dp)
            ) {
                AllFeaturesViewMode.entries.forEach { mode ->
                    val selected = viewMode == mode
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(11.dp))
                            .background(if (selected) theme.accent.copy(alpha = 0.2f) else Color.Transparent)
                            .dyneClickable(
                                testTag = "all_features_mode_${mode.name.lowercase()}",
                                onClick = { viewMode = mode }
                            )
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = mode.label,
                            style = type.caption.copy(
                                fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Medium,
                                fontSize = 12.sp
                            ),
                            color = if (selected) theme.accent else theme.textSecondary
                        )
                    }
                }
            }
        }

        // Search Input
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = {
                Text(
                    "Search ${FeatureRegistry.totalCount} features by name or alias...",
                    style = type.body,
                    color = theme.textSecondary
                )
            },
            leadingIcon = {
                Icon(Icons.Outlined.Search, contentDescription = null, tint = theme.accent)
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Outlined.Close, contentDescription = "Clear search", tint = theme.textSecondary)
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(18.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = theme.surface,
                unfocusedContainerColor = theme.surface,
                focusedBorderColor = theme.accent,
                unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                focusedTextColor = theme.textPrimary,
                unfocusedTextColor = theme.textPrimary
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .testTag("all_features_search_input")
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Filter Chips: All, Free, Premium, Favorites, Available on this phone
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            visibleFilters.forEach { filter ->
                val selected = selectedFilter == filter
                val chipCount = when (filter) {
                    AllFeaturesFilter.ALL -> FeatureRegistry.totalCount
                    AllFeaturesFilter.FREE -> FeatureRegistry.freeCount
                    AllFeaturesFilter.PREMIUM -> FeatureRegistry.premiumCount
                    AllFeaturesFilter.FAVORITES -> favoriteIdsSet.size
                    AllFeaturesFilter.AVAILABLE -> FeatureRegistry.allFeatures.count {
                        FeatureRegistry.isSupported(it, capabilities)
                    }
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            when {
                                selected && filter == AllFeaturesFilter.PREMIUM -> ProGold.copy(alpha = 0.22f)
                                selected -> theme.accent.copy(alpha = 0.2f)
                                else -> theme.surface
                            }
                        )
                        .border(
                            width = 1.dp,
                            color = when {
                                selected && filter == AllFeaturesFilter.PREMIUM -> ProGold
                                selected -> theme.accent
                                else -> Color.White.copy(alpha = 0.08f)
                            },
                            shape = RoundedCornerShape(14.dp)
                        )
                        .dyneClickable(
                            testTag = "all_features_filter_${filter.name.lowercase()}",
                            onClick = { selectedFilter = filter }
                        )
                        .padding(horizontal = 12.dp, vertical = 7.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        if (filter == AllFeaturesFilter.PREMIUM) {
                            Icon(
                                imageVector = Icons.Default.WorkspacePremium,
                                contentDescription = null,
                                tint = ProGold,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                        Text(
                            text = "${filter.label} ($chipCount)",
                            style = type.caption.copy(
                                fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold
                            ),
                            color = when {
                                selected && filter == AllFeaturesFilter.PREMIUM -> ProGold
                                selected -> theme.accent
                                else -> theme.textSecondary
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (filteredFeatures.isEmpty()) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.SearchOff,
                        contentDescription = null,
                        tint = theme.textSecondary,
                        modifier = Modifier.size(40.dp)
                    )
                    Text(
                        text = "No matching features found",
                        style = type.title,
                        color = theme.textPrimary
                    )
                    Text(
                        text = "Try searching another keyword or switching filter chips.",
                        style = type.caption,
                        color = theme.textSecondary
                    )
                }
            }
        } else if (viewMode == AllFeaturesViewMode.AZ_LIST) {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(
                    items = filteredFeatures,
                    key = { it.id },
                    contentType = { "all_feature_row" }
                ) { feature ->
                    val supported = FeatureRegistry.isSupported(feature, capabilities)
                    val unsupportedReason = FeatureRegistry.unsupportedReason(feature, capabilities)
                    val isUnlocked = isPro || (featureGate?.isCatalogFeatureUnlocked(feature, preferences) ?: !feature.isPremium)
                    val isFav = favoriteIdsSet.contains(feature.id) || favoriteIdsSet.contains(feature.toolId)

                    AllFeaturesListRow(
                        feature = feature,
                        isSupported = supported,
                        unsupportedReason = unsupportedReason,
                        isUnlocked = isUnlocked,
                        isAccountPro = isPro,
                        isFavorite = isFav,
                        onClick = {
                            if (!supported) return@AllFeaturesListRow
                            onOpenFeature(feature)
                        },
                        onToggleFavorite = { onToggleFavorite(feature) }
                    )
                }
            }
        } else {
            val groupedByCategory = remember(filteredFeatures) {
                ToolCategory.entries
                    .sortedBy { it.ordinal }
                    .mapNotNull { cat ->
                        val itemsForCat = filteredFeatures.filter { it.category == cat }
                        if (itemsForCat.isNotEmpty()) cat to itemsForCat else null
                    }
            }
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                groupedByCategory.forEach { (category, catFeatures) ->
                    item(
                        key = "header_${category.name}",
                        span = { GridItemSpan(2) }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp, bottom = 2.dp)
                        ) {
                            Text(
                                text = category.title,
                                style = type.section.copy(fontWeight = FontWeight.ExtraBold),
                                color = theme.accent
                            )
                            Text(
                                text = "${catFeatures.size} features",
                                style = type.caption,
                                color = theme.textSecondary
                            )
                        }
                    }
                    items(
                        items = catFeatures,
                        key = { "grid_${it.id}" },
                        contentType = { "all_feature_grid" }
                    ) { feature ->
                        val supported = FeatureRegistry.isSupported(feature, capabilities)
                        val unsupportedReason = FeatureRegistry.unsupportedReason(feature, capabilities)
                        val isUnlocked = isPro || (featureGate?.isCatalogFeatureUnlocked(feature, preferences) ?: !feature.isPremium)

                        AllFeaturesGridCard(
                            feature = feature,
                            isSupported = supported,
                            unsupportedReason = unsupportedReason,
                            isUnlocked = isUnlocked,
                            isAccountPro = isPro,
                            onClick = {
                                if (!supported) return@AllFeaturesGridCard
                                onOpenFeature(feature)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AllFeaturesListRow(
    feature: Feature,
    isSupported: Boolean,
    unsupportedReason: String?,
    isUnlocked: Boolean,
    isAccountPro: Boolean = false,
    isFavorite: Boolean,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    val theme = LocalDyneTheme.current
    val type = LocalDyneType.current

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (isSupported) 1f else 0.52f)
            .clip(RoundedCornerShape(18.dp))
            .background(theme.surface)
            .border(
                width = 1.dp,
                color = if (!isSupported) Color.White.copy(alpha = 0.05f) else Color.White.copy(alpha = 0.08f),
                shape = RoundedCornerShape(18.dp)
            )
            .dyneClickable(
                enabled = isSupported,
                testTag = "all_features_row_${feature.id}",
                onClick = onClick
            )
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(theme.accent.copy(alpha = 0.14f))
        ) {
            Icon(
                imageVector = feature.icon,
                contentDescription = null,
                tint = theme.accent,
                modifier = Modifier.size(22.dp)
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = feature.title,
                    style = type.body.copy(fontWeight = FontWeight.Bold),
                    color = theme.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (!isAccountPro && feature.isPremium && !isUnlocked) {
                    com.example.ui.components.ProCrownChip()
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = if (!isSupported && !unsupportedReason.isNullOrBlank()) {
                    unsupportedReason
                } else {
                    feature.shortBenefit
                },
                style = type.caption,
                color = if (!isSupported) Color(0xFFF87171) else theme.textSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (isSupported) {
            IconButton(
                onClick = onToggleFavorite,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = if (isFavorite) Icons.Default.Star else Icons.Outlined.StarOutline,
                    contentDescription = if (isFavorite) "Remove favorite" else "Add favorite",
                    tint = if (isFavorite) theme.accent else theme.textSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun AllFeaturesGridCard(
    feature: Feature,
    isSupported: Boolean,
    unsupportedReason: String?,
    isUnlocked: Boolean,
    isAccountPro: Boolean = false,
    onClick: () -> Unit
) {
    val theme = LocalDyneTheme.current
    val type = LocalDyneType.current

    Column(
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (isSupported) 1f else 0.52f)
            .clip(RoundedCornerShape(18.dp))
            .background(theme.surface)
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.08f),
                shape = RoundedCornerShape(18.dp)
            )
            .dyneClickable(
                enabled = isSupported,
                testTag = "all_features_grid_${feature.id}",
                onClick = onClick
            )
            .padding(12.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(theme.accent.copy(alpha = 0.14f))
            ) {
                Icon(
                    imageVector = feature.icon,
                    contentDescription = null,
                    tint = theme.accent,
                    modifier = Modifier.size(20.dp)
                )
            }

            if (!isAccountPro && feature.isPremium && !isUnlocked) {
                com.example.ui.components.ProCrownChip()
            }
        }

        Text(
            text = feature.title,
            style = type.body.copy(fontWeight = FontWeight.Bold),
            color = theme.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Text(
            text = if (!isSupported && !unsupportedReason.isNullOrBlank()) unsupportedReason else feature.shortBenefit,
            style = type.caption,
            color = if (!isSupported) Color(0xFFF87171) else theme.textSecondary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Full-screen grid dialog opened when tapping "See all (N)" on a category row
 * or "See all (M)" on the gold "Dyne Premium" row.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CategorySeeAllGridDialog(
    title: String,
    subtitle: String,
    isGoldPremiumSection: Boolean = false,
    features: List<Feature>,
    preferences: UserPreferences,
    featureGate: FeatureGate?,
    isPro: Boolean,
    onDismiss: () -> Unit,
    onFeatureClick: (Feature) -> Unit,
    onFeatureLongPress: (Feature) -> Unit
) {
    val theme = LocalDyneTheme.current
    val type = LocalDyneType.current

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(theme.background)
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(theme.surface)
                            .border(1.dp, Color.White.copy(alpha = 0.1f), CircleShape)
                            .dyneClickable(onClick = onDismiss)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Back",
                            tint = theme.textPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (isGoldPremiumSection) {
                                Icon(
                                    imageVector = Icons.Default.WorkspacePremium,
                                    contentDescription = null,
                                    tint = ProGold,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Text(
                                text = title,
                                style = type.title.copy(fontWeight = FontWeight.ExtraBold),
                                color = if (isGoldPremiumSection) ProGold else theme.textPrimary
                            )
                        }
                        Text(
                            text = subtitle,
                            style = type.caption,
                            color = theme.textSecondary
                        )
                    }
                }
            }

            val pageSize = 24
            var visibleCount by remember(features) { mutableIntStateOf(pageSize.coerceAtMost(features.size)) }
            val pagedFeatures = remember(features, visibleCount) {
                features.take(visibleCount)
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(
                    items = pagedFeatures,
                    key = { it.id },
                    contentType = { "see_all_grid_item" }
                ) { feature ->
                    val isUnlocked = isPro || (featureGate?.isCatalogFeatureUnlocked(feature, preferences) ?: !feature.isPremium)
                    Column(
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(24.dp))
                            .background(theme.surface)
                            .border(
                                width = 1.dp,
                                color = Color.White.copy(alpha = 0.08f),
                                shape = RoundedCornerShape(24.dp)
                            )
                            .combinedClickable(
                                onClick = { onFeatureClick(feature) },
                                onLongClick = { onFeatureLongPress(feature) }
                            )
                            .padding(14.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(theme.accent.copy(alpha = 0.14f))
                            ) {
                                Icon(
                                    imageVector = feature.icon,
                                    contentDescription = null,
                                    tint = theme.accent,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            if (!isPro && feature.isPremium && !isUnlocked) {
                                com.example.ui.components.ProCrownChip()
                            }
                        }
                        Text(
                            text = feature.title,
                            style = type.body.copy(fontWeight = FontWeight.Bold),
                            color = theme.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = feature.shortBenefit,
                            style = type.caption,
                            color = theme.textSecondary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                if (visibleCount < features.size) {
                    item(
                        key = "load_more_footer",
                        span = { GridItemSpan(2) },
                        contentType = "footer"
                    ) {
                        LaunchedEffect(visibleCount, features.size) {
                            visibleCount = (visibleCount + pageSize).coerceAtMost(features.size)
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Showing ${pagedFeatures.size} of ${features.size}",
                                style = type.caption,
                                color = theme.textSecondary
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Long-press context menu for any Home feature tile:
 * - Free users: Favorite (up to FREE_FAVORITES_LIMIT = 3) and Open work;
 *   Hide, Move, and Resize show a gold Crown and open the Unlock sheet.
 * - Premium / 1h-unlocked users: Full Hide, Move to section, Resize (S/M/L), Pin to top, and Pin Launcher Shortcut.
 */
@Composable
fun FeatureTileLongPressDialog(
    feature: Feature,
    isFavorite: Boolean,
    currentTileSize: FeatureTileSize,
    isPinnedTop: Boolean,
    isCustomizationUnlocked: Boolean,
    sections: List<CustomHomeSection>,
    onDismiss: () -> Unit,
    onOpen: () -> Unit,
    onToggleFavorite: () -> Unit,
    onHideFeature: () -> Unit,
    onMoveToSection: (CustomHomeSection) -> Unit,
    onResizeTile: (FeatureTileSize) -> Unit,
    onTogglePinTop: () -> Unit,
    onPinLauncherShortcut: () -> Unit,
    onRequireCustomizationUnlock: () -> Unit
) {
    val theme = LocalDyneTheme.current
    val type = LocalDyneType.current
    var showMovePicker by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(theme.surface)
                .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(24.dp))
                .padding(18.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(13.dp))
                        .background(
                            if (feature.isPremium) ProGold.copy(alpha = 0.16f)
                            else theme.accent.copy(alpha = 0.15f)
                        )
                ) {
                    Icon(
                        imageVector = feature.icon,
                        contentDescription = null,
                        tint = if (feature.isPremium) ProGold else theme.accent,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = feature.title,
                        style = type.title.copy(fontWeight = FontWeight.ExtraBold),
                        color = theme.textPrimary
                    )
                    Text(
                        text = feature.shortBenefit,
                        style = type.caption,
                        color = theme.textSecondary,
                        maxLines = 2
                    )
                }
            }

            HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

            // 1. Open
            LongPressActionRow(
                icon = Icons.Outlined.OpenInNew,
                title = "Open ${feature.title}",
                subtitle = "Launch directly on this mode",
                showCrown = false,
                onClick = {
                    onDismiss()
                    onOpen()
                }
            )

            // 2. Favorite (Crown if Free — zero customization on Free)
            LongPressActionRow(
                icon = if (isFavorite) Icons.Default.Star else Icons.Outlined.StarOutline,
                title = if (isFavorite) "Remove from Favorites" else "Add to Favorites",
                subtitle = if (isCustomizationUnlocked) {
                    "Unlimited Favorites enabled"
                } else {
                    "Unlock Favorites with Premium or a 1-hour unlock"
                },
                showCrown = !isCustomizationUnlocked,
                onClick = {
                    onDismiss()
                    if (isCustomizationUnlocked) onToggleFavorite() else onRequireCustomizationUnlock()
                }
            )

            // 3. Hide (Crown if Free)
            LongPressActionRow(
                icon = Icons.Outlined.VisibilityOff,
                title = "Hide from Home",
                subtitle = "Can be restored anytime in Customize Home",
                showCrown = !isCustomizationUnlocked,
                onClick = {
                    onDismiss()
                    if (isCustomizationUnlocked) onHideFeature() else onRequireCustomizationUnlock()
                }
            )

            // 4. Move to Section / Folder (Crown if Free)
            LongPressActionRow(
                icon = Icons.Outlined.DriveFileMove,
                title = "Move to Section / Folder",
                subtitle = "Move into any built-in or custom folder",
                showCrown = !isCustomizationUnlocked,
                onClick = {
                    if (isCustomizationUnlocked) {
                        showMovePicker = !showMovePicker
                    } else {
                        onDismiss()
                        onRequireCustomizationUnlock()
                    }
                }
            )

            AnimatedVisibility(visible = showMovePicker && isCustomizationUnlocked) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(theme.elevated)
                        .padding(10.dp)
                ) {
                    Text(
                        text = "Choose destination section:",
                        style = type.caption.copy(fontWeight = FontWeight.Bold),
                        color = theme.textSecondary
                    )
                    sections.forEach { sec ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    onDismiss()
                                    onMoveToSection(sec)
                                }
                                .padding(horizontal = 10.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = sec.name,
                                style = type.body.copy(fontWeight = FontWeight.SemiBold),
                                color = theme.textPrimary
                            )
                            Icon(
                                imageVector = Icons.Outlined.ChevronRight,
                                contentDescription = null,
                                tint = theme.textSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            // 5. Resize S / M / L (Crown if Free)
            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(theme.elevated.copy(alpha = 0.65f))
                    .padding(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.AspectRatio,
                            contentDescription = null,
                            tint = if (isCustomizationUnlocked) theme.accent else ProGold,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Resize Tile",
                            style = type.body.copy(fontWeight = FontWeight.Bold),
                            color = theme.textPrimary
                        )
                    }
                    if (!isCustomizationUnlocked) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(ProGold.copy(alpha = 0.18f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.WorkspacePremium,
                                contentDescription = "Premium",
                                tint = ProGold,
                                modifier = Modifier.size(11.dp)
                            )
                            Text(
                                text = "Premium",
                                style = type.caption.copy(fontSize = 9.sp, fontWeight = FontWeight.ExtraBold),
                                color = ProGold
                            )
                        }
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    FeatureTileSize.entries.forEach { sizeOption ->
                        val selected = currentTileSize == sizeOption
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (selected) theme.accent.copy(alpha = 0.22f)
                                    else theme.surface
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (selected) theme.accent else Color.White.copy(alpha = 0.08f),
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .clickable {
                                    onDismiss()
                                    if (isCustomizationUnlocked) {
                                        onResizeTile(sizeOption)
                                    } else {
                                        onRequireCustomizationUnlock()
                                    }
                                }
                                .padding(vertical = 8.dp)
                        ) {
                            Text(
                                text = sizeOption.label,
                                style = type.caption.copy(
                                    fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold
                                ),
                                color = if (selected) theme.accent else theme.textPrimary
                            )
                        }
                    }
                }
            }

            // 6. Pin to Top & Pin Launcher Shortcut (Premium)
            LongPressActionRow(
                icon = Icons.Outlined.PushPin,
                title = if (isPinnedTop) "Unpin from Top of Home" else "Pin to Top of Home",
                subtitle = "Keep this feature at the top of your Home screen",
                showCrown = !isCustomizationUnlocked,
                onClick = {
                    onDismiss()
                    if (isCustomizationUnlocked) onTogglePinTop() else onRequireCustomizationUnlock()
                }
            )

            LongPressActionRow(
                icon = Icons.Outlined.AppShortcut,
                title = "Pin Android App Shortcut",
                subtitle = "Add a launcher icon shortcut to your phone's home screen",
                showCrown = !isCustomizationUnlocked,
                onClick = {
                    onDismiss()
                    if (isCustomizationUnlocked) onPinLauncherShortcut() else onRequireCustomizationUnlock()
                }
            )
        }
    }
}

@Composable
private fun LongPressActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    showCrown: Boolean,
    onClick: () -> Unit
) {
    val theme = LocalDyneTheme.current
    val type = LocalDyneType.current

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(theme.elevated.copy(alpha = 0.65f))
            .dyneClickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (showCrown) ProGold else theme.accent,
            modifier = Modifier.size(20.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = type.body.copy(fontWeight = FontWeight.Bold),
                color = theme.textPrimary
            )
            Text(
                text = subtitle,
                style = type.caption,
                color = theme.textSecondary
            )
        }
        if (showCrown) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(ProGold.copy(alpha = 0.18f))
                    .border(1.dp, ProGold.copy(alpha = 0.45f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 7.dp, vertical = 3.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.WorkspacePremium,
                    contentDescription = "Premium",
                    tint = ProGold,
                    modifier = Modifier.size(12.dp)
                )
                Text(
                    text = "Pro",
                    style = type.caption.copy(fontSize = 10.sp, fontWeight = FontWeight.ExtraBold),
                    color = ProGold
                )
            }
        }
    }
}

enum class CustomizeFilter(val label: String) {
    ALL("All"),
    VISIBLE("Visible"),
    HIDDEN("Hidden"),
    FAVORITES("Favorites")
}

private data class ActiveFeatureOptionsTarget(
    val sectionId: String,
    val feature: Feature
)

private data class MovingFeatureState(
    val sourceSectionId: String,
    val feature: Feature
)

/**
 * Rebuilt Customize Home screen:
 * 1) LAYOUT: Normal top bar (Back, title "Customize Home", "Reset" action) directly at the top with no empty space.
 *    Below it: slim locked-preview banner for Free users ("Customize everything with Premium", "Unlock",
 *    "Try 1 hour (watch ad or coins)"), then search field and filter chips (All, Visible, Hidden, Favorites),
 *    then a list of SECTION CARDS collapsed by default showing section name, feature count, and show/hide switch.
 * 2) FEATURE ROW (inside an expanded section): min height 64dp: drag handle (left), icon, FULL name (up to 2 lines, 16sp)
 *    with a one-line caption (13sp), and ONE switch for show/hide on the right. Nothing else on the row.
 *    Tapping the row opens a bottom sheet "Feature options" with large labeled buttons:
 *    Favorite, Tile size (S/M/L), Pin to top, Add launcher shortcut, Move to another section, Hide.
 *    Long-press the drag handle to drag and drop within a section, across sections, and into folders,
 *    with haptics, auto-scroll, and an undo snackbar. A "Move" mode is the fallback for precise placement.
 * 3) SECTION TOOLS: three-dot menu per section (Rename, Columns list/2/3/4/row, Move up, Move down, Hide,
 *    Delete for custom sections only with confirmation); "New section or folder" button at the bottom;
 *    saved layouts (up to 5) in a sheet.
 * 4) FREE users: locked preview, read-only and greyed, with one slim banner under the top bar.
 * 5) PREMIUM: full editing, no crowns or banners.
 * 6) Performance: LazyColumn with stable keys; render rows only for the expanded section.
 */
@Composable
fun CustomizeHomeEditorScreen(
    viewModel: SettingsViewModel,
    preferences: UserPreferences,
    capabilities: DeviceCapabilities,
    featureGate: FeatureGate?,
    isPro: Boolean,
    showTopBar: Boolean = true,
    onBack: () -> Unit = {},
    onOpenPaywall: () -> Unit = {},
    onOpenAllFeatures: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    extraFooterContent: (@Composable () -> Unit)? = null
) {
    val context = LocalContext.current
    val theme = LocalDyneTheme.current
    val type = LocalDyneType.current
    val coroutineScope = rememberCoroutineScope()
    val lazyListState = rememberLazyListState()

    val isCustomizationUnlocked = isPro ||
        (featureGate?.isHomeCustomizationUnlocked(preferences) == true)

    val effectiveSectionsJson = preferences.effectiveCustomSectionsJson(isPro)
    val sections = remember(effectiveSectionsJson, capabilities) {
        FeatureRegistry.parseSectionsFromJson(effectiveSectionsJson, capabilities)
    }
    val hiddenIds = remember(preferences, isPro) {
        preferences.effectiveHiddenFeatureIdsSet(isPro)
    }
    val effectiveTileSizesCsv = preferences.effectivePerTileSizeCsv(isPro)
    val tileSizes = remember(effectiveTileSizesCsv) {
        FeatureRegistry.parseTileSizes(effectiveTileSizesCsv)
    }
    val pinnedTopIds = remember(preferences, isPro) {
        preferences.effectivePinnedTopFeatureIdsList(isPro)
    }
    val favoriteIds = remember(preferences.favoriteToolIdsCsv, isPro) {
        preferences.effectiveFavoriteIds(isPro)
    }
    val effectiveSavedLayoutsJson = preferences.effectiveSavedHomeLayoutsJson(isPro)
    val savedLayouts = remember(effectiveSavedLayoutsJson) {
        FeatureRegistry.parseSavedLayouts(effectiveSavedLayoutsJson)
    }

    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedFilter by rememberSaveable { mutableStateOf(CustomizeFilter.ALL) }
    // Collapsed by default
    var expandedSectionIds by rememberSaveable { mutableStateOf(emptySet<String>()) }

    var activeOptionsTarget by remember { mutableStateOf<ActiveFeatureOptionsTarget?>(null) }
    var showMoveToSectionPickerFor by remember { mutableStateOf<ActiveFeatureOptionsTarget?>(null) }
    var movingFeatureState by remember { mutableStateOf<MovingFeatureState?>(null) }

    var openMenuForSectionId by remember { mutableStateOf<String?>(null) }
    var columnsPickerSection by remember { mutableStateOf<CustomHomeSection?>(null) }
    var deletingCustomSection by remember { mutableStateOf<CustomHomeSection?>(null) }
    var showCreateSectionDialog by remember { mutableStateOf(false) }
    var editingSection by remember { mutableStateOf<CustomHomeSection?>(null) }
    var showSavedLayoutsSheet by remember { mutableStateOf(false) }

    // Undo snackbar state
    var undoMessage by remember { mutableStateOf<String?>(null) }
    var undoSnapshotSections by remember { mutableStateOf<List<CustomHomeSection>?>(null) }
    var undoSnapshotHidden by remember { mutableStateOf<Set<String>?>(null) }

    fun pushUndo(label: String) {
        undoMessage = label
        undoSnapshotSections = sections
        undoSnapshotHidden = hiddenIds
    }

    fun triggerUnlockSheet() {
        if (featureGate != null) {
            featureGate.require(
                feature = PremiumFeature.HOME_CUSTOMIZATION,
                customTitle = "Customize Everything with Premium",
                customDescription = "Reorder tiles & sections, create custom folders, hide tools, resize individual tiles (S/M/L), save up to 5 named layouts, and pin Android launcher shortcuts."
            )
        } else {
            onOpenPaywall()
        }
    }

    fun guardOrRun(action: () -> Unit) {
        if (isCustomizationUnlocked) {
            action()
        } else {
            triggerUnlockSheet()
        }
    }

    fun moveFeatureBetweenOrWithinSections(
        feature: Feature,
        sourceSectionId: String,
        targetSectionId: String,
        targetIndex: Int? = null
    ) {
        pushUndo("Moved \"${feature.title}\"")
        val updated = sections.map { sec ->
            when {
                sec.id == sourceSectionId && sec.id == targetSectionId -> {
                    val mutable = sec.featureIds.toMutableList()
                    mutable.remove(feature.id)
                    val dest = (targetIndex ?: mutable.size).coerceIn(0, mutable.size)
                    mutable.add(dest, feature.id)
                    sec.copy(featureIds = mutable)
                }
                sec.id == sourceSectionId -> {
                    sec.copy(featureIds = sec.featureIds.filterNot { it == feature.id })
                }
                sec.id == targetSectionId -> {
                    val mutable = sec.featureIds.filterNot { it == feature.id }.toMutableList()
                    val dest = (targetIndex ?: mutable.size).coerceIn(0, mutable.size)
                    mutable.add(dest, feature.id)
                    sec.copy(featureIds = mutable)
                }
                else -> sec
            }
        }
        viewModel.saveCustomSections(updated)
        expandedSectionIds = expandedSectionIds + targetSectionId
    }

    val previewAlpha = if (isCustomizationUnlocked) 1f else 0.6f

    Box(modifier = modifier.fillMaxSize().background(theme.background)) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 1. NORMAL TOP BAR (directly at the top with no empty space: Back, "Customize Home", "Reset")
            if (showTopBar) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = DesignTokens.ScreenMargin,
                            vertical = DesignTokens.Space8
                        )
                        .testTag("customize_top_bar")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(DesignTokens.Space12),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(DesignTokens.MinTouchTarget)
                                .clip(CircleShape)
                                .background(theme.surface)
                                .border(1.dp, Color.White.copy(alpha = 0.12f), CircleShape)
                                .dyneClickable(
                                    testTag = "customize_back_btn",
                                    onClick = onBack
                                )
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                                contentDescription = "Back",
                                tint = theme.textPrimary,
                                modifier = Modifier.size(DesignTokens.IconSize)
                            )
                        }
                        Text(
                            text = "Customize Home",
                            style = type.title.copy(
                                fontSize = DesignTokens.TitleTextSize,
                                fontWeight = DesignTokens.TitleWeight
                            ),
                            color = theme.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.testTag("customize_home_title")
                        )
                    }

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .heightIn(min = DesignTokens.MinTouchTarget)
                            .clip(RoundedCornerShape(DesignTokens.Space12))
                            .background(theme.surface)
                            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(DesignTokens.Space12))
                            .dyneClickable(
                                testTag = "customize_reset_default_btn",
                                onClick = {
                                    guardOrRun {
                                        pushUndo("Reset Home layout to default")
                                        viewModel.resetHomeCustomizationToDefault()
                                    }
                                }
                            )
                            .padding(horizontal = DesignTokens.Space12, vertical = DesignTokens.Space8)
                    ) {
                        Text(
                            text = "Reset",
                            style = type.label.copy(
                                fontSize = DesignTokens.LabelTextSize,
                                fontWeight = DesignTokens.LabelWeight
                            ),
                            color = theme.textPrimary
                        )
                    }
                }
            }

            // 2. SLIM FREE LOCKED PREVIEW BANNER (directly under the top bar for Free users; hidden for Premium/1h)
            if (!isCustomizationUnlocked) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(DesignTokens.Space8),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            start = DesignTokens.ScreenMargin,
                            end = DesignTokens.ScreenMargin,
                            bottom = DesignTokens.Space8
                        )
                        .clip(RoundedCornerShape(DesignTokens.Space16))
                        .background(theme.surface)
                        .border(1.dp, ProGold.copy(alpha = 0.55f), RoundedCornerShape(DesignTokens.Space16))
                        .padding(horizontal = DesignTokens.Space12, vertical = DesignTokens.Space8)
                        .testTag("customize_locked_banner")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Customize everything with Premium",
                            style = type.label.copy(
                                fontSize = DesignTokens.LabelTextSize,
                                fontWeight = FontWeight.Bold
                            ),
                            color = ProGold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(DesignTokens.Space8))
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .clip(RoundedCornerShape(DesignTokens.Space12))
                                .background(brush = ProGoldGradient)
                                .dyneClickable(
                                    testTag = "customize_go_premium_button",
                                    onClick = onOpenPaywall
                                )
                                .padding(horizontal = DesignTokens.Space12, vertical = 6.dp)
                        ) {
                            Text(
                                text = "Unlock",
                                style = type.caption.copy(fontWeight = FontWeight.ExtraBold),
                                color = BentoTextBlack
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Read-only preview",
                            style = type.caption,
                            color = theme.textSecondary
                        )
                        Text(
                            text = "Try 1 hour (watch ad or coins)",
                            style = type.caption.copy(fontWeight = FontWeight.Bold),
                            color = theme.accent,
                            modifier = Modifier
                                .clip(RoundedCornerShape(DesignTokens.Space8))
                                .dyneClickable(
                                    testTag = "customize_try_1h_ad_button",
                                    onClick = { triggerUnlockSheet() }
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // Active "Move" mode fallback banner for precise placement
            if (movingFeatureState != null) {
                val mf = movingFeatureState!!
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            start = DesignTokens.ScreenMargin,
                            end = DesignTokens.ScreenMargin,
                            bottom = DesignTokens.Space8
                        )
                        .clip(RoundedCornerShape(DesignTokens.Space16))
                        .background(theme.elevated)
                        .border(1.dp, theme.accent, RoundedCornerShape(DesignTokens.Space16))
                        .padding(horizontal = DesignTokens.Space12, vertical = DesignTokens.Space8)
                        .testTag("customize_move_mode_banner")
                ) {
                    Text(
                        text = "Moving \"${mf.feature.title}\" — tap any section or row to place here",
                        style = type.caption.copy(fontWeight = FontWeight.Bold),
                        color = theme.accent,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(DesignTokens.Space8))
                    Text(
                        text = "Cancel",
                        style = type.caption.copy(fontWeight = FontWeight.ExtraBold),
                        color = theme.textPrimary,
                        modifier = Modifier
                            .clip(RoundedCornerShape(DesignTokens.Space8))
                            .clickable { movingFeatureState = null }
                            .padding(horizontal = DesignTokens.Space8, vertical = 4.dp)
                    )
                }
            }

            // 3. SEARCH FIELD & FILTER CHIPS (All, Visible, Hidden, Favorites) directly below top bar / slim banner
            Column(
                verticalArrangement = Arrangement.spacedBy(DesignTokens.Space8),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = DesignTokens.ScreenMargin,
                        end = DesignTokens.ScreenMargin,
                        bottom = DesignTokens.Space8
                    )
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            "Search all ${FeatureRegistry.totalCount} features or sections…",
                            style = type.body.copy(fontSize = DesignTokens.BodyTextSize),
                            color = theme.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Search,
                            contentDescription = null,
                            tint = theme.accent
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Outlined.Close,
                                    contentDescription = "Clear search",
                                    tint = theme.textSecondary
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(DesignTokens.Space16),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = theme.surface,
                        unfocusedContainerColor = theme.surface,
                        focusedBorderColor = theme.accent,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.12f),
                        focusedTextColor = theme.textPrimary,
                        unfocusedTextColor = theme.textPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("customize_search_input")
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(DesignTokens.Space8),
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                ) {
                    CustomizeFilter.entries.forEach { filter ->
                        val selected = selectedFilter == filter
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .heightIn(min = 38.dp)
                                .clip(RoundedCornerShape(DesignTokens.ButtonRadius))
                                .background(if (selected) theme.accent.copy(alpha = 0.22f) else theme.surface)
                                .border(
                                    width = 1.dp,
                                    color = if (selected) theme.accent else Color.White.copy(alpha = 0.10f),
                                    shape = RoundedCornerShape(DesignTokens.ButtonRadius)
                                )
                                .clickable { selectedFilter = filter }
                                .padding(horizontal = DesignTokens.Space16, vertical = DesignTokens.Space8)
                                .testTag("customize_filter_${filter.name}")
                        ) {
                            Text(
                                text = filter.label,
                                style = type.label.copy(
                                    fontSize = DesignTokens.LabelTextSize,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                ),
                                color = if (selected) theme.accent else theme.textPrimary
                            )
                        }
                    }
                }
            }

            // 4. LAZY LIST WITH STABLE KEYS (Collapsed Section Cards + Expanded Feature Rows only)
            LazyColumn(
                state = lazyListState,
                contentPadding = PaddingValues(
                    start = DesignTokens.ScreenMargin,
                    end = DesignTokens.ScreenMargin,
                    top = 0.dp,
                    bottom = 112.dp
                ),
                verticalArrangement = Arrangement.spacedBy(DesignTokens.Space12),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .testTag("customize_lazy_list")
            ) {
                // SECTION CARDS (collapsed by default; render rows ONLY for expanded sections)
                sections.forEachIndexed { secIdx, section ->
                    val allSectionFeatures = section.featureIds.mapNotNull { FeatureRegistry.getFeatureById(it) }
                    val q = searchQuery.trim().lowercase()
                    val filteredFeatures = allSectionFeatures.filter { feat ->
                        val matchesQuery = q.isEmpty() ||
                            feat.title.lowercase().contains(q) ||
                            feat.shortBenefit.lowercase().contains(q) ||
                            feat.aliases.any { a -> a.lowercase().contains(q) } ||
                            section.name.lowercase().contains(q)
                        val isHidden = feat.id in hiddenIds
                        val isFav = favoriteIds.contains(feat.id) || favoriteIds.contains(feat.toolId)
                        val matchesFilter = when (selectedFilter) {
                            CustomizeFilter.ALL -> true
                            CustomizeFilter.VISIBLE -> !isHidden && !section.isHidden
                            CustomizeFilter.HIDDEN -> isHidden || section.isHidden
                            CustomizeFilter.FAVORITES -> isFav
                        }
                        matchesQuery && matchesFilter
                    }

                    val hasActiveSearchOrFilter = q.isNotEmpty() || selectedFilter != CustomizeFilter.ALL
                    if (hasActiveSearchOrFilter && filteredFeatures.isEmpty() && !section.name.lowercase().contains(q)) {
                        return@forEachIndexed
                    }

                    val isSectionExpanded = (section.id in expandedSectionIds) || hasActiveSearchOrFilter
                    val sectionColor = Color(section.colorHex)

                    // Section Card Header Item
                    item(
                        key = "sec_card_${section.id}",
                        contentType = "section_card"
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .alpha(previewAlpha * (if (section.isHidden) 0.65f else 1f))
                                .clip(DesignTokens.CardShape)
                                .background(theme.surface)
                                .border(
                                    width = 1.dp,
                                    color = if (isSectionExpanded) sectionColor.copy(alpha = 0.55f) else Color.White.copy(alpha = 0.10f),
                                    shape = DesignTokens.CardShape
                                )
                                .clickable {
                                    val moving = movingFeatureState
                                    if (moving != null) {
                                        guardOrRun {
                                            moveFeatureBetweenOrWithinSections(
                                                feature = moving.feature,
                                                sourceSectionId = moving.sourceSectionId,
                                                targetSectionId = section.id,
                                                targetIndex = 0
                                            )
                                            FeedbackHelper.hapticTick(context)
                                            movingFeatureState = null
                                        }
                                    } else {
                                        expandedSectionIds = if (section.id in expandedSectionIds) {
                                            expandedSectionIds - section.id
                                        } else {
                                            expandedSectionIds + section.id
                                        }
                                    }
                                }
                                .padding(horizontal = DesignTokens.Space16, vertical = DesignTokens.Space12)
                                .testTag("section_card_${section.id}")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(DesignTokens.Space12),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = if (isSectionExpanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                                        contentDescription = if (isSectionExpanded) "Collapse section" else "Expand section",
                                        tint = sectionColor,
                                        modifier = Modifier.size(DesignTokens.IconSize)
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = section.name,
                                            style = type.section.copy(
                                                fontSize = DesignTokens.SectionHeaderTextSize,
                                                fontWeight = DesignTokens.SectionHeaderWeight
                                            ),
                                            color = theme.textPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${allSectionFeatures.size} features • ${section.layoutStyle.label}",
                                            style = type.caption.copy(fontSize = DesignTokens.CaptionTextSize),
                                            color = theme.textSecondary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(DesignTokens.Space8)
                                ) {
                                    Switch(
                                        checked = !section.isHidden,
                                        enabled = isCustomizationUnlocked,
                                        onCheckedChange = { wantVisible ->
                                            guardOrRun {
                                                pushUndo(if (wantVisible) "Showed \"${section.name}\"" else "Hid \"${section.name}\"")
                                                val updated = sections.map {
                                                    if (it.id == section.id) it.copy(isHidden = !wantVisible) else it
                                                }
                                                viewModel.saveCustomSections(updated)
                                            }
                                        },
                                        modifier = Modifier.testTag("section_switch_${section.id}")
                                    )

                                    Box {
                                        IconButton(
                                            onClick = {
                                                guardOrRun {
                                                    openMenuForSectionId = section.id
                                                }
                                            },
                                            modifier = Modifier
                                                .size(DesignTokens.MinTouchTarget)
                                                .testTag("section_menu_${section.id}")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.MoreVert,
                                                contentDescription = "Section tools for ${section.name}",
                                                tint = theme.textSecondary,
                                                modifier = Modifier.size(DesignTokens.IconSize)
                                            )
                                        }

                                        DropdownMenu(
                                            expanded = openMenuForSectionId == section.id,
                                            onDismissRequest = { openMenuForSectionId = null }
                                        ) {
                                            DropdownMenuItem(
                                                text = { Text("Rename") },
                                                onClick = {
                                                    openMenuForSectionId = null
                                                    editingSection = section
                                                }
                                            )
                                            DropdownMenuItem(
                                                text = { Text("Columns (list/2/3/4/row)") },
                                                onClick = {
                                                    openMenuForSectionId = null
                                                    columnsPickerSection = section
                                                }
                                            )
                                            DropdownMenuItem(
                                                text = { Text("Move up") },
                                                enabled = secIdx > 0,
                                                onClick = {
                                                    openMenuForSectionId = null
                                                    if (secIdx > 0) {
                                                        FeedbackHelper.hapticTick(context)
                                                        pushUndo("Moved section \"${section.name}\" up")
                                                        val updated = sections.toMutableList()
                                                        val item = updated.removeAt(secIdx)
                                                        updated.add(secIdx - 1, item)
                                                        viewModel.saveCustomSections(updated)
                                                    }
                                                }
                                            )
                                            DropdownMenuItem(
                                                text = { Text("Move down") },
                                                enabled = secIdx < sections.lastIndex,
                                                onClick = {
                                                    openMenuForSectionId = null
                                                    if (secIdx < sections.lastIndex) {
                                                        FeedbackHelper.hapticTick(context)
                                                        pushUndo("Moved section \"${section.name}\" down")
                                                        val updated = sections.toMutableList()
                                                        val item = updated.removeAt(secIdx)
                                                        updated.add(secIdx + 1, item)
                                                        viewModel.saveCustomSections(updated)
                                                    }
                                                }
                                            )
                                            DropdownMenuItem(
                                                text = { Text(if (section.isHidden) "Show" else "Hide") },
                                                onClick = {
                                                    openMenuForSectionId = null
                                                    pushUndo(if (section.isHidden) "Showed \"${section.name}\"" else "Hid \"${section.name}\"")
                                                    val updated = sections.map {
                                                        if (it.id == section.id) it.copy(isHidden = !it.isHidden) else it
                                                    }
                                                    viewModel.saveCustomSections(updated)
                                                }
                                            )
                                            if (!section.isBuiltIn) {
                                                DropdownMenuItem(
                                                    text = { Text("Delete", color = Color(0xFFF87171)) },
                                                    onClick = {
                                                        openMenuForSectionId = null
                                                        deletingCustomSection = section
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Render feature rows ONLY when this section is expanded
                    if (isSectionExpanded) {
                        itemsIndexed(
                            items = filteredFeatures,
                            key = { _, feat -> "feat_row_${section.id}_${feat.id}" },
                            contentType = { _, _ -> "feature_row" }
                        ) { featIdx, feature ->
                            val isHidden = feature.id in hiddenIds

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(DesignTokens.Space8),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 64.dp)
                                    .alpha(previewAlpha * (if (isHidden) 0.55f else 1f))
                                    .clip(RoundedCornerShape(DesignTokens.Space16))
                                    .background(theme.elevated.copy(alpha = 0.85f))
                                    .border(
                                        width = 1.dp,
                                        color = Color.White.copy(alpha = 0.08f),
                                        shape = RoundedCornerShape(DesignTokens.Space16)
                                    )
                                    .clickable {
                                        val moving = movingFeatureState
                                        if (moving != null) {
                                            guardOrRun {
                                                moveFeatureBetweenOrWithinSections(
                                                    feature = moving.feature,
                                                    sourceSectionId = moving.sourceSectionId,
                                                    targetSectionId = section.id,
                                                    targetIndex = featIdx
                                                )
                                                FeedbackHelper.hapticTick(context)
                                                movingFeatureState = null
                                            }
                                        } else {
                                            guardOrRun {
                                                activeOptionsTarget = ActiveFeatureOptionsTarget(
                                                    sectionId = section.id,
                                                    feature = feature
                                                )
                                            }
                                        }
                                    }
                                    .padding(horizontal = DesignTokens.Space8, vertical = DesignTokens.Space8)
                                    .testTag("customize_feature_row_${feature.id}")
                            ) {
                                // 1. DRAG HANDLE (left, long-press drag & drop within/across sections with haptics & auto-scroll)
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(width = 28.dp, height = DesignTokens.MinTouchTarget)
                                        .pointerInput(section.id, feature.id, isCustomizationUnlocked, sections) {
                                            var accumulatedDy = 0f
                                            detectDragGesturesAfterLongPress(
                                                onDragStart = {
                                                    accumulatedDy = 0f
                                                    if (isCustomizationUnlocked) {
                                                        FeedbackHelper.hapticTick(context)
                                                    } else {
                                                        triggerUnlockSheet()
                                                    }
                                                },
                                                onDrag = { change, dragAmount ->
                                                    change.consume()
                                                    if (!isCustomizationUnlocked) return@detectDragGesturesAfterLongPress
                                                    accumulatedDy += dragAmount.y

                                                    // Auto-scroll LazyColumn when dragging strongly up or down
                                                    if (kotlin.math.abs(dragAmount.y) > 12f) {
                                                        coroutineScope.launch {
                                                            lazyListState.scrollBy(dragAmount.y * 0.6f)
                                                        }
                                                    }

                                                    if (kotlin.math.abs(accumulatedDy) > 48f) {
                                                        val step = if (accumulatedDy > 0) 1 else -1
                                                        accumulatedDy = 0f
                                                        val currentSec = sections.firstOrNull { it.id == section.id }
                                                            ?: return@detectDragGesturesAfterLongPress
                                                        val currentList = currentSec.featureIds.toMutableList()
                                                        val currentPos = currentList.indexOf(feature.id)
                                                        if (currentPos >= 0) {
                                                            val nextPos = currentPos + step
                                                            if (nextPos in 0..currentList.lastIndex) {
                                                                // Reorder within current section
                                                                FeedbackHelper.hapticTick(context)
                                                                pushUndo("Reordered \"${feature.title}\"")
                                                                val moved = currentList.removeAt(currentPos)
                                                                currentList.add(nextPos, moved)
                                                                viewModel.saveCustomSections(
                                                                    sections.map {
                                                                        if (it.id == section.id) it.copy(featureIds = currentList) else it
                                                                    }
                                                                )
                                                            } else {
                                                                // Cross-section / into-folder drop when dragging past section boundary
                                                                val targetSecIdx = secIdx + step
                                                                if (targetSecIdx in sections.indices) {
                                                                    val targetSec = sections[targetSecIdx]
                                                                    FeedbackHelper.hapticTick(context)
                                                                    moveFeatureBetweenOrWithinSections(
                                                                        feature = feature,
                                                                        sourceSectionId = section.id,
                                                                        targetSectionId = targetSec.id,
                                                                        targetIndex = if (step > 0) 0 else targetSec.featureIds.size
                                                                    )
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            )
                                        }
                                        .testTag("customize_drag_handle_${feature.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.DragIndicator,
                                        contentDescription = "Long-press and drag to reorder ${feature.title}",
                                        tint = theme.textSecondary,
                                        modifier = Modifier.size(DesignTokens.IconSize)
                                    )
                                }

                                // 2. ICON (24dp)
                                Icon(
                                    imageVector = feature.icon,
                                    contentDescription = null,
                                    tint = theme.accent,
                                    modifier = Modifier.size(DesignTokens.IconSize)
                                )

                                // 3. FULL NAME (up to 2 lines, 16sp) + ONE-LINE CAPTION (13sp)
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(2.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = feature.title,
                                        style = type.body.copy(
                                            fontSize = DesignTokens.BodyTextSize,
                                            fontWeight = FontWeight.SemiBold
                                        ),
                                        color = theme.textPrimary,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.testTag("customize_feature_title_${feature.id}")
                                    )
                                    Text(
                                        text = feature.shortBenefit,
                                        style = type.caption.copy(
                                            fontSize = DesignTokens.CaptionTextSize,
                                            fontWeight = DesignTokens.CaptionWeight
                                        ),
                                        color = theme.textSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                // 4. ONE SWITCH FOR SHOW/HIDE ON THE RIGHT (nothing else on the row!)
                                Switch(
                                    checked = !isHidden,
                                    enabled = isCustomizationUnlocked,
                                    onCheckedChange = { wantVisible ->
                                        guardOrRun {
                                            val nextHidden = if (wantVisible) hiddenIds - feature.id else hiddenIds + feature.id
                                            pushUndo(if (wantVisible) "Showed \"${feature.title}\"" else "Hid \"${feature.title}\"")
                                            viewModel.setHiddenFeatures(nextHidden)
                                        }
                                    },
                                    modifier = Modifier.testTag("customize_feature_switch_${feature.id}")
                                )
                            }
                        }
                    }
                }

                // Bottom actions: "New section or folder" button + "Saved layouts (up to 5)" button
                item(key = "customize_bottom_actions", contentType = "bottom_actions") {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(DesignTokens.Space12),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = DesignTokens.Space8)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(DesignTokens.ButtonHeight)
                                .clip(DesignTokens.ButtonShape)
                                .background(theme.surface)
                                .border(1.dp, theme.accent.copy(alpha = 0.6f), DesignTokens.ButtonShape)
                                .dyneClickable(
                                    testTag = "customize_new_folder_btn",
                                    onClick = {
                                        guardOrRun { showCreateSectionDialog = true }
                                    }
                                )
                                .padding(horizontal = DesignTokens.Space16)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.CreateNewFolder,
                                    contentDescription = null,
                                    tint = theme.accent,
                                    modifier = Modifier.size(DesignTokens.IconSize)
                                )
                                Spacer(modifier = Modifier.width(DesignTokens.Space8))
                                Text(
                                    text = "New section or folder",
                                    style = type.button.copy(
                                        fontSize = DesignTokens.ButtonTextSize,
                                        fontWeight = DesignTokens.ButtonWeight
                                    ),
                                    color = theme.accent
                                )
                            }
                        }

                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(DesignTokens.ButtonHeight)
                                .clip(DesignTokens.ButtonShape)
                                .background(theme.surface)
                                .border(1.dp, Color.White.copy(alpha = 0.14f), DesignTokens.ButtonShape)
                                .dyneClickable(
                                    testTag = "customize_saved_layouts_btn",
                                    onClick = {
                                        guardOrRun { showSavedLayoutsSheet = true }
                                    }
                                )
                                .padding(horizontal = DesignTokens.Space16)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Bookmarks,
                                    contentDescription = null,
                                    tint = theme.textPrimary,
                                    modifier = Modifier.size(DesignTokens.IconSize)
                                )
                                Spacer(modifier = Modifier.width(DesignTokens.Space8))
                                Text(
                                    text = "Saved layouts (${savedLayouts.size}/$MAX_SAVED_HOME_LAYOUTS)",
                                    style = type.button.copy(
                                        fontSize = DesignTokens.ButtonTextSize,
                                        fontWeight = DesignTokens.ButtonWeight
                                    ),
                                    color = theme.textPrimary
                                )
                            }
                        }
                    }
                }

                if (extraFooterContent != null) {
                    item(key = "customize_extra_footer", contentType = "extra_footer") {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(DesignTokens.Space16),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = DesignTokens.Space8)
                        ) {
                            extraFooterContent()
                        }
                    }
                }
            }
        }

        // Floating Undo Snackbar at the bottom
        AnimatedVisibility(
            visible = undoMessage != null,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = DesignTokens.ScreenMargin, vertical = DesignTokens.Space16)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(DesignTokens.Space16))
                    .background(theme.elevated)
                    .border(1.dp, theme.accent.copy(alpha = 0.55f), RoundedCornerShape(DesignTokens.Space16))
                    .padding(horizontal = DesignTokens.Space16, vertical = DesignTokens.Space12)
                    .testTag("customize_undo_snackbar")
            ) {
                Text(
                    text = undoMessage.orEmpty(),
                    style = type.caption.copy(fontWeight = FontWeight.SemiBold),
                    color = theme.textPrimary,
                    modifier = Modifier.weight(1f)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(DesignTokens.Space12)) {
                    Text(
                        text = "UNDO",
                        style = type.caption.copy(fontWeight = FontWeight.ExtraBold),
                        color = theme.accent,
                        modifier = Modifier.clickable {
                            undoSnapshotSections?.let { viewModel.saveCustomSections(it) }
                            undoSnapshotHidden?.let { viewModel.setHiddenFeatures(it) }
                            undoMessage = null
                        }
                    )
                    Text(
                        text = "Dismiss",
                        style = type.caption,
                        color = theme.textSecondary,
                        modifier = Modifier.clickable { undoMessage = null }
                    )
                }
            }
        }
    }

    // BOTTOM SHEET: "Feature options" (Favorite, Tile size S/M/L, Pin to top, Add launcher shortcut, Move to another section, Hide)
    if (activeOptionsTarget != null) {
        val target = activeOptionsTarget!!
        val feat = target.feature
        val isFav = favoriteIds.contains(feat.id) || favoriteIds.contains(feat.toolId)
        val currentSize = tileSizes[feat.id] ?: FeatureTileSize.MEDIUM
        val isPinned = feat.id in pinnedTopIds
        val isHidden = feat.id in hiddenIds

        GlassSheet(
            onDismissRequest = { activeOptionsTarget = null },
            hazeState = null
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(DesignTokens.Space12),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("feature_options_sheet")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(DesignTokens.Space12),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = feat.icon,
                        contentDescription = null,
                        tint = theme.accent,
                        modifier = Modifier.size(28.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Feature options",
                            style = type.caption.copy(fontWeight = FontWeight.Bold),
                            color = theme.accent
                        )
                        Text(
                            text = feat.title,
                            style = type.title.copy(
                                fontSize = DesignTokens.TitleTextSize,
                                fontWeight = DesignTokens.TitleWeight
                            ),
                            color = theme.textPrimary
                        )
                    }
                }

                // 1. Favorite button
                FeatureOptionLargeButton(
                    icon = if (isFav) Icons.Default.Star else Icons.Outlined.StarOutline,
                    label = if (isFav) "Favorite (Starred)" else "Favorite",
                    testTag = "feature_opt_favorite",
                    onClick = {
                        viewModel.toggleFavoriteFeature(
                            context = context,
                            featureId = feat.id,
                            isCustomizationUnlocked = isCustomizationUnlocked
                        )
                    }
                )

                // 2. Tile size (S / M / L)
                Column(verticalArrangement = Arrangement.spacedBy(DesignTokens.Space8)) {
                    Text(
                        text = "Tile size (S / M / L)",
                        style = type.label.copy(
                            fontSize = DesignTokens.LabelTextSize,
                            fontWeight = DesignTokens.LabelWeight
                        ),
                        color = theme.textSecondary
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(DesignTokens.Space8),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        FeatureTileSize.entries.forEach { sz ->
                            val selected = currentSize == sz
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(DesignTokens.ButtonHeight)
                                    .clip(RoundedCornerShape(DesignTokens.Space16))
                                    .background(if (selected) theme.accent.copy(alpha = 0.22f) else theme.elevated)
                                    .border(
                                        width = 1.dp,
                                        color = if (selected) theme.accent else Color.White.copy(alpha = 0.12f),
                                        shape = RoundedCornerShape(DesignTokens.Space16)
                                    )
                                    .clickable {
                                        viewModel.setPerTileSize(feat.id, sz)
                                    }
                                    .testTag("feature_opt_size_${sz.code}")
                            ) {
                                Text(
                                    text = sz.label,
                                    style = type.button.copy(
                                        fontSize = DesignTokens.ButtonTextSize,
                                        fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold
                                    ),
                                    color = if (selected) theme.accent else theme.textPrimary
                                )
                            }
                        }
                    }
                }

                // 3. Pin to top
                FeatureOptionLargeButton(
                    icon = Icons.Outlined.PushPin,
                    label = if (isPinned) "Unpin from top" else "Pin to top",
                    testTag = "feature_opt_pin_top",
                    onClick = {
                        val nextPinned = if (isPinned) pinnedTopIds - feat.id else pinnedTopIds + feat.id
                        viewModel.setPinnedTopFeatures(nextPinned)
                        activeOptionsTarget = null
                    }
                )

                // 4. Add launcher shortcut
                FeatureOptionLargeButton(
                    icon = Icons.Outlined.AppShortcut,
                    label = "Add launcher shortcut",
                    testTag = "feature_opt_shortcut",
                    onClick = {
                        val ok = FeatureRegistry.requestPinFeatureShortcut(context, feat)
                        if (!ok) {
                            viewModel.showStatusMessage("Shortcut pinned for ${feat.title}")
                        }
                        activeOptionsTarget = null
                    }
                )

                // 5. Move to another section (plus Move Mode precise placement fallback)
                FeatureOptionLargeButton(
                    icon = Icons.Outlined.DriveFileMove,
                    label = "Move to another section",
                    testTag = "feature_opt_move_section",
                    onClick = {
                        showMoveToSectionPickerFor = target
                        activeOptionsTarget = null
                    }
                )

                // 6. Hide / Show
                FeatureOptionLargeButton(
                    icon = if (isHidden) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff,
                    label = if (isHidden) "Show" else "Hide",
                    testTag = "feature_opt_hide",
                    onClick = {
                        val nextHidden = if (isHidden) hiddenIds - feat.id else hiddenIds + feat.id
                        pushUndo(if (isHidden) "Showed \"${feat.title}\"" else "Hid \"${feat.title}\"")
                        viewModel.setHiddenFeatures(nextHidden)
                        activeOptionsTarget = null
                    }
                )
            }
        }
    }

    // Move to another section / Enter Move Mode picker
    if (showMoveToSectionPickerFor != null) {
        val target = showMoveToSectionPickerFor!!
        GlassSheet(
            onDismissRequest = { showMoveToSectionPickerFor = null },
            hazeState = null
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(DesignTokens.Space12),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("move_to_section_sheet")
            ) {
                Text(
                    text = "Move \"${target.feature.title}\"",
                    style = type.title.copy(
                        fontSize = DesignTokens.TitleTextSize,
                        fontWeight = DesignTokens.TitleWeight
                    ),
                    color = theme.textPrimary
                )

                // Fallback Move Mode button for precise placement
                FeatureOptionLargeButton(
                    icon = Icons.Outlined.OpenWith,
                    label = "Move mode (tap any row for precise placement)",
                    testTag = "enter_precise_move_mode_btn",
                    onClick = {
                        movingFeatureState = MovingFeatureState(
                            sourceSectionId = target.sectionId,
                            feature = target.feature
                        )
                        showMoveToSectionPickerFor = null
                    }
                )

                sections.forEach { destSec ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = DesignTokens.ButtonHeight)
                            .clip(RoundedCornerShape(DesignTokens.Space16))
                            .background(theme.elevated)
                            .clickable {
                                moveFeatureBetweenOrWithinSections(
                                    feature = target.feature,
                                    sourceSectionId = target.sectionId,
                                    targetSectionId = destSec.id
                                )
                                showMoveToSectionPickerFor = null
                            }
                            .padding(horizontal = DesignTokens.Space16, vertical = DesignTokens.Space12)
                    ) {
                        Text(
                            text = destSec.name,
                            style = type.body.copy(fontWeight = FontWeight.Bold),
                            color = theme.textPrimary
                        )
                        Icon(
                            imageVector = Icons.Outlined.ChevronRight,
                            contentDescription = null,
                            tint = theme.textSecondary
                        )
                    }
                }
            }
        }
    }

    // Section Columns picker (list / 2 / 3 / 4 / row)
    if (columnsPickerSection != null) {
        val sec = columnsPickerSection!!
        GlassSheet(
            onDismissRequest = { columnsPickerSection = null },
            hazeState = null
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(DesignTokens.Space12),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("section_columns_sheet")
            ) {
                Text(
                    text = "Columns for ${sec.name}",
                    style = type.title.copy(
                        fontSize = DesignTokens.TitleTextSize,
                        fontWeight = DesignTokens.TitleWeight
                    ),
                    color = theme.textPrimary
                )
                listOf(
                    SectionLayoutStyle.LIST,
                    SectionLayoutStyle.GRID_2,
                    SectionLayoutStyle.GRID_3,
                    SectionLayoutStyle.GRID_4,
                    SectionLayoutStyle.ROW
                ).forEach { style ->
                    val selected = sec.layoutStyle == style
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(DesignTokens.ButtonHeight)
                            .clip(RoundedCornerShape(DesignTokens.Space16))
                            .background(if (selected) theme.accent.copy(alpha = 0.22f) else theme.elevated)
                            .border(
                                width = 1.dp,
                                color = if (selected) theme.accent else Color.White.copy(alpha = 0.10f),
                                shape = RoundedCornerShape(DesignTokens.Space16)
                            )
                            .clickable {
                                val updated = sections.map {
                                    if (it.id == sec.id) it.copy(layoutStyle = style) else it
                                }
                                viewModel.saveCustomSections(updated)
                                columnsPickerSection = null
                            }
                            .padding(horizontal = DesignTokens.Space16)
                    ) {
                        Text(
                            text = style.label,
                            style = type.button.copy(
                                fontSize = DesignTokens.ButtonTextSize,
                                fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold
                            ),
                            color = if (selected) theme.accent else theme.textPrimary
                        )
                        if (selected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = theme.accent
                            )
                        }
                    }
                }
            }
        }
    }

    // Custom section Delete Confirmation Dialog
    if (deletingCustomSection != null) {
        val secToDelete = deletingCustomSection!!
        Dialog(onDismissRequest = { deletingCustomSection = null }) {
            Column(
                verticalArrangement = Arrangement.spacedBy(DesignTokens.Space12),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(DesignTokens.CardShape)
                    .background(theme.surface)
                    .border(1.dp, Color.White.copy(alpha = 0.14f), DesignTokens.CardShape)
                    .padding(DesignTokens.Space24)
                    .testTag("confirm_delete_section_dialog")
            ) {
                Text(
                    text = "Delete \"${secToDelete.name}\"?",
                    style = type.title.copy(
                        fontSize = DesignTokens.TitleTextSize,
                        fontWeight = DesignTokens.TitleWeight
                    ),
                    color = theme.textPrimary
                )
                Text(
                    text = "This removes the custom section. Its features stay available in the catalog.",
                    style = type.body,
                    color = theme.textSecondary
                )
                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TextButton(onClick = { deletingCustomSection = null }) {
                        Text("Cancel", color = theme.textSecondary)
                    }
                    Spacer(modifier = Modifier.width(DesignTokens.Space8))
                    Button(
                        onClick = {
                            pushUndo("Deleted section \"${secToDelete.name}\"")
                            val updated = sections.filterNot { it.id == secToDelete.id }
                            viewModel.saveCustomSections(updated)
                            deletingCustomSection = null
                        }
                    ) {
                        Text("Delete")
                    }
                }
            }
        }
    }

    // Create / Rename Section or Folder Dialog
    if (showCreateSectionDialog || editingSection != null) {
        val initial = editingSection
        var folderName by remember(initial) { mutableStateOf(initial?.name ?: "") }
        var selectedColor by remember(initial) { mutableStateOf(initial?.colorHex ?: 0xFF38BDF8L) }
        val colorChoices = listOf(0xFF38BDF8L, 0xFFF472B6L, 0xFF34D399L, 0xFFFBBF24L, 0xFFA78BFAL, 0xFFFB923CL)

        Dialog(
            onDismissRequest = {
                showCreateSectionDialog = false
                editingSection = null
            }
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(DesignTokens.Space12),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(DesignTokens.CardShape)
                    .background(theme.surface)
                    .border(1.dp, Color.White.copy(alpha = 0.14f), DesignTokens.CardShape)
                    .padding(DesignTokens.Space24)
            ) {
                Text(
                    text = if (initial != null) "Rename Section" else "New section or folder",
                    style = type.title.copy(
                        fontSize = DesignTokens.TitleTextSize,
                        fontWeight = DesignTokens.TitleWeight
                    ),
                    color = theme.textPrimary
                )
                OutlinedTextField(
                    value = folderName,
                    onValueChange = { folderName = it },
                    label = { Text("Section or folder name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(DesignTokens.Space8)) {
                    colorChoices.forEach { hex ->
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Color(hex))
                                .border(
                                    width = if (selectedColor == hex) 2.5.dp else 0.dp,
                                    color = Color.White,
                                    shape = CircleShape
                                )
                                .clickable { selectedColor = hex }
                        ) {
                            if (selectedColor == hex) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = BentoTextBlack,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TextButton(
                        onClick = {
                            showCreateSectionDialog = false
                            editingSection = null
                        }
                    ) {
                        Text("Cancel", color = theme.textSecondary)
                    }
                    Spacer(modifier = Modifier.width(DesignTokens.Space8))
                    Button(
                        onClick = {
                            val clean = folderName.trim().ifEmpty { "Custom Folder" }
                            val updated = if (initial != null) {
                                sections.map {
                                    if (it.id == initial.id) it.copy(name = clean, colorHex = selectedColor) else it
                                }
                            } else {
                                val newSec = CustomHomeSection(
                                    id = "folder_${System.currentTimeMillis()}",
                                    name = clean,
                                    colorHex = selectedColor,
                                    iconName = "Folder",
                                    isBuiltIn = false,
                                    featureIds = emptyList()
                                )
                                listOf(newSec) + sections
                            }
                            viewModel.saveCustomSections(updated)
                            showCreateSectionDialog = false
                            editingSection = null
                        }
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }

    // SAVED LAYOUTS (up to 5) IN A SHEET
    if (showSavedLayoutsSheet) {
        var newLayoutName by remember { mutableStateOf("") }
        GlassSheet(
            onDismissRequest = { showSavedLayoutsSheet = false },
            hazeState = null
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(DesignTokens.Space12),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("saved_layouts_sheet")
            ) {
                Text(
                    text = "Saved Layouts (${savedLayouts.size}/$MAX_SAVED_HOME_LAYOUTS)",
                    style = type.title.copy(
                        fontSize = DesignTokens.TitleTextSize,
                        fontWeight = DesignTokens.TitleWeight
                    ),
                    color = theme.textPrimary
                )
                Text(
                    text = "Save up to $MAX_SAVED_HOME_LAYOUTS named Home layouts and switch anytime with one tap.",
                    style = type.caption,
                    color = theme.textSecondary
                )

                if (savedLayouts.size < MAX_SAVED_HOME_LAYOUTS) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(DesignTokens.Space8),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = newLayoutName,
                            onValueChange = { newLayoutName = it },
                            placeholder = { Text("Layout name (e.g. Outdoor, Work)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        Button(
                            onClick = {
                                val clean = newLayoutName.trim().ifEmpty { "Layout ${savedLayouts.size + 1}" }
                                viewModel.saveNamedHomeLayout(clean)
                                newLayoutName = ""
                            },
                            modifier = Modifier.height(DesignTokens.ButtonHeight)
                        ) {
                            Text("Save")
                        }
                    }
                }

                if (savedLayouts.isEmpty()) {
                    Text(
                        text = "No saved layouts yet.",
                        style = type.body,
                        color = theme.textSecondary
                    )
                } else {
                    savedLayouts.forEach { layout ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = DesignTokens.ButtonHeight)
                                .clip(RoundedCornerShape(DesignTokens.Space16))
                                .background(theme.elevated)
                                .padding(horizontal = DesignTokens.Space16, vertical = DesignTokens.Space8)
                        ) {
                            Text(
                                text = layout.name,
                                style = type.body.copy(fontWeight = FontWeight.Bold),
                                color = theme.textPrimary,
                                modifier = Modifier.weight(1f)
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(DesignTokens.Space8)) {
                                TextButton(
                                    onClick = {
                                        viewModel.applyNamedHomeLayout(layout)
                                        showSavedLayoutsSheet = false
                                    }
                                ) {
                                    Text("Apply", color = theme.accent, fontWeight = FontWeight.Bold)
                                }
                                IconButton(
                                    onClick = {
                                        viewModel.deleteNamedHomeLayout(layout.id, layout.name)
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.DeleteOutline,
                                        contentDescription = "Delete ${layout.name}",
                                        tint = theme.textSecondary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FeatureOptionLargeButton(
    icon: ImageVector,
    label: String,
    testTag: String,
    onClick: () -> Unit
) {
    val theme = LocalDyneTheme.current
    val type = LocalDyneType.current

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DesignTokens.Space12),
        modifier = Modifier
            .fillMaxWidth()
            .height(DesignTokens.ButtonHeight)
            .clip(RoundedCornerShape(DesignTokens.Space16))
            .background(theme.elevated)
            .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(DesignTokens.Space16))
            .clickable(onClick = onClick)
            .padding(horizontal = DesignTokens.Space16)
            .testTag(testTag)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = theme.accent,
            modifier = Modifier.size(DesignTokens.IconSize)
        )
        Text(
            text = label,
            style = type.button.copy(
                fontSize = DesignTokens.ButtonTextSize,
                fontWeight = DesignTokens.ButtonWeight
            ),
            color = theme.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}


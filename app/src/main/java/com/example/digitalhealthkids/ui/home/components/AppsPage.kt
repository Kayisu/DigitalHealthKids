package com.example.digitalhealthkids.ui.home.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import android.content.Context
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.digitalhealthkids.core.util.AppUtils
import com.example.digitalhealthkids.ui.home.HomeViewModel

@Composable
fun AppsPage(
    appList: List<HomeViewModel.AppUiModel>,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    sortOption: HomeViewModel.SortOption,
    onSortChange: (HomeViewModel.SortOption) -> Unit,
    topApps: List<HomeViewModel.AppUiModel>,
    topCategories: List<HomeViewModel.CategoryHighlight>,
    allCategories: List<HomeViewModel.CategoryHighlight>,
    listMode: HomeViewModel.ListMode,
    selectedCategory: String?,
    viewMode: HomeViewModel.ViewMode,
    onViewAll: () -> Unit,
    onViewCategories: () -> Unit,
    onViewCategory: (String) -> Unit,
    onBackToAll: () -> Unit,
    onLoadMore: () -> Unit,
    hasMore: Boolean,
    onAppClick: (HomeViewModel.AppUiModel) -> Unit,
    onToggleBlock: (String) -> Unit
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        when (viewMode) {
            HomeViewModel.ViewMode.OVERVIEW -> {
                UsageHighlights(
                    topApps = topApps,
                    topCategories = topCategories,
                    onViewAll = onViewAll,
                    onViewCategories = onViewCategories,
                    onViewCategory = onViewCategory
                )
            }
            HomeViewModel.ViewMode.ALL_APPS -> {
                AppSearchBar(value = searchQuery, onValueChange = onSearchChange)
                SortSelector(selected = sortOption, onSelected = onSortChange)
                AppListSection(
                    title = "Tüm Uygulamalar",
                    apps = appList,
                    onAppClick = onAppClick,
                    onToggleBlock = onToggleBlock,
                    onLoadMore = onLoadMore,
                    hasMore = hasMore,
                    context = context
                )
            }
            HomeViewModel.ViewMode.CATEGORIES -> {
                CategoryGrid(
                    categories = allCategories,
                    onCategoryClick = onViewCategory,
                    onViewAllApps = onViewAll
                )
            }
            HomeViewModel.ViewMode.CATEGORY_DETAIL -> {
                AppSearchBar(value = searchQuery, onValueChange = onSearchChange)
                SortSelector(selected = sortOption, onSelected = onSortChange, hideCategory = true)
                selectedCategory?.let { cat ->
                    CategoryHeader(cat, onBack = onBackToAll)
                }
                AppListSection(
                    title = selectedCategory ?: "Kategori",
                    apps = appList,
                    onAppClick = onAppClick,
                    onToggleBlock = onToggleBlock,
                    onLoadMore = onLoadMore,
                    hasMore = hasMore,
                    context = context
                )
            }
        }
    }
}

@Composable
private fun AppSearchBar(value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        label = { Text("Uygulama ara") },
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun SortSelector(
    selected: HomeViewModel.SortOption,
    onSelected: (HomeViewModel.SortOption) -> Unit,
    hideCategory: Boolean = false
) {
    val options = remember(hideCategory) {
        HomeViewModel.SortOption.values().toList().filterNot { hideCategory && it == HomeViewModel.SortOption.CATEGORY }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { option ->
            FilterChip(
                selected = option == selected,
                onClick = { onSelected(option) },
                label = { Text(optionLabel(option)) }
            )
        }
    }
}

@Composable
private fun UsageHighlights(
    topApps: List<HomeViewModel.AppUiModel>,
    topCategories: List<HomeViewModel.CategoryHighlight>,
    onViewAll: () -> Unit,
    onViewCategories: () -> Unit,
    onViewCategory: (String) -> Unit
) {
    if (topApps.isEmpty() && topCategories.isEmpty()) return

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (topApps.isNotEmpty()) {
            Text("En çok kullanılanlar", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                topApps.forEach { app ->
                    ElevatedCard(modifier = Modifier.weight(1f, fill = false)) {
                        Column(Modifier.padding(12.dp)) {
                            Text(app.appName, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                            Text("${app.averageMinutes} dk/hafta", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            FilledTonalButton(
                onClick = onViewAll,
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text("Tüm uygulamaları gör")
            }
        }

        if (topCategories.isNotEmpty()) {
            Text("Popüler kategoriler", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                topCategories.forEach { cat ->
                    ElevatedCard(
                        modifier = Modifier.weight(1f, fill = false),
                        onClick = { onViewCategory(cat.name) }
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Text(cat.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                            Text("${cat.totalMinutes} dk/hafta", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            FilledTonalButton(
                onClick = onViewCategories,
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text("Kategorileri görüntüle")
            }
        }
    }
}

@Composable
private fun AppListSection(
    title: String,
    apps: List<HomeViewModel.AppUiModel>,
    onAppClick: (HomeViewModel.AppUiModel) -> Unit,
    onToggleBlock: (String) -> Unit,
    onLoadMore: () -> Unit,
    hasMore: Boolean,
    context: android.content.Context
) {
    Text(
        title,
        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
    )
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(apps) { app ->
            val cleanName = AppUtils.getAppName(context, app.packageName, app.appName)
            AppUsageRowItem(
                name = cleanName,
                packageName = app.packageName,
                minutes = app.averageMinutes,
                isBlocked = app.isBlocked,
                onBlockToggle = { onToggleBlock(app.packageName) },
                onClick = { onAppClick(app) }
            )
        }

        if (apps.isEmpty()) {
            item {
                Text(
                    text = "Sonuç bulunamadı",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (hasMore) {
            item {
                OutlinedButton(onClick = onLoadMore, modifier = Modifier.fillMaxWidth()) {
                    Text("Daha fazlasını yükle")
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CategoryGrid(
    categories: List<HomeViewModel.CategoryHighlight>,
    onCategoryClick: (String) -> Unit,
    onViewAllApps: () -> Unit
) {
    if (categories.isEmpty()) return

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Kategoriler", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            categories.forEach { cat ->
                ElevatedCard(onClick = { onCategoryClick(cat.name) }) {
                    Column(Modifier.padding(12.dp)) {
                        Text(cat.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                        Text("${cat.appCount} uygulama", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        FilledTonalButton(
            onClick = onViewAllApps,
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Text("Tüm uygulamaları gör")
        }
    }
}

private fun optionLabel(option: HomeViewModel.SortOption): String = when (option) {
    HomeViewModel.SortOption.WEEKLY_DESC -> "Süre ↓"
    HomeViewModel.SortOption.WEEKLY_ASC -> "Süre ↑"
    HomeViewModel.SortOption.NAME -> "Ad"
    HomeViewModel.SortOption.CATEGORY -> "Kategori"
}

@Composable
private fun CategoryHeader(title: String, onBack: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedButton(onClick = onBack) { Text("Tüm uygulamalar") }
        Text(
            title,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
        )
    }
}
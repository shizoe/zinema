package com.zinema.app.feature.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zinema.app.core.domain.model.Content
import com.zinema.app.core.domain.model.ContentType
import com.zinema.app.core.ui.components.ContentRail
import com.zinema.app.core.ui.theme.ZinemaColors

/**
 * Search screen for mobile + TV (blueprint T-054): a search field, recent-search
 * chips when the query is empty, and results grouped by content type otherwise.
 *
 * Uses a styled text field rather than the M3 SearchBar for reliability + TV
 * compatibility (PHASE-8 §Deviations).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SearchScreen(
    onItemClick: (Content) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SearchViewModel = hiltViewModel(),
) {
    val query by viewModel.query.collectAsStateWithLifecycle()
    val recentSearches by viewModel.recentSearches.collectAsStateWithLifecycle()
    val results by viewModel.results.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ZinemaColors.Background),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Text(text = "←", color = ZinemaColors.OnBackground, fontSize = 22.sp)
            }
            OutlinedTextField(
                value = query,
                onValueChange = viewModel::onQueryChange,
                placeholder = { Text("Search movies, shows, more") },
                singleLine = true,
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onQueryChange("") }) {
                            Text(text = "✕", color = ZinemaColors.TextSecondary)
                        }
                    }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { viewModel.onQuerySubmit() }),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = ZinemaColors.OnBackground,
                    unfocusedTextColor = ZinemaColors.OnBackground,
                    focusedBorderColor = ZinemaColors.Primary,
                    unfocusedBorderColor = ZinemaColors.TextTertiary,
                    cursorColor = ZinemaColors.Primary,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
        }

        if (query.isBlank()) {
            RecentSearches(
                recents = recentSearches,
                onSelect = viewModel::onRecentSelected,
                onClear = viewModel::clearRecentSearches,
            )
        } else {
            when (val state = results) {
                SearchUiState.Idle -> Unit
                SearchUiState.Empty -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(text = "No results for \"$query\"", color = ZinemaColors.TextSecondary)
                    }
                }
                is SearchUiState.Results -> {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        state.grouped.forEach { (type, items) ->
                            item(key = type.name) {
                                ContentRail(title = typeLabel(type), items = items, onItemClick = onItemClick)
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RecentSearches(
    recents: List<String>,
    onSelect: (String) -> Unit,
    onClear: () -> Unit,
) {
    if (recents.isEmpty()) return
    Column(modifier = Modifier.padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = "Recent searches", color = ZinemaColors.OnBackground)
            TextButton(onClick = onClear) {
                Text(text = "Clear", color = ZinemaColors.TextSecondary)
            }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            recents.forEach { term ->
                Text(
                    text = term,
                    color = ZinemaColors.OnSurface,
                    fontSize = 13.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(ZinemaColors.SurfaceVariant)
                        .clickable { onSelect(term) }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                )
            }
        }
    }
}

private fun typeLabel(type: ContentType): String = when (type) {
    ContentType.MOVIE -> "Movies"
    ContentType.TV -> "TV Shows"
    ContentType.ANIME -> "Anime"
    ContentType.SHORT -> "Short Clips"
    ContentType.SPORTS -> "Sports"
}

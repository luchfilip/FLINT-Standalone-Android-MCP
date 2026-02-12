package dev.mcphub.acp.sample.music.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.mcphub.acp.Acp
import dev.mcphub.acp.sample.music.data.FakeData
import dev.mcphub.acp.semantics.acpAction
import dev.mcphub.acp.semantics.acpContent
import dev.mcphub.acp.semantics.acpItem
import dev.mcphub.acp.semantics.acpList

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchResultsScreen(
    query: String,
    onTrackClick: (String) -> Unit,
    onBack: () -> Unit
) {
    Acp.screen("search_results")

    val results = FakeData.searchTracks(query)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Search: $query") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        if (results.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "No results found for \"$query\"",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .acpList("results", "Search results for songs")
            ) {
                itemsIndexed(results) { index, track ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .acpItem(index)
                            .clickable { onTrackClick(track.id) }
                            .acpAction("select", "Select this track")
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = null,
                            modifier = Modifier.size(40.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 12.dp)
                        ) {
                            Text(
                                text = track.title,
                                style = MaterialTheme.typography.titleSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.acpContent("title")
                            )
                            Text(
                                text = track.artist,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.acpContent("artist")
                            )
                        }
                        Text(
                            text = track.duration,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.acpContent("duration")
                        )
                    }
                    if (index < results.lastIndex) {
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    }
                }
            }
        }
    }
}

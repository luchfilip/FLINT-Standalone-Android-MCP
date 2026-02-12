package dev.mcphub.acp.sample.music.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.mcphub.acp.Acp
import dev.mcphub.acp.sample.music.data.FakeData
import dev.mcphub.acp.semantics.acpAction
import dev.mcphub.acp.semantics.acpContent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackDetailScreen(
    trackId: String,
    onBack: () -> Unit
) {
    Acp.screen("track_detail")

    val track = FakeData.getTrack(trackId)
    var isFavorite by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Track Details") },
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
        if (track == null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Track not found",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Album,
                    contentDescription = null,
                    modifier = Modifier.size(200.dp),
                    tint = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = track.title,
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.acpContent("title")
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = track.artist,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.acpContent("artist")
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = track.duration,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.acpContent("duration")
                )

                Spacer(modifier = Modifier.height(32.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilledTonalButton(
                        onClick = { /* Play action */ },
                        modifier = Modifier
                            .padding(end = 16.dp)
                            .acpAction("play", "Play this track")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Play",
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "Play",
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }

                    IconButton(
                        onClick = { isFavorite = !isFavorite },
                        modifier = Modifier.acpAction("favorite", "Toggle favorite")
                    ) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = if (isFavorite) "Unfavorite" else "Favorite",
                            tint = if (isFavorite) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }
        }
    }
}

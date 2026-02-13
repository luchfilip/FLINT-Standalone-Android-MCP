package com.flintsdk.sample.music.data

data class Track(
    val id: String,
    val title: String,
    val artist: String,
    val duration: String,
    val albumArt: String
)

data class Playlist(
    val id: String,
    val name: String,
    val description: String,
    val trackIds: List<String>
)

data class Artist(
    val id: String,
    val name: String
)

object FakeData {

    val artists = listOf(
        Artist(id = "artist-1", name = "Luna Wave"),
        Artist(id = "artist-2", name = "The Drift"),
        Artist(id = "artist-3", name = "Echo Chamber")
    )

    val tracks = listOf(
        Track(
            id = "track-1",
            title = "Midnight Drive",
            artist = "Luna Wave",
            duration = "3:42",
            albumArt = "https://placehold.co/300x300/1a1a2e/e94560?text=MD"
        ),
        Track(
            id = "track-2",
            title = "Neon Lights",
            artist = "Luna Wave",
            duration = "4:15",
            albumArt = "https://placehold.co/300x300/16213e/0f3460?text=NL"
        ),
        Track(
            id = "track-3",
            title = "Ocean Breeze",
            artist = "The Drift",
            duration = "3:58",
            albumArt = "https://placehold.co/300x300/1a1a2e/533483?text=OB"
        ),
        Track(
            id = "track-4",
            title = "Echoes of Tomorrow",
            artist = "Echo Chamber",
            duration = "5:01",
            albumArt = "https://placehold.co/300x300/0f3460/e94560?text=ET"
        ),
        Track(
            id = "track-5",
            title = "Rooftop Sunset",
            artist = "The Drift",
            duration = "4:33",
            albumArt = "https://placehold.co/300x300/533483/e94560?text=RS"
        ),
        Track(
            id = "track-6",
            title = "Static Dreams",
            artist = "Echo Chamber",
            duration = "3:27",
            albumArt = "https://placehold.co/300x300/e94560/1a1a2e?text=SD"
        )
    )

    val playlists = listOf(
        Playlist(
            id = "playlist-1",
            name = "Chill Vibes",
            description = "Relaxing tracks for winding down",
            trackIds = listOf("track-1", "track-3", "track-5")
        ),
        Playlist(
            id = "playlist-2",
            name = "Late Night Sessions",
            description = "Perfect for late night coding",
            trackIds = listOf("track-2", "track-4", "track-6")
        ),
        Playlist(
            id = "playlist-3",
            name = "All Hits",
            description = "Every track in the library",
            trackIds = listOf("track-1", "track-2", "track-3", "track-4", "track-5", "track-6")
        )
    )

    fun searchTracks(query: String): List<Track> {
        val lowerQuery = query.lowercase()
        return tracks.filter { track ->
            track.title.lowercase().contains(lowerQuery) ||
                track.artist.lowercase().contains(lowerQuery)
        }
    }

    fun getTrack(id: String): Track? {
        return tracks.find { it.id == id }
    }

    fun getPlaylist(id: String): Playlist? {
        return playlists.find { it.id == id }
    }

    fun getTracksForPlaylist(id: String): List<Track> {
        val playlist = getPlaylist(id) ?: return emptyList()
        return playlist.trackIds.mapNotNull { trackId -> getTrack(trackId) }
    }
}

package dev.acpsdk.sample.music.tools

import dev.acpsdk.annotations.AcpParam
import dev.acpsdk.annotations.AcpTool
import dev.acpsdk.annotations.AcpToolHost
import dev.acpsdk.sample.music.di.AppNavigatorHolder
import javax.inject.Inject

@AcpToolHost
class MusicTools @Inject constructor(
    private val navigator: AppNavigatorHolder
) {
    @AcpTool(
        name = "search",
        description = "Search for tracks. Opens search results screen.",
        target = "search_results"
    )
    fun search(@AcpParam(description = "Search query") query: String) {
        navigator.navigate("search/$query")
    }

    @AcpTool(
        name = "open_playlist",
        description = "Open a playlist by ID.",
        target = "playlist_detail"
    )
    fun openPlaylist(@AcpParam(description = "Playlist ID") playlistId: String) {
        navigator.navigate("playlist/$playlistId")
    }

    @AcpTool(
        name = "go_home",
        description = "Navigate to home screen.",
        target = "home"
    )
    fun goHome() {
        navigator.navigate("home")
    }
}

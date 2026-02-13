package com.flintsdk.sample.music.tools

import com.flintsdk.annotations.FlintParam
import com.flintsdk.annotations.FlintTool
import com.flintsdk.annotations.FlintToolHost
import com.flintsdk.sample.music.di.AppNavigatorHolder
import javax.inject.Inject

@FlintToolHost
class MusicTools @Inject constructor(
    private val navigator: AppNavigatorHolder
) {
    @FlintTool(
        name = "search",
        description = "Search for tracks. Opens search results screen.",
        target = "search_results"
    )
    fun search(@FlintParam(description = "Search query") query: String) {
        navigator.navigateToSearch(query)
    }

    @FlintTool(
        name = "open_playlist",
        description = "Open a playlist by ID.",
        target = "playlist_detail"
    )
    fun openPlaylist(@FlintParam(description = "Playlist ID") playlistId: String) {
        navigator.navigateToPlaylist(playlistId)
    }

    @FlintTool(
        name = "go_home",
        description = "Navigate to home screen.",
        target = "home"
    )
    fun goHome() {
        navigator.navigateHome()
    }
}

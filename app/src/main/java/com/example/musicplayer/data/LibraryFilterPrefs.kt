package com.example.musicplayer.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

enum class LibraryGroupMode(val title: String) {
    TRACKS("Треки"),
    ARTISTS("Артисты"),
    ALBUMS("Альбомы"),
    ARTISTS_ALBUMS("Артисты и альбомы")
}

enum class LibraryQualityFilter(val title: String) {
    ALL("Все форматы"),
    LOSSLESS("Lossless"),
    FLAC("FLAC"),
    MP3("MP3"),
    FAVORITES("В избранном")
}

enum class LibrarySortMode(val title: String, val shortLabel: String) {
    TITLE_ASC("Название (А–Я)", "А–Я"),
    TITLE_DESC("Название (Я–А)", "Я–А"),
    ARTIST("Исполнитель", "Исполн."),
    ALBUM("Альбом", "Альбом"),
    NEWEST("Сначала новые", "Новые"),
    DURATION("Длительность", "Длина"),
    CUSTOM_ORDER("Порядок плейлиста", "Порядок")
}

object LibraryFilterPrefs {
    private const val PREFS_NAME = "music_player_prefs"

    private const val KEY_SHOW_FAV = "pl_show_favorites"
    private const val KEY_SHOW_LOSSLESS = "pl_show_lossless"
    private const val KEY_SHOW_ARTISTS = "pl_show_artists"
    private const val KEY_SHOW_ALBUMS = "pl_show_albums"
    private const val KEY_SHOW_ARTISTS_ALBUMS = "pl_show_artists_albums"
    private const val KEY_SHOW_FOLDERS = "pl_show_folders"
    private const val KEY_SHOW_FILTER_BAR = "pl_show_filter_bar"
    private const val KEY_SHOW_ALBUM_SUBTITLE = "lib_show_album_subtitle"

    var showFavoritesPlaylist by mutableStateOf(true)
        private set
    var showLosslessPlaylist by mutableStateOf(true)
        private set
    var showArtistsFilter by mutableStateOf(true)
        private set
    var showAlbumsFilter by mutableStateOf(true)
        private set
    var showArtistsAlbumsFilter by mutableStateOf(true)
        private set
    var showFoldersInPlaylists by mutableStateOf(true)
        private set
    var showFilterBar by mutableStateOf(true)
        private set
    var showAlbumSubtitle by mutableStateOf(true)
        private set

    @Volatile
    private var loaded = false

    fun ensureLoaded(context: Context) {
        if (loaded) return
        synchronized(this) {
            if (loaded) return
            val sp = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            showFavoritesPlaylist = sp.getBoolean(KEY_SHOW_FAV, true)
            showLosslessPlaylist = sp.getBoolean(KEY_SHOW_LOSSLESS, true)
            showArtistsFilter = sp.getBoolean(KEY_SHOW_ARTISTS, true)
            showAlbumsFilter = sp.getBoolean(KEY_SHOW_ALBUMS, true)
            showArtistsAlbumsFilter = sp.getBoolean(KEY_SHOW_ARTISTS_ALBUMS, true)
            showFoldersInPlaylists = sp.getBoolean(KEY_SHOW_FOLDERS, true)
            showFilterBar = sp.getBoolean(KEY_SHOW_FILTER_BAR, true)
            showAlbumSubtitle = sp.getBoolean(KEY_SHOW_ALBUM_SUBTITLE, true)
            loaded = true
        }
    }

    fun setShowFavoritesPlaylist(context: Context, value: Boolean) {
        ensureLoaded(context)
        showFavoritesPlaylist = value
        saveBool(context, KEY_SHOW_FAV, value)
    }

    fun setShowLosslessPlaylist(context: Context, value: Boolean) {
        ensureLoaded(context)
        showLosslessPlaylist = value
        saveBool(context, KEY_SHOW_LOSSLESS, value)
    }

    fun setShowArtistsFilter(context: Context, value: Boolean) {
        ensureLoaded(context)
        showArtistsFilter = value
        saveBool(context, KEY_SHOW_ARTISTS, value)
    }

    fun setShowAlbumsFilter(context: Context, value: Boolean) {
        ensureLoaded(context)
        showAlbumsFilter = value
        saveBool(context, KEY_SHOW_ALBUMS, value)
    }

    fun setShowArtistsAlbumsFilter(context: Context, value: Boolean) {
        ensureLoaded(context)
        showArtistsAlbumsFilter = value
        saveBool(context, KEY_SHOW_ARTISTS_ALBUMS, value)
    }

    fun setShowFoldersInPlaylists(context: Context, value: Boolean) {
        ensureLoaded(context)
        showFoldersInPlaylists = value
        saveBool(context, KEY_SHOW_FOLDERS, value)
    }

    fun setShowFilterBar(context: Context, value: Boolean) {
        ensureLoaded(context)
        showFilterBar = value
        saveBool(context, KEY_SHOW_FILTER_BAR, value)
    }

    fun setShowAlbumSubtitle(context: Context, value: Boolean) {
        ensureLoaded(context)
        showAlbumSubtitle = value
        saveBool(context, KEY_SHOW_ALBUM_SUBTITLE, value)
    }

    private fun saveBool(context: Context, key: String, value: Boolean) {
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean(key, value).apply()
    }
}

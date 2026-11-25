package com.example.musicplayer.data

import android.net.Uri

data class LocalTrack(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val contentUri: Uri,
    val artworkUri: Uri?,
    val folderName: String,
    val formatLabel: String,
    val bitrateKbps: Int,
    val sampleRateHz: Int = 44100,
    val dateAddedSec: Long = 0L
)

data class UserPlaylist(
    val id: String,
    val name: String,
    val trackIds: List<Long>
)

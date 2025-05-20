package com.theveloper.pixelplay.data.model

import android.net.Uri

data class Song(
    val id: String, // MediaStore.Audio.Media._ID
    val title: String,
    val artist: String,
    val album: String,
    val albumId: Long, // MediaStore.Audio.Media.ALBUM_ID para obtener carátula
    val contentUri: Uri, // Uri para cargar la canción
    val albumArtUri: Uri?, // Uri de la carátula del álbum
    val duration: Long // en milisegundos
)
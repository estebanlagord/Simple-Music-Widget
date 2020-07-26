package com.smartpocket.musicwidget.activities

import android.database.Cursor
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartpocket.musicwidget.backend.AlbumArtLoader
import com.smartpocket.musicwidget.backend.SongListLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class SongListVM(private val songLoader: SongListLoader,
                 private val albumArtLoader: AlbumArtLoader)
    : ViewModel() {

    private val tag = javaClass.simpleName
    private var currentSearch: Job? = null
    val cursorLD = MutableLiveData<Cursor>()

    fun getCursor(query: String? = null) {
        currentSearch?.cancel("Cancelling previous search")
        currentSearch = viewModelScope.launch(Dispatchers.IO) {
            Log.i(tag, "Getting song list cursor for query: $query")
//            delay(5000)
            val cursor = if (query.isNullOrBlank())
                songLoader.getCursor()
            else
                songLoader.getFilteredCursor(query)
            cursorLD.postValue(cursor)
            Log.i(tag, "Song list cursor retrieved")
        }
/*        viewModelScope.launch(Dispatchers.IO) {
//            albumArtLoader.cacheAllAlbumArt()
        }*/
    }
}
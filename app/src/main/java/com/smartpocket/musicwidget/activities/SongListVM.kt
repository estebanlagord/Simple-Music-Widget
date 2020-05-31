package com.smartpocket.musicwidget.activities

import android.app.Application
import android.database.Cursor
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.smartpocket.musicwidget.backend.SongListLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class SongListVM(application: Application) : AndroidViewModel(application) {
    private val songLoader = SongListLoader.getInstance(application)
    private val tag = javaClass.simpleName
    private var currentSearch: Job? = null
    val cursorLD = MutableLiveData<Cursor>()

    init {
        getCursor()
    }

    fun getCursor(query: String? = null) {
        currentSearch?.cancel("Cancelling previous search")
        currentSearch = viewModelScope.launch(Dispatchers.IO) {
            Log.i(tag, "Getting song list cursor for query: $query")
//            delay(5000)
            val cursor = if (query.isNullOrBlank())
                songLoader.cursor
            else
                songLoader.getFilteredCursor(query)
            cursorLD.postValue(cursor)
            Log.i(tag, "Song list cursor retrieved")
        }
    }
}
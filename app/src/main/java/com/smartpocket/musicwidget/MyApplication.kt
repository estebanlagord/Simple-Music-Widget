package com.smartpocket.musicwidget

import androidx.multidex.MultiDexApplication
import com.smartpocket.musicwidget.activities.SongListVM
import com.smartpocket.musicwidget.backend.AlbumArtLoader
import com.smartpocket.musicwidget.backend.MusicLoader
import com.smartpocket.musicwidget.backend.SongListLoader
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.android.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.dsl.module

class MyApplication : MultiDexApplication() {
    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidLogger()
            androidContext(this@MyApplication)
            modules(appModule)
        }
    }

    val appModule = module {
        single { AlbumArtLoader(get()) }
        single { MusicLoader(get(), get()) }
        single { SongListLoader(get()) }
        viewModel { SongListVM(get()) }
    }
}
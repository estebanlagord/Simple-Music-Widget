package com.smartpocket.musicwidget.activities

import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.DividerItemDecoration
import com.smartpocket.musicwidget.MusicWidget
import com.smartpocket.musicwidget.R
import com.smartpocket.musicwidget.backend.AdViewHelper
import com.smartpocket.musicwidget.backend.SongClickListener
import com.smartpocket.musicwidget.backend.SongCursorRecyclerAdapter
import com.smartpocket.musicwidget.model.Song
import com.smartpocket.musicwidget.service.MusicService
import kotlinx.android.synthetic.main.song_list_activity.*
import org.koin.android.viewmodel.ext.android.viewModel
import java.util.concurrent.atomic.AtomicBoolean

private const val REQ_CODE = 1

class SongListActivity : AppCompatActivity(), SearchView.OnQueryTextListener {
    private lateinit var adapter: SongCursorRecyclerAdapter
    private lateinit var adViewHelper: AdViewHelper
    private val viewModel: SongListVM by viewModel()
    private var isSearching = AtomicBoolean(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.song_list_activity)
        setSupportActionBar(toolbar)
        handleIntent(intent)

        adapter = SongCursorRecyclerAdapter(null, object : SongClickListener {
            override fun onSongSelected(song: Song) {
                Log.i("SongListActivity click", song.toString())
                val serviceIntent = Intent(this@SongListActivity, MusicService::class.java)
                serviceIntent.action = MusicWidget.ACTION_JUMP_TO
                serviceIntent.putExtra("song", song)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(serviceIntent)
                } else {
                    startService(serviceIntent)
                }
                finish()
            }
        })

        listView.also {
            val dividerItemDecoration = DividerItemDecoration(it.context, LinearLayout.VERTICAL)
            it.addItemDecoration(dividerItemDecoration)
            it.adapter = adapter
            it.addOnScrollListener(MyRecyclerViewOnScrollListener(fabBtn, isSearching))

            //has to be called AFTER RecyclerView.setAdapter()
            with(fastscroll) {
                setRecyclerView(it)
                setViewProvider(MyFastScrollScrollerViewProvider(fabBtn))
            }
        }

        viewModel.cursorLD.observe(this, Observer {
            adapter.changeCursor(it)
        })

        if (needsToRequestPermissions()) {
            val intent = Intent(this, ConfigurationActivity::class.java)
            startActivityForResult(intent, REQ_CODE)
        } else {
            viewModel.getCursor()
        }
        adViewHelper = AdViewHelper(adViewContainer, this)
        adViewHelper.showBanner(true)
    }

    override fun onDestroy() {
        adViewHelper.destroy()
        super.onDestroy()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQ_CODE) {
            if (needsToRequestPermissions()) {
                finish()
            } else {
                viewModel.getCursor()
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.song_list, menu)
        val searchItem = menu.findItem(R.id.search)
        searchItem.setOnActionExpandListener(object : MenuItem.OnActionExpandListener {
            override fun onMenuItemActionExpand(item: MenuItem?): Boolean {
                isSearching.set(true)
                fabBtn.shrink()
                return true
            }

            override fun onMenuItemActionCollapse(item: MenuItem?): Boolean {
                isSearching.set(false)
                fabBtn.extend()
                return true
            }
        })

        // Associate searchable configuration with the SearchView
        val searchManager = getSystemService(Context.SEARCH_SERVICE) as SearchManager
        val searchView = searchItem.actionView as SearchView
        searchView.setSearchableInfo(searchManager.getSearchableInfo(componentName))
        searchView.setOnQueryTextListener(this)

        fabBtn.setOnClickListener {
            if (searchItem.isActionViewExpanded) searchItem.collapseActionView()
            else searchItem.expandActionView()
        }
        return true
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        if (Intent.ACTION_SEARCH == intent.action) {
            val searchText = intent.getStringExtra(SearchManager.QUERY)
            Log.d("SongListActivity", "handleIntent Searched for: $searchText")
            doSearch(searchText!!)
        }
    }

    override fun onQueryTextChange(newText: String): Boolean {
        Log.d("SongListActivity", "onQueryTextChange Searched for: $newText")
        doSearch(newText)
        return true
    }

    private fun doSearch(newText: String) = viewModel.getCursor(newText)

    override fun onQueryTextSubmit(query: String) = false
}
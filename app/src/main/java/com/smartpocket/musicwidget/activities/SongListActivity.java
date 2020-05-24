package com.smartpocket.musicwidget.activities;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.RecyclerView;

import com.futuremind.recyclerviewfastscroll.FastScroller;
import com.smartpocket.musicwidget.MusicWidget;
import com.smartpocket.musicwidget.R;
import com.smartpocket.musicwidget.backend.SongClickListener;
import com.smartpocket.musicwidget.backend.SongCursorRecyclerAdapter;
import com.smartpocket.musicwidget.backend.SongListLoader;
import com.smartpocket.musicwidget.model.Song;
import com.smartpocket.musicwidget.service.MusicService;

import org.jetbrains.annotations.NotNull;

public class SongListActivity extends AppCompatActivity implements SearchView.OnQueryTextListener {
    private final SongListLoader songLoader = SongListLoader.getInstance(this);
    private SongCursorRecyclerAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.song_list_activity);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(getResources().getString(R.string.title_activity_song_list));
        getSupportActionBar().setLogo(R.drawable.ic_launcher);

        handleIntent(getIntent());

        RecyclerView recyclerView = findViewById(R.id.listView);
        FastScroller fastScroller = findViewById(R.id.fastscroll);

        Cursor listCursor = songLoader.getCursor();
        adapter = new SongCursorRecyclerAdapter(listCursor, new SongClickListener() {
            @Override
            public void onSongSelected(@NotNull Song song) {
                Log.i("SongListactivity click", song.toString());
                Intent serviceIntent = new Intent(SongListActivity.this, MusicService.class);
                serviceIntent.setAction(MusicWidget.ACTION_JUMP_TO);
                serviceIntent.putExtra("song", song);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    SongListActivity.this.startForegroundService(serviceIntent);
                } else {
                    SongListActivity.this.startService(serviceIntent);
                }
                finish();
            }
        });

        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(recyclerView.getContext(),
                LinearLayout.VERTICAL);
        recyclerView.addItemDecoration(dividerItemDecoration);
        recyclerView.setAdapter(adapter);

        //has to be called AFTER RecyclerView.setAdapter()
        fastScroller.setRecyclerView(recyclerView);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.song_list, menu);
        MenuItem searchItem = menu.findItem(R.id.search);

        // Associate searchable configuration with the SearchView
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        SearchView searchView = (SearchView) searchItem.getActionView();
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        searchView.setOnQueryTextListener(this);

        return true;
    }


    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            String searchText = intent.getStringExtra(SearchManager.QUERY);
            Log.d("SongListActivity", "handleIntent Searched for: " + searchText);
            doSearch(searchText);
        }
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        Log.d("SongListActivity", "onQueryTextChange Searched for: " + newText);
        doSearch(newText);
        return true;
    }

    private void doSearch(String newText) {
        Cursor newCursor = songLoader.getFilteredCursor(newText);
        adapter.changeCursor(newCursor);
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        return false;
    }
}

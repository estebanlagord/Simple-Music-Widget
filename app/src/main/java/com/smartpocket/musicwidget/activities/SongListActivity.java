package com.smartpocket.musicwidget.activities;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.FilterQueryProvider;
import android.widget.ListView;

import com.smartpocket.musicwidget.MusicWidget;
import com.smartpocket.musicwidget.R;
import com.smartpocket.musicwidget.backend.SongListAdapter;
import com.smartpocket.musicwidget.backend.SongListLoader;
import com.smartpocket.musicwidget.model.Song;
import com.smartpocket.musicwidget.service.MusicService;

public class SongListActivity extends AppCompatActivity implements SearchView.OnQueryTextListener {
    private SongListAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.content_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(getResources().getString(R.string.title_activity_song_list));
        getSupportActionBar().setLogo(R.drawable.ic_launcher);

        handleIntent(getIntent());

        ListView list = (ListView) findViewById(R.id.listView);
        Cursor listCursor = SongListLoader.getInstance(this).getCursor();
        adapter = new SongListAdapter(this, listCursor);
        adapter.setFilterQueryProvider(new FilterQueryProvider() {
            @Override
            public Cursor runQuery(CharSequence constraint) {
                return SongListLoader.getInstance(SongListActivity.this).getFilteredCursor(constraint);
            }
        });

        list.setAdapter(adapter);

        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Song song = ((SongListAdapter)parent.getAdapter()).getSong(position);
                Log.i("SongListactivity click", song.toString());
                Intent serviceIntent = new Intent(SongListActivity.this, MusicService.class);
                serviceIntent.setAction(MusicWidget.ACTION_JUMP_TO);
                serviceIntent.putExtra("song", song);
                SongListActivity.this.startService(serviceIntent);
                finish();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.song_list, menu);
        MenuItem searchItem = menu.findItem(R.id.search);

        // Associate searchable configuration with the SearchView
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        SearchView searchView = (SearchView) MenuItemCompat.getActionView(searchItem);
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        searchView.setOnQueryTextListener(this);

        return true;
    }



    @Override
    protected void onNewIntent(Intent intent) {
        setIntent(intent);
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            String searchText = intent.getStringExtra(SearchManager.QUERY);

            Log.d("SongListActivity", "Searched for: " + searchText);
            adapter.getFilter().filter(searchText);
        }
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        Log.d("SongListActivity", "Searched for: " + newText);
        adapter.getFilter().filter(newText);
        return true;
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        return false;
    }
}

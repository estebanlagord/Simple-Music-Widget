package com.smartpocket.musicwidget.backend;

import android.database.Cursor;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;

import com.smartpocket.musicwidget.R;
import com.smartpocket.musicwidget.model.Song;

public class SongCursorRecyclerAdapter extends CursorRecyclerAdapter<SongViewHolder> implements ItemClickListener {

    private SongClickListener listener;

    public SongCursorRecyclerAdapter(Cursor c, SongClickListener listener) {
        super(c);
        this.listener = listener;
    }

    @Override
    public void onBindViewHolder(SongViewHolder holder, Cursor cursor) {
        Song song = getSongFromCurrentCursorPos(cursor);

        holder.getArtist().setText(song.getArtist());
        holder.getTitle().setText(song.getTitle());
        holder.getDuration().setText(song.getDurationStr());
    }

    private Song getSongFromCurrentCursorPos(Cursor cursor) {
        String artist = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST));
        String title = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.TITLE));
        long duration = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media.DURATION));

        return new Song(0, title, artist, duration);
    }

    @NonNull
    @Override
    public SongViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.song_list_row, parent, false);
        return new SongViewHolder(view, this);
    }

    @Override
    public void onItemClick(int position) {
        if (!mCursor.moveToPosition(position)) {
            throw new IllegalStateException("couldn't move cursor to position " + position);
        }
        listener.onSongSelected(getSongFromCurrentCursorPos(mCursor));
    }
}

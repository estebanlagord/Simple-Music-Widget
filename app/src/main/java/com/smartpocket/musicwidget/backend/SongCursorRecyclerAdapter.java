package com.smartpocket.musicwidget.backend;

import android.database.Cursor;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;

import com.futuremind.recyclerviewfastscroll.SectionTitleProvider;
import com.smartpocket.musicwidget.R;
import com.smartpocket.musicwidget.model.Song;

import org.apache.commons.lang3.StringUtils;

public class SongCursorRecyclerAdapter extends CursorRecyclerAdapter<SongViewHolder> implements ItemClickListener, SectionTitleProvider {

    private SongClickListener listener;

    public SongCursorRecyclerAdapter(Cursor c, SongClickListener listener) {
        super(c);
        this.listener = listener;
    }

    @Override
    public void onBindViewHolder(SongViewHolder holder, Cursor cursor) {
        Song song = getSongFromCurrentCursorPos(cursor);

        if (song.isUnknownArtist()) {
            holder.getArtist().setVisibility(View.GONE);
        } else {
            holder.getArtist().setVisibility(View.VISIBLE);
        }
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

    @Override
    public String getSectionTitle(int position) {
        String result = "";
        Cursor cursor = getCursor();
        String artist = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST));
        if (artist != null) {
            artist = artist.trim();
            if (StringUtils.isNotBlank(artist) && !artist.equalsIgnoreCase("<unknown>")) {
                result = artist.substring(0, 1);
            }
        }
        return result;
    }
}

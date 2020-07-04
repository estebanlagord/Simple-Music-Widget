package com.smartpocket.musicwidget.model;

import android.content.ContentUris;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.MediaStore;

import androidx.annotation.NonNull;

import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Song implements Serializable {
    private final long id;
    private final String title;
    private final String artist;
    private final long duration;
    private final long albumId;
    private String albumArtPath;
    private Bitmap albumArt;

    public Song(long id, String title, String artist, long duration, long albumId) {
        this.id = id;
        this.title = title;
        this.artist = artist;
        this.duration = duration;
        this.albumId = albumId;
    }

    public String getTitle() {
        return title;
    }

    public String getArtist() {
        return artist;
    }

    public String getDurationStr() {
        SimpleDateFormat df = new SimpleDateFormat("mm:ss", Locale.US);
        return df.format(new Date(duration));
    }

    public long getAlbumId() {
        return albumId;
    }

    public String getAlbumArtPath() {
        return albumArtPath;
    }

    public Bitmap getAlbumArt() {
        if (albumArt == null && albumArtPath != null) {
            albumArt = BitmapFactory.decodeFile(albumArtPath);
        }
        return albumArt;
    }

    public void setAlbumArtPath(String albumArtPath) {
        this.albumArtPath = albumArtPath;
    }

    public Uri getURI() {
        return ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id);
    }

    public boolean isUnknownArtist() {
        return StringUtils.isBlank(artist) || artist.equalsIgnoreCase("<unknown>");
    }

    @NonNull
    @Override
    public String toString() {
        return title + " - " + artist + " - " + getDurationStr();
    }
}

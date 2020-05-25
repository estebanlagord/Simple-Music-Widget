package com.smartpocket.musicwidget.model;

import android.content.ContentUris;
import android.net.Uri;

import androidx.annotation.NonNull;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.FastDateFormat;

import java.io.Serializable;
import java.util.Date;

public class Song implements Serializable {
    private static final FastDateFormat dateFormat = FastDateFormat.getInstance("mm:ss");
    private final long id;
    private final String title;
    private final String artist;
    private final long duration;

    public Song(long id, String title, String artist, long duration) {
        this.id = id;
        this.title = title;
        this.artist = artist;
        this.duration = duration;
    }

    public String getTitle() {
        return title;
    }

    public String getArtist() {
        return artist;
    }

    public String getDurationStr() {
        return dateFormat.format(new Date(duration));
    }

    public Uri getURI() {
        return ContentUris.withAppendedId(android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id);
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

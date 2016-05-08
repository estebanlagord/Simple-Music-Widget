package com.smartpocket.musicwidget.model;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import android.content.ContentUris;
import android.net.Uri;

public class Song implements Serializable{
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
		SimpleDateFormat df = new SimpleDateFormat("mm:ss", Locale.US);
		return df.format(new Date(duration));
	}

	public Uri getURI() {
		return ContentUris.withAppendedId(android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id);
	}
	
	@Override
	public String toString() {
		return title + " - " + artist + " - " + getDurationStr();
	}
}

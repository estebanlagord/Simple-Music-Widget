package com.smartpocket.musicwidget.backend;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.util.Log;

import com.smartpocket.musicwidget.model.Song;

public class MusicLoader {
	private static final String TAG = "Music Loader";
	private static final String LAST_SONG_TITLE = "Last Song Title";
	private static final String LAST_SONG_ARTIST = "Last Song Artist";
	private static final String IS_SHUFFLE_ON = "Is Shuffle On";
	private static MusicLoader instance;
	private final Context context;
	private Cursor cur;
	private boolean isShuffleOn;

	public static MusicLoader getInstance(Context context){
		if (instance == null) {
			instance = new MusicLoader(context);
			instance.prepare();
		}
		
		return instance;
	}
	
    private MusicLoader(Context context) {
        this.context = context;
    }

    
    private void prepare() {
		// Check if shuffle mode is on
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		this.isShuffleOn = prefs.getBoolean(IS_SHUFFLE_ON, false);
		String sortOrder = isShuffleOn ? "RANDOM()" : null;

        Log.d(TAG, "Querying media...");

        
		//Some audio may be explicitly marked as not being music
		String selection = MediaStore.Audio.Media.IS_MUSIC + " != 0";

		String[] projection = {
		        MediaStore.Audio.Media._ID,
		        MediaStore.Audio.Media.ARTIST,
		        MediaStore.Audio.Media.TITLE,
		        MediaStore.Audio.Media.DURATION
		};

		cur = context.getContentResolver().query(
		        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
		        projection,
		        selection,
		        null,
				sortOrder);
        
        Log.d(TAG, "Query finished. " + (cur == null ? "Returned NULL." : "Returned a cursor."));

        if (cur == null) {
            // Query failed...
            Log.e(TAG, "Failed to retrieve music: cursor is null");
            return;
        }

    	// Move the cursor to the position of the song which was playing the last time the application was running
        String lastSongTitle  = prefs.getString(LAST_SONG_TITLE, null);
        String lastSongArtist = prefs.getString(LAST_SONG_ARTIST, null);
        
        if (lastSongTitle != null && lastSongArtist != null){
            // Attempt to restore the cursor to its previous position
        	Log.d(TAG, "Searching cursor for: " + lastSongTitle + " - " + lastSongArtist);
        	while(cur.moveToNext()){
        		String title  = cur.getString(cur.getColumnIndex(android.provider.MediaStore.Audio.Media.TITLE));
        		String artist = cur.getString(cur.getColumnIndex(android.provider.MediaStore.Audio.Media.ARTIST));
        		if (title.equals(lastSongTitle) && artist.equals(lastSongArtist)){
        			Log.d(TAG, "Song found!");
        			return;
        		}
        	}
        	Log.d(TAG, "Song not found");
        }
        
        // If we are here, either there was no lastSong preference or we can no longer find it. Move to the first position
        if (!cur.moveToFirst()) {
        	Log.e(TAG, "Failed to move cursor to first row (no music found).");
        	return;
        }

        Log.d(TAG, "Done querying media. MusicLoader is ready.");
    }
    
    public Song getCurrent() {
    	String title  = cur.getString(cur.getColumnIndex(android.provider.MediaStore.Audio.Media.TITLE));
    	String artist = cur.getString(cur.getColumnIndex(android.provider.MediaStore.Audio.Media.ARTIST));
    	long duration = cur.getLong(  cur.getColumnIndex(android.provider.MediaStore.Audio.Media.DURATION));
    	long id       = cur.getLong(  cur.getColumnIndex(android.provider.MediaStore.Audio.Media._ID));

		return new Song(id, title, artist, duration);
    }
    
    public Song getNext() {
    	if (!cur.moveToNext())
    		cur.moveToFirst();

    	return getCurrent();
    }
    
    public Song getPrevious() {
    	if (!cur.moveToPrevious())
    		cur.moveToLast();
    	
    	return getCurrent();
    }

	public boolean isShuffleOn () {
		return isShuffleOn;
	}

	public void toggleShuffle() {
		boolean newValue = !isShuffleOn;
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		Editor editor = prefs.edit();
		editor.putBoolean(IS_SHUFFLE_ON, newValue);
		editor.commit();

		close(); // to trigger a new query
	}

	public void jumpTo(Song song) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		Editor editor = prefs.edit();
		editor.putString(LAST_SONG_TITLE, song.getTitle());
		editor.putString(LAST_SONG_ARTIST, song.getArtist());
		editor.commit();

		if (cur != null)
			cur.close();

		instance = null;
	}

    public void close() {
    	if (cur != null){
        	// Save current song in a Shared Preference
    		String title  = cur.getString(cur.getColumnIndex(android.provider.MediaStore.Audio.Media.TITLE));
        	String artist = cur.getString(cur.getColumnIndex(android.provider.MediaStore.Audio.Media.ARTIST));
    		
        	SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        	Editor editor = prefs.edit();
        	editor.putString(LAST_SONG_TITLE, title);
        	editor.putString(LAST_SONG_ARTIST, artist);
        	editor.commit();

    		cur.close();
    	}
    	
    	instance = null;
    }
}

package com.smartpocket.musicwidget.backend;

import android.content.Context;
import android.database.Cursor;
import android.provider.MediaStore;
import android.util.Log;

public class SongListLoader {
	private static final String TAG = "Music Loader";
	private static final String SORT_BY = MediaStore.Audio.Media.ARTIST + ", " + MediaStore.Audio.Media.TITLE;
	private static SongListLoader instance;
	private final Context context;
	private Cursor cur;


	public static SongListLoader getInstance(Context context){
		if (instance == null) {
			instance = new SongListLoader(context);
		}

		return instance;
	}

    private SongListLoader(Context context) {
        this.context = context;
    }


    public Cursor getCursor() {
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
				SORT_BY);

        Log.d(TAG, "Query finished. " + (cur == null ? "Returned NULL." : "Returned a cursor."));

        if (cur == null) {
            // Query failed...
            Log.e(TAG, "Failed to retrieve music: cursor is null");
            return null;
        }

        Log.d(TAG, "Done querying media. SongListLoader is ready.");
		return cur;
    }

	public Cursor getFilteredCursor(CharSequence constraint) {
		Log.d(TAG, "Querying media for filter...");


		//Some audio may be explicitly marked as not being music
		String selection = MediaStore.Audio.Media.IS_MUSIC + " != 0 and "
				+ "( " + MediaStore.Audio.Media.ARTIST + " LIKE ? or "
				+ MediaStore.Audio.Media.TITLE + " LIKE ? )";

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
				new String[] { "%"+constraint+"%", "%"+constraint+"%" },
				SORT_BY);

		Log.d(TAG, "Query for filter finished. " + (cur == null ? "Returned NULL." : "Returned a cursor."));

		if (cur == null) {
			// Query failed...
			Log.e(TAG, "Failed to retrieve music: cursor is null");
			return null;
		}

		Log.d(TAG, "Done querying media. SongListLoader is ready.");
		return cur;
	}
}

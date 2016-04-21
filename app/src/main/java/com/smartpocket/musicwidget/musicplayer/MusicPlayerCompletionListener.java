package com.smartpocket.musicwidget.musicplayer;

import java.io.IOException;

/**
 * Interface used to define a callback method to be called when the MusicPlayer's current song finishes playing.
 * The subscriber will use this to know when to change to the next song automatically.
 */
public interface MusicPlayerCompletionListener {
	/**
	 * To be called when the current song ends
	 * @throws IOException 
	 */
	void onMusicCompletion() throws IOException;
}

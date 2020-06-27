package com.smartpocket.musicwidget.activities

import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import java.util.concurrent.atomic.AtomicBoolean

class MyRecyclerViewOnScrollListener(private val fabBtn: ExtendedFloatingActionButton,
                                     private val isSearching: AtomicBoolean)
    : RecyclerView.OnScrollListener() {

    override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
        if (newState == RecyclerView.SCROLL_STATE_IDLE) {
            if (fabBtn.isExtended.not() && isSearching.get().not())
                fabBtn.extend()
        }
        super.onScrollStateChanged(recyclerView, newState)
    }

    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
        if (dy > 0 || dy < 0 && fabBtn.isExtended) {
            fabBtn.shrink()
        }
    }
}
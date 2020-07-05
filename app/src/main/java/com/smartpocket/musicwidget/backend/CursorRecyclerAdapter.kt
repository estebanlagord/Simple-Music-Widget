package com.smartpocket.musicwidget.backend

import android.database.Cursor
import androidx.recyclerview.widget.RecyclerView

abstract class CursorRecyclerAdapter<VH : RecyclerView.ViewHolder?>(c: Cursor?) : RecyclerView.Adapter<VH>() {
    private var mDataValid = false
    var cursor: Cursor? = null
    private var mRowIDColumn = 0

    init {
        val cursorPresent = c != null
        cursor = c
        mDataValid = cursorPresent
        mRowIDColumn = if (cursorPresent) c!!.getColumnIndexOrThrow("_id") else -1
        setHasStableIds(true)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        check(mDataValid) { "this should only be called when the cursor is valid" }
        check(cursor!!.moveToPosition(position)) { "couldn't move cursor to position $position" }
        onBindViewHolder(holder, cursor!!)
    }

    abstract fun onBindViewHolder(holder: VH, cursor: Cursor)

    override fun getItemCount() = if (mDataValid) {
        cursor?.count ?: 0
    } else {
        0
    }

    override fun getItemId(position: Int): Long =
            if (hasStableIds() && mDataValid) {
                if (cursor?.moveToPosition(position) == true) {
                    cursor?.getLong(mRowIDColumn) ?: RecyclerView.NO_ID
                } else {
                    RecyclerView.NO_ID
                }
            } else {
                RecyclerView.NO_ID
            }

    /**
     * Change the underlying cursor to a new cursor. If there is an existing cursor it will be
     * closed.
     *
     * @param cursor The new cursor to be used
     */
    fun changeCursor(cursor: Cursor?) {
        val old = swapCursor(cursor)
        old?.close()
    }

    /**
     * Swap in a new Cursor, returning the old Cursor.  Unlike
     * [.changeCursor], the returned old Cursor is *not*
     * closed.
     *
     * @param newCursor The new cursor to be used.
     * @return Returns the previously set Cursor, or null if there wasa not one.
     * If the given new Cursor is the same instance is the previously set
     * Cursor, null is also returned.
     */
    private fun swapCursor(newCursor: Cursor?): Cursor? {
        if (newCursor === cursor) {
            return null
        }
        val oldCursor = cursor
        cursor = newCursor
        if (newCursor != null) {
            mRowIDColumn = newCursor.getColumnIndexOrThrow("_id")
            mDataValid = true
            // notify the observers about the new cursor
            notifyDataSetChanged()
        } else {
            mRowIDColumn = -1
            mDataValid = false
            // notify the observers about the lack of a data set
            notifyItemRangeRemoved(0, itemCount)
        }
        return oldCursor
    }

    /**
     *
     * Converts the cursor into a CharSequence. Subclasses should override this
     * method to convert their results. The default implementation returns an
     * empty String for null values or the default String representation of
     * the value.
     *
     * @param cursor the cursor to convert to a CharSequence
     * @return a CharSequence representing the value
     */
    fun convertToString(cursor: Cursor?): CharSequence {
        return cursor?.toString() ?: ""
    }
}
package com.smartpocket.musicwidget.activities

import android.view.View
import android.view.ViewGroup
import com.futuremind.recyclerviewfastscroll.viewprovider.DefaultBubbleBehavior
import com.futuremind.recyclerviewfastscroll.viewprovider.DefaultScrollerViewProvider
import com.futuremind.recyclerviewfastscroll.viewprovider.ViewBehavior
import com.futuremind.recyclerviewfastscroll.viewprovider.VisibilityAnimationManager
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton

class MyFastScrollScrollerViewProvider(val fabBtn: ExtendedFloatingActionButton) : DefaultScrollerViewProvider() {

    lateinit var bubble: View

    override fun provideBubbleView(container: ViewGroup?): View {
        bubble = super.provideBubbleView(container)
        return bubble
    }

    override fun provideBubbleBehavior(): ViewBehavior? {
        return object : DefaultBubbleBehavior(
                VisibilityAnimationManager.Builder(bubble)
                        .withPivotX(1f)
                        .withPivotY(1f)
                        .build()) {

            override fun onHandleGrabbed() {
                fabBtn.hide()
                super.onHandleGrabbed()
            }

            override fun onHandleReleased() {
                super.onHandleReleased()
                fabBtn.show()
            }
        }
    }
}
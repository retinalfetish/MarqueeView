/*
 * Copyright 2021 Christopher Zaborsky
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.unary.marqueeview;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.view.MotionEvent;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

/**
 * A styleable marquee text widget extended from MarqueeView that adds simple touch interaction.
 * Note that this is not extended from TextView.
 */
public class TouchMarqueeView extends MarqueeView {

    private boolean mPaused;
    private long mCurrentPlayTime;

    /**
     * Simple constructor to use when creating the view from code.
     *
     * @param context Context given for the view. This determines the resources and theme.
     */
    public TouchMarqueeView(Context context) {
        super(context);
    }

    /**
     * Constructor that is called when inflating the view from XML.
     *
     * @param context Context given for the view. This determines the resources and theme.
     * @param attrs   The attributes for the inflated XML tag.
     */
    public TouchMarqueeView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    /**
     * Constructor called when inflating from XML and applying a style.
     *
     * @param context      Context given for the view. This determines the resources and theme.
     * @param attrs        The attributes for the inflated XML tag.
     * @param defStyleAttr Default style attributes to apply to this view.
     */
    public TouchMarqueeView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    /**
     * Constructor that is used when given a default shared style.
     *
     * @param context      Context given for the view. This determines the resources and theme.
     * @param attrs        The attributes for the inflated XML tag.
     * @param defStyleAttr Default style attributes to apply to this view.
     * @param defStyleRes  Default style resource to apply to this view.
     */
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public TouchMarqueeView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        super.onTouchEvent(event);

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                pause();
                break;
            case MotionEvent.ACTION_UP:
                resume();
                break;
        }

        return true;
    }

    /**
     * Pause animator and provide some backwards compatibility if instance of ValueAnimator.
     */
    private void pause() {
        Animator animator = getTextAnimator();
        if (animator == null) return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            animator.pause();
            return;
        }

        if (!mPaused && animator instanceof ValueAnimator) {
            mPaused = true;
            mCurrentPlayTime = ((ValueAnimator) animator).getCurrentPlayTime();
            animator.cancel();
        }
    }

    /**
     * Resume animator and provide some backwards compatibility if instance of ValueAnimator.
     */
    private void resume() {
        Animator animator = getTextAnimator();
        if (animator == null) return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            animator.resume();
            return;
        }

        if (mPaused && animator instanceof ValueAnimator) {
            animator.start();
            ((ValueAnimator) animator).setCurrentPlayTime(mCurrentPlayTime);
            mPaused = false;
        }
    }
}
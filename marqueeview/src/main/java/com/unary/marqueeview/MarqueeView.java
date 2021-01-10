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
import android.animation.AnimatorInflater;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Region;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Property;
import android.view.View;
import android.view.animation.LinearInterpolator;

import androidx.annotation.AnimatorRes;
import androidx.annotation.AttrRes;
import androidx.annotation.ColorInt;
import androidx.annotation.Dimension;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import java.text.Bidi;

/**
 * A styleable marquee text widget that creates the classic effect but with a few more features.
 * Note that this is not extended from TextView.
 *
 * <p><strong>XML attributes</strong></p>
 * <p>The following optional attributes can be used to change the look and feel of the view:</p>
 * <pre>
 *   app:textAnimator="reference"        // Animator to use for the text marquee
 *   app:repeatCount="integer"           // Default is ValueAnimator.INFINITE
 *   app:scrollSpeed="percent"           // Unit interval used to determine speed
 *
 *   android:autoStart="boolean"         // If scrolling should start automatically
 *   android:enabled="boolean"           // Changes the view state
 *   android:text="string"               // Only scrolls if necessary
 *   android:textColor="reference|color" // Reference to a color selector or simple color
 *   android:textSize="dimension"        // Text size used. Default is 14sp
 * </pre>
 * <p>See {@link R.styleable#MarqueeView MarqueeView Attributes}, {@link R.styleable#View View Attributes}</p>
 */
public class MarqueeView extends View implements ValueAnimator.AnimatorUpdateListener {

    public static final String WHITESPACE = "   ";

    private static final int TEXT_COLOR = R.attr.colorControlNormal;
    private static final int TEXT_COLOR_DISABLED = R.attr.colorControlHighlight;
    private static final float TEXT_SIZE = 14; // sp
    private static final int REPEAT_COUNT = ValueAnimator.INFINITE;
    private static final float SCROLL_SPEED = 1f;
    private static final int SCROLL_MULTIPLIER = 5;
    private static final boolean AUTO_START = true;

    private String mText;
    private String mScrollingText;
    private boolean mLtrText;
    private ColorStateList mTextColor;
    private float mScroll; // Internal animator
    private float mOffset; // External animator
    private float mScrollSpeed;
    private boolean mAutoStart;
    private Paint mTextPaint;
    private Animator mTextAnimator;
    private boolean mTextAnimatorSet;
    private Rect mDrawingRect;

    /**
     * Static field to provide a property wrapper for the "offset" get and set methods.
     */
    public static final Property<MarqueeView, Float> OFFSET = Property.of(MarqueeView.class, Float.class, "offset");

    /**
     * Simple constructor to use when creating the view from code.
     *
     * @param context Context given for the view. This determines the resources and theme.
     */
    public MarqueeView(Context context) {
        super(context);
        init(context, null, 0, 0);
    }

    /**
     * Constructor that is called when inflating the view from XML.
     *
     * @param context Context given for the view. This determines the resources and theme.
     * @param attrs   The attributes for the inflated XML tag.
     */
    public MarqueeView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs, 0, 0);
    }

    /**
     * Constructor called when inflating from XML and applying a style.
     *
     * @param context      Context given for the view. This determines the resources and theme.
     * @param attrs        The attributes for the inflated XML tag.
     * @param defStyleAttr Default style attributes to apply to this view.
     */
    public MarqueeView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs, defStyleAttr, 0);
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
    public MarqueeView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context, attrs, defStyleAttr, defStyleRes);
    }

    /**
     * Shared method to initialize the member variables from the XML and create the drawing objects.
     * Input values are checked for sanity.
     *
     * @param context      Context given for the view. This determines the resources and theme.
     * @param attrs        The attributes for the inflated XML tag.
     * @param defStyleAttr Default style attributes to apply to this view.
     * @param defStyleRes  Default style resource to apply to this view.
     */
    private void init(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        TypedArray typedArray = context.getTheme()
                .obtainStyledAttributes(attrs, R.styleable.MarqueeView, defStyleAttr, defStyleRes);

        int animatorRes;
        int repeatCount;
        boolean enabled;
        float textSize;

        try {
            animatorRes = typedArray.getResourceId(R.styleable.MarqueeView_textAnimator, 0);
            repeatCount = typedArray.getInt(R.styleable.MarqueeView_repeatCount, REPEAT_COUNT);
            mScrollSpeed = typedArray.getFraction(R.styleable.MarqueeView_scrollSpeed, 1, 1, SCROLL_SPEED);
            mAutoStart = typedArray.getBoolean(R.styleable.MarqueeView_android_autoStart, AUTO_START);
            enabled = typedArray.getBoolean(R.styleable.MarqueeView_android_enabled, isEnabled());
            mText = typedArray.getString(R.styleable.MarqueeView_android_text);
            mTextColor = typedArray.getColorStateList(R.styleable.MarqueeView_android_textColor);
            textSize = typedArray.getDimension(R.styleable.MarqueeView_android_textSize, dpToPixels(context, TEXT_SIZE));
        } finally {
            typedArray.recycle();
        }

        // Sanitize the input values
        mText = mText != null ? mText : "";
        mScrollSpeed = mScrollSpeed > 0 ? mScrollSpeed : 0;

        // Provide a default animator
        if (animatorRes != 0) {
            setTextAnimatorResource(animatorRes);
        } else {
            mTextAnimator = ValueAnimator.ofFloat(0, 0);
            mTextAnimator.setInterpolator(new LinearInterpolator());
            ((ValueAnimator) mTextAnimator).setRepeatCount(repeatCount);
        }

        if (mTextAnimator instanceof ValueAnimator) {
            ((ValueAnimator) mTextAnimator).addUpdateListener(this);
        }

        // Provide some default colors
        if (mTextColor == null) {
            int[][] states = new int[][]{new int[]{-android.R.attr.state_enabled}, new int[]{}};
            int[] colors = new int[]{getAttrColor(context, TEXT_COLOR_DISABLED), getAttrColor(context, TEXT_COLOR)};

            mTextColor = new ColorStateList(states, colors);
        }

        // Initialize the drawing objects
        mTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mTextPaint.setStyle(Paint.Style.FILL);
        mTextPaint.setTextSize(textSize);

        mDrawingRect = new Rect();

        // Set the text paint color
        setEnabled(enabled);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = getDefaultSize(getSuggestedMinimumWidth(), widthMeasureSpec);
        int height = getDefaultSize(getSuggestedMinimumHeight(), heightMeasureSpec);

        setMeasuredDimension(width, height);
    }

    @Override
    protected int getSuggestedMinimumWidth() {
        float width = getTextWidth(mTextPaint, mText) + getPaddingLeft() + getPaddingRight();
        return Math.max(super.getSuggestedMinimumWidth(), (int) Math.ceil(width));
    }

    @Override
    protected int getSuggestedMinimumHeight() {
        float height = getFontHeight(mTextPaint) + getPaddingTop() + getPaddingBottom();
        return Math.max(super.getSuggestedMinimumHeight(), (int) Math.ceil(height));
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);

        // Setup values for onDraw()
        mScrollingText = mText;
        mLtrText = isLtrText(mText);
        mScroll = 0;
        mOffset = 0;

        int paddingStart = getPaddingLeft();
        int paddingEnd = getPaddingRight();

        // Use RTL if available
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            if (isLtrLayout(this)) {
                paddingStart = getPaddingStart();
                paddingEnd = getPaddingEnd();
            } else {
                paddingStart = getPaddingEnd();
                paddingEnd = getPaddingStart();
            }
        }

        mDrawingRect.set(paddingStart, getPaddingTop(),
                getWidth() - paddingEnd, getHeight() - getPaddingBottom());

        // Start the scroll animator
        if (isScrollable()) {
            mScrollingText = mScrollingText.concat(WHITESPACE);

            if (!mTextAnimatorSet) {
                mTextAnimator.setDuration((int) (getTextWidth(mTextPaint, mScrollingText) / mScrollSpeed * SCROLL_MULTIPLIER));
                ((ValueAnimator) mTextAnimator).setFloatValues(0, -getTextWidth(mTextPaint, mScrollingText));
            }

            mScrollingText = mScrollingText.concat(mScrollingText).concat(mScrollingText);

            if (mAutoStart && mTextAnimator != null) {
                mTextAnimator.start();
            }
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // Calculate the difference
        float width = getTextWidth(mTextPaint, mScrollingText) / 3;
        float dx = (mScroll + (isLtrText(mText) ? mOffset : -mOffset)) % width - (isScrollable() ? width : 0);

        // Mirror if text is RTL
        if (!mLtrText) {
            dx = mDrawingRect.width() - dx - width * 3;
        }

        canvas.clipRect(mDrawingRect, Region.Op.INTERSECT);
        canvas.drawText(mScrollingText, dx + mDrawingRect.left, getTextHeight(mTextPaint) + mDrawingRect.top, mTextPaint);
    }

    @Override
    public void onAnimationUpdate(ValueAnimator animation) {
        mScroll = (float) animation.getAnimatedValue();
        invalidate();
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);

        int statefulColor = mTextColor.getColorForState(getDrawableState(), mTextColor.getDefaultColor());
        mTextPaint.setColor(statefulColor);
    }

    /**
     * Determine if the current text is long enough to be scrollable in the view configuration.
     *
     * @return True if text is scrollable.
     */
    public boolean isScrollable() {
        return getTextWidth(getTextPaint(), getText()) > getWidth() - getPaddingLeft() - getPaddingRight();
    }

    /**
     * Utility method to find the pixel resolution of a density pixel value.
     *
     * @param context Context given for the metrics. This determines the resources and theme.
     * @param dp      Density pixels to convert.
     * @return The pixel resolution.
     */
    private static int dpToPixels(Context context, @Dimension float dp) {
        return (int) (dp * context.getResources().getDisplayMetrics().density + 0.5);
    }

    /**
     * Utility method to find the density pixel value of a pixel resolution.
     *
     * @param context Context given for the metrics. This determines the resources and theme.
     * @param px      Pixel resolution to convert.
     * @return The density pixels.
     */
    @Dimension
    private static float pixelsToDp(Context context, int px) {
        return px / context.getResources().getDisplayMetrics().density;
    }

    /**
     * Utility method to find a color as defined in the attribute of a theme.
     *
     * @param context   Context given for the attribute. This determines the resources and theme.
     * @param attrResId The color resource.
     * @return An ARGB color integer.
     */
    @ColorInt
    private static int getAttrColor(Context context, @AttrRes int attrResId) {
        TypedArray typedArray = context.getTheme().obtainStyledAttributes(new int[]{attrResId});
        int color = typedArray.getColor(0, 0);
        typedArray.recycle();

        return color;
    }

    /**
     * A standardized answer to the question "what is the text width?" This may not be an exact
     * measurement of the text content.
     *
     * @param paint Paint object
     * @param text  Text to measure.
     * @return Text width in pixels.
     * @see Paint#getTextBounds(String, int, int, Rect)
     */
    protected static float getTextWidth(@NonNull Paint paint, String text) {
        return paint.measureText(text);
    }

    /**
     * A standardized answer to the question "what is the text height?" This may not be an exact
     * measurement of the text content.
     *
     * @param paint Paint object.
     * @return Text height in pixels.
     * @see Paint#getTextBounds(String, int, int, Rect)
     */
    protected static float getTextHeight(@NonNull Paint paint) {
        return paint.getTextSize();
    }

    /**
     * Get the total font height. This is more than the height of the current text content and only
     * used to determine the measured view.
     *
     * @param paint Paint object.
     * @return Font height in pixels.
     */
    protected static float getFontHeight(Paint paint) {
        return paint.getFontMetrics().bottom - paint.getFontMetrics().top;
    }

    /**
     * Check if the layout direction for the given view or configuration is left-to-right.
     *
     * @param view View to check.
     * @return True if likely LTR.
     */
    protected static boolean isLtrLayout(View view) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            if (view.isLayoutDirectionResolved()) {
                return view.getLayoutDirection() == LAYOUT_DIRECTION_LTR;
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            return view.getResources().getConfiguration().getLayoutDirection() == LAYOUT_DIRECTION_LTR;
        }

        return true;
    }

    /**
     * Check if the base direction of the given text has a strong left-to-right language bias.
     *
     * @param text Sample text paragraph.
     * @return True if likely LTR.
     */
    protected static boolean isLtrText(@NonNull String text) {
        Bidi bidi = new Bidi(text, Bidi.DIRECTION_DEFAULT_LEFT_TO_RIGHT);
        return bidi.baseIsLeftToRight();
    }

    /**
     * Utility method to find the preferred measurements of this view for the view parent.
     *
     * @param defaultSize Default size of the view.
     * @param measureSpec Constraints imposed by the parent.
     * @return Preferred size for this view.
     * @see View#getDefaultSize(int, int)
     */
    public static int getDefaultSize(int defaultSize, int measureSpec) {
        int mode = MeasureSpec.getMode(measureSpec);
        int size = MeasureSpec.getSize(measureSpec);

        switch (mode) {
            case MeasureSpec.EXACTLY:
                return size;
            case MeasureSpec.AT_MOST:
                return Math.min(size, defaultSize);
            case MeasureSpec.UNSPECIFIED:
            default:
                return defaultSize;
        }
    }

    /**
     * Get the marquee text. If the contents fits within the view width no marquee will occur.
     *
     * @return Text for the marquee
     */
    @NonNull
    public String getText() {
        return mText;
    }

    /**
     * Set the marquee text. If the contents fits within the view width no marquee will occur.
     *
     * @param text Text for the marquee.
     */
    public void setText(@NonNull String text) {
        mText = mText != null ? text : "";
        requestLayout();
    }

    /**
     * Get the marquee text color. The default and disabled states are used for the paint color.
     *
     * @return ColorStateList color.
     */
    public ColorStateList getTextColor() {
        return mTextColor;
    }

    /**
     * Set the marquee text color. The default and disabled states are used for the paint color.
     *
     * @param textColor ColorStateList color.
     */
    public void setTextColor(ColorStateList textColor) {
        mTextColor = textColor;
        requestLayout();
    }

    /**
     * Get the marquee text size. This gets the equivalent property in the text paint object.
     *
     * @return Size of the marquee text.
     */
    public float getTextSize() {
        return mTextPaint.getTextSize();
    }

    /**
     * Set the marquee text size. This sets the equivalent property in the text paint object.
     *
     * @param textSize Size of the marquee text.
     */
    public void setTextSize(float textSize) {
        mTextPaint.setTextSize(textSize);
        requestLayout();
    }

    /**
     * Get the marquee text paint. It can be used to set other properties not available directly.
     * This should not be null.
     *
     * @return Paint for the text.
     */
    @NonNull
    public Paint getTextPaint() {
        return mTextPaint;
    }

    /**
     * Set the marquee text paint. It can be used to set other properties not available directly.
     * This should not be null.
     *
     * @param textPaint Paint for the text.
     */
    public void setTextPaint(@NonNull Paint textPaint) {
        mTextPaint = textPaint;
        requestLayout();
    }

    /**
     * Get the animator repeat count. This gets the equivalent property in the animator object.
     *
     * @return Repeat count for the animator.
     */
    public int getRepeatCount() {
        return mTextAnimator instanceof ValueAnimator
                ? ((ValueAnimator) mTextAnimator).getRepeatCount() : 0;
    }

    /**
     * Set the animator repeat count. This sets the equivalent property in the animator object.
     *
     * @param repeatCount Repeat count for the animator.
     */
    public void setRepeatCount(int repeatCount) {
        if (mTextAnimator instanceof ValueAnimator) {
            ((ValueAnimator) mTextAnimator).setRepeatCount(repeatCount);
            requestLayout();
        }
    }

    /**
     * Get the additional offset used when scrolling the text marquee. Any effector should zero out
     * this value before the animator finishes.
     *
     * @return Offset for the scroll.
     */
    public float getOffset() {
        return mOffset;
    }

    /**
     * Set the additional offset used when scrolling the text marquee. Any effector should zero out
     * this value before the animator finishes.
     *
     * @param offset Offset for the scroll.
     */
    public void setOffset(float offset) {
        mOffset = offset;
        invalidate();
    }

    /**
     * Get the animator scroll speed. The interval is a percentage of speed with 1 being normal.
     *
     * @return Scroll speed for the animator.
     */
    public float getScrollSpeed() {
        return mScrollSpeed;
    }

    /**
     * Set the animator scroll speed. The interval is a percentage of speed with 1 being normal.
     *
     * @param scrollSpeed Scroll speed for the animator.
     */
    public void setScrollSpeed(float scrollSpeed) {
        mScrollSpeed = mScrollSpeed > 0 ? scrollSpeed : 0;
        requestLayout();
    }

    /**
     * Get the marquee text animator. An initial default animator is assigned if one has not been
     * provided by the client. This may be null.
     *
     * @return Animator for the marquee.
     */
    @Nullable
    public Animator getTextAnimator() {
        return mTextAnimator;
    }

    /**
     * Set the marquee text animator. An initial default animator is assigned if one has not been
     * provided by the client. This may be null.
     *
     * @param textAnimator Animator for the marquee.
     */
    public void setTextAnimator(@Nullable Animator textAnimator) {
        if (mTextAnimator instanceof ValueAnimator) {
            ((ValueAnimator) mTextAnimator).removeUpdateListener(this);
        }

        mTextAnimator = textAnimator;
        mTextAnimatorSet = true;

        if (mTextAnimator instanceof ValueAnimator) {
            ((ValueAnimator) mTextAnimator).addUpdateListener(this);
        }

        requestLayout();
    }

    /**
     * Set the marquee text animator resource. A default animator is assigned if one has not been
     * provided by the client.
     *
     * @param animatorResId Resource for the animator.
     */
    public void setTextAnimatorResource(@AnimatorRes int animatorResId) {
        setTextAnimator(AnimatorInflater.loadAnimator(getContext(), animatorResId));
        mTextAnimator.setTarget(this);
    }

    /**
     * Get the animator auto start status. This determines if it will run when layout is completed.
     *
     * @return Auto start status for the animator.
     */
    public boolean isAutoStart() {
        return mAutoStart;
    }

    /**
     * Set the animator auto start status. This determines if it will run when layout is completed.
     *
     * @param autoStart Auto start status for the animator.
     */
    public void setAutoStart(boolean autoStart) {
        mAutoStart = autoStart;
        requestLayout();
    }
}
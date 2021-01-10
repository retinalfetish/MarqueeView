package com.unary.test;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import com.unary.marqueeview.MarqueeView;

public class MainActivity extends AppCompatActivity {

    private MarqueeView mTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mTextView = findViewById(R.id.text_view);

        // Change the marquee scroll
        //mTextView.setRepeatCount(3);
        //mTextView.setScrollSpeed(0.5f);

        // Set paint characteristics
        //mTextView.getTextPaint().setStyle(Paint.Style.STROKE);
        //mTextView.getTextPaint().setStrokeWidth(4);

        // Use an external animator
        //mTextView.setTextAnimator(null);
        //mTextView.postDelayed(new Runnable() {
        //    @Override
        //    public void run() {
        //        float width = mTextView.getTextPaint().measureText(mTextView.getText()) - mTextView.getWidth();
        //        ObjectAnimator animator = ObjectAnimator
        //                .ofFloat(mTextView, MarqueeView.OFFSET, mTextView.getOffset(), -width)
        //                .setDuration(2000);
        //        animator.setRepeatCount(ObjectAnimator.INFINITE);
        //        animator.setRepeatMode(ObjectAnimator.REVERSE);
        //        animator.start();
        //    }
        //}, 1000);
    }
}
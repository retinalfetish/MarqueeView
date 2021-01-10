# MarqueeView
A styleable marquee text widget that creates the classic effect but with a few more features. Note that this is not extended from TextView.
## Screenshots
<img src="/art/screenshot-animation-fling.gif" alt="Screenshot" height=600> <img src="/art/screenshot-animation-repeat.gif" alt="Screenshot" height=600>

## Usage
The library is part of [JCenter](https://bintray.com/rogue/maven/com.unary:marqueeview) (a default repository) and can be included in your project by adding `implementation 'com.unary:marqueeview:1.0.0'` as a module dependency. The latest build can also be found at [JitPack](https://jitpack.io/#com.unary/marqueeview).
```
allprojects {
    repositories {
        ...
        maven { url 'https://jitpack.io' }
    }
}
```
This widget has a number of options that can be configured in both the XML and code. An example app is provided in the project repository to illustrate its use and the `TouchMarqueeView` class.
```
<?xml version="1.0" encoding="utf-8"?>
<FrameLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    xmlns:tools="http://schemas.android.com/tools"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    tools:context=".MainActivity">

    <com.unary.marqueeview.MarqueeView
        android:id="@+id/text_view"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_gravity="center"
        android:text="This space for rent..."
        android:textSize="72sp"
        app:repeatCount="3"
        app:scrollSpeed="150%" />

</FrameLayout>
```
Using the offset property:
```
ObjectAnimator animator = ObjectAnimator
        .ofFloat(textView, MarqueeView.OFFSET, textView.getOffset(), textView.getOffset() - 1000)
        .setDuration(1000);
animator.setInterpolator(new DecelerateInterpolator());
animator.start();
```
### XML attributes
The following optional attributes can be used to change the look and feel of the view:
```
app:textAnimator="reference"        // Animator to use for the text marquee
app:repeatCount="integer"           // Default is ValueAnimator.INFINITE
app:scrollSpeed="percent"           // Unit interval used to determine speed

android:autoStart="boolean"         // If scrolling should start automatically
android:enabled="boolean"           // Changes the view state
android:text="string"               // Only scrolls if necessary
android:textColor="reference|color" // Reference to a color selector or simple color
android:textSize="dimension"        // Text size used. Default is 14sp
```

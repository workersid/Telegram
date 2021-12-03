package org.telegram.ui.Components.reaction;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.Property;
import android.view.View;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.ui.Components.AnimationProperties;

import java.util.ArrayList;

public class AnimatedReactionNumberLayout {

    private ArrayList<StaticLayout> letters = new ArrayList<>();
    private ArrayList<StaticLayout> oldLetters = new ArrayList<>();
    private final TextPaint textPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private ObjectAnimator animator;
    private float progress = 0.0f;
    private int currentNumber = 1;
    private final View parentView;

    public static final Property<AnimatedReactionNumberLayout, Float> PROGRESS = new AnimationProperties.FloatProperty<AnimatedReactionNumberLayout>("progress") {
        @Override
        public void setValue(AnimatedReactionNumberLayout object, float value) {
            object.setProgress(value);
        }

        @Override
        public Float get(AnimatedReactionNumberLayout object) {
            return object.progress;
        }
    };

    public AnimatedReactionNumberLayout(View parent) {
        parentView = parent;
        textPaint.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        textPaint.setTextSize(AndroidUtilities.dp(14));
        textPaint.setColor(Color.BLACK);
    }

    public void setTextColor(int color) {
        textPaint.setColor(color);
    }

    private void setProgress(float value) {
        if (progress == value) {
            return;
        }
        progress = value;
        parentView.invalidate();
    }

    private float getProgress() {
        return progress;
    }

    public int getWidth() {
        float width = 0;
        int count = letters.size();
        for (int a = 0; a < count; a++) {
            width += letters.get(a).getLineWidth(0);
        }
        return (int) Math.ceil(width);
    }

    public void setNumber(int number, boolean animated) {
        if (currentNumber == number && !letters.isEmpty()) {
            return;
        }
        if (animator != null) {
            animator.cancel();
            animator = null;
        }
        oldLetters.clear();
        oldLetters.addAll(letters);
        letters.clear();
        String oldText = String.format("%s", LocaleController.formatShortNumber(Math.max(1, currentNumber), null));
        String text = String.format("%s", LocaleController.formatShortNumber(Math.max(1, number), null));
        boolean forwardAnimation = number > currentNumber;
        currentNumber = number;
        progress = 0;
        for (int a = 0; a < text.length(); a++) {
            String ch = text.substring(a, a + 1);
            String oldCh = !oldLetters.isEmpty() && a < oldText.length() ? oldText.substring(a, a + 1) : null;
            if (oldCh != null && oldCh.equals(ch)) {
                letters.add(oldLetters.get(a));
                oldLetters.set(a, null);
            } else {
                StaticLayout layout = new StaticLayout(ch, textPaint, (int) Math.ceil(textPaint.measureText(ch)), Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false);
                letters.add(layout);
            }
        }
        if (animated && !oldLetters.isEmpty()) {
            animator = ObjectAnimator.ofFloat(this, PROGRESS, forwardAnimation ? -1 : 1, 0);
            animator.setDuration(150);
            animator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    animator = null;
                    oldLetters.clear();
                }
            });
            animator.start();
        }
        parentView.invalidate();
    }

    public void draw(Canvas canvas) {
        if (letters.isEmpty()) {
            return;
        }
        float height = letters.get(0).getHeight();
        int count = Math.max(letters.size(), oldLetters.size());
        canvas.save();
        int currentAlpha = textPaint.getAlpha();
        for (int a = 0; a < count; a++) {
            canvas.save();
            StaticLayout old = a < oldLetters.size() ? oldLetters.get(a) : null;
            StaticLayout layout = a < letters.size() ? letters.get(a) : null;
            if (progress > 0) {
                if (old != null) {
                    textPaint.setAlpha((int) (currentAlpha * progress));
                    canvas.save();
                    canvas.translate(0, (progress - 1.0f) * height);
                    old.draw(canvas);
                    canvas.restore();
                    if (layout != null) {
                        textPaint.setAlpha((int) (currentAlpha * (1.0f - progress)));
                        canvas.translate(0, progress * height);
                    }
                } else {
                    textPaint.setAlpha(currentAlpha);
                }
            } else if (progress < 0) {
                if (old != null) {
                    textPaint.setAlpha((int) (currentAlpha * -progress));
                    canvas.save();
                    canvas.translate(0, (1.0f + progress) * height);
                    old.draw(canvas);
                    canvas.restore();
                }
                if (layout != null) {
                    if (a == count - 1 || old != null) {
                        textPaint.setAlpha((int) (currentAlpha * (1.0f + progress)));
                        canvas.translate(0, progress * height);
                    } else {
                        textPaint.setAlpha(currentAlpha);
                    }
                }
            } else if (layout != null) {
                textPaint.setAlpha(currentAlpha);
            }
            if (layout != null) {
                layout.draw(canvas);
            }
            canvas.restore();
            canvas.translate((layout != null ? layout.getLineWidth(0) : old.getLineWidth(0)), 0);
        }
        canvas.restore();
    }
}

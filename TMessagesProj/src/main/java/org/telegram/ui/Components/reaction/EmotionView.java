package org.telegram.ui.Components.reaction;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.LayoutTransition;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.view.Gravity;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.FileLoader;
import org.telegram.messenger.ImageLocation;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.AvatarsImageView;
import org.telegram.ui.Components.BackupImageView;
import org.telegram.ui.Components.CubicBezierInterpolator;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.NumberTextView;

import java.util.ArrayList;
import java.util.List;

public class EmotionView extends LinearLayout {

    private final AvatarsImageView avatarsImageView;
    private final BackupImageView imageView;
    private final NumberTextView numberTextView;

    private final RectF bgRect = new RectF();
    private final Path bgClipPath = new Path();
    private final int roundBgRadius = AndroidUtilities.dp(24);
    private final Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint selectedPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private EmotionInfo emotionInfo;
    private boolean wasSelected;
    private final int currentAccount = UserConfig.selectedAccount;

    @SuppressLint("ClickableViewAccessibility")
    public EmotionView(@NonNull Context context) {
        super(context);
        setOrientation(HORIZONTAL);
        bgPaint.setColor(Color.BLUE);
        selectedPaint.setColor(Color.WHITE);
        selectedPaint.setStrokeWidth(AndroidUtilities.dp(2));
        selectedPaint.setStyle(Paint.Style.STROKE);

        imageView = new BackupImageView(context);
        imageView.setAspectFit(true);
        imageView.setLayerNum(1);
        addView(imageView, LayoutHelper.createLinear(24, 24, Gravity.LEFT | Gravity.CENTER_VERTICAL, 11, 0, 0, 0));

        numberTextView = new NumberTextView(context);
        numberTextView.setTextSize(18);
        numberTextView.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        numberTextView.setTextColor(Theme.getColor(Theme.key_actionBarActionModeDefaultIcon));
        addView(numberTextView, LayoutHelper.createLinear(0, LayoutHelper.MATCH_PARENT, 1.0f, 65, 0, 0, 0));
        numberTextView.setOnTouchListener((v, event) -> true);

        avatarsImageView = new AvatarsImageView(context, false);
        avatarsImageView.setStyle(AvatarsImageView.STYLE_MESSAGE_SEEN);
        addView(avatarsImageView, LayoutHelper.createLinear(24 + 12 + 12 + 8, LayoutHelper.MATCH_PARENT, Gravity.RIGHT | Gravity.CENTER_VERTICAL, 0, 0, 0, 0));

        setWillNotDraw(false);
        setLayoutTransition(new LayoutTransition());
    }

    public void setEmotionInfo(EmotionInfo inputEmotionInfo, boolean animated) {
        if (inputEmotionInfo == null) return;

        if (this.emotionInfo != null && this.emotionInfo.isSelectedByCurrentUser && !inputEmotionInfo.isSelectedByCurrentUser && animated) {
            wasSelected = true;
        }

        emotionInfo = inputEmotionInfo;

        if (animated) animatedStroke(!wasSelected);

        TLRPC.PhotoSize thumb = FileLoader.getClosestPhotoSizeWithSize(emotionInfo.staticIcon.thumbs, 90);
        imageView.setImage(ImageLocation.getForDocument(thumb, emotionInfo.staticIcon), "50_50", "webp", null, inputEmotionInfo);
        numberTextView.setNumber(emotionInfo.count, animated);

        List<TLRPC.User> users = emotionInfo.lastThreeUsers;
        for (int i = 0; i < 3; i++) {
            if (i < users.size()) {
                avatarsImageView.setObject(i, currentAccount, users.get(i));
            } else {
                avatarsImageView.setObject(i, currentAccount, null);
            }
        }
        if (users.size() == 1) {
            avatarsImageView.setTranslationX(AndroidUtilities.dp(24));
        } else if (users.size() == 2) {
            avatarsImageView.setTranslationX(AndroidUtilities.dp(12));
        } else {
            avatarsImageView.setTranslationX(0);
        }

        avatarsImageView.commitTransition(animated);
    }

    private void animatedStroke(boolean show) {
        ValueAnimator anim;
        if (show) {
            anim = ValueAnimator.ofFloat(AndroidUtilities.dp(0), AndroidUtilities.dp(2));
        } else {
            anim = ValueAnimator.ofFloat(AndroidUtilities.dp(2), AndroidUtilities.dp(1));
        }
        anim.addUpdateListener(animation -> {
            int val = (int) animation.getAnimatedValue();
            selectedPaint.setStrokeWidth(val);
            invalidate();
        });
        anim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                wasSelected = false;
                selectedPaint.setStrokeWidth(AndroidUtilities.dp(2));
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                wasSelected = false;
                selectedPaint.setStrokeWidth(AndroidUtilities.dp(2));
            }
        });
        anim.setInterpolator(CubicBezierInterpolator.EASE_OUT_QUINT);
        anim.setDuration(150);
        anim.start();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        bgClipPath.reset();
        bgRect.set(0, 0, w, h - AndroidUtilities.dp(18));
        bgClipPath.addRoundRect(bgRect, roundBgRadius, roundBgRadius, Path.Direction.CW);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawRoundRect(bgRect, roundBgRadius, roundBgRadius, bgPaint);
        if (emotionInfo != null && emotionInfo.isSelectedByCurrentUser || wasSelected) {
            canvas.drawRoundRect(bgRect, roundBgRadius, roundBgRadius, selectedPaint);
        }
        canvas.clipPath(bgClipPath);
        super.onDraw(canvas);
    }
}

class EmotionInfo {
    public List<TLRPC.User> lastThreeUsers = new ArrayList<>();
    public boolean isSelectedByCurrentUser;
    public int count;
    public TLRPC.Document staticIcon;
}

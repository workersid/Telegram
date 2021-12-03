package org.telegram.ui.Components.reaction;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.LayoutTransition;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.view.Gravity;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.FileLoader;
import org.telegram.messenger.ImageLocation;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.Components.AvatarsImageView;
import org.telegram.ui.Components.BackupImageView;
import org.telegram.ui.Components.CubicBezierInterpolator;
import org.telegram.ui.Components.LayoutHelper;

import java.util.ArrayList;
import java.util.List;

public class EmotionCell extends LinearLayout {

    private final AvatarsImageView avatarsImageView;
    private final BackupImageView imageView;
    private final ReactionNumberTextView numberTextView;

    private final RectF bgRect = new RectF();
    private final Path bgClipPath = new Path();
    private final int roundBgRadius = AndroidUtilities.dp(24);
    private final Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint selectedPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private EmotionInfo emotionInfo;
    private boolean wasSelected;
    private final int currentAccount = UserConfig.selectedAccount;

    @SuppressLint("ClickableViewAccessibility")
    public EmotionCell(@NonNull Context context) {
        super(context);
        setOrientation(HORIZONTAL);
        bgPaint.setColor(0x1A378dd1);
        selectedPaint.setColor(0xFF378DD1);
        selectedPaint.setStrokeWidth(AndroidUtilities.dp(2));
        selectedPaint.setStyle(Paint.Style.STROKE);

        imageView = new BackupImageView(context);
        imageView.setAspectFit(true);
        imageView.setLayerNum(1);
        addView(imageView, LayoutHelper.createLinear(24, 24, Gravity.LEFT | Gravity.CENTER_VERTICAL, 12, 0, 0, 0));

        numberTextView = new ReactionNumberTextView(context);
        numberTextView.setTextSize(14);
        numberTextView.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        numberTextView.setTextColor(0xFF378DD1);
        addView(numberTextView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.MATCH_PARENT, 6, 0, 0, 0));
        numberTextView.setOnTouchListener((v, event) -> true);

        avatarsImageView = new AvatarsImageView(context, false);
        avatarsImageView.setStyle(AvatarsImageView.STYLE_MESSAGE_SEEN);
        addView(avatarsImageView, LayoutHelper.createLinear(24 + 12 + 12 + 8, LayoutHelper.MATCH_PARENT, Gravity.CENTER_VERTICAL, 4, 0, 0, 0));

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

        if (emotionInfo.staticIcon != null) {
            TLRPC.PhotoSize thumb = FileLoader.getClosestPhotoSizeWithSize(emotionInfo.staticIcon.thumbs, 90);
            imageView.setImage(ImageLocation.getForDocument(thumb, emotionInfo.staticIcon), "50_50", "webp", null, emotionInfo);
        } else {
            Drawable drawable = ContextCompat.getDrawable(getContext(), R.drawable.msg_reactions_filled).mutate();
            drawable.setColorFilter(0xff378DD1, PorterDuff.Mode.MULTIPLY);
            Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
            drawable.draw(canvas);
            imageView.setImageBitmap(bitmap);
        }

        numberTextView.setNumber(emotionInfo.count, animated);

        List<Long> userIds = emotionInfo.lastThreeUsers;

        boolean hasUnknownUsers = false;

        if (emotionInfo.count <= 3 && userIds.size() > 0 && emotionInfo.count == userIds.size()) {
            numberTextView.setVisibility(GONE);
            List<TLRPC.User> users = new ArrayList<>(3);

            for (long userId : userIds) {
                TLRPC.User user = MessagesController.getInstance(currentAccount).getUser(userId);
                users.add(user);
                if (user == null) {
                    hasUnknownUsers = true;
                }
            }

            for (int i = 0; i < 3; i++) {
                if (i < userIds.size()) {
                    avatarsImageView.setObject(i, currentAccount, users.get(i));
                } else {
                    avatarsImageView.setObject(i, currentAccount, null);
                }
            }
            if (userIds.size() == 1) {
                avatarsImageView.setTranslationX(AndroidUtilities.dp(24));
            } else if (userIds.size() == 2) {
                avatarsImageView.setTranslationX(AndroidUtilities.dp(12));
            } else {
                avatarsImageView.setTranslationX(0);
            }
            avatarsImageView.commitTransition(animated);
            avatarsImageView.setCount(users.size());
            if (users.size() == 0) {
                avatarsImageView.getLayoutParams().width = 0;
            }
            if (users.size() == 1) {
                avatarsImageView.getLayoutParams().width = AndroidUtilities.dp(24 + 12 + 12 + 8);
                ((MarginLayoutParams) avatarsImageView.getLayoutParams()).leftMargin = -AndroidUtilities.dp(12 + 8);
            }
            if (users.size() == 2) {
                avatarsImageView.getLayoutParams().width = AndroidUtilities.dp(24 + 12 + 12 + 8);
                ((MarginLayoutParams) avatarsImageView.getLayoutParams()).leftMargin = -AndroidUtilities.dp(8);
            }
            if (users.size() == 3) {
                avatarsImageView.getLayoutParams().width = AndroidUtilities.dp(24 + 12 + 12 + 8);
                ((MarginLayoutParams) avatarsImageView.getLayoutParams()).leftMargin = AndroidUtilities.dp(4);
            }
        } else {
            numberTextView.setVisibility(VISIBLE);

            for (int i = 0; i < 3; i++) {
                avatarsImageView.setObject(i, currentAccount, null);
            }
            avatarsImageView.setTranslationX(0);
            avatarsImageView.commitTransition(animated);
            avatarsImageView.setCount(0);
            avatarsImageView.getLayoutParams().width = 0;
        }

        if (hasUnknownUsers) {
            TLRPC.TL_messages_getMessageReactionsList req = new TLRPC.TL_messages_getMessageReactionsList();
            req.limit = 50;
            req.id = emotionInfo.messageId;
            req.peer = MessagesController.getInstance(currentAccount).getInputPeer(emotionInfo.dialogId);

            ConnectionsManager.getInstance(currentAccount).sendRequest(req, (response1, error1) -> AndroidUtilities.runOnUIThread(() -> {
                if (response1 != null) {
                    TLRPC.TL_messages_messageReactionsList users = (TLRPC.TL_messages_messageReactionsList) response1;
                    for (int i = 0; i < users.users.size(); i++) {
                        TLRPC.User user = users.users.get(i);
                        MessagesController.getInstance(currentAccount).putUser(user, false);
                    }
                    setEmotionInfo(emotionInfo, true);
                }
            }));
        }
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
        anim.setDuration(100);
        anim.start();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        bgClipPath.reset();
        bgRect.set(AndroidUtilities.dp(4), AndroidUtilities.dp(4), w - AndroidUtilities.dp(4), h - AndroidUtilities.dp(4));
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

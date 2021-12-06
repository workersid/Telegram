package org.telegram.ui.Components.reaction;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.R;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.Cells.ChatMessageCell;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;

import java.util.List;

public class FullScreenReactionDialog extends Dialog {
    private FrameLayout container;
    private FullScreenReactionStickerCell stickerView;
    private FullScreenReactionStickerCell effectView;
    private Handler handler;
    private final Runnable checkAnimationReadyRunnable = new Runnable() {
        @Override
        public void run() {
            if (stickerView.isReadyAnimation() && effectView.isReadyAnimation()) {
                stickerView.runLottie();
                effectView.runLottie();
            } else {
                handler.postDelayed(checkAnimationReadyRunnable, 100);
            }
        }
    };
    private int startAnimX;
    private int startAnimY;
    private RecyclerListView listView;
    private int msgId;
    private boolean isUserDialog;

    public FullScreenReactionDialog(@NonNull Context context, TLRPC.TL_availableReaction reaction, int startAnimX, int startAnimY, RecyclerListView listView, int msgId, boolean isUserDialog) {
        super(context);
        this.startAnimX = startAnimX;
        this.startAnimY = startAnimY;
        this.listView = listView;
        this.msgId = msgId;
        this.isUserDialog = isUserDialog;
        init(context, reaction);
    }

    private void init(final Context context, final TLRPC.TL_availableReaction reaction) {
        handler = new Handler(Looper.getMainLooper());
        container = new FrameLayout(context);
        stickerView = new FullScreenReactionStickerCell(context);
        effectView = new FullScreenReactionStickerCell(context);
        container.addView(stickerView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
        container.addView(effectView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, Gravity.CENTER));
        stickerView.setSticker(reaction.activate_animation, reaction, false, "reaction_" + reaction.reaction + "_" + "sticker", () -> {
            if (isShowing()) {
                FullScreenReactionDialog.this.dismiss();
            }
        }, isUserDialog);
        stickerView.setDelegate(() -> {
            if (listView != null) {
                for (int i = 0; i < listView.getChildCount(); i++) {
                    View view = listView.getChildAt(i);
                    if (view instanceof ChatMessageCell) {
                        ChatMessageCell cell = (ChatMessageCell) view;
                        if (cell.getMessageObject() != null && cell.getMessageObject().getId() == msgId) {
                            List<EmotionInfo> emotionInfoList = cell.getEmotionsInChatMessage();
                            for (int a = 0; a < emotionInfoList.size(); a++) {
                                EmotionInfo emotionInfo = emotionInfoList.get(a);
                                if (emotionInfo.reaction != null && emotionInfo.reaction.equals(reaction.reaction)) {
                                    int x = (int) (emotionInfo.emotionRegion.left);
                                    int y = (int) (emotionInfo.emotionRegion.bottom + cell.getTop() + cell.getBackgroundDrawableTop() + AndroidUtilities.statusBarHeight + AndroidUtilities.dp(7));
                                    return new int[]{x, y};
                                }
                            }
                        }
                    }
                }
            }
            return new int[]{0, 0};
        });
        container.setOnClickListener(v -> {
            if (isShowing()) {
                FullScreenReactionDialog.this.dismiss();
            }
        });
        effectView.setSticker(reaction.effect_animation, reaction, true, "reaction_" + reaction.reaction + "_" + "effect", null, isUserDialog);
        stickerView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setWindowAnimations(R.style.DialogNoAnimation);
        getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        setContentView(container, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        WindowManager.LayoutParams params = getWindow().getAttributes();
        params.width = ViewGroup.LayoutParams.MATCH_PARENT;
        params.height = ViewGroup.LayoutParams.MATCH_PARENT;
        params.dimAmount = 0;
        params.flags &= ~WindowManager.LayoutParams.FLAG_DIM_BEHIND;
        params.gravity = Gravity.TOP | Gravity.LEFT;
        if (Build.VERSION.SDK_INT >= 21) {
            params.flags |= WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN |
                    WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR |
                    WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS;
        }
        getWindow().setAttributes(params);
    }

    @Override
    public void show() {
        super.show();
        stickerView.startLaunchAnimation(startAnimX, startAnimY);
        handler.postDelayed(checkAnimationReadyRunnable, 1);
    }

    @Override
    public void dismiss() {
        handler.removeCallbacksAndMessages(null);
        stickerView.stopLottie();
        effectView.stopLottie();
        super.dismiss();
    }

    @Override
    public void onDetachedFromWindow() {
        handler.removeCallbacksAndMessages(null);
        super.onDetachedFromWindow();
    }
}

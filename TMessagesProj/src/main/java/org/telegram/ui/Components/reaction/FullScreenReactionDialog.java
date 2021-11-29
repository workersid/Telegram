package org.telegram.ui.Components.reaction;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;

import org.telegram.messenger.R;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.Components.LayoutHelper;

public class FullScreenReactionDialog extends Dialog {
    private FrameLayout container;
    private FullScreenReactionStickerCell stickerView;
    private FullScreenReactionStickerCell effectView;

    public FullScreenReactionDialog(@NonNull Context context, TLRPC.TL_availableReaction reaction) {
        super(context);
        init(context, reaction);
    }

    private void init(Context context, TLRPC.TL_availableReaction reaction) {
        container = new FrameLayout(context);
        stickerView = new FullScreenReactionStickerCell(context);
        effectView = new FullScreenReactionStickerCell(context);
        container.addView(stickerView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, Gravity.CENTER));
        container.addView(effectView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, Gravity.CENTER));
        stickerView.setSticker(reaction.activate_animation, reaction, false, "reaction_" + reaction.reaction + "_" + "sticker", () -> {
            if (isShowing()) {
                FullScreenReactionDialog.this.dismiss();
            }
        });
        effectView.setSticker(reaction.effect_animation, reaction, true, "reaction_" + reaction.reaction + "_" + "effect", null);
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
        stickerView.runLottie();
        effectView.runLottie();
    }

    @Override
    public void dismiss() {
        stickerView.stopLottie();
        effectView.stopLottie();
        super.dismiss();
    }
}

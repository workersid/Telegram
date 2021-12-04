package org.telegram.ui.Components.reaction;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.view.Gravity;
import android.view.View;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.animation.AccelerateInterpolator;
import android.widget.FrameLayout;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.DocumentObject;
import org.telegram.messenger.FileLoader;
import org.telegram.messenger.ImageLocation;
import org.telegram.messenger.ImageReceiver;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.R;
import org.telegram.messenger.SvgHelper;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.BackupImageView;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RLottieDrawable;

import java.util.Random;

public class ChooseReactionStickerCell extends FrameLayout {

    private final BackupImageView imageView;
    private TLRPC.Document sticker;
    private Object parentObject;
    private long lastUpdateTime;
    private boolean scaled;
    private float scale;
    private boolean clearsInputField;
    private static final AccelerateInterpolator interpolator = new AccelerateInterpolator(0.5f);
    private final ImageReceiver imageReceiverEffect = new ImageReceiver();
    private final ImageReceiver imageReceiverActivate = new ImageReceiver();
    private boolean isAnimationReady;
    private final Handler handler;
    private final Random random = new Random();
    private final Runnable randomAnimationRunnable = new Runnable() {
        @Override
        public void run() {
            RLottieDrawable rLottieDrawable = imageView.getImageReceiver().getLottieAnimation();
            if (rLottieDrawable != null && isAnimationReady) {
                if (!rLottieDrawable.isRunning()) {
                    if (random.nextInt(4) == 0) {
                        imageView.getImageReceiver().setAutoRepeat(2);
                        imageView.getImageReceiver().setAllowStartAnimation(true);
                        imageView.getImageReceiver().startAnimation();
                    }
                }
            }

            handler.postDelayed(randomAnimationRunnable, 600);
        }
    };

    public ChooseReactionStickerCell(Context context) {
        super(context);
        handler = new Handler();
        imageView = new BackupImageView(context);
        imageView.setAspectFit(true);
        imageView.setLayerNum(1);
        //1 - зацикливать, 2 - единожды отобразит, 3 - только первый кадр
        imageView.getImageReceiver().setAutoRepeat(3);

        addView(imageView, LayoutHelper.createFrame(36, 36, Gravity.TOP, 0, 6, 0, 0));
        setFocusable(true);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(44) + getPaddingLeft() + getPaddingRight(), MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(56), MeasureSpec.EXACTLY));
    }

    public boolean isAnimationReady() {
        return isAnimationReady;
    }

    @Override
    public void setPressed(boolean pressed) {
        if (imageView.getImageReceiver().getPressed() != pressed) {
            imageView.getImageReceiver().setPressed(pressed ? 1 : 0);
            imageView.invalidate();
        }
        super.setPressed(pressed);
    }

    public void setClearsInputField(boolean value) {
        clearsInputField = value;
    }

    public boolean isClearsInputField() {
        return clearsInputField;
    }

    public void setSticker(TLRPC.Document document, TLRPC.TL_availableReaction tlAvailableReaction) {
        isAnimationReady = false;
        if (document == null && tlAvailableReaction == null) return;
        parentObject = tlAvailableReaction;
        sticker = document;
        setSelectSticker(document, tlAvailableReaction.reaction);
        setEffect(tlAvailableReaction.effect_animation);
        setActivation(tlAvailableReaction.activate_animation);
        Drawable background = getBackground();
        if (background != null) {
            background.setAlpha(230);
            background.setColorFilter(new PorterDuffColorFilter(Theme.getColor(Theme.key_chat_stickersHintPanel), PorterDuff.Mode.MULTIPLY));
        }
    }

    private void setSelectSticker(TLRPC.Document document, String reactionName) {
        TLRPC.PhotoSize thumb = FileLoader.getClosestPhotoSizeWithSize(document.thumbs, 90);
        SvgHelper.SvgDrawable svgThumb = DocumentObject.getSvgThumb(document, Theme.key_windowBackgroundGray, 1.0f);
        imageView.getImageReceiver().setUniqKeyPrefix("reaction_" + reactionName);
        if (svgThumb != null) {
            imageView.setImage(ImageLocation.getForDocument(document), "80_80", null, svgThumb, parentObject);
        } else if (thumb != null) {
            imageView.setImage(ImageLocation.getForDocument(document), "80_80", ImageLocation.getForDocument(thumb, document), null, 0, parentObject);
        } else {
            imageView.setImage(ImageLocation.getForDocument(document), "80_80", null, null, parentObject);
        }

        imageView.getImageReceiver().setDelegate(new ImageReceiver.ImageReceiverDelegate() {
            @Override
            public void didSetImage(ImageReceiver imageReceiver, boolean set, boolean thumb, boolean memCache) {

            }

            @Override
            public void onAnimationReady(ImageReceiver imageReceiver) {
                isAnimationReady = true;
            }
        });
    }

    private void setEffect(TLRPC.Document document) {
        TLRPC.PhotoSize thumb = FileLoader.getClosestPhotoSizeWithSize(document.thumbs, 90);
        SvgHelper.SvgDrawable svgThumb = DocumentObject.getSvgThumb(document, Theme.key_windowBackgroundGray, 1.0f);
        int size = Math.min(AndroidUtilities.displaySize.x, AndroidUtilities.displaySize.y) / 2;
        String imageFilter = size + "_" + size + "_pcache";//прекеш
        if (svgThumb != null) {
            imageReceiverEffect.setImage(ImageLocation.getForDocument(document), imageFilter, null, null, svgThumb, 0, "tgs", document, 0);
        } else if (thumb != null) {
            imageReceiverEffect.setImage(ImageLocation.getForDocument(document), imageFilter, ImageLocation.getForDocument(thumb, document), null, null, 0, "tgs", document, 0);
        } else {
            imageReceiverEffect.setImage(ImageLocation.getForDocument(document), imageFilter, null, null, null, 0, "tgs", document, 0);
        }
        imageReceiverEffect.setLayerNum(Integer.MAX_VALUE);
        imageReceiverEffect.setAllowStartAnimation(true);
        imageReceiverEffect.setAutoRepeat(0);
        if (imageReceiverEffect.getLottieAnimation() != null) {
            imageReceiverEffect.getLottieAnimation().start();
        }
    }

    private void setActivation(TLRPC.Document document) {
        TLRPC.PhotoSize thumb = FileLoader.getClosestPhotoSizeWithSize(document.thumbs, 90);
        SvgHelper.SvgDrawable svgThumb = DocumentObject.getSvgThumb(document, Theme.key_windowBackgroundGray, 1.0f);
        int size = Math.min(AndroidUtilities.displaySize.x, AndroidUtilities.displaySize.y) / 2;
        String imageFilter = size + "_" + size + "_pcache";//прекеш
        if (svgThumb != null) {
            imageReceiverActivate.setImage(ImageLocation.getForDocument(document), imageFilter, null, null, svgThumb, 0, "tgs", parentObject, 0);
        } else if (thumb != null) {
            imageReceiverActivate.setImage(ImageLocation.getForDocument(document), imageFilter, ImageLocation.getForDocument(thumb, document), null, null, 0, "tgs", parentObject, 0);
        } else {
            imageReceiverActivate.setImage(ImageLocation.getForDocument(document), imageFilter, null, null, null, 0, "tgs", parentObject, 0);
        }
        imageReceiverActivate.setLayerNum(Integer.MAX_VALUE);
        imageReceiverActivate.setAllowStartAnimation(true);
        imageReceiverActivate.setAutoRepeat(0);
        if (imageReceiverActivate.getLottieAnimation() != null) {
            imageReceiverActivate.getLottieAnimation().start();
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        imageReceiverActivate.onAttachedToWindow();
        imageReceiverEffect.onAttachedToWindow();
        handler.postDelayed(randomAnimationRunnable, 600);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        handler.removeCallbacksAndMessages(null);
        imageReceiverActivate.onDetachedFromWindow();
        imageReceiverEffect.onDetachedFromWindow();
    }

    public TLRPC.Document getSticker() {
        return sticker;
    }

    public Object getParentObject() {
        return parentObject;
    }

    public void setScaled(boolean value) {
        scaled = value;
        lastUpdateTime = System.currentTimeMillis();
        invalidate();
    }

    public boolean showingBitmap() {
        return imageView.getImageReceiver().getBitmap() != null;
    }

    public MessageObject.SendAnimationData getSendAnimationData() {
        ImageReceiver imageReceiver = imageView.getImageReceiver();
        if (!imageReceiver.hasNotThumb()) {
            return null;
        }
        MessageObject.SendAnimationData data = new MessageObject.SendAnimationData();
        int[] position = new int[2];
        imageView.getLocationInWindow(position);
        data.x = imageReceiver.getCenterX() + position[0];
        data.y = imageReceiver.getCenterY() + position[1];
        data.width = imageReceiver.getImageWidth();
        data.height = imageReceiver.getImageHeight();
        return data;
    }

    @Override
    protected boolean drawChild(Canvas canvas, View child, long drawingTime) {
        boolean result = super.drawChild(canvas, child, drawingTime);
        if (child == imageView && (scaled && scale != 0.8f || !scaled && scale != 1.0f)) {
            long newTime = System.currentTimeMillis();
            long dt = (newTime - lastUpdateTime);
            lastUpdateTime = newTime;
            if (scaled && scale != 0.8f) {
                scale -= dt / 400.0f;
                if (scale < 0.8f) {
                    scale = 0.8f;
                }
            } else {
                scale += dt / 400.0f;
                if (scale > 1.0f) {
                    scale = 1.0f;
                }
            }
            imageView.setScaleX(scale);
            imageView.setScaleY(scale);
            imageView.invalidate();
            invalidate();
        }
        return result;
    }

    @Override
    public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
        super.onInitializeAccessibilityNodeInfo(info);
        if (sticker == null)
            return;
        String emoji = null;
        for (int a = 0; a < sticker.attributes.size(); a++) {
            TLRPC.DocumentAttribute attribute = sticker.attributes.get(a);
            if (attribute instanceof TLRPC.TL_documentAttributeSticker) {
                emoji = attribute.alt != null && attribute.alt.length() > 0 ? attribute.alt : null;
            }
        }
        if (emoji != null)
            info.setText(emoji + " " + LocaleController.getString("AttachSticker", R.string.AttachSticker));
        else
            info.setText(LocaleController.getString("AttachSticker", R.string.AttachSticker));
        info.setEnabled(true);
    }
}

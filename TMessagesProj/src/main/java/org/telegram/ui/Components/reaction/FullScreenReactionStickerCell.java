package org.telegram.ui.Components.reaction;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.view.animation.AccelerateInterpolator;
import android.widget.FrameLayout;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.DocumentObject;
import org.telegram.messenger.FileLoader;
import org.telegram.messenger.ImageLocation;
import org.telegram.messenger.SvgHelper;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.BackupImageView;
import org.telegram.ui.Components.CubicBezierInterpolator;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RLottieDrawable;

import java.util.Random;

@SuppressLint("ViewConstructor")
public class FullScreenReactionStickerCell extends FrameLayout {

    private final BackupImageView imageView;
    private TLRPC.Document sticker;
    private Object parentObject;
    private static final AccelerateInterpolator interpolator = new AccelerateInterpolator(0.5f);
    private Handler handler;
    /*private Runnable checkerRunnable = new Runnable() {
        @Override
        public void run() {
            if(isReadyAnimation()){

            }
            handler.postDelayed(checkerRunnable, 350);
        }
    };*/
    private Runnable mFinishCallback;

    public FullScreenReactionStickerCell(Context context) {
        super(context);
        imageView = new BackupImageView(context);
        imageView.setAspectFit(true);
        imageView.setLayerNum(1);
        imageView.getImageReceiver().setAutoRepeat(3);
        addView(imageView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
        setFocusable(true);
        handler = new Handler(Looper.getMainLooper());
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(MeasureSpec.makeMeasureSpec(MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.EXACTLY));
    }

    public void setSticker(TLRPC.Document document, Object parent, boolean isEffect, String key, final Runnable finishCallback) {
        int size = Math.min(AndroidUtilities.displaySize.x, AndroidUtilities.displaySize.y) / 2;
        if (document != null) {
            parentObject = parent;
            sticker = document;
            if (!isEffect) {
                imageView.setSize(size, size);
            } else {
                int backgroundSize = Math.min(AndroidUtilities.displaySize.x, AndroidUtilities.displaySize.y);
                imageView.setSize(backgroundSize, backgroundSize);
            }
            imageView.getImageReceiver().setUniqKeyPrefix(key + "" + new Random().nextInt());
            TLRPC.PhotoSize thumb = FileLoader.getClosestPhotoSizeWithSize(document.thumbs, 90);
            SvgHelper.SvgDrawable svgThumb = DocumentObject.getSvgThumb(document, Theme.key_windowBackgroundGray, 1.0f);
            String imageFilter = size + "_" + size;//прекеш тут не нужен
            if (svgThumb != null) {
                imageView.setImage(ImageLocation.getForDocument(document), imageFilter, "tgs", svgThumb, parentObject);
            } else if (thumb != null) {
                imageView.setImage(ImageLocation.getForDocument(document), imageFilter, ImageLocation.getForDocument(thumb, document), null, 0, parentObject);
            } else {
                imageView.setImage(ImageLocation.getForDocument(document), imageFilter, "tgs", null, parentObject);
            }
            if (!isEffect) {
                mFinishCallback = finishCallback;
                imageView.getImageReceiver().setDelegate((imageReceiver, set, thumb1, memCache) -> {
                    imageReceiver.getLottieAnimation().setOnFinishCallback(() -> {
                        runFinishAnimation();
                    }, imageReceiver.getLottieAnimation().getFramesCount() - 5);//нельзя расчитывать именно на последний кадр, он может не отрендарится из-за троттлинга
                });
            }
        }
    }

    private void runFinishAnimation() {
        final int bigSize = Math.min(AndroidUtilities.displaySize.x, AndroidUtilities.displaySize.y) / 2;
        final int smallSize = AndroidUtilities.dp(2);
        ValueAnimator anim = ValueAnimator.ofInt(bigSize, smallSize);
        anim.addUpdateListener(animation -> {
            int val = (int) animation.getAnimatedValue();
            imageView.setSize(val, val);
            imageView.invalidate();
        });
        anim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                if (mFinishCallback != null) mFinishCallback.run();
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                super.onAnimationCancel(animation);
                if (mFinishCallback != null) mFinishCallback.run();
            }
        });
        anim.setInterpolator(CubicBezierInterpolator.EASE_OUT_QUINT);
        anim.setDuration(400);
        anim.start();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        //handler.postDelayed(checkerRunnable, 350);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        handler.removeCallbacksAndMessages(null);
    }

    public void runLottie() {
        imageView.getImageReceiver().setAutoRepeat(2);//или 0?
        imageView.getImageReceiver().setAllowStartAnimation(true);
        imageView.getImageReceiver().startAnimation();
    }

    public boolean isReadyAnimation() {
        if (imageView.getImageReceiver() != null && imageView.getImageReceiver().getLottieAnimation() != null) {
            RLottieDrawable rLottieDrawable = imageView.getImageReceiver().getLottieAnimation();
            return rLottieDrawable.hasBitmap();
        }
        return false;
    }

    public void startLaunchAnimation(final boolean move) {
        final int bigSize = Math.min(AndroidUtilities.displaySize.x, AndroidUtilities.displaySize.y) / 2;
        final int smallSize = AndroidUtilities.dp(48);
        if (move) {
            ValueAnimator anim = ValueAnimator.ofInt(smallSize, bigSize);
            anim.addUpdateListener(animation -> {
                int val = (int) animation.getAnimatedValue();
                imageView.setSize(val, val);
                imageView.invalidate();
            });

            anim.setInterpolator(CubicBezierInterpolator.EASE_OUT_QUINT);
            anim.setDuration(500);
            anim.start();
        }
    }

    public void stopLottie() {
        try {
            imageView.getImageReceiver().getLottieAnimation().setOnFinishCallback(null, 0);
            imageView.getImageReceiver().setDelegate(null);
            imageView.getImageReceiver().setAllowStartAnimation(false);
            imageView.getImageReceiver().getLottieAnimation().stop();
            //imageView.getImageReceiver().clearImage();
        } catch (Exception e) {
            //пожарный
        }
    }

    public TLRPC.Document getSticker() {
        return sticker;
    }

    public Object getParentObject() {
        return parentObject;
    }
}

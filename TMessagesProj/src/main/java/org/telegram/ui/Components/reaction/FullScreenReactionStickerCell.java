package org.telegram.ui.Components.reaction;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Configuration;
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

    public interface Delegate {
        int[] findFinisPosition();
    }

    private final BackupImageView imageView;
    private TLRPC.Document sticker;
    private Object parentObject;
    private static final AccelerateInterpolator interpolator = new AccelerateInterpolator(0.5f);
    private Handler handler;
    private int lastFrame = 0;
    private boolean mIsEffect;
    private boolean isFinishRunning;
    private boolean isUserDialog;
    private final Runnable checkerRunnable = new Runnable() {
        @Override
        public void run() {
            try {
                if (mIsEffect) {
                    //мегахак для защиты от застывания
                    if (isReadyAnimation()) {
                        int frame = imageView.getImageReceiver().getLottieAnimation().getCurrentFrame();
                        if (lastFrame > 0 && frame <= 0) {
                            runFinishAnimation();
                            return;
                        }
                        lastFrame = frame;
                    }
                    handler.postDelayed(checkerRunnable, 350);
                }
            } catch (Exception e) {
                //пожарный
            }
        }
    };
    private Runnable mFinishCallback;
    private AnimatorSet startAnimatorSet;
    private Delegate delegate;

    public FullScreenReactionStickerCell(Context context) {
        super(context);
        imageView = new BackupImageView(context);
        imageView.setAspectFit(true);
        imageView.setLayerNum(1);
        imageView.getImageReceiver().setAutoRepeat(3);
        imageView.setLayerType(LAYER_TYPE_HARDWARE, null);
        addView(imageView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
        setFocusable(true);
        handler = new Handler(Looper.getMainLooper());
    }

    public void setDelegate(Delegate delegate) {
        this.delegate = delegate;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (mIsEffect) {
            super.onMeasure(MeasureSpec.makeMeasureSpec(MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.EXACTLY));
        } else {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        }
    }

    public void setSticker(TLRPC.Document document, Object parent, boolean isEffect, String key, final Runnable finishCallback, boolean isUserDialog) {
        this.isUserDialog = isUserDialog;
        int size = Math.min(AndroidUtilities.displaySize.x, AndroidUtilities.displaySize.y) / 2;
        if (document != null) {
            parentObject = parent;
            sticker = document;
            mIsEffect = isEffect;
            if (!isEffect) {
                imageView.setAspectFit(false);
                imageView.setSize(AndroidUtilities.displaySize.x, AndroidUtilities.displaySize.y);
            } else {
                imageView.setAspectFit(true);
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
                    }, imageReceiver.getLottieAnimation().getFramesCount() - 4);//нельзя расчитывать именно на последний кадр, он может не отрендарится из-за троттлинга
                });
            }
        }
    }

    private void runFinishAnimation() {
        if (isFinishRunning) return;
        isFinishRunning = true;
        final int bigSize = Math.min(AndroidUtilities.displaySize.x, AndroidUtilities.displaySize.y) / 2;
        final int smallSize;

        if (isUserDialog) {
            smallSize = AndroidUtilities.dp(14);
        } else {
            smallSize = AndroidUtilities.dp(23);
        }

        final int posX;
        final int posY;
        if (delegate != null) {
            int[] pos = delegate.findFinisPosition();
            posX = pos[0];
            posY = pos[1];
        } else {
            posX = 0;
            posY = 0;
        }

        AnimatorSet endAnimatorSet = new AnimatorSet();
        ValueAnimator anim1 = ValueAnimator.ofInt(bigSize, smallSize);
        anim1.addUpdateListener(animation -> {
            int val = (int) animation.getAnimatedValue();

            if (posX > 0 && posY > 0) {
                imageView.getImageReceiver().setImageWidth(val);
                imageView.getImageReceiver().setImageHeight(val);
            } else {
                imageView.getImageReceiver().setImageCoords((AndroidUtilities.displaySize.x - val) / 2, (AndroidUtilities.displaySize.y - val) / 2, val, val);
            }

            imageView.invalidate();
        });
        ValueAnimator anim2 = ValueAnimator.ofInt((AndroidUtilities.displaySize.y - bigSize) / 2, posY);
        anim2.addUpdateListener(animation -> {
            int val = (int) animation.getAnimatedValue();
            imageView.getImageReceiver().setImageY(val);
            imageView.invalidate();
        });
        ValueAnimator anim3 = ValueAnimator.ofInt((AndroidUtilities.displaySize.x - bigSize) / 2, posX);
        anim3.addUpdateListener(animation -> {
            int val = (int) animation.getAnimatedValue();
            imageView.getImageReceiver().setImageX(val);
            imageView.invalidate();
        });

        if (posX > 0 && posY > 0) {
            endAnimatorSet.playTogether(anim1, anim2, anim3);
        } else {
            endAnimatorSet.playTogether(anim1);
        }

        endAnimatorSet.addListener(new AnimatorListenerAdapter() {
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

        endAnimatorSet.setInterpolator(CubicBezierInterpolator.EASE_OUT_QUINT);
        endAnimatorSet.setDuration(400);
        endAnimatorSet.start();
        invalidate();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        handler.postDelayed(checkerRunnable, 350);
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

    public void startLaunchAnimation(final int posX, final int posY) {
        final int bigSize = Math.min(AndroidUtilities.displaySize.x, AndroidUtilities.displaySize.y) / 2;
        final int smallSize = AndroidUtilities.dp(40);

        startAnimatorSet = new AnimatorSet();
        imageView.ignoreSetCoords = true;
        ValueAnimator anim1 = ValueAnimator.ofInt(smallSize, bigSize);
        anim1.addUpdateListener(animation -> {
            int val = (int) animation.getAnimatedValue();

            if (posX > 0 && posY > 0) {
                imageView.getImageReceiver().setImageWidth(val);
                imageView.getImageReceiver().setImageHeight(val);
            } else {
                imageView.getImageReceiver().setImageCoords((AndroidUtilities.displaySize.x - val) / 2, (AndroidUtilities.displaySize.y - val) / 2, val, val);
            }

            imageView.invalidate();
        });
        ValueAnimator anim2 = ValueAnimator.ofInt(posY, (AndroidUtilities.displaySize.y - bigSize) / 2);
        anim2.addUpdateListener(animation -> {
            int val = (int) animation.getAnimatedValue();
            imageView.getImageReceiver().setImageY(val);
            imageView.invalidate();
        });
        ValueAnimator anim3 = ValueAnimator.ofInt(posX, (AndroidUtilities.displaySize.x - bigSize) / 2);
        anim3.addUpdateListener(animation -> {
            int val = (int) animation.getAnimatedValue();
            imageView.getImageReceiver().setImageX(val);
            imageView.invalidate();
        });

        if (posX > 0 && posY > 0) {
            startAnimatorSet.playTogether(anim1, anim2, anim3);
        } else {
            startAnimatorSet.playTogether(anim1);
        }

        startAnimatorSet.setInterpolator(CubicBezierInterpolator.EASE_IN);
        startAnimatorSet.setDuration(250);
        startAnimatorSet.start();
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (!mIsEffect) {
            if (startAnimatorSet != null) {
                startAnimatorSet.cancel();
                startAnimatorSet = null;
            }
            final int bigSize = Math.min(AndroidUtilities.displaySize.x, AndroidUtilities.displaySize.y) / 2;
            imageView.getImageReceiver().setImageCoords((AndroidUtilities.displaySize.x - bigSize) / 2, (AndroidUtilities.displaySize.y - bigSize) / 2, bigSize, bigSize);
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

package org.telegram.ui.Components.reaction;

import android.annotation.SuppressLint;
import android.content.Context;
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
import org.telegram.ui.Components.LayoutHelper;

import java.util.Random;

@SuppressLint("ViewConstructor")
public class FullScreenReactionStickerCell extends FrameLayout {

    private final BackupImageView imageView;
    private TLRPC.Document sticker;
    private Object parentObject;
    private static final AccelerateInterpolator interpolator = new AccelerateInterpolator(0.5f);

    public FullScreenReactionStickerCell(Context context) {
        super(context);
        imageView = new BackupImageView(context);
        imageView.setAspectFit(true);
        imageView.setLayerNum(1);
        imageView.getImageReceiver().setAutoRepeat(3);
        addView(imageView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
        setFocusable(true);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(MeasureSpec.makeMeasureSpec(MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.EXACTLY));
    }

    public void setSticker(TLRPC.Document document, Object parent, boolean isEffect, String key, Runnable finishCallback) {
        int size = Math.min(AndroidUtilities.displaySize.x, AndroidUtilities.displaySize.y) / 2;
        if (document != null) {
            parentObject = parent;
            sticker = document;
            if (!isEffect) imageView.setSize(size, size);
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
                imageView.getImageReceiver().setDelegate((imageReceiver, set, thumb1, memCache) -> {
                    imageReceiver.getLottieAnimation().setOnFinishCallback(() -> {
                        //todo анимируем переход и потом закрываем
                        if (finishCallback != null) finishCallback.run();
                    }, imageReceiver.getLottieAnimation().getFramesCount() - 5);//нельзя расчитывать именно на последний кадр, он может не отрендарится из-за троттлинга
                });
            }
        }
    }

    public void runLottie() {
        imageView.getImageReceiver().setAutoRepeat(2);//или 0?
        imageView.getImageReceiver().setAllowStartAnimation(true);
        imageView.getImageReceiver().startAnimation();
    }

    public void stopLottie() {
        try {
            imageView.getImageReceiver().setAllowStartAnimation(false);
            imageView.getImageReceiver().getLottieAnimation().stop();
            imageView.getImageReceiver().getLottieAnimation().setOnFinishCallback(null, 0);
            imageView.getImageReceiver().setDelegate(null);
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

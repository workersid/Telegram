package org.telegram.ui.Components.reaction;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.DialogObject;
import org.telegram.messenger.DocumentObject;
import org.telegram.messenger.FileLoader;
import org.telegram.messenger.ImageLocation;
import org.telegram.messenger.ImageReceiver;
import org.telegram.messenger.MediaDataController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.SvgHelper;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.AvatarDrawable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class EmotionsInChatMessage {
    private static final int STYLE_TRANSPARENT = 1;
    private static final int STYLE_GREEN = 2;
    private static final int STYLE_BLUE = 3;

    public interface OnItemClick {
        void onItemClick(EmotionInfo emotionInfo);

        void onItemLongClick(EmotionInfo emotionInfo);
    }

    private final int currentAccount = UserConfig.selectedAccount;

    private final ImageReceiver[] avatarImages = new ImageReceiver[3];
    private final AvatarDrawable[] avatarDrawables = new AvatarDrawable[3];
    private final boolean[] avatarImagesVisible = new boolean[3];

    private final AnimatedReactionNumberLayout[] numberLayouts = new AnimatedReactionNumberLayout[16];
    private final ImageReceiver[] iconImages = new ImageReceiver[16];

    private final Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint selectedPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final List<EmotionInfo> emotionInfoList = new ArrayList<>();

    private boolean isInitialized;
    private MessageObject messageObject;
    private TLRPC.TL_messageReactions reactions;
    private int totalHeight = 0;
    private View parent;
    private final int oneRowHeight = AndroidUtilities.dp(30);
    private final int oneRowMarginVertical = AndroidUtilities.dp(4);
    private final int oneItemMarginHorizontal = AndroidUtilities.dp(4);
    private final int oneItemMaxWidth = AndroidUtilities.dp(92);
    private final int avatarSize = AndroidUtilities.dp(24);
    private final int iconSize = AndroidUtilities.dp(23);
    private final int halfAvatarPadding = AndroidUtilities.dp(2);
    private final RectF rectF = new RectF();
    private boolean pressed;
    private OnItemClick onItemClick;
    private Handler handler;
    private int buttonStyle = STYLE_BLUE;
    private final int iconSizeMini = AndroidUtilities.dp(14);

    public void createForView(View parentView) {
        if (!isInitialized) {
            handler = new Handler(Looper.getMainLooper());
            for (int a = 0; a < avatarImages.length; a++) {
                avatarImages[a] = new ImageReceiver(parentView);
                avatarImages[a].setRoundRadius(AndroidUtilities.dp(12));
                avatarDrawables[a] = new AvatarDrawable();
                avatarDrawables[a].setTextSize(AndroidUtilities.dp(8));
                avatarImages[a].setImageCoords(0, 0, avatarSize, avatarSize);
                avatarImages[a].setInvalidateAll(true);
            }

            for (int a = 0; a < numberLayouts.length; a++) {
                numberLayouts[a] = new AnimatedReactionNumberLayout(parentView);
            }

            for (int a = 0; a < iconImages.length; a++) {
                iconImages[a] = new ImageReceiver(parentView);
                //iconImages[a].setAspectFit(true);
                //iconImages[a].setLayerNum(1);
                //iconImages[a].setAutoRepeat(3);
                iconImages[a].setImageCoords(0, 0, iconSize, iconSize);
                iconImages[a].setInvalidateAll(true);
            }

            bgPaint.setColor(Color.BLACK);
            selectedPaint.setColor(Color.BLUE);
            selectedPaint.setStrokeWidth(AndroidUtilities.dp(2));
            selectedPaint.setStyle(Paint.Style.STROKE);
            parent = parentView;
            isInitialized = true;
        }
    }

    public void setReactions(MessageObject messageObject, MessageObject.GroupedMessages groupedMessages, OnItemClick onItemClick) {
        this.onItemClick = onItemClick;
        if (messageObject != null) {
            if (groupedMessages != null && groupedMessages.messages.size() > 0) {
                MessageObject object = groupedMessages.messages.get(0);
                if (object.hasReactions()) {
                    buttonStyle = object.isOutOwner() ? STYLE_GREEN : STYLE_BLUE;
                    this.messageObject = object;
                    this.reactions = object.messageOwner.reactions;
                    bind();
                    return;
                }
            } else {
                buttonStyle = messageObject.isOutOwner() ? STYLE_GREEN : STYLE_BLUE;
                this.messageObject = messageObject;
                this.reactions = messageObject.messageOwner.reactions;
                bind();
                return;
            }
        }

        this.messageObject = null;
        this.reactions = null;
        this.emotionInfoList.clear();
    }

    public void setButtonStyleTransparent() {
        buttonStyle = STYLE_TRANSPARENT;
        selectedPaint.setColor(0xFFffffff);
        bgPaint.setColor(0x33214119);
        for (int i = 0; i < emotionInfoList.size(); i++) {
            if (i < iconImages.length) {
                numberLayouts[i].setTextColor(0xFFffffff);
            }
        }
    }

    private void bind() {
        switch (buttonStyle) {
            case STYLE_BLUE:
                bgPaint.setColor(0x1A378dd1);
                selectedPaint.setColor(0xFF378DD1);
                break;
            case STYLE_GREEN:
                bgPaint.setColor(0x1A5ba756);
                selectedPaint.setColor(0xFF53ac50);
                break;
            default:
                bgPaint.setColor(0x33214119);
                selectedPaint.setColor(0xFFffffff);
                break;
        }
        emotionInfoList.clear();
        emotionInfoList.addAll(EmotionUtils.extractEmotionInfoList(messageObject, MediaDataController.getInstance(currentAccount), true));

        Arrays.fill(avatarImagesVisible, false);

        int lastSetImagePos = 0;

        for (int i = 0; i < emotionInfoList.size(); i++) {
            EmotionInfo emotionInfo = emotionInfoList.get(i);
            if (i < iconImages.length) {
                //todo emotionInfo.staticIcon null почему??
                if (emotionInfo.staticIcon != null) {
                    TLRPC.PhotoSize thumb = FileLoader.getClosestPhotoSizeWithSize(emotionInfo.staticIcon.thumbs, 90);
                    SvgHelper.SvgDrawable svgThumb = DocumentObject.getSvgThumb(emotionInfo.staticIcon, Theme.key_windowBackgroundGray, 1.0f);

                    if (svgThumb != null) {
                        iconImages[i].setImage(ImageLocation.getForDocument(emotionInfo.staticIcon), "80_80", null, null, svgThumb, 0, null, messageObject, 0);
                    } else if (thumb != null) {
                        iconImages[i].setImage(ImageLocation.getForDocument(emotionInfo.staticIcon), "80_80", ImageLocation.getForDocument(thumb, emotionInfo.staticIcon), null, null, 0, null, messageObject, 0);
                    } else {
                        iconImages[i].setImage(ImageLocation.getForDocument(emotionInfo.staticIcon), "80_80", null, null, null, 0, null, messageObject, 0);
                    }
                }

                numberLayouts[i].setNumber(emotionInfo.count, false);
                switch (buttonStyle) {
                    case STYLE_BLUE:
                        numberLayouts[i].setTextColor(0xFF378DD1);
                        break;
                    case STYLE_GREEN:
                        numberLayouts[i].setTextColor(0xFF53ac50);
                        break;
                    default:
                        numberLayouts[i].setTextColor(0xFFffffff);
                        break;
                }
            }

            if (!emotionInfo.lastThreeUsers.isEmpty()) {
                for (long userId : emotionInfo.lastThreeUsers) {
                    TLRPC.User user = MessagesController.getInstance(currentAccount).getUser(userId);
                    if (user != null) {
                        avatarDrawables[lastSetImagePos].setInfo(user);
                        avatarImages[lastSetImagePos].setForUserOrChat(user, avatarDrawables[lastSetImagePos]);
                    } else {
                        avatarDrawables[lastSetImagePos].setInfo(userId, "", "");
                    }
                    avatarImagesVisible[lastSetImagePos] = true;
                    lastSetImagePos++;
                }
            }
        }

        for (int a = 0; a < avatarImagesVisible.length; a++) {
            if (!avatarImagesVisible[a]) {
                avatarImages[a].setImageBitmap((Drawable) null);
            }
        }

        //todo подгрузить тех кого нет
        parent.invalidate();
    }

    public int getMinimumSpaceWidth() {
        if (reactions != null && reactions.results.size() > 0) {
            if (messageObject != null && !DialogObject.isChatDialog(messageObject.getDialogId())) {
                return totalHeight = AndroidUtilities.dp(116);
            }
            return oneItemMaxWidth + oneItemMarginHorizontal;
        }
        return 0;
    }

    public int getSpaceHeight(int width) {
        //максимум две строчки
        if (reactions != null) {
            if (messageObject != null && !DialogObject.isChatDialog(messageObject.getDialogId())) {
                return totalHeight = 0;
            }
            int count = emotionInfoList.size();
            if (count > 0) {
                int maxCountInRow = width / (oneItemMaxWidth + oneItemMarginHorizontal);
                if (count > maxCountInRow) {
                    totalHeight = (oneRowHeight + oneRowMarginVertical + oneRowMarginVertical) * 2;
                } else {
                    totalHeight = oneRowHeight + oneRowMarginVertical + oneRowMarginVertical;
                }
                return totalHeight;
            }
        }

        return totalHeight = 0;
    }

    private final Path pathCircle = new Path();
    private final Path pathHalfCircle = new Path();

    public void onDraw(Canvas canvas, int startX, int startY, int availableWidth) {
        if (reactions == null) return;
        if (totalHeight == 0) {
            for (int i = 0; i < emotionInfoList.size(); i++) {
                if (i >= 2) break;
                if (emotionInfoList.size() == 1) {
                    //если одна реакция, то она должна быть ближе к часам
                    startX += AndroidUtilities.dp(16);
                }
                iconImages[i].setImageCoords(0, 0, iconSizeMini, iconSizeMini);
                iconImages[i].setImageX(startX + (i * AndroidUtilities.dp(16)));
                iconImages[i].setImageY(startY);
                iconImages[i].draw(canvas);
            }
            return;
        }

        int offsetX = startX;
        int offsetY = startY;

        int drawedRows = 1;

        int lastUserAvatarPos = 0;
        int posInRow = 0;
        for (int i = 0; i < emotionInfoList.size(); i++) {
            EmotionInfo emotionInfo = emotionInfoList.get(i);
            if (i < iconImages.length) {
                int itemWidth = measureWidth(emotionInfo, i);
                offsetX += oneItemMarginHorizontal;

                rectF.set(offsetX, offsetY + (oneRowMarginVertical / 2), offsetX + itemWidth, offsetY + oneRowHeight + (oneRowMarginVertical / 2));
                emotionInfo.drawRegion.set(rectF);
                canvas.drawRoundRect(rectF, AndroidUtilities.dp(18), AndroidUtilities.dp(18), bgPaint);

                if (emotionInfo.isSelectedByCurrentUser) {
                    canvas.drawRoundRect(rectF, AndroidUtilities.dp(18), AndroidUtilities.dp(18), selectedPaint);
                }

                offsetX += AndroidUtilities.dp(8);

                iconImages[i].setImageX(offsetX);
                iconImages[i].setImageY(offsetY + ((oneRowHeight + oneRowMarginVertical) - iconSize) / 2);
                iconImages[i].draw(canvas);

                offsetX += iconImages[i].getImageWidth() + AndroidUtilities.dp(4);

                //цифру НЕ рисуем только в случае наличия всех юзеров
                if (emotionInfo.count != emotionInfo.lastThreeUsers.size()) {
                    canvas.save();
                    canvas.translate(offsetX, offsetY + AndroidUtilities.dp(9));
                    numberLayouts[i].draw(canvas);
                    offsetX += numberLayouts[i].getWidth() + AndroidUtilities.dp(4);
                    canvas.restore();
                }

                if (emotionInfo.lastThreeUsers.size() > 0 && emotionInfo.count == emotionInfo.lastThreeUsers.size()) {
                    int avatarOffsetTotal = 0;
                    for (int a = 0; a < emotionInfo.lastThreeUsers.size(); a++) {
                        if (lastUserAvatarPos <= 2) {
                            int offsetAvatarX = offsetX;
                            if (a == 1) {
                                int offsetOneAvatar = ((avatarSize / 2) + halfAvatarPadding) + AndroidUtilities.dp(2);
                                offsetAvatarX += offsetOneAvatar;
                                avatarOffsetTotal += offsetOneAvatar;
                            } else if (a == 2) {
                                int offsetTwoAvatar = ((avatarSize / 2) + halfAvatarPadding) + ((avatarSize / 2) + halfAvatarPadding) + AndroidUtilities.dp(4);
                                offsetAvatarX += offsetTwoAvatar;
                                avatarOffsetTotal += offsetTwoAvatar;
                            } else {
                                avatarOffsetTotal += avatarSize;
                            }
                            avatarImages[lastUserAvatarPos].setImageX(offsetAvatarX);
                            avatarImages[lastUserAvatarPos].setImageY(offsetY + ((oneRowHeight + oneRowMarginVertical) - avatarSize) / 2);

                            //яблочная обрезка
                            if (a > 0 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                                pathCircle.reset();
                                pathHalfCircle.reset();
                                pathCircle.addCircle(avatarImages[lastUserAvatarPos].getCenterX() - (avatarSize / 2) - halfAvatarPadding, avatarImages[lastUserAvatarPos].getCenterY(), avatarSize / 2, Path.Direction.CW);
                                pathHalfCircle.addCircle(avatarImages[lastUserAvatarPos].getCenterX(), avatarImages[lastUserAvatarPos].getCenterY(), avatarSize / 2, Path.Direction.CW);
                                pathHalfCircle.op(pathCircle, Path.Op.DIFFERENCE);
                                canvas.save();
                                canvas.clipPath(pathHalfCircle);
                                avatarImages[lastUserAvatarPos].draw(canvas);
                                canvas.restore();
                            } else {
                                avatarImages[lastUserAvatarPos].draw(canvas);
                            }

                            lastUserAvatarPos++;
                        }
                    }
                    offsetX += avatarOffsetTotal;
                }

                offsetX += oneItemMarginHorizontal + AndroidUtilities.dp(2);

                if ((posInRow + 1) * (oneItemMaxWidth + oneItemMarginHorizontal) > availableWidth || needNewRow(i + 1, drawedRows)) {
                    if (offsetY != startY) {
                        break;
                    } else {
                        posInRow = 0;
                        drawedRows++;
                        offsetY += oneRowHeight + oneRowMarginVertical + oneRowMarginVertical;
                        offsetX = startX;
                    }
                } else {
                    posInRow++;
                }
            }
        }
    }

    private boolean needNewRow(int pos, int drawedRows) {
        //следущий элемент последний, но он будет отрисован на первой строке, а место выделено больше
        if (emotionInfoList.size() - 1 == pos && drawedRows == 1 && totalHeight > oneRowHeight + oneRowMarginVertical + oneRowMarginVertical) {
            return true;
        }
        return false;
    }

    private int measureWidth(EmotionInfo emotionInfo, int pos) {
        int size = 0;
        size += AndroidUtilities.dp(8);//отступ слева
        size += iconSize;//иконка
        size += AndroidUtilities.dp(4);//отступ после иконки

        if (emotionInfo.count != emotionInfo.lastThreeUsers.size()) {
            size += numberLayouts[pos].getWidth();
            size += AndroidUtilities.dp(4);//отступ справа
        }
        size += measureAvatarsWidth(emotionInfo);
        size += oneItemMarginHorizontal;
        return size;
    }

    private int measureAvatarsWidth(EmotionInfo emotionInfo) {
        int size = 0;
        if (emotionInfo.lastThreeUsers.size() > 0 && emotionInfo.count == emotionInfo.lastThreeUsers.size()) {
            if (emotionInfo.lastThreeUsers.size() == 1) {
                size += avatarSize;//юзеры
            }
            if (emotionInfo.lastThreeUsers.size() == 2) {
                size += avatarSize + ((avatarSize / 2) + halfAvatarPadding);//юзеры треть второй иконки откусана
            }
            if (emotionInfo.lastThreeUsers.size() == 3) {
                size += avatarSize + ((avatarSize / 2) + halfAvatarPadding) + ((avatarSize / 2) + halfAvatarPadding);//юзеры треть второй и третей иконки откусана
            }

            size += emotionInfo.count * AndroidUtilities.dp(2);//отступы для юзеров
            size -= AndroidUtilities.dp(1);//отступ справа
        }
        return size;
    }

    public void onAttachedToWindow() {
        for (ImageReceiver avatarImage : avatarImages) {
            avatarImage.onAttachedToWindow();
        }
        for (ImageReceiver iconImage : iconImages) {
            iconImage.onAttachedToWindow();
        }
    }

    public void onDetachedFromWindow() {
        for (ImageReceiver avatarImage : avatarImages) {
            avatarImage.onDetachedFromWindow();
        }
        for (ImageReceiver iconImage : iconImages) {
            iconImage.onDetachedFromWindow();
        }
        handler.removeCallbacksAndMessages(null);
    }

    private float pressedX;
    private float pressedY;
    private Runnable longPressRunnable;

    private void cleanLongPressRunnable() {
        if (longPressRunnable != null) {
            handler.removeCallbacks(longPressRunnable);
            longPressRunnable = null;
        }
    }

    public boolean checkEmotionsButtonMotionEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            cleanLongPressRunnable();
            pressedX = event.getX();
            pressedY = event.getY();
            for (int i = 0; i < emotionInfoList.size(); i++) {
                EmotionInfo emotionInfo = emotionInfoList.get(i);
                if (emotionInfo.drawRegion.contains(pressedX, pressedY)) {
                    pressed = true;
                    longPressRunnable = () -> {
                        if (pressed) {
                            pressed = false;
                            for (int i2 = 0; i2 < emotionInfoList.size(); i2++) {
                                EmotionInfo emotionInfo2 = emotionInfoList.get(i2);
                                if (emotionInfo2.drawRegion.contains(pressedX, pressedY)) {
                                    if (onItemClick != null) {
                                        onItemClick.onItemLongClick(emotionInfo2);
                                    }
                                }
                            }
                        }
                    };
                    handler.postDelayed(longPressRunnable, ViewConfiguration.getLongPressTimeout() - ViewConfiguration.getTapTimeout());
                }
            }
        } else if (event.getAction() == MotionEvent.ACTION_UP) {
            if (pressed) {
                for (int i = 0; i < emotionInfoList.size(); i++) {
                    EmotionInfo emotionInfo = emotionInfoList.get(i);
                    if (emotionInfo.drawRegion.contains(pressedX, pressedY)) {
                        try {
                            if (onItemClick != null && iconImages[i].hasBitmapImage()) {
                                onItemClick.onItemClick(emotionInfo);
                            }
                        } catch (Exception e) {
                            //пожарный
                        }
                    }
                }
            }
            pressed = false;
        } else if (event.getAction() == MotionEvent.ACTION_CANCEL) {
            cleanLongPressRunnable();
            pressed = false;
        }
        return pressed;
    }

    public void onLayout(int width, int height) {

    }
}

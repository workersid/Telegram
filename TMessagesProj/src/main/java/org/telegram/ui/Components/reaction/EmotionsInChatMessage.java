package org.telegram.ui.Components.reaction;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.view.View;

import org.telegram.messenger.AndroidUtilities;
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
import org.telegram.ui.Components.AnimatedNumberLayout;
import org.telegram.ui.Components.AvatarDrawable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class EmotionsInChatMessage {

    private final int currentAccount = UserConfig.selectedAccount;

    private final ImageReceiver[] avatarImages = new ImageReceiver[3];
    private final AvatarDrawable[] avatarDrawables = new AvatarDrawable[3];
    private final boolean[] avatarImagesVisible = new boolean[3];

    private final AnimatedNumberLayout[] numberLayouts = new AnimatedNumberLayout[16];
    private final ImageReceiver[] iconImages = new ImageReceiver[16];

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final List<EmotionInfo> emotionInfoList = new ArrayList<>();

    private boolean isInitialized;
    private MessageObject messageObject;
    private TLRPC.TL_messageReactions reactions;
    private int totalHeight = 0;
    private View parent;
    private final int oneRowHeight = AndroidUtilities.dp(30);
    private final int oneRowMarginVertical = AndroidUtilities.dp(4);
    private final int oneItemMarginRight = AndroidUtilities.dp(4);
    private final int oneItemMaxWidth = AndroidUtilities.dp(92);
    private final RectF rectF = new RectF();

    public void createForView(View parentView) {
        if (!isInitialized) {
            for (int a = 0; a < avatarImages.length; a++) {
                avatarImages[a] = new ImageReceiver(parentView);
                avatarImages[a].setRoundRadius(AndroidUtilities.dp(12));
                avatarDrawables[a] = new AvatarDrawable();
                avatarDrawables[a].setTextSize(AndroidUtilities.dp(8));
                avatarImages[a].setImageCoords(0, 0, AndroidUtilities.dp(24), AndroidUtilities.dp(24));
                avatarImages[a].setInvalidateAll(true);
            }

            for (int a = 0; a < numberLayouts.length; a++) {
                numberLayouts[a] = new AnimatedNumberLayout(parentView, Theme.chat_replyNamePaint);
            }

            for (int a = 0; a < iconImages.length; a++) {
                iconImages[a] = new ImageReceiver(parentView);
                //iconImages[a].setAspectFit(true);
                //iconImages[a].setLayerNum(1);
                //iconImages[a].setAutoRepeat(3);
                iconImages[a].setImageCoords(0, 0, AndroidUtilities.dp(23), AndroidUtilities.dp(23));
                iconImages[a].setInvalidateAll(true);
            }

            paint.setColor(Color.GRAY);
            parent = parentView;
            isInitialized = true;
        }
    }

    public void setReactions(MessageObject messageObject, MessageObject.GroupedMessages groupedMessages) {
        if (messageObject != null) {
            if (groupedMessages != null && groupedMessages.messages.size() > 0) {
                MessageObject object = groupedMessages.messages.get(0);
                if (object.hasReactions()) {
                    this.messageObject = object;
                    this.reactions = object.messageOwner.reactions;
                    bind();
                    return;
                }
            } else {
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

    private void bind() {
        emotionInfoList.clear();
        emotionInfoList.addAll(EmotionUtils.extractEmotionInfoList(messageObject, MediaDataController.getInstance(currentAccount), true));

        Arrays.fill(avatarImagesVisible, false);

        int lastSetImagePos = 0;

        for (int i = 0; i < emotionInfoList.size(); i++) {
            EmotionInfo emotionInfo = emotionInfoList.get(i);
            if (i < iconImages.length) {
                TLRPC.PhotoSize thumb = FileLoader.getClosestPhotoSizeWithSize(emotionInfo.staticIcon.thumbs, 90);
                SvgHelper.SvgDrawable svgThumb = DocumentObject.getSvgThumb(emotionInfo.staticIcon, Theme.key_windowBackgroundGray, 1.0f);

                if (svgThumb != null) {
                    iconImages[i].setImage(ImageLocation.getForDocument(emotionInfo.staticIcon), "80_80", null, null, svgThumb, 0, null, messageObject, 0);
                } else if (thumb != null) {
                    iconImages[i].setImage(ImageLocation.getForDocument(emotionInfo.staticIcon), "80_80", ImageLocation.getForDocument(thumb, emotionInfo.staticIcon), null, null, 0, null, messageObject, 0);
                } else {
                    iconImages[i].setImage(ImageLocation.getForDocument(emotionInfo.staticIcon), "80_80", null, null, null, 0, null, messageObject, 0);
                }

                numberLayouts[i].setNumber(emotionInfo.count, false);
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
        //todo дернуть кеширование анимаций

        parent.invalidate();
    }

    public int getMinimumSpaceWidth() {
        if (reactions != null) {
            return oneItemMaxWidth + oneItemMarginRight;
        }
        return 0;
    }

    public int getSpaceHeight(int width) {
        //максимум две строчки
        if (reactions != null) {
            int count = emotionInfoList.size();
            if (count > 0) {
                int maxCountInRow = width / (oneItemMaxWidth + oneItemMarginRight);
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

    public void onDraw(Canvas canvas, int startX, int startY, int availableWidth) {
        if (totalHeight == 0 || reactions == null) {
            return;
        }

        int offsetX = startX;
        int offsetY = startY;
        int countInRow = availableWidth / (oneItemMaxWidth + oneItemMarginRight);

        int lastUserAvatarPos = 0;
        for (int i = 0; i < emotionInfoList.size(); i++) {
            EmotionInfo emotionInfo = emotionInfoList.get(i);
            if (i < iconImages.length) {
                //рисуем иконку
                //рисуем цифру
                //рисуем аватары

                int itemWidth = measureWidth(emotionInfo, i);
                rectF.set(startX, startY + (oneRowMarginVertical / 2), startX + itemWidth, startY + oneRowHeight + (oneRowMarginVertical / 2));
                canvas.drawRoundRect(rectF, AndroidUtilities.dp(18), AndroidUtilities.dp(18), paint);

                offsetX += AndroidUtilities.dp(8);

                iconImages[i].setImageX(offsetX);
                iconImages[i].setImageY(offsetY + ((oneRowHeight + oneRowMarginVertical) - AndroidUtilities.dp(23)) / 2);
                iconImages[i].draw(canvas);

                offsetX += iconImages[i].getImageWidth() + AndroidUtilities.dp(4);

                //цифру НЕ рисуем только в случае наличия всех юзеров
                if (emotionInfo.count != emotionInfo.lastThreeUsers.size()) {
                    canvas.save();
                    canvas.translate(offsetX, offsetY);
                    numberLayouts[i].draw(canvas);
                    offsetX += numberLayouts[i].getWidth();
                    canvas.restore();
                }

                if (emotionInfo.lastThreeUsers.size() > 0 && emotionInfo.count == emotionInfo.lastThreeUsers.size()) {
                    for (int a = 0; a < emotionInfo.lastThreeUsers.size(); a++) {
                        if (lastUserAvatarPos < 2) {
                            avatarImages[lastUserAvatarPos].setImageX(offsetX);
                            avatarImages[lastUserAvatarPos].setImageY(offsetY + ((oneRowHeight + oneRowMarginVertical) - AndroidUtilities.dp(24)) / 2);
                            avatarImages[lastUserAvatarPos].draw(canvas);
                            lastUserAvatarPos++;
                        }
                    }
                }

                if ((i + 1) * (oneItemMaxWidth + oneItemMarginRight) > availableWidth) {
                    if (offsetY != startY) {
                        break;
                    } else {
                        offsetY += oneRowHeight + oneRowMarginVertical + oneRowMarginVertical;
                        offsetX = startX;
                    }
                }
            }
        }
    }

    private int measureWidth(EmotionInfo emotionInfo, int pos) {
        int size = 0;
        size += AndroidUtilities.dp(8);//отступ слева
        size += AndroidUtilities.dp(23);//иконка
        size += AndroidUtilities.dp(4);//отступ после иконки

        if (emotionInfo.count != emotionInfo.lastThreeUsers.size()) {
            size += numberLayouts[pos].getWidth();
            size += AndroidUtilities.dp(2);//отступ справа
        }

        if (emotionInfo.lastThreeUsers.size() > 0 && emotionInfo.count == emotionInfo.lastThreeUsers.size()) {
            size += emotionInfo.count * AndroidUtilities.dp(24);//юзеры
            size += emotionInfo.count * AndroidUtilities.dp(1);//отступы для юзеров
            size += AndroidUtilities.dp(1);//отступ справа
        }
        size += oneItemMarginRight;

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
    }

    public boolean onTouchDown() {
        return true;
    }

    public void onLayout(int width, int height) {

    }
}

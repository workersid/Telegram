package org.telegram.ui.Components.reaction;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ContactsController;
import org.telegram.messenger.FileLoader;
import org.telegram.messenger.ImageLocation;
import org.telegram.messenger.MediaDataController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.AvatarsImageView;
import org.telegram.ui.Components.BackupImageView;
import org.telegram.ui.Components.FlickerLoadingView;
import org.telegram.ui.Components.HideViewAfterAnimation;
import org.telegram.ui.Components.LayoutHelper;

import java.util.ArrayList;

@SuppressLint("ViewConstructor")
public class MenuReactionCounterView extends FrameLayout {

    private final ArrayList<TLRPC.User> users = new ArrayList<>();
    private final AvatarsImageView avatarsImageView;
    private final TextView titleView;
    private final ImageView iconView;
    private final BackupImageView imageView;
    private final int currentAccount = UserConfig.selectedAccount;
    private final boolean isOut;
    private final long chatId;
    private final long dialogId;
    private final int messageId;
    private int totalReactions;
    private int totalSeen;
    private MessageObject selectedObject;
    FlickerLoadingView flickerLoadingView;

    public MenuReactionCounterView(@NonNull Context context, MessageObject selectedObject) {
        super(context);
        chatId = selectedObject.getChatId();
        dialogId = selectedObject.getDialogId();
        messageId = selectedObject.getId();
        isOut = selectedObject.isOutOwner();
        this.selectedObject = selectedObject;

        if (selectedObject.messageOwner.reactions != null && !selectedObject.messageOwner.reactions.results.isEmpty()) {
            int counter = 0;
            for (TLRPC.TL_reactionCount result : selectedObject.messageOwner.reactions.results) {
                counter += result.count;
            }
            totalReactions = counter;
        }

        flickerLoadingView = new FlickerLoadingView(context);
        flickerLoadingView.setColors(Theme.key_actionBarDefaultSubmenuBackground, Theme.key_listSelector, null);
        flickerLoadingView.setViewType(FlickerLoadingView.MESSAGE_SEEN_TYPE);
        flickerLoadingView.setIsSingleCell(false);
        addView(flickerLoadingView, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.MATCH_PARENT));

        titleView = new TextView(context);
        titleView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        titleView.setLines(1);
        titleView.setEllipsize(TextUtils.TruncateAt.END);

        addView(titleView, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.LEFT | Gravity.CENTER_VERTICAL, 40, 0, 62, 0));

        avatarsImageView = new AvatarsImageView(context, false);
        avatarsImageView.setStyle(AvatarsImageView.STYLE_MESSAGE_SEEN);
        addView(avatarsImageView, LayoutHelper.createFrame(24 + 12 + 12 + 8, LayoutHelper.MATCH_PARENT, Gravity.RIGHT | Gravity.CENTER_VERTICAL, 0, 0, 0, 0));

        titleView.setTextColor(Theme.getColor(Theme.key_actionBarDefaultSubmenuItem));

        iconView = new ImageView(context);
        addView(iconView, LayoutHelper.createFrame(24, 24, Gravity.LEFT | Gravity.CENTER_VERTICAL, 11, 0, 0, 0));
        Drawable drawable = ContextCompat.getDrawable(context, R.drawable.msg_reactions).mutate();
        drawable.setColorFilter(new PorterDuffColorFilter(Theme.getColor(Theme.key_actionBarDefaultSubmenuItemIcon), PorterDuff.Mode.MULTIPLY));
        iconView.setImageDrawable(drawable);

        imageView = new BackupImageView(context);
        imageView.setAspectFit(true);
        imageView.setLayerNum(1);
        addView(imageView, LayoutHelper.createFrame(24, 24, Gravity.LEFT | Gravity.CENTER_VERTICAL, 11, 0, 0, 0));
        imageView.setVisibility(INVISIBLE);

        avatarsImageView.setAlpha(0);
        titleView.setAlpha(0);
        setEnabled(false);
        setBackground(Theme.createRadSelectorDrawable(Theme.getColor(Theme.key_dialogButtonSelector), AndroidUtilities.dp(2), AndroidUtilities.dp(0)));
        loadData(selectedObject);
    }

    boolean ignoreLayout;

    @Override
    public void requestLayout() {
        if (ignoreLayout) {
            return;
        }
        super.requestLayout();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (flickerLoadingView.getVisibility() == View.VISIBLE) {
            ignoreLayout = true;
            flickerLoadingView.setVisibility(View.GONE);
            super.onMeasure(widthMeasureSpec, View.MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(44), View.MeasureSpec.EXACTLY));
            flickerLoadingView.getLayoutParams().width = getMeasuredWidth();
            flickerLoadingView.setVisibility(View.VISIBLE);
            ignoreLayout = false;
            super.onMeasure(widthMeasureSpec, View.MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(44), View.MeasureSpec.EXACTLY));
        } else {
            super.onMeasure(widthMeasureSpec, View.MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(44), View.MeasureSpec.EXACTLY));
        }
    }

    public int getTotalReactions() {
        return totalReactions;
    }

    public int getTotalSeen() {
        return totalSeen;
    }

    public int getMessageId() {
        return messageId;
    }

    public long getChatId() {
        return chatId;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        //todo подписаться на обновление fullChat и обновление реакций для сообщения
        //todo заглушку добавить для юзеров которых сейчас нет локально
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
    }

    private void loadData(MessageObject selectedObject) {
        final TLRPC.Chat chat = MessagesController.getInstance(currentAccount).getChat(selectedObject.getChatId());

        TLRPC.TL_messageReactions tlMessageReactions = selectedObject.messageOwner.reactions;

        final ArrayList<Long> unknownUsers = new ArrayList<>();
        final ArrayList<TLRPC.User> usersLocal = new ArrayList<>();

        for (TLRPC.TL_messageUserReaction tlMessageReaction : tlMessageReactions.recent_reactons) {
            long userId = tlMessageReaction.user_id;
            TLRPC.User user = MessagesController.getInstance(currentAccount).getUser(userId);
            if (user == null) {
                unknownUsers.add(userId);
            } else {
                usersLocal.add(user);
            }
        }

        if (unknownUsers.isEmpty()) {
            users.addAll(usersLocal);
            if (isOut) {
                loadSeenCount();
            } else {
                updateView();
            }
        } else {
            TLRPC.TL_messages_getMessageReactionsList req = new TLRPC.TL_messages_getMessageReactionsList();
            req.limit = 3;
            req.id = selectedObject.getId();
            req.peer = MessagesController.getInstance(currentAccount).getInputPeer(chat.id);

            ConnectionsManager.getInstance(currentAccount).sendRequest(req, (response1, error1) -> AndroidUtilities.runOnUIThread(() -> {
                if (response1 != null) {
                    final ArrayList<TLRPC.User> tmpUsers = new ArrayList<>();
                    TLRPC.TL_messages_messageReactionsList users = (TLRPC.TL_messages_messageReactionsList) response1;
                    for (int i = 0; i < users.users.size(); i++) {
                        TLRPC.User user = users.users.get(i);
                        MessagesController.getInstance(currentAccount).putUser(user, false);
                        tmpUsers.add(user);
                    }
                    this.users.clear();
                    this.users.addAll(tmpUsers);
                }
                if (isOut) {
                    loadSeenCount();
                } else {
                    updateView();
                }
            }));
        }
    }

    private void loadSeenCount() {
        //грузим актуальное кол-во просмотров
        TLRPC.TL_messages_getMessageReadParticipants req = new TLRPC.TL_messages_getMessageReadParticipants();
        req.msg_id = messageId;
        req.peer = MessagesController.getInstance(currentAccount).getInputPeer(dialogId);
        ConnectionsManager.getInstance(currentAccount).sendRequest(req, (response, error) -> AndroidUtilities.runOnUIThread(() -> {
            if (error == null) {
                TLRPC.Vector vector = (TLRPC.Vector) response;
                totalSeen = vector.objects.size();
            }
            updateView();
        }));
    }

    private void updateView() {
        setEnabled(totalReactions > 0 || totalSeen > 0);
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

        avatarsImageView.commitTransition(false);

        boolean isImageSet = false;

        if (users.size() == 1 && /*!isOut*/totalSeen <= 1) {
            TLRPC.TL_messageUserReaction tlMessageUserReaction = selectedObject.messageOwner.reactions.recent_reactons.get(0);
            if (tlMessageUserReaction != null) {
                TLRPC.TL_availableReaction tlAvailableReaction = MediaDataController.getInstance(currentAccount).getAvailableReactionByName(tlMessageUserReaction.reaction);
                if (tlAvailableReaction != null) {
                    titleView.setText(ContactsController.formatName(users.get(0).first_name, users.get(0).last_name));
                    TLRPC.PhotoSize thumb = FileLoader.getClosestPhotoSizeWithSize(tlAvailableReaction.static_icon.thumbs, 90);
                    ImageLocation imageLocation = ImageLocation.getForDocument(thumb, tlAvailableReaction.static_icon);
                    imageView.setVisibility(VISIBLE);
                    iconView.setVisibility(INVISIBLE);
                    imageView.setImage(imageLocation, "50_50", "webp", null, selectedObject);
                    imageView.animate().alpha(1f).setDuration(220).start();
                    isImageSet = true;
                }
            }
        }

        if (!isImageSet) {
            imageView.setVisibility(INVISIBLE);
            iconView.setVisibility(VISIBLE);
            if (isOut && totalSeen > 0) {
                //от меня
                titleView.setText(totalReactions + "/" + totalSeen + " Reacted");
            } else {
                if (totalReactions == 1) {
                    titleView.setText(totalReactions + " reaction");
                } else {
                    titleView.setText(totalReactions + " reactions");
                }
            }
        }

        titleView.animate().alpha(1f).setDuration(220).start();
        avatarsImageView.animate().alpha(1f).setDuration(220).start();
        flickerLoadingView.animate().alpha(0f).setDuration(220).setListener(new HideViewAfterAnimation(flickerLoadingView)).start();
    }
}

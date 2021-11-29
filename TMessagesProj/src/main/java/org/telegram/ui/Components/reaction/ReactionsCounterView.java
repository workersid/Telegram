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

import com.google.android.exoplayer2.util.Log;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ChatObject;
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
import org.telegram.ui.Components.LayoutHelper;

import java.util.ArrayList;
import java.util.HashMap;

@SuppressLint("ViewConstructor")
public class ReactionsCounterView extends FrameLayout {

    private final ArrayList<Long> peerIds = new ArrayList<>();
    private final ArrayList<TLRPC.User> users = new ArrayList<>();
    private final AvatarsImageView avatarsImageView;
    private final TextView titleView;
    private final ImageView iconView;
    private final BackupImageView imageView;
    private final int currentAccount = UserConfig.selectedAccount;
    private final boolean isOut;
    private final long chatId;
    private int totalReactions;
    private MessageObject selectedObject;

    public ReactionsCounterView(@NonNull Context context, MessageObject selectedObject) {
        super(context);
        chatId = selectedObject.getChatId();
        isOut = selectedObject.isOutOwner();
        this.selectedObject = selectedObject;

        if (selectedObject.messageOwner.reactions != null && !selectedObject.messageOwner.reactions.results.isEmpty()) {
            int counter = 0;
            for (TLRPC.TL_reactionCount result : selectedObject.messageOwner.reactions.results) {
                counter += result.count;
            }
            totalReactions = counter;
        }

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
        initUsers(selectedObject);
        setBackground(Theme.createRadSelectorDrawable(Theme.getColor(Theme.key_dialogButtonSelector), AndroidUtilities.dp(4), AndroidUtilities.dp(4)));
        setEnabled(true);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, View.MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(56), View.MeasureSpec.EXACTLY));
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        //подписаться на обновление fullChat
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
    }

    private void initUsers(MessageObject selectedObject) {
        TLRPC.Chat chat = MessagesController.getInstance(currentAccount).getChat(selectedObject.getChatId());

        TLRPC.TL_messageReactions tlMessageReactions = selectedObject.messageOwner.reactions;

        final ArrayList<Long> unknownUsers = new ArrayList<>();
        final HashMap<Long, TLRPC.User> usersLocal = new HashMap<>();
        final ArrayList<Long> allPeers = new ArrayList<>();

        for (TLRPC.TL_messageUserReaction tlMessageReaction : tlMessageReactions.recent_reactons) {
            long userId = tlMessageReaction.user_id;
            TLRPC.User user = MessagesController.getInstance(currentAccount).getUser(userId);
            allPeers.add(userId);
            if (user == null) {
                unknownUsers.add(userId);
            } else {
                usersLocal.put(userId, user);
            }
        }

        if (unknownUsers.isEmpty()) {
            for (int i = 0; i < allPeers.size(); i++) {
                peerIds.add(allPeers.get(i));
                users.add(usersLocal.get(allPeers.get(i)));
            }
            updateView();
        } else {
            if (ChatObject.isChannel(chat)) {
                TLRPC.TL_channels_getParticipants usersReq = new TLRPC.TL_channels_getParticipants();
                usersReq.limit = 50;
                usersReq.offset = 0;
                usersReq.filter = new TLRPC.TL_channelParticipantsRecent();
                usersReq.channel = MessagesController.getInstance(currentAccount).getInputChannel(chat.id);
                ConnectionsManager.getInstance(currentAccount).sendRequest(usersReq, (response1, error1) -> AndroidUtilities.runOnUIThread(() -> {
                    if (response1 != null) {
                        TLRPC.TL_channels_channelParticipants users = (TLRPC.TL_channels_channelParticipants) response1;
                        for (int i = 0; i < users.users.size(); i++) {
                            TLRPC.User user = users.users.get(i);
                            MessagesController.getInstance(currentAccount).putUser(user, false);
                            usersLocal.put(user.id, user);
                        }
                        for (int i = 0; i < allPeers.size(); i++) {
                            peerIds.add(allPeers.get(i));
                            this.users.add(usersLocal.get(allPeers.get(i)));
                        }
                    }
                    updateView();
                }));
            } else {
                TLRPC.TL_messages_getFullChat usersReq = new TLRPC.TL_messages_getFullChat();
                usersReq.chat_id = chat.id;
                ConnectionsManager.getInstance(currentAccount).sendRequest(usersReq, (response1, error1) -> AndroidUtilities.runOnUIThread(() -> {
                    if (response1 != null) {
                        TLRPC.TL_messages_chatFull chatFull = (TLRPC.TL_messages_chatFull) response1;
                        for (int i = 0; i < chatFull.users.size(); i++) {
                            TLRPC.User user = chatFull.users.get(i);
                            MessagesController.getInstance(currentAccount).putUser(user, false);
                            usersLocal.put(user.id, user);
                        }
                        for (int i = 0; i < allPeers.size(); i++) {
                            peerIds.add(allPeers.get(i));
                            this.users.add(usersLocal.get(allPeers.get(i)));
                        }
                    }
                    updateView();
                }));
            }
        }

    }

    private void updateView() {
        Log.e("users","users=" + users.size());
        setEnabled(users.size() > 0);
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
        if (peerIds.size() == 1 && users.get(0) != null && selectedObject != null && selectedObject.messageOwner.reactions != null && !selectedObject.messageOwner.reactions.recent_reactons.isEmpty()) {
            TLRPC.TL_messageUserReaction tlMessageUserReaction = selectedObject.messageOwner.reactions.recent_reactons.get(0);
            boolean isImageSet = false;
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
            if (!isImageSet) {
                titleView.setText(totalReactions + " reactions");
            }
        } else {
            TLRPC.ChatFull full = MessagesController.getInstance(currentAccount).getChatFull(chatId);
            if (isOut && full != null) {
                //от меня
                titleView.setText(totalReactions + "/" + full.participants_count + " Reacted");
            } else {
                titleView.setText(totalReactions + " reactions");
            }
        }
        titleView.animate().alpha(1f).setDuration(220).start();
        avatarsImageView.animate().alpha(1f).setDuration(220).start();
    }
}

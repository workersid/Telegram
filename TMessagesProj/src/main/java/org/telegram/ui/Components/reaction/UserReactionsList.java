package org.telegram.ui.Components.reaction;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Rect;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ChatObject;
import org.telegram.messenger.FileLoader;
import org.telegram.messenger.ImageLocation;
import org.telegram.messenger.MediaDataController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.UserObject;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.AvatarDrawable;
import org.telegram.ui.Components.BackupImageView;
import org.telegram.ui.Components.FlickerLoadingView;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

@SuppressLint("ViewConstructor")
public class UserReactionsList extends FrameLayout {
    private static final String END_FLAG = "end";

    interface Delegate {
        void onItemClick(TLRPC.User user);
    }

    private final ArrayList<UserInfoHolder> allUsers = new ArrayList<>();
    private final HashMap<Long, TLRPC.User> allUsersMap = new HashMap<>();

    private final int currentAccount = UserConfig.selectedAccount;

    private final RecyclerListView.SelectionAdapter usersListAdapter;
    private final LinearLayoutManager usersLayoutManager;
    private final RecyclerListView usersListView;

    private String loadNextReactionsId;
    private String loadNextSeenId;
    private boolean isLoading;

    private final boolean isOut;
    private final long chatId;
    private final long dialogId;
    private final int messageId;
    private int totalSeen;//тут именно кол-во всех просмотренных, оно нужжно только на первой вкладке
    private int totalReactions;//тут кол-во реакций в зависимости от переданного reaction
    private final String currentReaction;

    public UserReactionsList(@NonNull Context context, MessageObject selectedObject, int seen, String reaction, final Delegate delegate) {
        super(context);
        chatId = selectedObject.getChatId();
        dialogId = selectedObject.getDialogId();
        messageId = selectedObject.getId();
        isOut = selectedObject.isOutOwner();
        currentReaction = reaction;
        totalSeen = currentReaction == null ? seen : 0;
        totalReactions = EmotionUtils.extractTotalReactions(selectedObject, currentReaction);
        loadNextSeenId = currentReaction != null ? END_FLAG : null;

        usersListView = new RecyclerListView(getContext());
        usersListView.setLayoutManager(usersLayoutManager = new LinearLayoutManager(getContext()));
        usersListView.addItemDecoration(new RecyclerView.ItemDecoration() {
            @Override
            public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
                int p = parent.getChildAdapterPosition(view);
                if (p == 0) {
                    outRect.top = AndroidUtilities.dp(4);
                }
                if (p == allUsers.size() - 1) {
                    outRect.bottom = AndroidUtilities.dp(4);
                }
            }
        });
        usersListView.setAdapter(usersListAdapter = new RecyclerListView.SelectionAdapter() {

            private static final int TYPE_USER = 0;
            private static final int TYPE_HOLDER_REACTION = 1;
            private static final int TYPE_HOLDER_SEEN = 2;

            @Override
            public boolean isEnabled(RecyclerView.ViewHolder holder) {
                if (holder.itemView instanceof UserCell) {
                    UserCell cell = (UserCell) holder.itemView;
                    return cell.isEnabled();
                }
                return false;
            }

            @NonNull
            @Override
            public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                switch (viewType) {
                    case TYPE_USER:
                        UserCell userCell = new UserCell(parent.getContext());
                        userCell.setLayoutParams(new RecyclerView.LayoutParams(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
                        userCell.setMinimumWidth(AndroidUtilities.dp(260));
                        return new RecyclerListView.Holder(userCell);
                    case TYPE_HOLDER_REACTION:
                    case TYPE_HOLDER_SEEN:
                        LoadingHolder cell = new LoadingHolder(parent.getContext());
                        cell.setLayoutParams(new RecyclerView.LayoutParams(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
                        cell.setMinimumWidth(AndroidUtilities.dp(260));
                        return new RecyclerListView.Holder(cell);
                }
                return new RecyclerListView.Holder(new View(parent.getContext()));
            }

            @Override
            public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
                if (holder.itemView instanceof LoadingHolder) {
                    LoadingHolder cell = (LoadingHolder) holder.itemView;
                    if (position >= totalReactions) {
                        cell.setHolder(FlickerLoadingView.REACTION_USERS_SEEN_TYPE);
                    } else {
                        cell.setHolder(FlickerLoadingView.REACTION_USERS_TYPE);
                    }
                    loadNext();
                } else if (holder.itemView instanceof UserCell) {
                    UserCell cell = (UserCell) holder.itemView;
                    UserInfoHolder infoHolder = allUsers.get(position);
                    if (infoHolder.hasReaction && infoHolder.reaction != null) {
                        TLRPC.TL_availableReaction tlAvailableReaction = MediaDataController.getInstance(currentAccount).getAvailableReactionByName(infoHolder.reaction);
                        if (tlAvailableReaction != null) {
                            cell.setUser(infoHolder, tlAvailableReaction.static_icon);
                        } else {
                            cell.setUser(infoHolder, null);
                        }
                    } else {
                        cell.setUser(infoHolder, null);
                    }
                }
            }

            @Override
            public int getItemViewType(int position) {
                if (position < allUsers.size()) {
                    return TYPE_USER;
                }
                if (position >= totalReactions) {
                    return TYPE_HOLDER_SEEN;
                }
                return TYPE_HOLDER_REACTION;
            }

            @Override
            public int getItemCount() {
                return totalReactions + totalSeen;
                //return totalReactions + Math.max((totalSeen - totalReactions), 0);
            }
        });
        usersListView.setOnItemClickListener((view, position) -> {
            if (delegate != null) {
                delegate.onItemClick(allUsers.get(position).user);
            }
        });

        addView(usersListView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
    }

    public int getTotalReactions() {
        return totalReactions;
    }

    public RecyclerListView getUsersListView() {
        return usersListView;
    }

    public TLRPC.User getUserByPos(int pos) {
        if (allUsers.size() > pos) {
            return allUsers.get(pos).user;
        }
        return null;
    }

    private boolean hasEmptyElements() {
        if (usersListView != null) {
            for (int i = 0; i < usersListView.getChildCount(); i++) {
                View view = usersListView.getChildAt(i);
                if (view instanceof LoadingHolder) {
                    return true;
                }
            }
        }
        return false;
    }

    @SuppressLint("NotifyDataSetChanged")
    private void finishLoading(boolean withError) {
        if (usersListAdapter != null && !withError) usersListAdapter.notifyDataSetChanged();

        if (!withError && hasEmptyElements()) {
            isLoading = false;
            loadNext();
        } else {
            isLoading = false;
        }
    }

    private void loadNext() {
        //после загрузки, проверять нет ли пустых отображаемых элементов, если есть, подгрузка опять.
        if (!isLoading) {
            isLoading = true;
            if (!END_FLAG.equals(loadNextReactionsId)) {
                TLRPC.TL_messages_getMessageReactionsList req = new TLRPC.TL_messages_getMessageReactionsList();
                req.limit = currentReaction == null ? 100 : 50;
                req.id = messageId;
                req.reaction = currentReaction;
                req.peer = MessagesController.getInstance(currentAccount).getInputPeer(dialogId/*chatId*/);

                if (currentReaction != null) {
                    req.flags |= 1;
                }

                if (loadNextReactionsId != null) {
                    req.flags |= 2;
                    req.offset = loadNextReactionsId;
                }

                ConnectionsManager.getInstance(currentAccount).sendRequest(req, (response1, error1) -> AndroidUtilities.runOnUIThread(() -> {
                    if (response1 != null) {
                        final ArrayList<UserInfoHolder> tmpUsers = new ArrayList<>();
                        final HashMap<Long, TLRPC.User> tmpUsersMap = new HashMap<>();
                        TLRPC.TL_messages_messageReactionsList messageReactionsList = (TLRPC.TL_messages_messageReactionsList) response1;

                        HashMap<Long, String> userIdWithReactionMap = new HashMap<>();
                        for (int i = 0; i < messageReactionsList.reactions.size(); i++) {
                            TLRPC.TL_messageUserReaction tlMessageUserReaction = messageReactionsList.reactions.get(i);
                            userIdWithReactionMap.put(tlMessageUserReaction.user_id, tlMessageUserReaction.reaction);
                        }

                        for (int i = 0; i < messageReactionsList.users.size(); i++) {
                            TLRPC.User user = messageReactionsList.users.get(i);
                            MessagesController.getInstance(currentAccount).putUser(user, false);
                            String reaction = null;
                            if (userIdWithReactionMap.containsKey(user.id)) {
                                reaction = userIdWithReactionMap.get(user.id);
                            }
                            tmpUsers.add(new UserInfoHolder(user, reaction != null, reaction));
                            tmpUsersMap.put(user.id, user);
                        }

                        this.allUsersMap.putAll(tmpUsersMap);
                        this.allUsers.addAll(tmpUsers);

                        if (messageReactionsList.next_offset == null) {
                            //конец списка юзеров с реакциями
                            loadNextReactionsId = END_FLAG;
                            totalReactions = this.allUsers.size();
                        } else {
                            loadNextReactionsId = messageReactionsList.next_offset;
                        }
                        finishLoading(false);
                    } else {
                        loadNextReactionsId = END_FLAG;
                        totalReactions = this.allUsers.size();
                        finishLoading(false);
                    }
                }));
                return;
            }

            if (!END_FLAG.equals(loadNextSeenId) && isOut) {
                TLRPC.TL_messages_getMessageReadParticipants req = new TLRPC.TL_messages_getMessageReadParticipants();
                req.msg_id = messageId;
                req.peer = MessagesController.getInstance(currentAccount).getInputPeer(dialogId);

                ConnectionsManager.getInstance(currentAccount).sendRequest(req, (response, error) -> AndroidUtilities.runOnUIThread(() -> {
                    if (error == null) {
                        TLRPC.Vector vector = (TLRPC.Vector) response;
                        //id-шники тех кто просмотрел
                        final HashSet<Long> seenIds = new HashSet<>();
                        for (int i = 0, n = vector.objects.size(); i < n; i++) {
                            Object object = vector.objects.get(i);
                            if (object instanceof Long) seenIds.add((Long) object);
                        }

                        if (seenIds.isEmpty()) {
                            loadNextSeenId = END_FLAG;
                            totalSeen = this.allUsers.size() - totalReactions;
                            finishLoading(false);
                            return;
                        }

                        final TLRPC.Chat chat = MessagesController.getInstance(currentAccount).getChat(chatId);
                        if (ChatObject.isChannel(chat)) {
                            TLRPC.TL_channels_getParticipants usersReq = new TLRPC.TL_channels_getParticipants();
                            usersReq.limit = 50;
                            usersReq.offset = 0;
                            usersReq.filter = new TLRPC.TL_channelParticipantsRecent();
                            usersReq.channel = MessagesController.getInstance(currentAccount).getInputChannel(chat.id);
                            ConnectionsManager.getInstance(currentAccount).sendRequest(usersReq, (response1, error1) -> AndroidUtilities.runOnUIThread(() -> {
                                if (response1 != null) {
                                    TLRPC.TL_channels_channelParticipants channelsChannelParticipants = (TLRPC.TL_channels_channelParticipants) response1;
                                    final ArrayList<UserInfoHolder> tmpUsers = new ArrayList<>();

                                    for (int i = 0; i < channelsChannelParticipants.users.size(); i++) {
                                        TLRPC.User user = channelsChannelParticipants.users.get(i);
                                        MessagesController.getInstance(currentAccount).putUser(user, false);
                                        if (!allUsersMap.containsKey(user.id) && seenIds.contains(user.id)) {
                                            tmpUsers.add(new UserInfoHolder(user, false, null));
                                        }
                                    }

                                    this.allUsers.addAll(tmpUsers);
                                    loadNextSeenId = END_FLAG;
                                    totalSeen = this.allUsers.size() - totalReactions;
                                    finishLoading(false);
                                } else {
                                    loadNextSeenId = END_FLAG;
                                    totalSeen = this.allUsers.size() - totalReactions;
                                    finishLoading(false);
                                }
                            }));
                        } else {
                            TLRPC.TL_messages_getFullChat usersReq = new TLRPC.TL_messages_getFullChat();
                            usersReq.chat_id = chat.id;
                            ConnectionsManager.getInstance(currentAccount).sendRequest(usersReq, (response1, error1) -> AndroidUtilities.runOnUIThread(() -> {
                                if (response1 != null) {
                                    TLRPC.TL_messages_chatFull chatFull = (TLRPC.TL_messages_chatFull) response1;
                                    final ArrayList<UserInfoHolder> tmpUsers = new ArrayList<>();
                                    for (int i = 0; i < chatFull.users.size(); i++) {
                                        TLRPC.User user = chatFull.users.get(i);
                                        MessagesController.getInstance(currentAccount).putUser(user, false);
                                        if (!allUsersMap.containsKey(user.id) && seenIds.contains(user.id)) {
                                            tmpUsers.add(new UserInfoHolder(user, false, null));
                                        }
                                    }

                                    this.allUsers.addAll(tmpUsers);
                                    loadNextSeenId = END_FLAG;
                                    totalSeen = this.allUsers.size() - totalReactions;
                                    finishLoading(false);
                                } else {
                                    loadNextSeenId = END_FLAG;
                                    totalSeen = this.allUsers.size() - totalReactions;
                                    finishLoading(false);
                                }
                            }));
                        }
                    } else {
                        loadNextSeenId = END_FLAG;
                        totalSeen = this.allUsers.size() - totalReactions;
                        finishLoading(false);
                    }
                }));
                return;
            }

            finishLoading(true);
        }
    }

    private static class LoadingHolder extends FrameLayout {

        FlickerLoadingView flickerLoadingView;

        public LoadingHolder(Context context) {
            super(context);

            flickerLoadingView = new FlickerLoadingView(context);
            flickerLoadingView.setColors(Theme.key_actionBarDefaultSubmenuBackground, Theme.key_listSelector, null);
            flickerLoadingView.setIsSingleCell(true);
            addView(flickerLoadingView, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.MATCH_PARENT));
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(44), View.MeasureSpec.EXACTLY));
        }

        public void setHolder(int type) {
            flickerLoadingView.setViewType(type);
            flickerLoadingView.setVisibility(VISIBLE);
        }
    }

    private static class UserCell extends FrameLayout {

        private final BackupImageView avatarImageView;
        private final TextView nameView;
        private final BackupImageView reactionImageView;
        private final AvatarDrawable avatarDrawable = new AvatarDrawable();

        public UserCell(Context context) {
            super(context);

            avatarImageView = new BackupImageView(context);
            addView(avatarImageView, LayoutHelper.createFrame(34, 34, Gravity.CENTER_VERTICAL, 13, 0, 0, 0));
            avatarImageView.setRoundRadius(AndroidUtilities.dp(17));

            nameView = new TextView(context);
            nameView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
            nameView.setLines(1);
            nameView.setEllipsize(TextUtils.TruncateAt.END);
            addView(nameView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.LEFT | Gravity.CENTER_VERTICAL, 59, 0, 44, 0));
            nameView.setTextColor(Theme.getColor(Theme.key_actionBarDefaultSubmenuItem));

            reactionImageView = new BackupImageView(context);
            reactionImageView.setSize(AndroidUtilities.dp(26), AndroidUtilities.dp(26));
            addView(reactionImageView, LayoutHelper.createFrame(26, LayoutHelper.MATCH_PARENT, Gravity.RIGHT | Gravity.CENTER_VERTICAL, 0, 0, 13, 0));
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(44), View.MeasureSpec.EXACTLY));
        }

        public void setUser(UserInfoHolder holder, TLRPC.Document reactionDocument) {
            if (holder != null && holder.user != null) {
                TLRPC.User user = holder.user;
                avatarDrawable.setInfo(user);
                ImageLocation imageLocation = ImageLocation.getForUser(user, ImageLocation.TYPE_SMALL);
                avatarImageView.setImage(imageLocation, "50_50", avatarDrawable, user);
                nameView.setText(UserObject.getUserName(user));
                avatarImageView.setVisibility(VISIBLE);
                nameView.setVisibility(VISIBLE);
                if (reactionDocument != null) {
                    TLRPC.PhotoSize thumb = FileLoader.getClosestPhotoSizeWithSize(reactionDocument.thumbs, 90);
                    reactionImageView.setImage(ImageLocation.getForDocument(thumb, reactionDocument), "50_50", "webp", null, holder);
                    reactionImageView.setVisibility(VISIBLE);
                } else {
                    reactionImageView.setVisibility(INVISIBLE);
                }
                setEnabled(true);
            }
        }
    }

    private static class UserInfoHolder {
        public TLRPC.User user;
        public boolean hasReaction;
        public String reaction;

        public UserInfoHolder(TLRPC.User user, boolean hasReaction, String reaction) {
            this.user = user;
            this.hasReaction = hasReaction;
            this.reaction = reaction;
        }
    }
}

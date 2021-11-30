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
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ContactsController;
import org.telegram.messenger.FileLoader;
import org.telegram.messenger.ImageLocation;
import org.telegram.messenger.MediaDataController;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.UserConfig;
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

@SuppressLint("ViewConstructor")
public class ReactionsListView extends LinearLayout {

    private static final String END_FLAG = "end";

    private final RecyclerListView listView;
    private final ArrayList<UserInfoHolder> allUsers = new ArrayList<>();
    private final int currentAccount = UserConfig.selectedAccount;
    private int totalSeen;
    private int totalReactions;
    private LinearLayoutManager layoutManager;
    private boolean isLoading;
    private int messageId;
    private long chatId;
    private String loadNextReactionsId;
    private String loadNextSeenId;
    private RecyclerListView.SelectionAdapter listAdapter;

    public ReactionsListView(Context context, int totalSeen, int totalReactions, int messageId, long chatId) {
        super(context);
        setOrientation(VERTICAL);
        this.totalSeen = totalSeen;
        this.totalReactions = totalReactions;
        this.messageId = messageId;
        this.chatId = chatId;

        listView = new RecyclerListView(getContext());
        listView.setLayoutManager(layoutManager = new LinearLayoutManager(getContext()));
        listView.addItemDecoration(new RecyclerView.ItemDecoration() {
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
        listView.setAdapter(listAdapter = new RecyclerListView.SelectionAdapter() {

            private static final int TYPE_USER = 0;
            private static final int TYPE_HOLDER_REACTION = 1;
            private static final int TYPE_HOLDER_SEEN = 2;

            @Override
            public boolean isEnabled(RecyclerView.ViewHolder holder) {
                if (holder.itemView instanceof ReactionsListView.UserCell) {
                    ReactionsListView.UserCell cell = (ReactionsListView.UserCell) holder.itemView;
                    return cell.isEnabled();
                }
                return false;
            }

            @NonNull
            @Override
            public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                switch (viewType) {
                    case TYPE_USER:
                        ReactionsListView.UserCell userCell = new ReactionsListView.UserCell(parent.getContext());
                        userCell.setLayoutParams(new RecyclerView.LayoutParams(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
                        userCell.setMinimumWidth(AndroidUtilities.dp(260));
                        return new RecyclerListView.Holder(userCell);
                    case TYPE_HOLDER_REACTION:
                    case TYPE_HOLDER_SEEN:
                        ReactionsListView.LoadingHolder cell = new ReactionsListView.LoadingHolder(parent.getContext());
                        cell.setLayoutParams(new RecyclerView.LayoutParams(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
                        cell.setMinimumWidth(AndroidUtilities.dp(260));
                        return new RecyclerListView.Holder(cell);
                }
                return new RecyclerListView.Holder(new View(parent.getContext()));
            }

            @Override
            public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
                if (holder.itemView instanceof ReactionsListView.LoadingHolder) {
                    ReactionsListView.LoadingHolder cell = (ReactionsListView.LoadingHolder) holder.itemView;
                    if (position >= totalReactions) {
                        cell.setHolder(FlickerLoadingView.REACTION_USERS_SEEN_TYPE);
                    } else {
                        cell.setHolder(FlickerLoadingView.REACTION_USERS_TYPE);
                    }
                    loadNext();
                } else if (holder.itemView instanceof ReactionsListView.UserCell) {
                    ReactionsListView.UserCell cell = (ReactionsListView.UserCell) holder.itemView;
                    UserInfoHolder infoHolder = allUsers.get(position);
                    if (infoHolder.hasReaction && infoHolder.reaction != null) {
                        TLRPC.TL_availableReaction tlAvailableReaction = MediaDataController.getInstance(currentAccount).getAvailableReactionByName(infoHolder.reaction);
                        cell.setUser(infoHolder, tlAvailableReaction.static_icon);
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
                return totalReactions + Math.max((totalSeen - totalReactions), 0);
            }
        });

        addView(listView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        //todo подписка на ивенты
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
    }

    public RecyclerListView getListView() {
        return listView;
    }

    public TLRPC.User getUserByPos(int pos) {
        if (allUsers.size() > pos) {
            return allUsers.get(pos).user;
        }
        return null;
    }

    private boolean hasEmptyElements() {
        int firstVisible = layoutManager.findFirstVisibleItemPosition();
        int lastVisible = layoutManager.findLastVisibleItemPosition();
        for (int i = firstVisible; i <= lastVisible; i++) {
            if (allUsers.size() > i) {
                UserInfoHolder holder = allUsers.get(i);
                if (holder.user == null) {
                    return true;
                }
            }
        }
        return false;
    }

    @SuppressLint("NotifyDataSetChanged")
    private void loadNext() {
        //после загрузки, проверять нет ли пустых отображаемых элементов, если есть, подгрузка опять.
        if (!isLoading) {
            isLoading = true;
            if (!END_FLAG.equals(loadNextReactionsId)) {
                TLRPC.TL_messages_getMessageReactionsList req = new TLRPC.TL_messages_getMessageReactionsList();
                req.limit = 100;
                req.id = messageId;
                req.peer = MessagesController.getInstance(currentAccount).getInputPeer(chatId);

                if (loadNextReactionsId != null) {
                    req.flags |= 2;
                    req.offset = loadNextReactionsId;
                }

                ConnectionsManager.getInstance(currentAccount).sendRequest(req, (response1, error1) -> AndroidUtilities.runOnUIThread(() -> {
                    if (response1 != null) {
                        final ArrayList<UserInfoHolder> tmpUsers = new ArrayList<>();
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
                        }

                        this.allUsers.addAll(tmpUsers);

                        if (messageReactionsList.next_offset == null) {
                            //конец списка юзеров с реакциями
                            loadNextReactionsId = END_FLAG;
                            totalReactions = this.allUsers.size();
                        }

                        if (listAdapter != null) listAdapter.notifyDataSetChanged();

                        if (hasEmptyElements()) {
                            isLoading = false;
                            loadNext();
                        } else {
                            isLoading = false;
                        }
                    } else {
                        isLoading = false;
                    }
                }));
            }

            if (!END_FLAG.equals(loadNextSeenId)) {

            }
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
            addView(avatarImageView, LayoutHelper.createFrame(32, 32, Gravity.CENTER_VERTICAL, 13, 0, 0, 0));
            avatarImageView.setRoundRadius(AndroidUtilities.dp(16));

            nameView = new TextView(context);
            nameView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
            nameView.setLines(1);
            nameView.setEllipsize(TextUtils.TruncateAt.END);
            addView(nameView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.LEFT | Gravity.CENTER_VERTICAL, 59, 0, 62, 0));
            nameView.setTextColor(Theme.getColor(Theme.key_actionBarDefaultSubmenuItem));

            reactionImageView = new BackupImageView(context);
            reactionImageView.setSize(AndroidUtilities.dp(34), AndroidUtilities.dp(34));
            addView(reactionImageView, LayoutHelper.createFrame(24 + 12 + 12 + 8, LayoutHelper.MATCH_PARENT, Gravity.RIGHT | Gravity.CENTER_VERTICAL, 0, 0, 0, 0));
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
                nameView.setText(ContactsController.formatName(user.first_name, user.last_name));
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

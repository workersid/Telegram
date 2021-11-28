package org.telegram.ui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ChatObject;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.ActionBar.ThemeDescription;
import org.telegram.ui.Cells.HeaderCell;
import org.telegram.ui.Cells.ManageChatTextCell;
import org.telegram.ui.Cells.ManageChatUserCell;
import org.telegram.ui.Cells.ReactionCheckCell;
import org.telegram.ui.Cells.ShadowSectionCell;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Cells.TextInfoPrivacyCell;
import org.telegram.ui.Components.AlertsCreator;
import org.telegram.ui.Components.EmptyTextProgressView;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

@SuppressLint("NotifyDataSetChanged")
public class AdminReactionsActivity extends BaseFragment implements NotificationCenter.NotificationCenterDelegate {

    public static AdminReactionsActivity create(long id) {
        Bundle bundle = new Bundle();
        bundle.putLong("id", id);
        return new AdminReactionsActivity(bundle);
    }

    private static final int REACTION_CHECK_TYPE = 0;
    private static final int MAIN_CHECK_TYPE = 1;
    private static final int DIVIDER_TEXT_TYPE = 2;
    private static final int HEADER_TYPE = 3;

    private RecyclerListView listView;
    private ListAdapter listViewAdapter;
    private EmptyTextProgressView emptyView;
    private final List<TLRPC.TL_availableReaction> reactions = new ArrayList<>();
    private final HashSet<String> selectedReactions = new HashSet<>();
    private boolean isLoading;
    private final long id;
    private boolean isChannel;
    private boolean isEnabledReactions;

    public AdminReactionsActivity(Bundle args) {
        super(args);
        id = args.getLong("id", 0);
    }

    @Override
    public boolean onFragmentCreate() {
        super.onFragmentCreate();
        final TLRPC.ChatFull chatFull = getMessagesController().getChatFull(id);
        isEnabledReactions = chatFull != null && !chatFull.available_reactions.isEmpty();
        if (isEnabledReactions) {
            selectedReactions.clear();
            selectedReactions.addAll(chatFull.available_reactions);
        }
        NotificationCenter.getInstance(currentAccount).addObserver(this, NotificationCenter.availableReactionsDidLoad);
        return true;
    }

    @Override
    public void onFragmentDestroy() {
        NotificationCenter.getInstance(currentAccount).removeObserver(this, NotificationCenter.availableReactionsDidLoad);
        super.onFragmentDestroy();
    }

    @Override
    public boolean onBackPressed() {
        checkChanges();
        return super.onBackPressed();
    }

    private void sendReactions() {
        final long chatId = id;
        TLRPC.TL_messages_setChatAvailableReactions req = new TLRPC.TL_messages_setChatAvailableReactions();
        req.peer = getMessagesController().getInputPeer(-chatId);
        req.available_reactions = new ArrayList<>(isEnabledReactions ? selectedReactions : new ArrayList<>());

        AndroidUtilities.runOnUIThread(() -> {
            final TLRPC.ChatFull chatFull = getMessagesController().getChatFull(chatId);
            chatFull.available_reactions = new ArrayList<>(isEnabledReactions ? selectedReactions : new ArrayList<>());
            //todo новое поле в базе
            getMessagesStorage().updateChatInfo(chatFull, false);
            getNotificationCenter().postNotificationName(NotificationCenter.availableReactionsInChatChanged);
        });

        getConnectionsManager().sendRequest(req, (response, error) -> {
            if (error == null) {
                getMessagesController().processUpdates((TLRPC.Updates) response, false);
            } else {
                if (error.text != null && error.text.contains("CHAT_ADMIN_REQUIRED")) {
                    AlertsCreator.showSimpleToast(null, "You do not have permission to edit reactions.");
                }
            }
            AndroidUtilities.runOnUIThread(() -> getMessagesController().loadFullChat(chatId, 0, true), 1000);
        }, ConnectionsManager.RequestFlagInvokeAfter);
    }

    private void checkChanges() {
        final TLRPC.ChatFull chatFull = getMessagesController().getChatFull(AdminReactionsActivity.this.id);

        if (chatFull != null) {
            boolean haveChanges = false;
            final ArrayList<String> chatAvailableReactions = chatFull.available_reactions;
            if (chatAvailableReactions.isEmpty() && isEnabledReactions) {
                haveChanges = true;
            }
            if (!chatAvailableReactions.isEmpty() && !isEnabledReactions) {
                haveChanges = true;
            }
            if (!chatAvailableReactions.isEmpty() && isEnabledReactions) {
                for (String reaction : chatAvailableReactions) {
                    if (!selectedReactions.contains(reaction)) {
                        haveChanges = true;
                        break;
                    }
                }
                for (String reaction : selectedReactions) {
                    if (!chatAvailableReactions.contains(reaction)) {
                        haveChanges = true;
                        break;
                    }
                }
            }
            if (haveChanges) {
                sendReactions();
            }
        }
    }

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle("Reactions");
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    checkChanges();
                    finishFragment();
                }
            }
        });

        fragmentView = new FrameLayout(context);
        FrameLayout frameLayout = (FrameLayout) fragmentView;
        frameLayout.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray));

        emptyView = new EmptyTextProgressView(context);
        emptyView.setText("");
        emptyView.showProgress();
        frameLayout.addView(emptyView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        listView = new RecyclerListView(context);
        listView.setEmptyView(emptyView);
        listView.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false));
        listView.setVerticalScrollBarEnabled(false);
        listView.setAdapter(listViewAdapter = new ListAdapter(context));
        listView.setVerticalScrollbarPosition(LocaleController.isRTL ? RecyclerListView.SCROLLBAR_POSITION_LEFT : RecyclerListView.SCROLLBAR_POSITION_RIGHT);
        frameLayout.addView(listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        listView.setOnItemClickListener((view, position) -> {
            if (view instanceof TextCheckCell) {
                TextCheckCell cell = (TextCheckCell) view;
                boolean checked = cell.isChecked();
                isEnabledReactions = !checked;
                view.setTag(isEnabledReactions ? Theme.key_windowBackgroundChecked : Theme.key_windowBackgroundUnchecked);
                if (isEnabledReactions) {
                    cell.setBackgroundColorAnimated(true, Theme.getColor(Theme.key_windowBackgroundUnchecked), Theme.getColor(Theme.key_windowBackgroundChecked), false);
                } else {
                    cell.setBackgroundColorAnimated(false, Theme.getColor(Theme.key_windowBackgroundUnchecked), Theme.getColor(Theme.key_windowBackgroundChecked), true);
                }
                cell.setChecked(isEnabledReactions);
                if (isEnabledReactions) {
                    listViewAdapter.notifyItemRangeInserted(1, reactions.size() + 2);
                } else {
                    listViewAdapter.notifyItemRangeRemoved(1, reactions.size() + 2);
                }
            } else if (view instanceof ReactionCheckCell) {
                ReactionCheckCell cell = (ReactionCheckCell) view;
                boolean checked = cell.isChecked();
                String reaction = reactions.get(position - 3).reaction;
                if (checked) {
                    selectedReactions.remove(reaction);
                } else {
                    selectedReactions.add(reaction);
                }
                cell.setChecked(!checked);
            }
        });

        updateViews();
        return fragmentView;
    }

    private void updateViews() {
        reactions.clear();
        reactions.addAll(getMediaDataController().getAvailableReactions());

        final TLRPC.Chat chat = getMessagesController().getChat(id);

        if (chat != null) {
            isChannel = ChatObject.isChannel(chat);
        }

        isLoading = !getMediaDataController().hasAvailableReactions();
        if (listViewAdapter != null) listViewAdapter.notifyDataSetChanged();
    }

    @Override
    public void didReceivedNotification(int id, int account, Object... args) {
        if (id == NotificationCenter.availableReactionsDidLoad) {
            updateViews();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (listViewAdapter != null) listViewAdapter.notifyDataSetChanged();
    }

    private class ListAdapter extends RecyclerListView.SelectionAdapter {

        private final Context mContext;

        public ListAdapter(Context context) {
            mContext = context;
        }

        @Override
        public int getItemCount() {
            if (isLoading) return 0;
            if (!isEnabledReactions) return 1;
            return reactions.size() + 3;
        }

        @Override
        public boolean isEnabled(RecyclerView.ViewHolder holder) {
            int viewType = holder.getItemViewType();
            return viewType == MAIN_CHECK_TYPE || viewType == REACTION_CHECK_TYPE;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view;
            switch (viewType) {
                case REACTION_CHECK_TYPE:
                    view = new ReactionCheckCell(mContext);
                    view.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                    break;
                case MAIN_CHECK_TYPE:
                    TextCheckCell cell = new TextCheckCell(mContext);
                    cell.setColors(Theme.key_windowBackgroundCheckText, Theme.key_switchTrackBlue, Theme.key_switchTrackBlueChecked, Theme.key_switchTrackBlueThumb, Theme.key_switchTrackBlueThumbChecked);
                    cell.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
                    cell.setHeight(56);
                    view = cell;
                    break;
                case DIVIDER_TEXT_TYPE:
                    view = new TextInfoPrivacyCell(mContext);
                    break;
                case HEADER_TYPE:
                    HeaderCell headerCell = new HeaderCell(mContext, Theme.key_windowBackgroundWhiteBlueHeader, 21, 11, false);
                    headerCell.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                    headerCell.setHeight(43);
                    view = headerCell;
                    break;
                default:
                    view = new View(mContext);
                    break;
            }
            return new RecyclerListView.Holder(view);
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            switch (holder.getItemViewType()) {
                case REACTION_CHECK_TYPE:
                    final TLRPC.TL_availableReaction reaction = reactions.get(position - 3);
                    ReactionCheckCell textCheckCell = (ReactionCheckCell) holder.itemView;
                    textCheckCell.setTextAndCheck(reaction.title, selectedReactions.contains(reaction.reaction), position != getItemCount() - 1);
                    textCheckCell.setDocument(reaction.static_icon, reaction);
                    break;
                case MAIN_CHECK_TYPE:
                    TextCheckCell view = (TextCheckCell) holder.itemView;
                    view.setDrawCheckRipple(true);
                    view.setTextAndCheck("Enable Reactions", isEnabledReactions, false);
                    view.setTag(isEnabledReactions ? Theme.key_windowBackgroundChecked : Theme.key_windowBackgroundUnchecked);
                    view.setBackgroundColor(Theme.getColor(isEnabledReactions ? Theme.key_windowBackgroundChecked : Theme.key_windowBackgroundUnchecked));
                    break;
                case DIVIDER_TEXT_TYPE:
                    TextInfoPrivacyCell privacyCell = (TextInfoPrivacyCell) holder.itemView;
                    privacyCell.setText(isChannel ? "Allow subscribers to react to channel posts." : "Allow subscribers to react to group posts.");
                    privacyCell.setBackgroundDrawable(Theme.getThemedDrawable(mContext, R.drawable.greydivider_bottom, Theme.key_windowBackgroundGrayShadow));
                    break;
                case HEADER_TYPE:
                    HeaderCell headerCell = (HeaderCell) holder.itemView;
                    headerCell.setText("Available Reactions");
                    break;
            }
        }

        @Override
        public int getItemViewType(int position) {
            if (isLoading) return -1;
            switch (position) {
                case 0:
                    return MAIN_CHECK_TYPE;
                case 1:
                    return DIVIDER_TEXT_TYPE;
                case 2:
                    return HEADER_TYPE;
                default:
                    return REACTION_CHECK_TYPE;
            }
        }
    }

    @Override
    public ArrayList<ThemeDescription> getThemeDescriptions() {
        ArrayList<ThemeDescription> themeDescriptions = new ArrayList<>();
        themeDescriptions.add(new ThemeDescription(fragmentView, ThemeDescription.FLAG_BACKGROUND, null, null, null, null, Theme.key_windowBackgroundGray));
        themeDescriptions.add(new ThemeDescription(listView, ThemeDescription.FLAG_CELLBACKGROUNDCOLOR, new Class[]{ManageChatUserCell.class, ManageChatTextCell.class, HeaderCell.class}, null, null, null, Theme.key_windowBackgroundWhite));
        themeDescriptions.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_BACKGROUND, null, null, null, null, Theme.key_actionBarDefault));
        themeDescriptions.add(new ThemeDescription(listView, ThemeDescription.FLAG_LISTGLOWCOLOR, null, null, null, null, Theme.key_actionBarDefault));
        themeDescriptions.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_ITEMSCOLOR, null, null, null, null, Theme.key_actionBarDefaultIcon));
        themeDescriptions.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_TITLECOLOR, null, null, null, null, Theme.key_actionBarDefaultTitle));
        themeDescriptions.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_SELECTORCOLOR, null, null, null, null, Theme.key_actionBarDefaultSelector));
        themeDescriptions.add(new ThemeDescription(listView, ThemeDescription.FLAG_SELECTOR, null, null, null, null, Theme.key_listSelector));
        themeDescriptions.add(new ThemeDescription(emptyView, ThemeDescription.FLAG_TEXTCOLOR, null, null, null, null, Theme.key_emptyListPlaceholder));
        themeDescriptions.add(new ThemeDescription(emptyView, ThemeDescription.FLAG_PROGRESSBAR, null, null, null, null, Theme.key_progressCircle));
        themeDescriptions.add(new ThemeDescription(listView, ThemeDescription.FLAG_BACKGROUNDFILTER, new Class[]{ShadowSectionCell.class}, null, null, null, Theme.key_windowBackgroundGrayShadow));
        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{ManageChatUserCell.class}, new String[]{"nameTextView"}, null, null, null, Theme.key_windowBackgroundWhiteBlackText));
        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{HeaderCell.class}, new String[]{"textView"}, null, null, null, Theme.key_windowBackgroundWhiteBlueHeader));
        themeDescriptions.add(new ThemeDescription(listView, ThemeDescription.FLAG_CHECKTAG, new Class[]{ManageChatTextCell.class}, new String[]{"textView"}, null, null, null, Theme.key_windowBackgroundWhiteBlackText));
        themeDescriptions.add(new ThemeDescription(listView, ThemeDescription.FLAG_CHECKTAG, new Class[]{ManageChatTextCell.class}, new String[]{"imageView"}, null, null, null, Theme.key_windowBackgroundWhiteGrayIcon));
        themeDescriptions.add(new ThemeDescription(listView, ThemeDescription.FLAG_CHECKTAG, new Class[]{ManageChatTextCell.class}, new String[]{"imageView"}, null, null, null, Theme.key_windowBackgroundWhiteBlueButton));
        themeDescriptions.add(new ThemeDescription(listView, ThemeDescription.FLAG_CHECKTAG, new Class[]{ManageChatTextCell.class}, new String[]{"textView"}, null, null, null, Theme.key_windowBackgroundWhiteBlueIcon));
        themeDescriptions.add(new ThemeDescription(listView, ThemeDescription.FLAG_CELLBACKGROUNDCOLOR | ThemeDescription.FLAG_CHECKTAG, new Class[]{TextCheckCell.class}, null, null, null, Theme.key_windowBackgroundChecked));
        themeDescriptions.add(new ThemeDescription(listView, ThemeDescription.FLAG_CELLBACKGROUNDCOLOR | ThemeDescription.FLAG_CHECKTAG, new Class[]{TextCheckCell.class}, null, null, null, Theme.key_windowBackgroundUnchecked));
        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{TextCheckCell.class}, new String[]{"textView"}, null, null, null, Theme.key_windowBackgroundCheckText));
        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{TextCheckCell.class}, new String[]{"checkBox"}, null, null, null, Theme.key_switchTrackBlue));
        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{TextCheckCell.class}, new String[]{"checkBox"}, null, null, null, Theme.key_switchTrackBlueChecked));
        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{TextCheckCell.class}, new String[]{"checkBox"}, null, null, null, Theme.key_switchTrackBlueThumb));
        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{TextCheckCell.class}, new String[]{"checkBox"}, null, null, null, Theme.key_switchTrackBlueThumbChecked));
        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{TextCheckCell.class}, new String[]{"checkBox"}, null, null, null, Theme.key_switchTrackBlueSelector));
        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{TextCheckCell.class}, new String[]{"checkBox"}, null, null, null, Theme.key_switchTrackBlueSelectorChecked));
        themeDescriptions.add(new ThemeDescription(listView, ThemeDescription.FLAG_CELLBACKGROUNDCOLOR | ThemeDescription.FLAG_CHECKTAG, new Class[]{ReactionCheckCell.class}, null, null, null, Theme.key_windowBackgroundChecked));
        themeDescriptions.add(new ThemeDescription(listView, ThemeDescription.FLAG_CELLBACKGROUNDCOLOR | ThemeDescription.FLAG_CHECKTAG, new Class[]{ReactionCheckCell.class}, null, null, null, Theme.key_windowBackgroundUnchecked));
        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{ReactionCheckCell.class}, new String[]{"textView"}, null, null, null, Theme.key_windowBackgroundCheckText));
        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{ReactionCheckCell.class}, new String[]{"checkBox"}, null, null, null, Theme.key_switchTrackBlue));
        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{ReactionCheckCell.class}, new String[]{"checkBox"}, null, null, null, Theme.key_switchTrackBlueChecked));
        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{ReactionCheckCell.class}, new String[]{"checkBox"}, null, null, null, Theme.key_switchTrackBlueThumb));
        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{ReactionCheckCell.class}, new String[]{"checkBox"}, null, null, null, Theme.key_switchTrackBlueThumbChecked));
        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{ReactionCheckCell.class}, new String[]{"checkBox"}, null, null, null, Theme.key_switchTrackBlueSelector));
        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{ReactionCheckCell.class}, new String[]{"checkBox"}, null, null, null, Theme.key_switchTrackBlueSelectorChecked));
        return themeDescriptions;
    }
}

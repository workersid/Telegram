package org.telegram.ui.Components.reaction;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.DataSetObserver;
import android.os.Parcelable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.MediaDataController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;

import java.util.ArrayList;

@SuppressWarnings({"FieldCanBeLocal", "FieldMayBeFinal"})
@SuppressLint("ViewConstructor")
public class UserReactionsListWithTabs extends LinearLayout {
    interface Delegate {
        void onItemClick(TLRPC.User user);
    }

    private final ArrayList<EmotionInfo> emotionTabList = new ArrayList<>();
    private final int currentAccount = UserConfig.selectedAccount;

    private final RecyclerListView tabsListView;
    private final LinearLayoutManager tabsLayoutManager;
    private final RecyclerListView.SelectionAdapter tabsListAdapter;
    private final ViewPager viewPager;

    private int totalSeen;
    private int totalReactions;

    private int currentViewPagerPage;

    public UserReactionsListWithTabs(Context context, final MessageObject selectedObject, int seen, final Delegate delegate) {
        super(context);
        setOrientation(VERTICAL);

        totalSeen = seen;
        totalReactions = EmotionUtils.extractTotalReactions(selectedObject, null);
        emotionTabList.addAll(EmotionUtils.extractEmotionInfoList(selectedObject, MediaDataController.getInstance(currentAccount), false));

        viewPager = new ViewPager(context);
        viewPager.setAdapter(new PagerAdapter() {
            @Override
            public int getCount() {
                return emotionTabList.size();
            }

            @NonNull
            @Override
            public Object instantiateItem(@NonNull ViewGroup container, int position) {
                UserReactionsList userReactionsList = new UserReactionsList(container.getContext(), selectedObject, totalSeen, emotionTabList.get(position).reaction, user -> {
                    if (delegate != null) {
                        delegate.onItemClick(user);
                    }
                });
                container.addView(userReactionsList);
                return userReactionsList;
            }

            @Override
            public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
                container.removeView((View) object);
            }

            @Override
            public void setPrimaryItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
                super.setPrimaryItem(container, position, object);
                currentViewPagerPage = position;
            }

            @Override
            public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
                return view.equals(object);
            }

            @Override
            public void restoreState(Parcelable arg0, ClassLoader arg1) {
            }

            @Override
            public Parcelable saveState() {
                return null;
            }

            @Override
            public void unregisterDataSetObserver(@NonNull DataSetObserver observer) {
                super.unregisterDataSetObserver(observer);
            }
        });
        viewPager.setPageMargin(0);
        viewPager.setOffscreenPageLimit(1);
        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int i) {
                currentViewPagerPage = i;
            }

            @Override
            public void onPageScrollStateChanged(int i) {

            }
        });

        FrameLayout tabsContainer = new FrameLayout(getContext());
        tabsListView = new RecyclerListView(getContext());
        tabsListView.setLayoutManager(tabsLayoutManager = new LinearLayoutManager(getContext(), RecyclerView.HORIZONTAL, false));
        tabsListView.setAdapter(tabsListAdapter = new RecyclerListView.SelectionAdapter() {

            @Override
            public boolean isEnabled(RecyclerView.ViewHolder holder) {
                return false;
            }

            @NonNull
            @Override
            public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                EmotionCell cell = new EmotionCell(parent.getContext());
                cell.setLayoutParams(new RecyclerView.LayoutParams(LayoutHelper.WRAP_CONTENT, AndroidUtilities.dp(42)));
                return new RecyclerListView.Holder(cell);
            }

            @Override
            public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
                if (holder.itemView instanceof EmotionCell) {
                    EmotionCell cell = (EmotionCell) holder.itemView;
                    cell.setEmotionInfo(emotionTabList.get(position), false);
                }
            }

            @Override
            public int getItemCount() {
                return emotionTabList.size();
            }
        });
        tabsContainer.addView(tabsListView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
        addView(tabsContainer, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 48));
        addView(viewPager, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
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

    public int getMainTabCount() {
        return emotionTabList.get(0).count;
    }

    boolean hasTabs() {
        return true;
    }
}

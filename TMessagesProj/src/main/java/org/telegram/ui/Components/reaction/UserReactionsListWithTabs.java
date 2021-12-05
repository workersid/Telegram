package org.telegram.ui.Components.reaction;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.DataSetObserver;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Parcelable;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.LinearSmoothScroller;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.MediaDataController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;

import java.util.ArrayList;

@SuppressWarnings({"FieldCanBeLocal", "FieldMayBeFinal"})
@SuppressLint("ViewConstructor")
public class UserReactionsListWithTabs extends LinearLayout implements NotificationCenter.NotificationCenterDelegate {
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
    private boolean isMoreThanTenReactionsWithDifferentTypes;
    private MessageObject currentMessageObject;

    public UserReactionsListWithTabs(Context context, final MessageObject selectedObject, int seen, final Delegate delegate) {
        super(context);
        setOrientation(VERTICAL);
        currentMessageObject = selectedObject;
        totalSeen = seen;
        resetMainData();
        isMoreThanTenReactionsWithDifferentTypes = EmotionUtils.isMoreThanTenReactionsWithDifferentTypes(selectedObject);

        for (int i = 0; i < emotionTabList.size(); i++) {
            if (i == 0) {
                emotionTabList.get(i).isSelectedByCurrentUser = true;
            } else {
                emotionTabList.get(i).isSelectedByCurrentUser = false;
            }
        }

        FrameLayout viewPagerContainer = new FrameLayout(getContext());
        viewPager = new ViewPager(context);
        viewPager.setAdapter(new PagerAdapter() {
            @Override
            public int getCount() {
                if (isMoreThanTenReactionsWithDifferentTypes) {
                    return emotionTabList.size();
                }
                return 1;
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
            public void onPageSelected(int ii) {
                if (tabsListView != null) {
                    try {
                        final LinearSmoothScroller linearSmoothScroller = new LinearSmoothScroller(getContext()) {
                            @Override
                            protected float calculateSpeedPerPixel(DisplayMetrics displayMetrics) {
                                return 300f / displayMetrics.densityDpi;
                            }
                        };

                        int firstVisiblePosition = tabsLayoutManager.findFirstVisibleItemPosition();
                        int lastVisiblePosition = tabsLayoutManager.findLastVisibleItemPosition();
                        if (ii >= firstVisiblePosition && ii <= lastVisiblePosition) {
                            if (currentViewPagerPage > ii) {
                                //на лево
                                if (ii - 1 < 0) {
                                    linearSmoothScroller.setTargetPosition(0);
                                } else {
                                    linearSmoothScroller.setTargetPosition(ii - 1);
                                }
                                tabsLayoutManager.startSmoothScroll(linearSmoothScroller);
                            } else {
                                //право
                                linearSmoothScroller.setTargetPosition(ii + 1);
                                tabsLayoutManager.startSmoothScroll(linearSmoothScroller);
                            }
                        } else {
                            linearSmoothScroller.setTargetPosition(ii);
                            tabsLayoutManager.startSmoothScroll(linearSmoothScroller);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        //пожарный
                    }
                }
                currentViewPagerPage = ii;

                for (int i = 0; i < emotionTabList.size(); i++) {
                    EmotionInfo emotionInfo = emotionTabList.get(i);
                    if (i == currentViewPagerPage) {
                        if (!emotionInfo.isSelectedByCurrentUser) {
                            emotionInfo.isSelectedByCurrentUser = true;
                            RecyclerView.ViewHolder viewHolder = tabsListView.findViewHolderForLayoutPosition(i);
                            if (viewHolder != null && viewHolder.itemView instanceof EmotionCell) {
                                ((EmotionCell) viewHolder.itemView).setEmotionInfo(emotionInfo, true);
                            } else {
                                if (tabsListAdapter != null) tabsListAdapter.notifyDataSetChanged();
                            }
                        }
                    } else {
                        if (emotionInfo.isSelectedByCurrentUser) {
                            EmotionInfo copy = emotionInfo.copy();
                            copy.isSelectedByCurrentUser = false;
                            RecyclerView.ViewHolder viewHolder = tabsListView.findViewHolderForLayoutPosition(i);
                            if (viewHolder != null && viewHolder.itemView instanceof EmotionCell) {
                                ((EmotionCell) viewHolder.itemView).setEmotionInfo(copy, true);
                            }
                            emotionInfo.isSelectedByCurrentUser = false;
                        }
                    }
                }
            }

            @Override
            public void onPageScrollStateChanged(int i) {

            }
        });

        FrameLayout tabsContainer = new FrameLayout(getContext());
        tabsListView = new RecyclerListView(getContext());
        tabsListView.setLayoutManager(tabsLayoutManager = new LinearLayoutManager(getContext(), RecyclerView.HORIZONTAL, false));

        tabsListView.addItemDecoration(new RecyclerView.ItemDecoration() {
            @Override
            public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
                int p = parent.getChildAdapterPosition(view);
                if (p == 0) {
                    outRect.left = AndroidUtilities.dp(8);
                }
                if (p == emotionTabList.size() - 1) {
                    outRect.right = AndroidUtilities.dp(8);
                }
            }
        });
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
        tabsListView.setOnItemClickListener((view, position) -> {
            viewPager.setCurrentItem(position);
        });

        Drawable headerShadowDrawable = ContextCompat.getDrawable(context, R.drawable.header_shadow).mutate();
        View headerShadowView = new View(context) {
            @Override
            protected void onDraw(Canvas canvas) {
                super.onDraw(canvas);
                headerShadowDrawable.setBounds(0, getMeasuredHeight() - AndroidUtilities.dp(2), getMeasuredWidth(), getMeasuredHeight());
                headerShadowDrawable.draw(canvas);
            }
        };

        viewPagerContainer.addView(viewPager, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        if (isMoreThanTenReactionsWithDifferentTypes) {
            viewPagerContainer.addView(headerShadowView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 2));
            addView(tabsContainer, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 48));//табы
        } else {
            //разделитель
            FrameLayout gap = new FrameLayout(context);
            gap.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray));
            gap.setMinimumWidth(AndroidUtilities.dp(196));

            Drawable hDrawable = ContextCompat.getDrawable(context, R.drawable.header_shadow).mutate();
            View hView = new View(context) {
                @Override
                protected void onDraw(Canvas canvas) {
                    super.onDraw(canvas);
                    hDrawable.setBounds(0, getMeasuredHeight() - AndroidUtilities.dp(2), getMeasuredWidth(), getMeasuredHeight());
                    hDrawable.draw(canvas);
                }
            };
            Drawable bDrawable = ContextCompat.getDrawable(context, R.drawable.bottom_shadow).mutate();
            View bView = new View(context) {
                @Override
                protected void onDraw(Canvas canvas) {
                    super.onDraw(canvas);
                    bDrawable.setBounds(0, getMeasuredHeight() - AndroidUtilities.dp(2), getMeasuredWidth(), getMeasuredHeight());
                    bDrawable.draw(canvas);
                }
            };
            gap.addView(hView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 2));
            //gap.addView(bView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 2, Gravity.BOTTOM));
            addView(gap, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 8));
        }

        tabsContainer.addView(tabsListView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
        addView(viewPagerContainer, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
    }

    private void resetMainData() {
        //нужно оставлять выбранную позицию
        String selectedReaction = null;
        for (int i = 0; i < emotionTabList.size(); i++) {
            EmotionInfo info = emotionTabList.get(i);
            if (info.isSelectedByCurrentUser) {
                selectedReaction = info.reaction;
                break;
            }
        }
        totalReactions = EmotionUtils.extractTotalReactions(currentMessageObject, null);
        emotionTabList.clear();
        emotionTabList.addAll(EmotionUtils.extractEmotionInfoList(currentMessageObject, MediaDataController.getInstance(currentAccount), false));
        for (int i = 0; i < emotionTabList.size(); i++) {
            EmotionInfo info = emotionTabList.get(i);
            if (info.reaction == null && selectedReaction == null) {
                info.isSelectedByCurrentUser = true;
                break;
            }
            if (selectedReaction != null && info.reaction != null) {
                if (selectedReaction.equals(info.reaction)) {
                    info.isSelectedByCurrentUser = true;
                    break;
                }
            }
        }
    }

    @Override
    public void didReceivedNotification(int id, int account, Object... args) {
        if (tabsListAdapter == null || currentMessageObject == null) return;
        if (id == NotificationCenter.availableReactionsDidLoad) {
            resetMainData();
        }

        if (id == NotificationCenter.didAfterUpdateReactions) {
            long did = (Long) args[0];
            if (did == currentMessageObject.getDialogId()) {
                int msgId = (Integer) args[1];
                if (currentMessageObject.getId() == msgId) {
                    resetMainData();
                }
            }
        }

        tabsListAdapter.notifyDataSetChanged();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        NotificationCenter.getInstance(currentAccount).addObserver(this, NotificationCenter.availableReactionsDidLoad);
        NotificationCenter.getInstance(currentAccount).addObserver(this, NotificationCenter.didAfterUpdateReactions);
    }

    @Override
    protected void onDetachedFromWindow() {
        NotificationCenter.getInstance(currentAccount).removeObserver(this, NotificationCenter.availableReactionsDidLoad);
        NotificationCenter.getInstance(currentAccount).removeObserver(this, NotificationCenter.didAfterUpdateReactions);
        super.onDetachedFromWindow();
    }

    public int getMainTabCount() {
        if (totalSeen > 0) {
            return totalSeen;
        }

        if (emotionTabList.isEmpty()) {
            return 0;
        }

        return emotionTabList.get(0).count;
    }

    boolean hasTabs() {
        return true;
    }
}

package org.telegram.ui.Components.reaction;

import android.animation.LayoutTransition;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Build;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.DialogObject;
import org.telegram.messenger.MediaDataController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.CubicBezierInterpolator;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

@SuppressLint("ViewConstructor")
public class ChooseReactionLayout extends FrameLayout implements NotificationCenter.NotificationCenterDelegate {

    public interface Delegate {
        void onItemClick(TLRPC.TL_availableReaction reaction, int x, int y, int msgId);
    }

    private RecyclerListView listView;
    private ChooseReactionAdapter listViewAdapter;
    private final int currentAccount = UserConfig.selectedAccount;
    private final Paint bgPaintWithShadow = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF bgRect = new RectF();
    private final Path bgClipPath = new Path();
    private final Path pathCircleBig = new Path();
    private final int roundBgRadius = AndroidUtilities.dp(24);
    private Delegate delegate;
    private boolean isChatDialog = false;
    private final HashSet<String> adminsReactions = new HashSet<>();
    private int containerMaxWidth = 0;
    private MessageObject currentMessageObject;
    private static final AccelerateInterpolator interpolator = new AccelerateInterpolator(0.5f);

    public ChooseReactionLayout(@NonNull Context context, MessageObject messageObject) {
        super(context);
        init(context, messageObject);
    }

    public void setDelegate(Delegate delegate) {
        this.delegate = delegate;
    }

    private void init(Context context, MessageObject messageObject) {
        currentMessageObject = messageObject;
        isChatDialog = DialogObject.isChatDialog(messageObject.getDialogId());
        if (isChatDialog) {
            TLRPC.ChatFull chatFull = MessagesController.getInstance(currentAccount).getChatFull(messageObject.getChatId());
            if (chatFull != null && !chatFull.available_reactions.isEmpty()) {
                adminsReactions.clear();
                adminsReactions.addAll(chatFull.available_reactions);
            }
        }
        bgPaint.setColor(Theme.getColor(Theme.key_actionBarDefaultSubmenuBackground));
        bgPaintWithShadow.setColor(Theme.getColor(Theme.key_actionBarDefaultSubmenuBackground));
        bgPaintWithShadow.setShadowLayer(AndroidUtilities.dp(1.5f), 0.0f, 0.0f, 0x66000000);
        setWillNotDraw(false);
        listView = new RecyclerListView(context);
        listView.setVisibility(GONE);
        listView.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false));
        listView.setVerticalScrollBarEnabled(false);
        listView.addItemDecoration(new RecyclerView.ItemDecoration() {
            @Override
            public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
                int p = parent.getChildAdapterPosition(view);
                if (p == 0) {
                    outRect.left = AndroidUtilities.dp(14);
                }
                /*if (p == listViewAdapter.getItemCount() - 1) {
                    outRect.right = AndroidUtilities.dp(14);
                }*/
            }
        });
        listView.setAdapter(listViewAdapter = new ChooseReactionAdapter(context));
        listView.setFastScrollVisible(false);
        addView(listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 66, Gravity.RIGHT, 0, 0, 0, 0));
        listView.setOnItemClickListener((view, position) -> {
            if (delegate != null) {
                TLRPC.TL_availableReaction reaction = listViewAdapter.getItem(position);
                if (reaction != null && ((ChooseReactionStickerCell) view).isAnimationReady()) {
                    int[] location = new int[2];
                    view.getLocationOnScreen(location);
                    int x = location[0];
                    int y = location[1] - AndroidUtilities.statusBarHeight;
                    delegate.onItemClick(reaction, x, y, currentMessageObject.getId());
                }
            }
        });
        listView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        setLayoutTransition(new LayoutTransition());
    }

    public void updateData() {
        if (listViewAdapter != null) {
            List<TLRPC.TL_availableReaction> availableReactions = MediaDataController.getInstance(currentAccount).getAvailableReactions();
            if (!isChatDialog) {
                listViewAdapter.setData(availableReactions);
                changeLayerWidth(availableReactions.size());
            } else {
                List<TLRPC.TL_availableReaction> resultReaction = new ArrayList<>();
                for (int i = 0; i < availableReactions.size(); i++) {
                    TLRPC.TL_availableReaction tlAvailableReaction = availableReactions.get(i);
                    if (tlAvailableReaction != null && adminsReactions.contains(tlAvailableReaction.reaction)) {
                        resultReaction.add(tlAvailableReaction);
                    }
                }
                listViewAdapter.setData(resultReaction);
                changeLayerWidth(resultReaction.size());
            }
        }
    }

    private void changeLayerWidth(int allCount) {
        //31 отступ
        float availableWidth = containerMaxWidth - AndroidUtilities.dp(32);
        //44dp ширина одной ячейки
        int visibleCount = (int) (availableWidth / AndroidUtilities.dp(44));
        if (allCount > visibleCount) {
            //32 отступы
            ((MarginLayoutParams) getLayoutParams()).leftMargin = 0;
            getLayoutParams().width = containerMaxWidth;
        } else {
            getLayoutParams().width = allCount * AndroidUtilities.dp(44) + AndroidUtilities.dp(14) + AndroidUtilities.dp(4);
            ((MarginLayoutParams) getLayoutParams()).leftMargin = containerMaxWidth - getLayoutParams().width;
        }
        requestLayout();
    }

    public void show(int containerWidth) {
        containerMaxWidth = containerWidth + AndroidUtilities.dp(32);
        getLayoutParams().width = containerMaxWidth;
        listView.setVisibility(VISIBLE);
        updateData();

        final int startAnimWidth = getLayoutParams().width;
        final int startAnimMargin = ((MarginLayoutParams) getLayoutParams()).leftMargin;

        getLayoutParams().width = 0;
        ((MarginLayoutParams) getLayoutParams()).leftMargin = 0;

        postDelayed(() -> {
            ValueAnimator anim = ValueAnimator.ofInt(0, startAnimWidth);
            anim.addUpdateListener(valueAnimator -> {
                int val = (int) valueAnimator.getAnimatedValue();
                MarginLayoutParams layoutParams = (MarginLayoutParams) getLayoutParams();
                layoutParams.width = val;
                layoutParams.leftMargin = startAnimMargin + startAnimWidth - val;
                setLayoutParams(layoutParams);
            });
            anim.setInterpolator(CubicBezierInterpolator.EASE_IN);
            anim.setDuration(100);
            anim.start();
        }, 100);
    }

    @Override
    public void didReceivedNotification(int id, int account, Object... args) {
        if (id == NotificationCenter.availableReactionsDidLoad) {
            updateData();
        }

        if (id == NotificationCenter.chatInfoDidLoad && currentMessageObject != null) {
            TLRPC.ChatFull tmpChatFull = (TLRPC.ChatFull) args[0];
            if (isChatDialog && tmpChatFull.id == currentMessageObject.getChatId()) {
                TLRPC.ChatFull chatFull = MessagesController.getInstance(currentAccount).getChatFull(currentMessageObject.getChatId());
                if (chatFull != null) {
                    adminsReactions.clear();
                    adminsReactions.addAll(chatFull.available_reactions);
                    updateData();
                }
            }
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        NotificationCenter.getInstance(currentAccount).addObserver(this, NotificationCenter.availableReactionsDidLoad);
        NotificationCenter.getInstance(currentAccount).addObserver(this, NotificationCenter.chatInfoDidLoad);
    }

    @Override
    protected void onDetachedFromWindow() {
        NotificationCenter.getInstance(currentAccount).removeObserver(this, NotificationCenter.availableReactionsDidLoad);
        NotificationCenter.getInstance(currentAccount).removeObserver(this, NotificationCenter.chatInfoDidLoad);
        super.onDetachedFromWindow();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        bgClipPath.reset();
        pathCircleBig.reset();
        bgRect.set(AndroidUtilities.dp(1), AndroidUtilities.dp(1), w - AndroidUtilities.dp(1), h - AndroidUtilities.dp(18));
        bgClipPath.addRoundRect(bgRect, roundBgRadius, roundBgRadius, Path.Direction.CW);

        pathCircleBig.addCircle(w - AndroidUtilities.dp(31), h - AndroidUtilities.dp(20), AndroidUtilities.dp(8), Path.Direction.CW);
        pathCircleBig.addCircle(w - AndroidUtilities.dp(25), h - AndroidUtilities.dp(4.5f), AndroidUtilities.dp(3.5f), Path.Direction.CW);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            bgClipPath.op(pathCircleBig, Path.Op.UNION);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            canvas.drawPath(bgClipPath, bgPaintWithShadow);
        } else {
            canvas.drawCircle(getMeasuredWidth() - AndroidUtilities.dp(25), getMeasuredHeight() - AndroidUtilities.dp(4.5f), AndroidUtilities.dp(3.5f), bgPaint);
            canvas.drawCircle(getMeasuredWidth() - AndroidUtilities.dp(31), getMeasuredHeight() - AndroidUtilities.dp(20), AndroidUtilities.dp(8), bgPaint);
            canvas.drawRoundRect(bgRect, roundBgRadius, roundBgRadius, bgPaint);
        }
        canvas.clipPath(bgClipPath);
        super.onDraw(canvas);
    }
}

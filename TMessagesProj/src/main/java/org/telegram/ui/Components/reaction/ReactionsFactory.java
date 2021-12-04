package org.telegram.ui.Components.reaction;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import androidx.core.content.ContextCompat;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.ActionBarMenuSubItem;
import org.telegram.ui.ActionBar.ActionBarPopupWindow;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.ChatMessageCell;
import org.telegram.ui.ChatActivity;
import org.telegram.ui.Components.CubicBezierInterpolator;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.ProfileActivity;

public class ReactionsFactory {
    public interface ReactionsCounterDelegate {
        ActionBarPopupWindow getScrimPopupWindow();

        Context getContext();

        ChatActivity.ThemeDelegate getThemeDelegate();

        void openFragment(BaseFragment fragment);

        int getScrimPopupY();

        int getScrimPopupX();

        int getHeightWithKeyboard();

        int getKeyboardHeight();

        void showReactionsUsersPopupWindow(ActionBarPopupWindow popupWindow, int x, int y);

        void dismissReactionsUsersPopupWindow();

        View getReactionsUsersPopupWindowContent();

        void deleteReactionsUsersPopupWindowLink();
    }

    public static ChooseReactionLayout createChooseReactionLayout(LinearLayout parent, MessageObject object, MessageObject.GroupedMessages groupedMessages) {
        object = EmotionUtils.getMessageObjectForReactions(object, groupedMessages);
        ChooseReactionLayout view = new ChooseReactionLayout(parent.getContext(), object);
        parent.addView(view, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 0, -16));
        return view;
    }

    @SuppressLint("ClickableViewAccessibility")
    public static MenuReactionCounterView createReactionsCounterView(ViewGroup parent, final MessageObject selectedObject, ReactionsCounterDelegate delegate) {
        final MenuReactionCounterView menuReactionCounterView = new MenuReactionCounterView(parent.getContext(), selectedObject);
        parent.addView(menuReactionCounterView);

        FrameLayout gap = new FrameLayout(parent.getContext());
        gap.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray));
        gap.setMinimumWidth(AndroidUtilities.dp(196));
        gap.setTag(1001);
        gap.setTag(R.id.object_tag, 1);
        parent.addView(gap);
        LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) menuReactionCounterView.getLayoutParams();
        if (LocaleController.isRTL) layoutParams.gravity = Gravity.RIGHT;
        layoutParams.width = LayoutHelper.MATCH_PARENT;
        layoutParams.height = AndroidUtilities.dp(8);
        gap.setLayoutParams(layoutParams);

        Drawable headerShadowDrawable = ContextCompat.getDrawable(parent.getContext(), R.drawable.header_shadow).mutate();
        View headerShadowView = new View(parent.getContext()) {
            @Override
            protected void onDraw(Canvas canvas) {
                super.onDraw(canvas);
                headerShadowDrawable.setBounds(0, getMeasuredHeight() - AndroidUtilities.dp(2), getMeasuredWidth(), getMeasuredHeight());
                headerShadowDrawable.draw(canvas);
            }
        };

        Drawable bottomShadowDrawable = ContextCompat.getDrawable(parent.getContext(), R.drawable.bottom_shadow).mutate();
        View bottomShadowView = new View(parent.getContext()) {
            @Override
            protected void onDraw(Canvas canvas) {
                super.onDraw(canvas);
                bottomShadowDrawable.setBounds(0, getMeasuredHeight() - AndroidUtilities.dp(1), getMeasuredWidth(), getMeasuredHeight());
                bottomShadowDrawable.draw(canvas);
            }
        };

        gap.addView(headerShadowView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 2));
        gap.addView(bottomShadowView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 1, Gravity.BOTTOM));

        menuReactionCounterView.setOnClickListener(v -> {
            if (delegate != null && delegate.getScrimPopupWindow() != null) {
                int totalHeight = delegate.getHeightWithKeyboard();
                int availableHeight = totalHeight - delegate.getScrimPopupY() - AndroidUtilities.dp(46 + 16);
                availableHeight -= delegate.getKeyboardHeight() / 3f;

                View previousPopupContentView = delegate.getScrimPopupWindow().getContentView();

                ActionBarMenuSubItem cell = new ActionBarMenuSubItem(delegate.getContext(), true, true, delegate.getThemeDelegate());
                cell.setItemHeight(44);
                cell.setTextAndIcon(LocaleController.getString("Back", R.string.Back), R.drawable.msg_arrow_back);
                cell.getTextView().setPadding(LocaleController.isRTL ? 0 : AndroidUtilities.dp(40), 0, LocaleController.isRTL ? AndroidUtilities.dp(40) : 0, 0);

                FrameLayout backContainer = new FrameLayout(delegate.getContext());

                LinearLayout linearLayout = new LinearLayout(delegate.getContext()) {
                    @Override
                    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
                        super.onMeasure(MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(260), MeasureSpec.AT_MOST), heightMeasureSpec);
                        setPivotX(getMeasuredWidth() - AndroidUtilities.dp(8));
                        setPivotY(AndroidUtilities.dp(8));
                    }

                    @Override
                    public boolean dispatchKeyEvent(KeyEvent event) {
                        if (event.getKeyCode() == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0 && delegate.getScrimPopupWindow() != null && delegate.getScrimPopupWindow().isShowing()) {
                            delegate.dismissReactionsUsersPopupWindow();
                        }
                        return super.dispatchKeyEvent(event);
                    }
                };

                Drawable shadowDrawable2 = ContextCompat.getDrawable(delegate.getContext(), R.drawable.popup_fixed_alert).mutate();
                shadowDrawable2.setColorFilter(new PorterDuffColorFilter(Theme.getColor(Theme.key_actionBarDefaultSubmenuBackground), PorterDuff.Mode.MULTIPLY));
                linearLayout.setBackground(shadowDrawable2);

                linearLayout.setOnTouchListener(new View.OnTouchListener() {

                    private final int[] pos = new int[2];
                    private final RectF rect = new RectF();

                    @Override
                    public boolean onTouch(View v, MotionEvent event) {
                        if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                            if (delegate.getReactionsUsersPopupWindowContent() != null) {
                                View contentView = delegate.getReactionsUsersPopupWindowContent();
                                contentView.getLocationInWindow(pos);
                                rect.set(pos[0], pos[1], pos[0] + contentView.getMeasuredWidth(), pos[1] + contentView.getMeasuredHeight());
                                if (!rect.contains((int) event.getX(), (int) event.getY())) {
                                    delegate.dismissReactionsUsersPopupWindow();
                                }
                            }
                        } else if (event.getActionMasked() == MotionEvent.ACTION_OUTSIDE) {
                            delegate.dismissReactionsUsersPopupWindow();
                        }
                        return false;
                    }
                });
                linearLayout.setOrientation(LinearLayout.VERTICAL);
                UserReactionsListWithTabs listView = new UserReactionsListWithTabs(
                        delegate.getContext(),
                        selectedObject,
                        menuReactionCounterView.getTotalSeen(),
                        user -> {
                            if (user == null) return;
                            Bundle args = new Bundle();
                            args.putLong("user_id", user.id);
                            ProfileActivity fragment = new ProfileActivity(args);
                            delegate.openFragment(fragment);
                            delegate.dismissReactionsUsersPopupWindow();
                        }
                );
                int listViewTotalHeight = AndroidUtilities.dp(44) * listView.getMainTabCount();
                listViewTotalHeight += AndroidUtilities.dp(44);//кнопка назад

                if (EmotionUtils.isMoreThanTenReactionsWithDifferentTypes(selectedObject)) {
                    listViewTotalHeight += AndroidUtilities.dp(48);//табы
                } else {
                    listViewTotalHeight += AndroidUtilities.dp(8);//разделитель
                }

                listViewTotalHeight += AndroidUtilities.dp(8);//тень

                backContainer.addView(cell);
                linearLayout.addView(backContainer);
                linearLayout.addView(listView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 320, 0, 4, 0, 0));

                if (listViewTotalHeight > availableHeight) {
                    listView.getLayoutParams().height = Math.min(availableHeight, AndroidUtilities.dp(620));
                } else {
                    listView.getLayoutParams().height = listViewTotalHeight;
                }

                boolean[] backButtonPressed = new boolean[1];

                final ActionBarPopupWindow reactionsUsersPopupWindow = new ActionBarPopupWindow(linearLayout, LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT) {
                    @Override
                    public void dismiss(boolean animated) {
                        super.dismiss(animated);
                        if (backButtonPressed[0]) {
                            linearLayout.animate().alpha(0).scaleX(0).scaleY(0).setInterpolator(CubicBezierInterpolator.EASE_OUT_QUINT).setDuration(350);
                            previousPopupContentView.animate().alpha(1f).scaleX(1).scaleY(1).setInterpolator(CubicBezierInterpolator.EASE_OUT_QUINT).setDuration(350);
                        } else {
                            if (delegate.getScrimPopupWindow() != null) {
                                delegate.getScrimPopupWindow().dismiss();
                            }
                        }
                        delegate.deleteReactionsUsersPopupWindowLink();
                    }
                };
                reactionsUsersPopupWindow.setOutsideTouchable(true);
                reactionsUsersPopupWindow.setClippingEnabled(true);
                reactionsUsersPopupWindow.setFocusable(true);
                reactionsUsersPopupWindow.setInputMethodMode(ActionBarPopupWindow.INPUT_METHOD_NOT_NEEDED);
                reactionsUsersPopupWindow.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_UNSPECIFIED);
                reactionsUsersPopupWindow.getContentView().setFocusableInTouchMode(true);

                delegate.showReactionsUsersPopupWindow(reactionsUsersPopupWindow, delegate.getScrimPopupX(), delegate.getScrimPopupY());

                previousPopupContentView.setPivotX(AndroidUtilities.dp(8));
                previousPopupContentView.setPivotY(AndroidUtilities.dp(8));
                previousPopupContentView.animate().alpha(0).scaleX(0f).scaleY(0f).setInterpolator(CubicBezierInterpolator.EASE_OUT_QUINT).setDuration(350);

                linearLayout.setAlpha(0f);
                linearLayout.setScaleX(0f);
                linearLayout.setScaleY(0f);
                linearLayout.animate().alpha(1f).scaleX(1f).scaleY(1f).setInterpolator(CubicBezierInterpolator.EASE_OUT_QUINT).setDuration(350);

                backContainer.setOnClickListener(view -> {
                    reactionsUsersPopupWindow.setEmptyOutAnimation(250);
                    backButtonPressed[0] = true;
                    reactionsUsersPopupWindow.dismiss(true);
                });
            }
        });
        return menuReactionCounterView;
    }


    public interface PopupWithUsersForReactionDelegate {
        ActionBarPopupWindow getPopupWindow();

        void openFragment(BaseFragment fragment);

        int getHeightWithKeyboard();

        int getKeyboardHeight();

        void showPopupWindow(ActionBarPopupWindow popupWindow, int x, int y);

        void dismissPopupWindow();

        View getPopupWindowContent();

        void deletePopupWindowLink();
    }

    @SuppressLint("ClickableViewAccessibility")
    public static void createPopupWithUsersForReaction(EmotionInfo emotionInfo, ChatMessageCell cel, Context context, final PopupWithUsersForReactionDelegate delegate) {
        final int[] celPos = new int[2];
        cel.getLocationInWindow(celPos);
        int totalHeight = delegate.getHeightWithKeyboard();
        int availableHeight = totalHeight - (celPos[1] + (int) emotionInfo.drawRegion.bottom);
        availableHeight -= delegate.getKeyboardHeight() / 3f;
        if (availableHeight < AndroidUtilities.dp(200)) {
            availableHeight = AndroidUtilities.dp(200);
        }

        LinearLayout linearLayout = new LinearLayout(context) {
            @Override
            protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
                super.onMeasure(MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(260), MeasureSpec.AT_MOST), heightMeasureSpec);
                setPivotX(getMeasuredWidth() - AndroidUtilities.dp(8));
                setPivotY(AndroidUtilities.dp(8));
            }

            @Override
            public boolean dispatchKeyEvent(KeyEvent event) {
                if (event.getKeyCode() == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0 && delegate.getPopupWindow() != null && delegate.getPopupWindow().isShowing()) {
                    delegate.dismissPopupWindow();
                }
                return super.dispatchKeyEvent(event);
            }
        };

        Drawable shadowDrawable2 = ContextCompat.getDrawable(context, R.drawable.popup_fixed_alert).mutate();
        shadowDrawable2.setColorFilter(new PorterDuffColorFilter(Theme.getColor(Theme.key_actionBarDefaultSubmenuBackground), PorterDuff.Mode.MULTIPLY));
        linearLayout.setBackground(shadowDrawable2);

        linearLayout.setOnTouchListener(new View.OnTouchListener() {

            private final int[] pos = new int[2];
            private final RectF rect = new RectF();

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                    if (delegate.getPopupWindowContent() != null) {
                        View contentView = delegate.getPopupWindowContent();
                        contentView.getLocationInWindow(pos);
                        rect.set(pos[0], pos[1], pos[0] + contentView.getMeasuredWidth(), pos[1] + contentView.getMeasuredHeight());
                        if (!rect.contains((int) event.getX(), (int) event.getY())) {
                            delegate.dismissPopupWindow();
                        }
                    }
                } else if (event.getActionMasked() == MotionEvent.ACTION_OUTSIDE) {
                    delegate.dismissPopupWindow();
                }
                return false;
            }
        });
        linearLayout.setOrientation(LinearLayout.VERTICAL);

        MessageObject selectedObject = EmotionUtils.getMessageObjectForReactions(cel.getMessageObject(), cel.getCurrentMessagesGroup());

        UserReactionsList listView = new UserReactionsList(context, selectedObject, 0, emotionInfo.reaction, user -> {
            if (user == null) return;
            Bundle args = new Bundle();
            args.putLong("user_id", user.id);
            ProfileActivity fragment = new ProfileActivity(args);
            delegate.openFragment(fragment);
            delegate.dismissPopupWindow();
        });

        int listViewTotalHeight = AndroidUtilities.dp(10) + AndroidUtilities.dp(44) * listView.getTotalReactions();

        linearLayout.addView(listView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 320, 0, 0, 0, 0));

        if (listViewTotalHeight > availableHeight) {
            listView.getLayoutParams().height = Math.min(availableHeight, AndroidUtilities.dp(620));
        } else {
            listView.getLayoutParams().height = listViewTotalHeight;
        }

        final ActionBarPopupWindow popupWindow = new ActionBarPopupWindow(linearLayout, LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT) {
            @Override
            public void dismiss(boolean animated) {
                super.dismiss(animated);
                delegate.deletePopupWindowLink();
            }
        };
        popupWindow.setOutsideTouchable(true);
        popupWindow.setClippingEnabled(true);
        popupWindow.setFocusable(true);
        popupWindow.setInputMethodMode(ActionBarPopupWindow.INPUT_METHOD_NOT_NEEDED);
        popupWindow.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_UNSPECIFIED);
        popupWindow.getContentView().setFocusableInTouchMode(true);

        delegate.showPopupWindow(popupWindow, (int) emotionInfo.drawRegion.left - AndroidUtilities.dp(8), celPos[1] + (int) emotionInfo.drawRegion.bottom);

        linearLayout.setAlpha(0f);
        linearLayout.setScaleX(0f);
        linearLayout.setScaleY(0f);
        linearLayout.animate().alpha(1f).scaleX(1f).scaleY(1f).setInterpolator(CubicBezierInterpolator.EASE_OUT_QUINT).setDuration(350);
    }
}

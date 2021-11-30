package org.telegram.ui.Components.reaction;

import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.R;
import org.telegram.ui.Components.LayoutHelper;

public class ReactionsFactory {
    public static ChooseReactionLayout createChooseReactionLayout(LinearLayout parent) {
        ChooseReactionLayout view = new ChooseReactionLayout(parent.getContext());
        parent.addView(view, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 0, -16));
        return view;
    }

    public static ReactionsCounterView createReactionsCounterView(ViewGroup parent, MessageObject selectedObject) {
        ReactionsCounterView reactionsCounterView = new ReactionsCounterView(parent.getContext(), selectedObject);
        parent.addView(reactionsCounterView);

        View gap = new View(parent.getContext());
        gap.setBackgroundColor(Color.GRAY);
        gap.setMinimumWidth(AndroidUtilities.dp(196));
        gap.setTag(1001);
        gap.setTag(R.id.object_tag, 1);
        parent.addView(gap);
        LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) reactionsCounterView.getLayoutParams();
        if (LocaleController.isRTL) layoutParams.gravity = Gravity.RIGHT;
        layoutParams.width = LayoutHelper.MATCH_PARENT;
        layoutParams.height = AndroidUtilities.dp(6);
        gap.setLayoutParams(layoutParams);

        reactionsCounterView.setOnClickListener(v -> {

        });
        return reactionsCounterView;
    }
}

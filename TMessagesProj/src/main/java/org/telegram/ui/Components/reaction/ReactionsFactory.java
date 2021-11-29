package org.telegram.ui.Components.reaction;

import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import org.telegram.messenger.MessageObject;
import org.telegram.ui.Components.LayoutHelper;

public class ReactionsFactory {
    public static ChooseReactionLayout createChooseReactionLayout(LinearLayout parent) {
        ChooseReactionLayout view = new ChooseReactionLayout(parent.getContext());
        parent.addView(view, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 0, -16));
        return view;
    }

    public static ReactionsCounterView createReactionsCounterView(ViewGroup parent, MessageObject selectedObject, boolean withSeen) {
        ReactionsCounterView reactionsCounterView = new ReactionsCounterView(parent.getContext(), selectedObject);
        if (withSeen) {
            parent.addView(reactionsCounterView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 56));
        } else {
            parent.addView(reactionsCounterView);
        }
        reactionsCounterView.setOnClickListener(v -> {

        });
        return reactionsCounterView;
    }
}

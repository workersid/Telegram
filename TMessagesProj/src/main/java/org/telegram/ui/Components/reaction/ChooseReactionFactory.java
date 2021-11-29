package org.telegram.ui.Components.reaction;

import android.widget.LinearLayout;

import org.telegram.ui.Components.LayoutHelper;

public class ChooseReactionFactory {
    public static ChooseReactionLayout createView(LinearLayout parent) {
        ChooseReactionLayout view = new ChooseReactionLayout(parent.getContext());
        parent.addView(view, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 0, -16));
        return view;
    }
}

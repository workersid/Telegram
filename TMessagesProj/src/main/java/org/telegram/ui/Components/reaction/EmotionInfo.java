package org.telegram.ui.Components.reaction;

import android.graphics.RectF;

import org.telegram.tgnet.TLRPC;

import java.util.ArrayList;
import java.util.List;

public class EmotionInfo {
    public List<Long> lastThreeUsers = new ArrayList<>();
    public boolean isSelectedByCurrentUser;
    public int count;
    public TLRPC.Document staticIcon;
    public TLRPC.Document selectIcon;
    public String reaction;
    public int messageId;
    public long dialogId;
    public RectF drawRegion = new RectF();
}

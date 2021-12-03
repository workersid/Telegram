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

    public EmotionInfo copy() {
        EmotionInfo emotionInfo = new EmotionInfo();
        emotionInfo.drawRegion = drawRegion;
        emotionInfo.dialogId = dialogId;
        emotionInfo.messageId = messageId;
        emotionInfo.reaction = reaction;
        emotionInfo.selectIcon = selectIcon;
        emotionInfo.staticIcon = staticIcon;
        emotionInfo.count = count;
        emotionInfo.isSelectedByCurrentUser = isSelectedByCurrentUser;
        emotionInfo.lastThreeUsers = lastThreeUsers;
        return emotionInfo;
    }
}

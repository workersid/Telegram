package org.telegram.ui.Components.reaction;

import org.telegram.messenger.MediaDataController;
import org.telegram.messenger.MessageObject;
import org.telegram.tgnet.TLRPC;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class EmotionUtils {
    public static int extractTotalReactions(MessageObject selectedObject, String reaction) {
        if (selectedObject.messageOwner.reactions != null && !selectedObject.messageOwner.reactions.results.isEmpty()) {
            if (reaction == null) {
                int counter = 0;
                for (TLRPC.TL_reactionCount result : selectedObject.messageOwner.reactions.results) {
                    counter += result.count;
                }
                return counter;
            } else {
                for (TLRPC.TL_reactionCount result : selectedObject.messageOwner.reactions.results) {
                    if (result.reaction.equals(reaction)) {
                        return result.count;
                    }
                }
            }
        }
        return 0;
    }

    public static List<EmotionInfo> extractEmotionInfoList(MessageObject selectedObject, MediaDataController mediaDataController) {
        List<EmotionInfo> emotionInfoList = new ArrayList<>();
        HashMap<String, EmotionInfo> emotionInfoMap = new HashMap<>();

        EmotionInfo mainEmotionInfo = new EmotionInfo();
        mainEmotionInfo.dialogId = selectedObject.getDialogId();
        mainEmotionInfo.messageId = selectedObject.getId();
        mainEmotionInfo.count = extractTotalReactions(selectedObject, null);
        emotionInfoList.add(mainEmotionInfo);

        if (selectedObject.messageOwner.reactions != null && !selectedObject.messageOwner.reactions.results.isEmpty()) {
            for (TLRPC.TL_reactionCount result : selectedObject.messageOwner.reactions.results) {
                EmotionInfo emotionInfo = new EmotionInfo();
                emotionInfo.dialogId = selectedObject.getDialogId();
                emotionInfo.messageId = selectedObject.getId();
                TLRPC.TL_availableReaction tlAvailableReaction = mediaDataController.getAvailableReactionByName(result.reaction);
                if (tlAvailableReaction != null) {
                    emotionInfo.staticIcon = tlAvailableReaction.static_icon;
                }
                emotionInfo.count = result.count;
                emotionInfo.isSelectedByCurrentUser = result.chosen;
                emotionInfo.reaction = result.reaction;
                emotionInfoList.add(emotionInfo);
                emotionInfoMap.put(result.reaction, emotionInfo);
            }

            for (TLRPC.TL_messageUserReaction tlMessageUserReaction : selectedObject.messageOwner.reactions.recent_reactons) {
                if (emotionInfoMap.containsKey(tlMessageUserReaction.reaction)) {
                    EmotionInfo emotionInfo = emotionInfoMap.get(tlMessageUserReaction.reaction);
                    if (emotionInfo != null && emotionInfo.lastThreeUsers.size() < 3) {
                        emotionInfo.lastThreeUsers.add(tlMessageUserReaction.user_id);
                    }
                }
            }
        }

        return emotionInfoList;
    }
}

package org.telegram.ui.Components.reaction;

import static org.telegram.messenger.ChatObject.ACTION_VIEW;
import static org.telegram.ui.ChatActivity.MODE_SCHEDULED;

import org.telegram.messenger.ChatObject;
import org.telegram.messenger.DialogObject;
import org.telegram.messenger.MediaDataController;
import org.telegram.messenger.MessageObject;
import org.telegram.tgnet.TLRPC;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

public class EmotionUtils {

    public static boolean canShowChooseReactionDialog(MessageObject object, MessageObject.GroupedMessages groupedMessages, int chatMode, TLRPC.Chat chat) {
        if (object == null) return false;
        object = getMessageObjectForReactions(object, groupedMessages);

        if (!object.isSent() || object.isSponsored() || chatMode == MODE_SCHEDULED) //todo MODE_PINNED??
            return false;

        /*if (chat != null && !ChatObject.canUserDoAction(chat, ACTION_VIEW)) {
            return false;
        }*/

        return true;
    }

    public static MessageObject getMessageObjectForReactions(MessageObject object, MessageObject.GroupedMessages groupedMessages) {
        if (groupedMessages != null && groupedMessages.messages.size() > 0) {
            object = groupedMessages.messages.get(0);
        }
        return object;
    }

    public static boolean hasReactions(MessageObject object, MessageObject.GroupedMessages groupedMessages) {
        if (object == null) return false;
        return getMessageObjectForReactions(object, groupedMessages).hasReactions();
    }

    public static boolean hasReactionsAndNotChannelAndNotUserDialog(MessageObject object, MessageObject.GroupedMessages groupedMessages, TLRPC.Chat chat, int chatMode) {
        if (object == null) return false;
        return /*object.getId() > 0*/ object.isSent() //отправлено
                && !object.isSponsored()
                && getMessageObjectForReactions(object, groupedMessages).hasReactions()
                && !ChatObject.isChannelAndNotMegaGroup(chat)
                && !DialogObject.isUserDialog(object.getDialogId())
                && chatMode != MODE_SCHEDULED;//todo MODE_PINNED??
    }

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

    public static boolean isMoreThanTenReactionsWithDifferentTypes(MessageObject selectedObject) {
        if (selectedObject == null) return false;
        int counter = 0;
        HashSet<String> types = new HashSet<>(20);
        if (selectedObject.messageOwner.reactions != null && !selectedObject.messageOwner.reactions.results.isEmpty()) {
            for (TLRPC.TL_reactionCount result : selectedObject.messageOwner.reactions.results) {
                counter += result.count;
                types.add(result.reaction);
            }
        }
        return counter > 10 && types.size() > 1;
    }

    public static List<EmotionInfo> extractEmotionInfoList(MessageObject selectedObject, MediaDataController mediaDataController, boolean excludeMain) {
        List<EmotionInfo> emotionInfoList = new ArrayList<>();
        HashMap<String, EmotionInfo> emotionInfoMap = new HashMap<>();

        if (!excludeMain) {
            EmotionInfo mainEmotionInfo = new EmotionInfo();
            mainEmotionInfo.dialogId = selectedObject.getDialogId();
            mainEmotionInfo.messageId = selectedObject.getId();
            mainEmotionInfo.count = extractTotalReactions(selectedObject, null);
            emotionInfoList.add(mainEmotionInfo);
        }

        if (selectedObject.messageOwner.reactions != null && !selectedObject.messageOwner.reactions.results.isEmpty()) {
            for (TLRPC.TL_reactionCount result : selectedObject.messageOwner.reactions.results) {
                EmotionInfo emotionInfo = new EmotionInfo();
                emotionInfo.dialogId = selectedObject.getDialogId();
                emotionInfo.messageId = selectedObject.getId();
                TLRPC.TL_availableReaction tlAvailableReaction = mediaDataController.getAvailableReactionByName(result.reaction);
                if (tlAvailableReaction != null) {
                    emotionInfo.staticIcon = tlAvailableReaction.static_icon;
                    emotionInfo.selectIcon = tlAvailableReaction.select_animation;
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

        Collections.sort(emotionInfoList, (o1, o2) -> Integer.compare(o2.count, o1.count));

        return emotionInfoList;
    }
}

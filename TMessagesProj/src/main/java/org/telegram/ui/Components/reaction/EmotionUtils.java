package org.telegram.ui.Components.reaction;

import org.telegram.messenger.MessageObject;
import org.telegram.tgnet.TLRPC;

import java.util.List;

public class EmotionUtils {
    public static int extractTotalReactions(MessageObject selectedObject) {
        if (selectedObject.messageOwner.reactions != null && !selectedObject.messageOwner.reactions.results.isEmpty()) {
            int counter = 0;
            for (TLRPC.TL_reactionCount result : selectedObject.messageOwner.reactions.results) {
                counter += result.count;
            }
            return counter;
        }
        return 0;
    }

    /*public static List<Integer> extractReactionsInfo(MessageObject selectedObject) {

    }*/
}

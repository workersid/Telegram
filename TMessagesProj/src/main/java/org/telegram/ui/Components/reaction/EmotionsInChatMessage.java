package org.telegram.ui.Components.reaction;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.View;

import com.google.android.exoplayer2.util.Log;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ImageReceiver;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.AnimatedNumberLayout;
import org.telegram.ui.Components.AvatarDrawable;

public class EmotionsInChatMessage {

    private ImageReceiver[] avatarImages;
    private AvatarDrawable[] avatarDrawables;
    private boolean[] avatarImagesVisible;
    private boolean isInitialized;
    private AnimatedNumberLayout numberLayout;
    private TLRPC.TL_messageReactions reactions;
    private Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

    //1
    public void createForView(View view) {
        if (!isInitialized) {
            avatarImages = new ImageReceiver[3];
            avatarDrawables = new AvatarDrawable[3];
            avatarImagesVisible = new boolean[3];
            for (int a = 0; a < avatarImages.length; a++) {
                avatarImages[a] = new ImageReceiver(view);
                avatarImages[a].setRoundRadius(AndroidUtilities.dp(12));
                avatarDrawables[a] = new AvatarDrawable();
                avatarDrawables[a].setTextSize(AndroidUtilities.dp(8));
            }
            numberLayout = new AnimatedNumberLayout(view, Theme.chat_replyNamePaint);
            paint.setColor(Color.BLUE);
            isInitialized = true;
        }
    }

    //2
    public void setReactions(TLRPC.TL_messageReactions reactions) {
        this.reactions = reactions;
        //numberLayout.setNumber(commentCount, false);
    }

    //3
    public void calculateSpaceSize(int parentMaxWidth) {
        //если место мало и сообщение является текстом, то расширям чтобы влезла одна эмоция
    }

    //3
    public int getMinimumSpaceWidth() {
        return 0;//AndroidUtilities.dp(120);
    }

    //4
    public int getSpaceHeight() {
        return 0;//AndroidUtilities.dp(48);
    }

    //5
    public void onLayout(int width, int height) {
       // Log.e("onLayout","onLayout=" + width + "==" + height);
        /*private int commentNumberWidth;*/
        //getBackgroundDrawableLeft()
        //getGroupPhotosWidth()
         /*drawTimeX;
        titleX;
        nameX;
        commentAvatarDrawables*/
    }

    //6
    public void onDraw(Canvas canvas, int startX, int startY, int width) {
        //Log.e("cc","v=" + startX+ "=" + startY);

       canvas.save();
        canvas.translate(startX, startY);
        canvas.drawRect(0,0,width,getSpaceHeight(),paint);
        canvas.restore();
        /*if (commentAvatarImages != null) {
            int toAdd = AndroidUtilities.dp(17);
            int ax = x + getExtraTextX();
            for (int a = commentAvatarImages.length - 1; a >= 0; a--) {
                if (!commentAvatarImagesVisible[a] || !commentAvatarImages[a].hasImageSet()) {
                    continue;
                }
                commentAvatarImages[a].setImageX(ax + toAdd * a);
                commentAvatarImages[a].setImageY(y - AndroidUtilities.dp(4) + (pinnedBottom ? AndroidUtilities.dp(2) : 0));
                if (a != commentAvatarImages.length - 1) {
                    canvas.drawCircle(commentAvatarImages[a].getCenterX(), commentAvatarImages[a].getCenterY(), AndroidUtilities.dp(13), currentBackgroundDrawable.getPaint());
                }
                commentAvatarImages[a].draw(canvas);
                drawnAvatars = true;
                if (a != 0) {
                    avatarsOffset += 17;
                }
            }
        }*/
    }

    //7
    public boolean onTouchDown() {
        return true;
    }
}

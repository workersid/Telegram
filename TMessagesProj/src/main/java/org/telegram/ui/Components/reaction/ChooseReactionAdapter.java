package org.telegram.ui.Components.reaction;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.Components.RecyclerListView;

import java.util.ArrayList;
import java.util.List;

public class ChooseReactionAdapter extends RecyclerListView.SelectionAdapter {

    private final Context context;
    private final List<TLRPC.TL_availableReaction> cache = new ArrayList<>();

    public ChooseReactionAdapter(Context context) {
        this.context = context;
    }

    @SuppressLint("NotifyDataSetChanged")
    public void setData(List<TLRPC.TL_availableReaction> data) {
        cache.clear();
        cache.addAll(data);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = new ReactionStickerCell(context);
        view.setLayoutParams(new RecyclerView.LayoutParams(AndroidUtilities.dp(44), AndroidUtilities.dp(44)));
        return new RecyclerListView.Holder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        TLRPC.TL_availableReaction tlReaction = cache.get(position);
        ReactionStickerCell cell = (ReactionStickerCell) holder.itemView;
        cell.setSticker(tlReaction.select_animation, tlReaction);
    }

    @Override
    public int getItemCount() {
        return cache.size();
    }

    @Override
    public boolean isEnabled(RecyclerView.ViewHolder holder) {
        return false;
    }

    public TLRPC.TL_availableReaction getItem(int i) {
        return cache.get(i);
    }
}

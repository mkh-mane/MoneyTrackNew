package com.example.moneytrack;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class BalancePagerAdapter extends RecyclerView.Adapter<BalancePagerAdapter.ViewHolder> {
    private List<BalanceItem> list;
    private Context context;

    public BalancePagerAdapter(
            List<BalanceItem> list,
            Context context) {
        this.list = list;
        this.context = context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType) {

        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.balance_item,
                        parent,
                        false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(
            @NonNull ViewHolder holder,
            int position) {
        BalanceItem item = list.get(position);
        holder.tvTitle.setText(item.title);
        if(item.isAddButton){
            holder.tvAmount.setText("+");
            holder.itemView.setOnClickListener(v -> {
                if(context instanceof MainActivity){
                    ((MainActivity) context).showAddAccountDialog();
                }
            });
        }
        else{
            holder.tvAmount.setText(item.amount);
            holder.itemView.setOnClickListener(null);
        }
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle;
        TextView tvAmount;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvAmount = itemView.findViewById(R.id.tvAmount);
        }
    }
}
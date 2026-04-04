package com.example.moneytrack;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.moneytrack.data.db.TransactionEntity;

import java.util.List;

public class TransactionAdapter extends RecyclerView.Adapter<TransactionAdapter.ViewHolder> {

    private List<TransactionEntity> list;
    private OnTransactionClickListener listener;

    public TransactionAdapter(List<TransactionEntity> list, OnTransactionClickListener listener) {
        this.list = list;
        this.listener = listener;
    }

    public void setData(List<TransactionEntity> newList) {
        this.list = newList;
        notifyDataSetChanged();
    }

    public interface OnTransactionClickListener {
        void onTransactionClick(TransactionEntity transaction);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_transaction, parent, false);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {

        TransactionEntity transaction = list.get(position);

        holder.tvType.setText(transaction.type);
        holder.tvCategory.setText(transaction.category);
        holder.tvAmount.setText(String.valueOf(transaction.amount));

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onTransactionClick(transaction);
            }
        });
    }

    @Override
    public int getItemCount() {
        return list == null ? 0 : list.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        TextView tvType, tvAmount, tvCategory;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            tvType = itemView.findViewById(R.id.tvType);
            tvAmount = itemView.findViewById(R.id.tvAmount);
            tvCategory = itemView.findViewById(R.id.tvCategory);
        }
    }
}
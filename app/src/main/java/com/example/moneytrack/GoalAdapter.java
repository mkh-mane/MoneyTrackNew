package com.example.moneytrack;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.example.moneytrack.data.db.GoalEntity;

import java.util.List;

public class GoalAdapter extends RecyclerView.Adapter<GoalAdapter.GoalViewHolder> {

    private List<GoalEntity> goals;
    private OnGoalClickListener listener;

    public interface OnGoalClickListener {
        void onGoalClick(GoalEntity goal);
    }

    public GoalAdapter(List<GoalEntity> goals, OnGoalClickListener listener) {
        this.goals = goals;
        this.listener = listener;
    }

    @Override
    public GoalViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_goal, parent, false);

        return new GoalViewHolder(view);
    }

    @Override
    public void onBindViewHolder(GoalViewHolder holder, int position) {

        GoalEntity goal = goals.get(position);

        holder.goalName.setText(goal.name);

        double saved = goal.savedAmount;
        double target = goal.targetAmount;

        int percent = 0;
        if (target > 0) {
            percent = (int) ((saved / target) * 100);
        }

        holder.goalAmount.setText("$" + saved + " / $" + target);
        holder.goalPercent.setText(percent + "%");
        holder.goalProgress.setProgress(percent);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onGoalClick(goal);
            }
        });
    }

    @Override
    public int getItemCount() {
        return goals.size();
    }

    public void setGoals(List<GoalEntity> goals) {
        this.goals = goals;
        notifyDataSetChanged();
    }

    public static class GoalViewHolder extends RecyclerView.ViewHolder {

        TextView goalName;
        TextView goalAmount;
        TextView goalPercent;
        ProgressBar goalProgress;

        public GoalViewHolder(View itemView) {
            super(itemView);

            goalName = itemView.findViewById(R.id.goalName);
            goalAmount = itemView.findViewById(R.id.goalAmount);
            goalPercent = itemView.findViewById(R.id.goalPercent);
            goalProgress = itemView.findViewById(R.id.goalProgress);
        }
    }
    public GoalEntity getGoalAt(int position) {
        return goals.get(position);
    }

}

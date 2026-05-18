package com.example.moneytrack;

public class BalanceItem {

    public String title;
    public String amount;
    public boolean isAddButton;

    public BalanceItem(String title,
                       String amount,
                       boolean isAddButton) {

        this.title = title;
        this.amount = amount;
        this.isAddButton = isAddButton;
    }
}

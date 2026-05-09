package com.example.moneytrack;

public class CurrencyUtils {
    public static double amdPerUsd = 390;
    public static double convert(double amount,
                                 String currency) {
        switch (currency) {
            case "USD $":
                return amount / amdPerUsd;
            case "EUR €":
                return (amount / amdPerUsd) * 0.92;
            case "RUB ₽":
                return (amount / amdPerUsd) * 89;
            default:
                return amount;
        }
    }
}
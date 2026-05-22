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

    public static double convertBetweenAccounts(
            double amount,
            String fromCurrency,
            String toCurrency){
        double amd = convertToAMDStatic(
                        amount,
                        fromCurrency
                );

        return convert(amd, toCurrency);
    }

    private static double convertToAMDStatic(double amount, String currency){
        if(currency.equals("USD $")){
            return amount * 385;
        }
        if(currency.equals("EUR €")){
            return amount * 430;
        }
        if(currency.equals("RUB ₽")){
            return amount * 4.8;
        }
        return amount;
    }
}
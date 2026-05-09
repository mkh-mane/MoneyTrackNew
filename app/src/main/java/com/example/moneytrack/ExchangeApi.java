package com.example.moneytrack;

import retrofit2.Call;
import retrofit2.http.GET;

public interface ExchangeApi {
    @GET("latest?base=USD&symbols=AMD")
    Call<ExchangeResponse> getRates();
}
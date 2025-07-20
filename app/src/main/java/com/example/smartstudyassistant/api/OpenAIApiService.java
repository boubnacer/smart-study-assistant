package com.example.smartstudyassistant.api;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.POST;

public interface OpenAIApiService {
    @POST("v1/chat/completions")
    Call<OpenAIResponse> getCompletion(
            @Header("Authorization") String authorization,
            @Body OpenAIRequest request
    );
}
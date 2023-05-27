package com.example.monitoringlocation;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;

import java.util.List;

public interface LocationAPI {
    @Headers("Content-Type: application/json")
    @POST("location/insert")
    Call<Void> sendLocation(@Body LocationData locationData);

    @Headers("Content-Type: application/json")
    @POST("location/insertList")
    Call<Void> sendListLocation(@Body List<LocationData> list);
}
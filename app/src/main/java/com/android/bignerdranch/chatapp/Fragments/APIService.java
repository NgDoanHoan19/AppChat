package com.android.bignerdranch.chatapp.Fragments;




import com.android.bignerdranch.chatapp.Notifications.MyResponse;
import com.android.bignerdranch.chatapp.Notifications.Sender;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;

public interface APIService {
    @Headers(
            {
                    "Content-Type:application/json",
                    "Authorization:key=AAAAGvxtl9Q:APA91bHnlvTkmqDcpePQT9fkSpHNA-HBtyD3SJwS8iJVeCJ45ZsYI6mfp963C3uZlTerKLEAR6pvG9fdsiwEbN0ojhrz5MSic0WuwMUjbn53YlizIeRQU4kGEBjbd9H5qI_dBEofb_K6"
            }
    )

    @POST("fcm/send")
    Call<MyResponse> sendNotification(@Body Sender body);
}

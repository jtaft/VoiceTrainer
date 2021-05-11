package com.chattycheese.voicetrainer;

import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;

public interface TrainingDataService {
    @GET("trainingtext")
    Call<ResponseBody> getTrainingText();

    @Multipart
    @POST("audio")
    Call<ResponseBody> submitAudio(@Part("file\"; filename=\"test.mp4\" ") RequestBody file,
                                   @Part("audio-txt") RequestBody audioTxt);
}

package com.purpletea.cameratoraw.network;

import android.os.Handler;

import com.google.gson.annotations.SerializedName;

public class ResponseModel {

    @SerializedName("status")
    String status;
    @SerializedName("message")
    String message;
    @SerializedName("url")
    String url;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getUrl() {
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {  }
        }, 30000);
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}

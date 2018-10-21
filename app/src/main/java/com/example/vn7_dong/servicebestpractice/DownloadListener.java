package com.example.vn7_dong.servicebestpractice;

public interface DownloadListener  {
    void onProgress(int progerss);
    void onSuccess();
    void onFailed();
    void onPaused();
    void onCanceled();
}

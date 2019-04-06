package org.luyinbros.appupdate;

public interface OnDownloadApkListener<T extends AppUpdateInfo> {

    void onProgress(int progress);

    void onSuccess(DownloadApkInfo<T> info);

    void onFailure(Exception e);
}
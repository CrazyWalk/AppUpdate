package org.luyinbros.appupdate;

public interface AppUpdateDelegate<T extends AppUpdateInfo> {

    AppUpdateSession<T> openSession();

    boolean isDownloadingApk();

    void registerDownloadApkListener(OnDownloadApkListener<T> listener);

    void unregisterDownloadApkListener(OnDownloadApkListener<T> listener);


}

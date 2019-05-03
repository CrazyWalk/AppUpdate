package org.luyinbros.appupdate;

import android.content.Intent;

public interface AppUpdateDelegate<T extends AppUpdateInfo> {
    AppUpdateSession<T> openSession();

    boolean isDownloadingApk();

    void registerDownloadApkListener(OnDownloadApkListener<T> listener);

    void unregisterDownloadApkListener(OnDownloadApkListener<T> listener);

    boolean isAllowInstall();

    Intent getRequestInstallPermissionIntent();

    void install(T info);

    void startUpdateInBackground(final T updateInfo);

    void showUpdateDialog(T updateInfo);

}

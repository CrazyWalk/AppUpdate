package org.luyinbros.appupdate;

import android.content.Intent;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.io.File;

public interface ApkManager<T extends AppUpdateInfo> {

    @Nullable
    File getNewestApkFile();

    void installNewestApk();

    @Nullable
    File getApkFile(@NonNull T info);

    @Nullable
    Uri getUriForFile(@Nullable File file);

    void downloadApk(@NonNull T info);

    void installApk(@NonNull T info);

    @Nullable
    Intent getInstallApkIntent(@Nullable File file);

    boolean isDownloading();

    void registerDownloadApkListener(@Nullable OnDownloadApkListener<T> listener);

    void unregisterDownloadApkListener(@Nullable OnDownloadApkListener<T> listener);
}

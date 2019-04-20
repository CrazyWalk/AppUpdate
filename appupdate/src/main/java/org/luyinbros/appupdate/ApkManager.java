package org.luyinbros.appupdate;

import android.app.Activity;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.io.File;

public interface ApkManager<T extends AppUpdateInfo> {

    void requestInstallPermission(@NonNull Activity activity, int requestCode);

    boolean hasInstallPermission();

    @Nullable
    File getNewestApkFile();

    @Nullable
    File getApkFile(@NonNull T info);

    @Nullable
    Uri getUriForFile(@Nullable File file);

    void downloadApk(@NonNull T info);

    void installApk();

    boolean isDownloading();

    void registerDownloadApkListener(@Nullable OnDownloadApkListener<T> listener);

    void unregisterDownloadApkListener(@Nullable OnDownloadApkListener<T> listener);
}

package org.luyinbros.appupdate;

import android.support.annotation.NonNull;

public interface OnDownloadApkListener<T extends AppUpdateInfo> {

    void onProgress(int progress);

    void onSuccess(@NonNull ApkManager<T> apkManager,T appUpdateInfo);

    void onFailure(@NonNull Exception e);
}

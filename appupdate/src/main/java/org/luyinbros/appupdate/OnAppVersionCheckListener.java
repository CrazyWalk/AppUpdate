package org.luyinbros.appupdate;

import android.support.annotation.NonNull;

public interface OnAppVersionCheckListener<T extends AppUpdateInfo> {

    void onStart();

    void onSuccess(@NonNull T data);

    void onFailure(@NonNull Throwable e);
}

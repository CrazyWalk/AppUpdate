package org.luyinbros.appupdate;

import android.support.annotation.NonNull;

public interface AppUpdateSession<T extends AppUpdateInfo> {

    boolean isCheckingVersion();

    void checkVersion(@NonNull OnAppVersionCheckListener<T> listener);

    void destroy();
}

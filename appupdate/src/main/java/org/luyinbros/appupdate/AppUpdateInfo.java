package org.luyinbros.appupdate;

import android.os.Parcelable;

public interface AppUpdateInfo extends Parcelable {

    boolean isUpdate();

    boolean isForceUpdate();

    String getApkUrl();

    String getNewestVersion();

}

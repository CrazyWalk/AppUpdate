package org.luyinbros.appupdate;

public interface AppUpdateInfo  {

    boolean isUpdate();

    boolean isForceUpdate();

    String getApkUrl();

}

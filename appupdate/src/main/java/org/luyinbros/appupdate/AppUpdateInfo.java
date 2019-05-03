package org.luyinbros.appupdate;

import android.os.Parcelable;
import android.support.annotation.NonNull;

/**
 * App更新所需的模型
 */
public interface AppUpdateInfo extends Parcelable {

    /**
     * APP 是否更新
     *
     * @return true 有新的更新 false 没有新的更新
     */
    boolean isUpdate();

    /**
     * 是否强制更新
     *
     * @return true强制更新 false 没有强制更新
     */
    boolean isForceUpdate();

    /**
     * 获取app apk链接
     *
     * @return apk链接
     */
    @NonNull
    String getApkUrl();

}

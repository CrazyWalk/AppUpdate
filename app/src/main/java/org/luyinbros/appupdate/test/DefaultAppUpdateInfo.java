package org.luyinbros.appupdate.test;

import android.os.Parcel;
import android.os.Parcelable;

import org.luyinbros.appupdate.AppUpdateInfo;

public class DefaultAppUpdateInfo implements AppUpdateInfo, Parcelable {
    private String apkUrl;
    private String minVersion;
    private String newestVersion;
    private String updateDescription;
    private String currentVersion;
    private boolean isUpdate = false;

    public DefaultAppUpdateInfo() {

    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.apkUrl);
        dest.writeString(this.minVersion);
        dest.writeString(this.newestVersion);
        dest.writeString(this.updateDescription);
        dest.writeString(this.currentVersion);
        dest.writeInt(this.isUpdate ? 0 : 1);
    }


    protected DefaultAppUpdateInfo(Parcel in) {
        this.apkUrl = in.readString();
        this.minVersion = in.readString();
        this.newestVersion = in.readString();
        this.updateDescription = in.readString();
        this.currentVersion = in.readString();
        this.isUpdate = in.readInt() == 1;
    }

    public static final Creator<DefaultAppUpdateInfo> CREATOR = new Creator<DefaultAppUpdateInfo>() {
        @Override
        public DefaultAppUpdateInfo createFromParcel(Parcel source) {
            return new DefaultAppUpdateInfo(source);
        }

        @Override
        public DefaultAppUpdateInfo[] newArray(int size) {
            return new DefaultAppUpdateInfo[size];
        }
    };

    public boolean isUpdate() {
        return isUpdate;
    }

    public boolean isForceUpdate() {
        return false;
    }

    public String getUpdateDescription() {
        return updateDescription;
    }

    public String getNewestVersion() {
        return newestVersion;
    }

    public String getApkUrl() {
        return apkUrl;
    }
}

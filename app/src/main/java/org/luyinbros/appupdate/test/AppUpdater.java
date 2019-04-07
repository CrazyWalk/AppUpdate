package org.luyinbros.appupdate.test;

import android.app.Application;
import android.content.Context;
import android.os.Bundle;
import android.os.Parcel;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentManager;

import org.luyinbros.appupdate.AppUpdateDelegate;
import org.luyinbros.appupdate.AppUpdateInfo;
import org.luyinbros.appupdate.AppUpdateSession;
import org.luyinbros.appupdate.DownloadApkInfo;
import org.luyinbros.appupdate.OnDownloadApkListener;

import java.io.File;

import io.reactivex.Observable;

public class AppUpdater {
    private static volatile AppUpdater mInstance;
    private Application application;
    private AppUpdateDelegate<DefaultAppUpdateInfo, DefaultDownloadApkInfo> mDelegate;

    private AppUpdater(Application application) {
        this.application = application;
        mDelegate = new AppUpdateDelegate<DefaultAppUpdateInfo, DefaultDownloadApkInfo>(application) {
            @Override
            public DownloadApkInfo<DefaultAppUpdateInfo> createDownloadApkInfo(DefaultAppUpdateInfo updateInfo, File apkFile) {
                return new DefaultDownloadApkInfo(updateInfo, apkFile);
            }

            @Override
            public AppUpdateSession<DefaultAppUpdateInfo> openSession() {
                return new RxAppUpdateSession<DefaultAppUpdateInfo>(mDelegate) {
                    @Override
                    public Observable<DefaultAppUpdateInfo> checkUpdateObservable() {
                        return Observable.just(new DefaultAppUpdateInfo());
                    }
                };
            }
        };
    }

    public static AppUpdater getInstance(Context context) {
        if (mInstance == null) {
            synchronized (OriginalAppUpdater.class) {
                if (mInstance == null) {
                    mInstance = new AppUpdater((Application) context.getApplicationContext());
                }
            }
        }
        return mInstance;
    }

    public boolean isDownloadingApk() {
        return mDelegate.isDownloadingApk();
    }

    public AppUpdateSession<DefaultAppUpdateInfo> openSession() {
        return mDelegate.openSession();
    }

    public void registerDownloadApkListener(OnDownloadApkListener<DefaultAppUpdateInfo> listener) {
        mDelegate.registerDownloadApkListener(listener);
    }

    public void unregisterDownloadApkListener(OnDownloadApkListener<DefaultAppUpdateInfo> listener) {
        mDelegate.unregisterDownloadApkListener(listener);
    }

    public void install(DefaultAppUpdateInfo appUpdateInfo) {
        mDelegate.install(appUpdateInfo);

    }

    public void startUpdateInBackground(final DefaultAppUpdateInfo updateInfo) {
        mDelegate.startUpdateInBackground(updateInfo);
    }

    public void showUpdateDialog(@Nullable DefaultAppUpdateInfo appUpdateInfo,
                                 FragmentManager fragmentManager) {
        if (appUpdateInfo != null && appUpdateInfo.isUpdate()) {
            Bundle bundle = new Bundle();
            bundle.putParcelable("appUpdateInfo", appUpdateInfo);
            // DialogFactory.showDialog(new AppUpdateDialog(), fragmentManager, bundle, "AppUpdateDialog");
        }

    }

    public static class DefaultAppUpdateInfo implements AppUpdateInfo {
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

        @Override
        public boolean isUpdate() {
            return isUpdate;
        }

        @Override
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

    public static class DefaultDownloadApkInfo implements DownloadApkInfo<DefaultAppUpdateInfo> {
        private DefaultAppUpdateInfo defaultAppUpdateInfo;
        private File file;

        public DefaultDownloadApkInfo(DefaultAppUpdateInfo defaultAppUpdateInfo, File file) {
            this.defaultAppUpdateInfo = defaultAppUpdateInfo;
            this.file = file;
        }

        @Override
        public DefaultAppUpdateInfo getUpdateInfo() {
            return defaultAppUpdateInfo;
        }

        @Override
        public File getApkFile() {
            return file;
        }
    }
}

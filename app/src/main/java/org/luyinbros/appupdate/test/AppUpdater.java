package org.luyinbros.appupdate.test;

import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.FileProvider;
import android.text.TextUtils;

import org.luyinbros.appupdate.AppUpdateDelegate;
import org.luyinbros.appupdate.AppUpdateSession;
import org.luyinbros.appupdate.DefaultAppUpdateDelegate;
import org.luyinbros.appupdate.OnDownloadApkListener;

import java.io.File;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;

public class AppUpdater implements AppUpdateDelegate<AppUpdater.AppUpdateInfo> {
    private volatile static AppUpdater mInstance;
    private Context mContext;
    private AppUpdateDelegate mDelegate;

    private AppUpdater(Context context) {
        this.mContext = context.getApplicationContext();
        mDelegate = new AppUpdateDelegate(context);
    }


    public static AppUpdater getInstance(Context context) {
        if (mInstance == null) {
            synchronized (AppUpdater.class) {
                if (mInstance == null) {
                    mInstance = new AppUpdater(context);
                }
            }
        }
        return mInstance;
    }

    @Override
    public AppUpdateSession<AppUpdateInfo> openSession() {
        return mDelegate.openSession();
    }

    @Override
    public boolean isDownloadingApk() {
        return mDelegate.isDownloadingApk();
    }

    @Override
    public void registerDownloadApkListener(OnDownloadApkListener<AppUpdateInfo> listener) {
        mDelegate.registerDownloadApkListener(listener);
    }

    @Override
    public void unregisterDownloadApkListener(OnDownloadApkListener<AppUpdateInfo> listener) {
        mDelegate.unregisterDownloadApkListener(listener);
    }

    @Override
    public boolean isAllowInstall() {
        return mDelegate.isAllowInstall();
    }

    @Override
    public Intent getRequestInstallPermissionIntent() {
        return mDelegate.getRequestInstallPermissionIntent();
    }

    @Override
    public void install(AppUpdateInfo info) {
        mDelegate.install(info);
    }

    @Override
    public void showUpdateDialog(AppUpdateInfo updateInfo) {
        mDelegate.showUpdateDialog(updateInfo);
    }

    @Override
    public void startUpdateInBackground(AppUpdateInfo updateInfo) {
        mDelegate.startUpdateInBackground(updateInfo);
    }

    public static class AppUpdateInfo implements org.luyinbros.appupdate.AppUpdateInfo, Parcelable {
        private String apkUrl;
        private String minVersion;
        private String newestVersion;
        private String updateDescription;
        private String currentVersion;
        private boolean isUpdate = false;

        public AppUpdateInfo() {
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


        protected AppUpdateInfo(Parcel in) {
            this.apkUrl = in.readString();
            this.minVersion = in.readString();
            this.newestVersion = in.readString();
            this.updateDescription = in.readString();
            this.currentVersion = in.readString();
            this.isUpdate = in.readInt() == 1;
        }

        public static final Creator<AppUpdateInfo> CREATOR = new Creator<AppUpdateInfo>() {
            @Override
            public AppUpdateInfo createFromParcel(Parcel source) {
                return new AppUpdateInfo(source);
            }

            @Override
            public AppUpdateInfo[] newArray(int size) {
                return new AppUpdateInfo[size];
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

    private static class AppUpdateDelegate extends DefaultAppUpdateDelegate<AppUpdateInfo> {

        private AppUpdateDelegate(Context context) {
            super(new ApkManager(context), context);
        }

        @Override
        public AppUpdateSession<AppUpdateInfo> openSession() {
            return new RxAppUpdateSession<AppUpdateInfo>() {
                @Override
                public Observable<AppUpdateInfo> checkUpdateObservable() {
                    return Observable.create(new ObservableOnSubscribe<AppUpdateInfo>() {
                        @Override
                        public void subscribe(ObservableEmitter<AppUpdateInfo> emitter) throws Exception {
                            AppUpdateInfo defaultAppUpdateInfo = new AppUpdateInfo();
                            defaultAppUpdateInfo.apkUrl = "http://mb-shared.oss-cn-hangzhou.aliyuncs.com/apk/musicbible_4.2.1.apk";
                            defaultAppUpdateInfo.minVersion = "";
                            defaultAppUpdateInfo.newestVersion = "1.0.1";
                            defaultAppUpdateInfo.updateDescription = "1.  首页布局调整\n2.  个人中心-短评 长评\n3.  消息中心-系统消息\n4.  修复了榜单详情描述的显示问题";
                            defaultAppUpdateInfo.currentVersion = "1.0.0";
                            if (TextUtils.isEmpty(defaultAppUpdateInfo.apkUrl)) {
                                defaultAppUpdateInfo.isUpdate = false;
                            } else {
                                String gVersion = defaultAppUpdateInfo.currentVersion;
                                {
                                    int index = gVersion.indexOf("-");
                                    if (index > -1) {
                                        gVersion = gVersion.substring(0, index);
                                    }
                                }
                                {
                                    if (gVersion.length() == defaultAppUpdateInfo.newestVersion.length()) {
                                        defaultAppUpdateInfo.isUpdate = gVersion.compareTo(defaultAppUpdateInfo.newestVersion) < 0;
                                    }
                                }
                            }
                        }
                    });
                }
            };
        }

        @Override
        public void showUpdateDialog(AppUpdateInfo updateInfo) {

        }
    }

    private static class ApkManager extends DefaultAppUpdateDelegate.DefaultApkManager<AppUpdateInfo> {
        private Context mContext;

        private ApkManager(Context context) {
            super(context);
            mContext = context;
        }

        @Nullable
        @Override
        public File getNewestApkFile() {
            return null;
        }

        @Nullable
        @Override
        public File getApkFile(@NonNull AppUpdateInfo info) {
            File downloadedApk = new File(mContext.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
                    targetSubFile(info));
            if (downloadedApk.isFile()) {
                return downloadedApk;
            }
            return null;
        }

        @Nullable
        @Override
        public Uri getUriForFile(@Nullable File file) {
            //获取当前app的包名
            if (file != null && file.isFile()) {
                String FPAuth = mContext.getPackageName() + ".fileprovider";
                Uri uri;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    uri = FileProvider.getUriForFile(mContext, FPAuth, file);
                } else {
                    uri = Uri.fromFile(file);
                }
                return uri;
            } else {
                return null;
            }
        }


        @Override
        protected void configDownloadRequest(@NonNull DownloadManager.Request request, AppUpdateInfo info) {
            request.setTitle("音乐圣经");
            request.setDescription("");
            request.setMimeType("application/vnd.android.package-archive");
            request.setAllowedOverRoaming(false);
            request.setVisibleInDownloadsUi(true);
            request.setDestinationInExternalFilesDir(mContext, Environment.DIRECTORY_DOWNLOADS, targetSubFile(info));
        }


        private String createApkFileName(AppUpdateInfo appUpdateInfo) {
            return "musicbible_" + appUpdateInfo.getNewestVersion() + ".apk";
        }

        private String targetSubFile(AppUpdateInfo appUpdateInfo) {
            return "musicbible" + File.separator + createApkFileName(appUpdateInfo);
        }
    }
}

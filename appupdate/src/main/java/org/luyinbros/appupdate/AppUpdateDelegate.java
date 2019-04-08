package org.luyinbros.appupdate;

import android.app.Application;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.support.v4.content.FileProvider;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;

public abstract class AppUpdateDelegate<UpdateInfo extends AppUpdateInfo,
        DownloadApkInfo extends org.luyinbros.appupdate.DownloadApkInfo<UpdateInfo>> {
    private ApkManager<UpdateInfo> mApkManager;
    private OnDownloadApkListener<UpdateInfo> mOnDownloadListener;
    private Application application;

    public AppUpdateDelegate(Application application) {
        this.application = application;
        mApkManager = getApkManager();
    }

    public abstract org.luyinbros.appupdate.DownloadApkInfo<UpdateInfo> createDownloadApkInfo(UpdateInfo updateInfo,
                                                                                              File apkFile);

    public ApkManager<UpdateInfo> getApkManager() {
        return new DefaultApkManager<>(application, this);
    }

    public abstract AppUpdateSession<UpdateInfo> openSession();

    public boolean isDownloadingApk() {
        return mApkManager.isDownloading();
    }

    public void registerDownloadApkListener(OnDownloadApkListener<UpdateInfo> listener) {
        mApkManager.registerDownloadApkListener(listener);
    }

    public void unregisterDownloadApkListener(OnDownloadApkListener<UpdateInfo> listener) {
        mApkManager.unregisterDownloadApkListener(listener);
    }

    public void install(UpdateInfo appUpdateInfo) {
        try {
            Intent startIntent = new Intent();
            startIntent.setAction(Intent.ACTION_VIEW);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                startIntent.setFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
            }
            startIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startIntent.setDataAndType(mApkManager.getUriForFile(appUpdateInfo), "application/vnd.android.package-archive");
            application.startActivity(startIntent);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void startUpdateInBackground(final UpdateInfo updateInfo) {
        File file = mApkManager.getDownloadApkFile(updateInfo);
        if (file != null) {
            install(updateInfo);
        } else {
            if (mOnDownloadListener == null) {
                mOnDownloadListener = new OnDownloadApkListener<UpdateInfo>() {
                    @Override
                    public void onProgress(int progress) {

                    }

                    @Override
                    public void onSuccess(org.luyinbros.appupdate.DownloadApkInfo<UpdateInfo> info) {
                        install(updateInfo);
                        unregisterDownloadApkListener(mOnDownloadListener);
                        mOnDownloadListener = null;
                    }

                    @Override
                    public void onFailure(Exception e) {
                        unregisterDownloadApkListener(mOnDownloadListener);
                        mOnDownloadListener = null;
                    }

                };
                registerDownloadApkListener(mOnDownloadListener);
                mApkManager.downloadApk(updateInfo);
            }

        }
    }

    public static class DefaultApkManager<UpdateInfo extends AppUpdateInfo,
            DownloadApkInfo extends org.luyinbros.appupdate.DownloadApkInfo<UpdateInfo>> implements ApkManager<UpdateInfo> {
        private Context mContext;
        private Long taskId;
        private DownloadManager mDownloadManager;
        private List<OnDownloadApkListener<UpdateInfo>> onDownloadApkListenerList = new ArrayList<>();
        private Handler mHandler = new Handler();
        private int mProgressing = -1;
        private AppUpdateDelegate<UpdateInfo, DownloadApkInfo> mDelegate;
        private Runnable mRunnable = new Runnable() {
            @Override
            public void run() {
                if (taskId != null) {
                    DownloadManager.Query query = new DownloadManager.Query().setFilterById(taskId);
                    Cursor c = mDownloadManager.query(query);
                    if (c.moveToFirst()) {
                        int downloadBytesIdx = c.getColumnIndexOrThrow(
                                DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR);
                        int totalBytesIdx = c.getColumnIndexOrThrow(
                                DownloadManager.COLUMN_TOTAL_SIZE_BYTES);
                        long totalBytes = c.getLong(totalBytesIdx);
                        long downloadBytes = c.getLong(downloadBytesIdx);
                        int percent = (int) (downloadBytes * 100 / totalBytes);
                        if (percent < 100) {
                            onProgress(percent);
                            mHandler.postDelayed(this, 500);
                        } else {
                            mHandler.removeCallbacks(this);
                        }
                    }
                }


            }
        };

        public DefaultApkManager(Context context, AppUpdateDelegate<UpdateInfo, DownloadApkInfo> delegate) {
            this.mContext = context.getApplicationContext();
            this.mDelegate = delegate;
            mDownloadManager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
        }

        @Override
        public File getDownloadApkFile(UpdateInfo info) {
            File downloadedApk = new File(mContext.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
                    targetSubFile(info));
            if (downloadedApk.isFile()) {
                return downloadedApk;
            }
            return null;
        }

        @Override
        public Uri getUriForFile(UpdateInfo info) {
            File file = getDownloadApkFile(info);
            if (file != null) {
                return getUriForFile(file);
            }
            return null;
        }

        @SuppressWarnings("unchecked")
        @Override
        public void downloadApk(UpdateInfo info) {
            File targetFile = getDownloadApkFile(info);
            if (targetFile != null) {
                onSuccess(info, targetFile);
            } else {
                try {
                    if (taskId == null) {
                        Uri uri = Uri.parse(info.getApkUrl());
                        DownloadManager.Request request = new DownloadManager.Request(uri);
                        configDownloadRequest(request);
                        request.setMimeType("application/vnd.android.package-archive");
                        request.setDestinationInExternalFilesDir(mContext, Environment.DIRECTORY_DOWNLOADS, targetSubFile(info));
                        taskId = mDownloadManager.enqueue(request);
                        IntentFilter filter = new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
                        filter.addAction("android.intent.action.VIEW_DOWNLOADS");
                        mContext.registerReceiver(new DefaultApkManager.DownloadReceiver(info), filter);
                        onProgress(0);
                        mHandler.postDelayed(mRunnable, 500);
                    }
                } catch (Exception e) {
                    onFailure(e);
                }

            }
        }

        protected void configDownloadRequest(DownloadManager.Request request) {
            request.setAllowedOverRoaming(false);
            request.setVisibleInDownloadsUi(true);
        }

        @Override
        public boolean isDownloading() {
            return taskId != null;
        }

        @Override
        public void registerDownloadApkListener(OnDownloadApkListener<UpdateInfo> listener) {
            onDownloadApkListenerList.add(listener);
        }

        @Override
        public void unregisterDownloadApkListener(OnDownloadApkListener<UpdateInfo> listener) {
            onDownloadApkListenerList.remove(listener);
        }

        private String createApkFileName(UpdateInfo appUpdateInfo) {
            return "musicbible_" + appUpdateInfo.getNewestVersion();
        }

        private String targetSubFile(UpdateInfo appUpdateInfo) {
            return "musicbible" + File.separator + createApkFileName(appUpdateInfo);
        }

        private Uri getUriForFile(File file) {
            //获取当前app的包名
            String FPAuth = mContext.getPackageName() + ".fileprovider";
            Uri uri;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                uri = FileProvider.getUriForFile(mContext, FPAuth, file);
            } else {
                uri = Uri.fromFile(file);
            }
            return uri;
        }

        private String getFilePathByTaskId(long id) {
            String filePath = null;
            DownloadManager.Query query = new DownloadManager.Query();
            query.setFilterById(id);
            Cursor cursor = mDownloadManager.query(query);
            while (cursor.moveToNext()) {
                filePath = cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI));
            }
            cursor.close();
            return filePath;
        }

        private void onSuccess(UpdateInfo info, File apkFile) {
            for (OnDownloadApkListener<UpdateInfo> onDownloadApkListener : onDownloadApkListenerList) {
                onDownloadApkListener.onSuccess(mDelegate.createDownloadApkInfo(info, apkFile));
            }
            taskId = null;

        }

        private void onProgress(int progress) {
            this.mProgressing = progress;
            for (OnDownloadApkListener<UpdateInfo> onDownloadApkListener : onDownloadApkListenerList) {
                onDownloadApkListener.onProgress(progress);
            }
        }

        private void onFailure(Exception e) {
            for (OnDownloadApkListener<UpdateInfo> onDownloadApkListener : onDownloadApkListenerList) {
                onDownloadApkListener.onFailure(e);
            }
        }

        public class DownloadReceiver extends BroadcastReceiver {
            private UpdateInfo mExecuteInfo;

            public DownloadReceiver(UpdateInfo executeInfo) {
                this.mExecuteInfo = executeInfo;
            }

            @Override
            public void onReceive(Context context, Intent intent) {
                long reference = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
                if (taskId == reference) {
                    try {
                        onSuccess(mExecuteInfo, new File(getFilePathByTaskId(taskId)));
                    } catch (Exception e) {
                        onFailure(e);
                    }
                    mExecuteInfo = null;
                    mContext.unregisterReceiver(this);
                }
            }
        }
    }

    public abstract static class RxAppUpdateSession<UpdateInfo extends AppUpdateInfo> implements AppUpdateSession<UpdateInfo> {
        private AppUpdateDelegate<UpdateInfo, ?> mDelegate;
        private boolean isChecking;
        private Disposable mDisposable;
        private OnAppVersionCheckListener<UpdateInfo> mListener;

        private OnDownloadApkListener<UpdateInfo> mDownloadListener = new OnDownloadApkListener<UpdateInfo>() {

            @Override
            public void onProgress(int progress) {
                if (mListener != null) {
                    mDisposable.dispose();
                    mListener.onFailure(new IllegalStateException());
                    mListener = null;
                }
            }

            @Override
            public void onSuccess(org.luyinbros.appupdate.DownloadApkInfo<UpdateInfo> info) {

            }

            @Override
            public void onFailure(Exception e) {

            }
        };

        public RxAppUpdateSession(AppUpdateDelegate<UpdateInfo, ?> delegate) {
            this.mDelegate = delegate;
            mDelegate.registerDownloadApkListener(mDownloadListener);
            //  mRemoteAppRepository = RepositoryV2FactoryClient.getRemoteRepositoryFactory(appUpdater.application).appRepository();
        }

        @Override
        public boolean isCheckingVersion() {
            return isChecking;
        }

        @Override
        public void checkVersion(final OnAppVersionCheckListener<UpdateInfo> listener) {
            if (!isChecking) {
                mListener = listener;
                isChecking = true;
                if (mDelegate.isDownloadingApk()) {
                    listener.onFailure(new IllegalStateException());
                    isChecking = false;
                } else {
                    checkUpdateObservable()
                            .subscribe(new Observer<UpdateInfo>() {
                                @Override
                                public void onSubscribe(Disposable d) {
                                    mDisposable = d;
                                    listener.onStart();
                                }

                                @Override
                                public void onNext(UpdateInfo info) {
                                    listener.onSuccess(info);
                                    isChecking = false;
                                    mDisposable = null;
                                }

                                @Override
                                public void onError(Throwable e) {
                                    listener.onFailure(e);
                                    isChecking = false;
                                    mDisposable = null;
                                }

                                @Override
                                public void onComplete() {

                                }
                            });
                }
            }
        }

        public abstract Observable<UpdateInfo> checkUpdateObservable();

        @Override
        public void destroy() {
            mDelegate.unregisterDownloadApkListener(mDownloadListener);
            if (mDisposable != null) {
                mDisposable.dispose();
            }
        }
    }
}

package org.luyinbros.appupdate;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;

public abstract class DefaultAppUpdateDelegate<T extends AppUpdateInfo> implements AppUpdateDelegate<T> {
    private final ApkManager<T> mApkManager;
    private Context mContext;
    private OnDownloadApkListener<T> mOnDownloadListener;

    public DefaultAppUpdateDelegate(ApkManager<T> mApkManager, Context context) {
        this.mApkManager = mApkManager;
        this.mContext = context.getApplicationContext();
    }

    @Override
    public abstract AppUpdateSession<T> openSession();

    @Override
    public boolean isAllowInstall() {
        if (Build.VERSION.SDK_INT >= 26) {
            return mContext.getPackageManager().canRequestPackageInstalls();
        } else {
            return true;
        }
    }

    public Intent getRequestInstallPermissionIntent() {
        if (Build.VERSION.SDK_INT >= 26) {
            return new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:" + mContext.getPackageName()));
        } else {
            return new Intent();
        }
    }

    @Override
    public boolean isDownloadingApk() {
        return mApkManager.isDownloading();
    }

    @Override
    public void registerDownloadApkListener(OnDownloadApkListener<T> listener) {
        mApkManager.registerDownloadApkListener(listener);
    }

    @Override
    public void unregisterDownloadApkListener(OnDownloadApkListener<T> listener) {
        mApkManager.unregisterDownloadApkListener(listener);
    }

    @Override
    public void install(T info) {
        mApkManager.installApk(info);
    }

    @Override
    public abstract void showUpdateDialog(T updateInfo);

    @Override
    public void startUpdateInBackground(T updateInfo) {
        File file = mApkManager.getApkFile(updateInfo);
        if (file != null) {
            install(updateInfo);
        } else {
            if (mOnDownloadListener == null) {
                mOnDownloadListener = new OnDownloadApkListener<T>() {
                    @Override
                    public void onProgress(int progress) {

                    }

                    @Override
                    public void onSuccess(@NonNull ApkManager<T> apkManager) {
                        apkManager.installNewestApk();
                        unregisterDownloadApkListener(mOnDownloadListener);
                        mOnDownloadListener = null;
                    }

                    @Override
                    public void onFailure(@NonNull Exception e) {

                    }
                };
                registerDownloadApkListener(mOnDownloadListener);
                mApkManager.downloadApk(updateInfo);
            }

        }
    }


    public abstract static class RxAppUpdateSession<T extends AppUpdateInfo> implements AppUpdateSession<T> {
        private boolean isChecking = false;
        private Disposable mDisposable;
        private OnAppVersionCheckListener<T> mListener;

        @Override
        public boolean isCheckingVersion() {
            return isChecking;
        }

        @Override
        public void checkVersion(@NonNull final OnAppVersionCheckListener<T> listener) {
            destroy();
            mListener = listener;
            checkUpdateObservable()
                    .subscribe(new Observer<T>() {
                        @Override
                        public void onSubscribe(Disposable d) {
                            mDisposable = d;
                            isChecking = true;
                            listener.onStart();
                        }

                        @Override
                        public void onNext(T info) {
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

        public abstract Observable<T> checkUpdateObservable();

        @Override
        public void destroy() {
            if (mDisposable != null) {
                mDisposable.dispose();
            }
            isChecking = false;
        }
    }

    public static abstract class DefaultApkManager<T extends AppUpdateInfo> implements ApkManager<T> {
        private Context mContext;
        private Long taskId;
        private DownloadManager mDownloadManager;
        private List<OnDownloadApkListener<T>> onDownloadApkListenerList = new ArrayList<>();
        private Handler mHandler = new Handler();
        private int mProgressing = -1;
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

        public DefaultApkManager(Context context) {
            this.mContext = context;
            mDownloadManager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
        }

        @Nullable
        @Override
        public abstract File getNewestApkFile();

        @Nullable
        @Override
        public abstract File getApkFile(@NonNull T info);

        @Nullable
        @Override
        public abstract Uri getUriForFile(@Nullable File file);

        @Override
        public void installNewestApk() {
            Intent intent = getInstallApkIntent(getNewestApkFile());
            if (intent != null) {
                mContext.startActivity(intent);
            }
        }

        @Override
        public void installApk(@NonNull T info) {
            Intent intent = getInstallApkIntent(getApkFile(info));
            if (intent != null) {
                mContext.startActivity(intent);
            }
        }

        protected void configDownloadRequest(@NonNull DownloadManager.Request request, T info) {

        }

        @Nullable
        @Override
        public Intent getInstallApkIntent(@Nullable File file) {
            if (file != null && file.isFile()) {
                Intent startIntent = new Intent();
                startIntent.setAction(Intent.ACTION_VIEW);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    startIntent.setFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
                }
                startIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startIntent.setDataAndType(getUriForFile(file), "application/vnd.android.package-archive");
                return startIntent;
            } else {
                return null;
            }
        }

        @Override
        public void downloadApk(@NonNull T info) {
            File targetFile = getApkFile(info);
            if (targetFile != null) {
                onSuccess();
            } else {
                try {
                    if (taskId == null) {
                        Uri uri = Uri.parse(info.getApkUrl());
                        DownloadManager.Request request = new DownloadManager.Request(uri);
                        configDownloadRequest(request, info);
                        request.setMimeType("application/vnd.android.package-archive");
                        taskId = mDownloadManager.enqueue(request);
                        IntentFilter filter = new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
                        filter.addAction("android.intent.action.VIEW_DOWNLOADS");
                        mContext.registerReceiver(new DefaultApkManager.DownloadReceiver(), filter);
                        onProgress(0);
                        mHandler.postDelayed(mRunnable, 500);
                    }
                } catch (Exception e) {
                    onFailure(e);
                }

            }
        }

        private void onSuccess() {
            for (OnDownloadApkListener<T> onDownloadApkListener : onDownloadApkListenerList) {
                onDownloadApkListener.onSuccess(this);
            }
            taskId = null;

        }

        private void onProgress(int progress) {
            this.mProgressing = progress;
            for (OnDownloadApkListener<T> onDownloadApkListener : onDownloadApkListenerList) {
                onDownloadApkListener.onProgress(progress);
            }
        }

        private void onFailure(Exception e) {
            for (OnDownloadApkListener<T> onDownloadApkListener : onDownloadApkListenerList) {
                onDownloadApkListener.onFailure(e);
            }
        }


        //        private String getFilePathByTaskId(long id) {
//            String filePath = null;
//            DownloadManager.Query query = new DownloadManager.Query();
//            query.setFilterById(id);
//            Cursor cursor = mDownloadManager.query(query);
//            while (cursor.moveToNext()) {
//                filePath = cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI));
//            }
//            cursor.close();
//            return filePath;
//        }
        @Override
        public boolean isDownloading() {
            return taskId != null;
        }

        @Override
        public void registerDownloadApkListener(@Nullable OnDownloadApkListener<T> listener) {
            if (listener != null) {
                onDownloadApkListenerList.add(listener);
            }
        }

        @Override
        public void unregisterDownloadApkListener(@Nullable OnDownloadApkListener<T> listener) {
            if (listener != null) {
                onDownloadApkListenerList.remove(listener);
            }
        }

        public class DownloadReceiver extends BroadcastReceiver {

            @Override
            public void onReceive(Context context, Intent intent) {
                long reference = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
                if (taskId == reference) {
                    try {
                        onSuccess();
                    } catch (Exception e) {
                        onFailure(e);
                    }
                    mContext.unregisterReceiver(this);
                }
            }
        }
    }
}

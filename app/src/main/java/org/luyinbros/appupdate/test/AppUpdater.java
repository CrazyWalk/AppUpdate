package org.luyinbros.appupdate.test;

import android.content.Context;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.luyinbros.appupdate.AppUpdateDelegate;
import org.luyinbros.appupdate.AppUpdateSession;
import org.luyinbros.appupdate.DefaultAppUpdateDelegate;

import java.io.File;

public class AppUpdater {
    private volatile static AppUpdater mInstance;
    private Context mContext;
    private AppUpdateDelegate mDelegate;

    private AppUpdater(Context context) {
        this.mContext = context.getApplicationContext();
        mDelegate = new DefaultAppUpdateDelegate<DefaultAppUpdateInfo>(mContext,
                new DefaultAppUpdateDelegate.DefaultApkManager<DefaultAppUpdateInfo>(mContext) {
                    @Nullable
                    @Override
                    public File getNewestApkFile() {
                        return null;
                    }

                    @Nullable
                    @Override
                    public File getApkFile(@NonNull DefaultAppUpdateInfo info) {
                        return null;
                    }

                    @Nullable
                    @Override
                    public Uri getUriForFile(@Nullable File file) {
                        return null;
                    }
                }) {
            @Override
            public AppUpdateSession<DefaultAppUpdateInfo> openSession() {
                return null;
            }
        };

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




}

package org.luyinbros.appupdate.test;

import android.app.Application;
import android.content.Context;
import android.content.pm.PackageManager;
import android.support.annotation.Nullable;

public class App extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
    }

    public static int getAppIconResouce(@Nullable Context context) {
        if (context == null) {
            return 0;
        }
        try {
            PackageManager pm = context.getPackageManager();
            return pm.getApplicationInfo(context.getApplicationContext().getPackageName(), 0).icon;
        } catch (PackageManager.NameNotFoundException e) {
            return 0;

        }
    }

}

package org.luyinbros.appupdate.test;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

public class UpdateActivity extends AppCompatActivity {


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_update);


    }

    public void onDownload(View v) {

    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}

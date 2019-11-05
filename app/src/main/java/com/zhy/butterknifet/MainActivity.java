package com.zhy.butterknifet;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import com.zhy.lib_annotations.BindViewT;
import com.zhy.lib_annotations.RandomInt;
import com.zhy.lib_annotations.RandomString;
import com.zhy.lib_api.RandomUtil;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    @RandomInt(minValue = 10,maxValue = 100)
    int mRandomInt;
    @RandomString
    String mRandomString;
    @BindViewT(R.id.hello)
    TextView textView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        RandomUtil.inject(this);
        Log.e(TAG,"int == " + mRandomInt);
        Log.e(TAG,"String == " + mRandomString);
    }
}

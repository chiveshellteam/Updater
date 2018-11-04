package com.chiveshell.updatertest;

import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.chiveshell.updater.Appearance;
import com.chiveshell.updater.NetAccessor;
import com.chiveshell.updater.Updater;

/**
 * Created by wuyr on 18-11-4 下午6:15.
 */
public class MainActivity extends AppCompatActivity {

    static {
        Updater.init(Updater.Configure.newInstance()
                .accessor()//配置下载地址和获取最新版本
                .appearance()//配置自定义的UI界面
                .isForce(true)//强制更新
                .autoDetect(true));//自动检测
    }

    public void checkUpdate(View view) {
        //手动检查更新
        Updater.checkUpdate();
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.act_main_view);
    }
}

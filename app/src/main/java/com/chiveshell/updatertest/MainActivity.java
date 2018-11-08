package com.chiveshell.updatertest;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import com.chiveshell.updater.Appearance;
import com.chiveshell.updater.NetAccessor;
import com.chiveshell.updater.Updater;

/**
 * Created by wuyr on 18-11-4 下午6:15.
 */
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.act_main_view);

        initUpdater();
    }

    public void checkUpdate(View view) {
        Updater.checkUpdate(this);
    }

    private void initUpdater() {
        Updater.init(Updater.Configure.newInstance()
                .accessor(new NetAccessor() {
                    @Override
                    public String getNewestVersionName() {
                        //模拟网络请求
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        return "1.1";
                    }

                    @Override
                    public String getDownloadUrl() {
                        //apk下载地址
                        return "https://github.com/wuyr/PathLayoutManager/raw/master/app-debug.apk";
                    }

                    @Override
                    public String getSavePath() {
                        //apk保存路径
                        return getExternalCacheDir().getPath();
                    }
                })
                .appearance(new Appearance() {
                    @Override
                    public Dialog getCheckingDialog() {
                        ProgressDialog dialog = new ProgressDialog(MainActivity.this);
                        dialog.setMessage("checking...");
                        dialog.setCancelable(false);
                        return dialog;
                    }

                    @Override
                    public Dialog getUpdateDetailDialog() {
                        return new AlertDialog.Builder(MainActivity.this).setTitle("有新版本")
                                .setMessage("是否立即更新？").setNegativeButton("取消",null)
                                .setPositiveButton("确定", (dialog, which) -> {
                                    dialog.dismiss();
                                    Updater.startDownload(MainActivity.this);
                                }).create();
                    }

                    @Override
                    public Dialog getDownloadFinishedDialog() {
                        return new AlertDialog.Builder(MainActivity.this).setTitle("下载完成")
                                .setMessage("是否立即安装？").setNegativeButton("取消",null)
                                .setPositiveButton("确定", (dialog, which) -> {
                                    dialog.dismiss();
                                    Updater.installNewVersion(MainActivity.this);
                                }).create();
                    }
                })
                .isForce(true)
                .notifyIcon(R.mipmap.ic_launcher));//图标（必须）
    }
}

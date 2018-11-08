package com.chiveshell.updater;

import android.app.*;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;
import android.widget.Toast;
import com.yanzhenjie.permission.AndPermission;
import com.yanzhenjie.permission.Permission;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static android.content.Context.NOTIFICATION_SERVICE;

/**
 * @author wuyr
 * @since 2018-11-02 下午1:42
 */
public final class Updater {

    private volatile static Updater mInstance;
    private NetAccessor mAccessor;
    private Appearance mAppearance;
    private boolean isForce;
    private ExecutorService mThreadPool;
    private int mNotifyIcon;
    private String mLastVersionTips;
    private String mStartDownloadTips;
    private String mNotifyTitle;

    private Updater(Configure configure) {
        LogUtil.setDebugLevel(LogUtil.ERROR);
        mAccessor = configure.accessor;
        mAppearance = configure.appearance;
        isForce = configure.isForce;
        mNotifyIcon = configure.notifyIcon;
        mLastVersionTips = configure.lastVersionTips;
        mStartDownloadTips = configure.startDownloadTips;
        mNotifyTitle = configure.notifyTitle;
        mThreadPool = Executors.newCachedThreadPool();
    }

    static Updater getInstance() {
        return mInstance;
    }

    public static synchronized void init(Configure configure) {
        mInstance = new Updater(configure);
    }

    public static void checkUpdate(Context context) {
        checkUpdate(context, true);
    }

    public static void checkUpdate(Context context, boolean isShowDialog) {
        Dialog dialog = null;
        if (isShowDialog) {
            try {
                dialog = getInstance().mAppearance.getCheckingDialog();
                dialog.setCancelable(false);
                dialog.show();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        Dialog finalDialog = dialog;
        getInstance().mThreadPool.execute(() -> {
            if (needDownload(context)) {
                runOnUiThread(context, () -> {
                    if (finalDialog != null) {
                        finalDialog.dismiss();
                    }
                    getInstance().mAppearance.getUpdateDetailDialog().show();
                });
            } else {
                runOnUiThread(context, () -> {
                    if (finalDialog != null) {
                        finalDialog.dismiss();
                    }
                    Toast.makeText(context, getInstance().mLastVersionTips, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private static Activity getActivityFromContext(Context context) {
        while (context instanceof ContextWrapper) {
            if (context instanceof Activity) {
                return (Activity) context;
            }
            context = ((ContextWrapper) context).getBaseContext();
        }
        throw new RuntimeException("Activity not found!");
    }

    private static boolean needDownload(Context context) {
        try {
            String[] oldVersion = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName.split("\\.");
            String[] newVersion = getInstance().mAccessor.getNewestVersionName().split("\\.");
            if (oldVersion.length < newVersion.length) {
                int diff = newVersion.length - oldVersion.length;
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < diff; i++) {
                    sb.append("0").append(",");
                }
                oldVersion = insertElement(false, oldVersion, sb.toString().split(","));
            } else if (newVersion.length < oldVersion.length) {
                int diff = oldVersion.length - newVersion.length;
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < diff; i++) {
                    sb.append("0").append(",");
                }
                newVersion = insertElement(false, newVersion, sb.toString().split(","));
            }
            for (int i = 0; i < oldVersion.length; i++) {
                int neww = Integer.parseInt(newVersion[i]);
                int old = Integer.parseInt(oldVersion[i]);
                if (neww < old) {
                    return false;
                } else if (i == oldVersion.length - 1 && old == neww) {
                    return false;
                } else if (neww > old) {
                    return true;
                }
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }

    private static String[] insertElement(boolean isAddFromHead, @NonNull String[] target, @NonNull String... elements) {
        String[] result = new String[target.length + elements.length];
        if (isAddFromHead) {
            System.arraycopy(elements, 0, result, 0, elements.length);
            System.arraycopy(target, 0, result, elements.length, target.length);
        } else {
            System.arraycopy(target, 0, result, 0, target.length);
            System.arraycopy(elements, 0, result, target.length, elements.length);
        }
        return result;
    }

    public static void startDownload(Context context) {
        AndPermission.with(context).runtime().permission(Permission.Group.STORAGE)
                .onGranted(permissions -> {
                    Toast.makeText(context, getInstance().mStartDownloadTips, Toast.LENGTH_SHORT).show();
                    NotificationCompat.Builder builder = showNotify(context);

                    getInstance().mThreadPool.execute(() -> {
                        String url = getInstance().mAccessor.getDownloadUrl();
                        OkHttpClient okHttpClient = new OkHttpClient();
                        InputStream inputStream = null;
                        OutputStream outputStream = null;
                        try (Response response = okHttpClient.newCall(new Request.Builder().url(url).build()).execute()) {
                            inputStream = response.body().byteStream();
                            File apkFile = getApkFile();
                            outputStream = new FileOutputStream(apkFile);

                            long total = response.body().contentLength();
                            long current = 0;

                            byte[] buffer = new byte[(int) (total / 10)];
                            int count;
                            while ((count = inputStream.read(buffer)) != -1) {
                                current += count;
                                outputStream.write(buffer, 0, count);
                                long finalCurrent = current;
                                runOnUiThread(context, () -> {
                                    NotificationManager manager = (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);
                                    if (manager != null) {
                                        builder.setDefaults(NotificationCompat.COLOR_DEFAULT);
                                        builder.setProgress(100, (int) ((float) finalCurrent / total * 100), false);
                                        manager.notify(1, builder.build());
                                    }
                                });
                            }
                            outputStream.flush();
                            runOnUiThread(context, () -> {
                                NotificationManager manager = (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);
                                if (manager != null) {
                                    manager.cancel(1);
                                }
                                Dialog dialog = getInstance().mAppearance.getDownloadFinishedDialog();
                                dialog.setCancelable(getInstance().isForce);
                                dialog.show();
                            });
                        } catch (Exception e) {
                            e.printStackTrace();
                        } finally {
                            if (inputStream != null) {
                                try {
                                    inputStream.close();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                            if (outputStream != null) {
                                try {
                                    outputStream.close();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    });
                })
                .onDenied(permissions -> {

                })
                .start();
    }

    @NonNull
    private static File getApkFile() {
        return new File(getInstance().mAccessor.getSavePath(), "new.apk");
    }

    private static NotificationCompat.Builder showNotify(Context context) {
        String channelId = "download";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId, channelId, NotificationManager.IMPORTANCE_DEFAULT);
            NotificationManager notificationManager = (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
        NotificationManager manager = (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId)
                .setContentTitle(getInstance().mNotifyTitle)
                .setWhen(System.currentTimeMillis())
                .setSmallIcon(getInstance().mNotifyIcon)
                .setOngoing(true)
                .setProgress(100, 0, true)
                .setContentIntent(PendingIntent.getActivity(context, 0, new Intent(context, context.getClass()), 0));
        if (manager != null) {
            manager.notify(1, builder.build());
        }
        return builder;
    }

    private static void runOnUiThread(Context context, Runnable runnable) {
        Runnable task = () -> {
            try {
                runnable.run();
            } catch (Exception e) {
                e.printStackTrace();
            }
        };
        getActivityFromContext(context).runOnUiThread(task);
    }

    public static void installNewVersion(Context context) {
        AndPermission.with(context)
                .install()
                .file(getApkFile())
                .onGranted(file -> {
                    // App is allowed to install apps.
                })
                .onDenied(file -> {
                    // App is refused to install apps.
                })
                .start();
    }

    public static class Configure {

        private NetAccessor accessor;
        private Appearance appearance;
        private boolean isForce;
        private int notifyIcon;
        private String lastVersionTips = "已是最新版本";
        private String startDownloadTips = "开始后台下载新版本";
        private String notifyTitle = "正在下载最新版本";

        private Configure() {

        }

        public static Configure newInstance() {
            return new Configure();
        }

        public Configure accessor(NetAccessor accessor) {
            this.accessor = accessor;
            return this;
        }

        public Configure appearance(Appearance appearance) {
            this.appearance = appearance;
            return this;
        }

        public Configure isForce(boolean isForce) {
            this.isForce = isForce;
            return this;
        }

        public Configure notifyIcon(int notifyIcon) {
            this.notifyIcon = notifyIcon;
            return this;
        }

        public Configure lastVersionTips(String lastVersionTips) {
            this.lastVersionTips = lastVersionTips;
            return this;
        }

        public Configure startDownloadTips(String startDownloadTips) {
            this.startDownloadTips = startDownloadTips;
            return this;
        }

        public Configure notifyTitle(String notifyTitle) {
            this.notifyTitle = notifyTitle;
            return this;
        }
    }
}

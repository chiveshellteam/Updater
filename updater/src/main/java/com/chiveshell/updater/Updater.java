package com.chiveshell.updater;

import android.app.Dialog;
import android.content.Context;

/**
 * @author wuyr
 * @since 2018-11-02 下午1:42
 */
public final class Updater {

    private volatile static Updater mInstance;
    NetAccessor accessor;
    Appearance appearance;
    boolean isForce;
    boolean autoDetect;
    Context context;

    private Updater(Configure configure) {
        accessor = configure.accessor;
        appearance = configure.appearance;
        isForce = configure.isForce;
        autoDetect = configure.autoDetect;
        context = MyProvider.context;
    }

    static Updater getInstance() {
        return mInstance;
    }

    public static void init(Configure configure) {
        if (mInstance == null) {
            synchronized (Updater.class) {
                if (mInstance == null) {
                    mInstance = new Updater(configure);
                }
            }
        }
    }

    public static void checkUpdate() {
        Dialog dialog = getInstance().appearance.getCheckingDialog();
        dialog.setCancelable(!Updater.getInstance().isForce);
        dialog.show();
    }

    public static class Configure {

        private NetAccessor accessor;
        private Appearance appearance;
        private boolean autoDetect;
        private boolean isForce;

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

        public Configure autoDetect(boolean enable) {
            this.autoDetect = enable;
            return this;
        }
    }
}

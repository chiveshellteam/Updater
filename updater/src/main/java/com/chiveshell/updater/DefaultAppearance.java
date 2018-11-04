package com.chiveshell.updater;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;

/**
 * Created by wuyr on 18-11-4 下午11:03.
 */
public class DefaultAppearance implements Appearance {

    @Override
    public Dialog getCheckingDialog() {
        Context context = Updater.getInstance().context;
        ProgressDialog dialog = new ProgressDialog(context);
        dialog.setMessage(context.getResources().getString(R.string.checking_update));
        return dialog;
    }

    @Override
    public Dialog getUpdateDetailDialog() {
        return new AlertDialog.Builder(Updater.getInstance().context).setPositiveButton("",
                new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        }).create();
    }
}

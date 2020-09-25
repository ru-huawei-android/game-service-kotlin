package com.huawei.gameservice.utils;

import android.view.Gravity;
import android.widget.Toast;

import com.huawei.gameservice.MyApplication;

public class ToastUtil {
    private static Toast toast;

    public static void showShortToastTop(String msg) {
        if (MyApplication.getInstance() != null) {
            if (toast == null) {
                toast = Toast.makeText(MyApplication.getInstance(), msg, Toast.LENGTH_SHORT);
                toast.setGravity(Gravity.TOP, 0, 0);
            } else {
                toast.setText(msg);
            }
            toast.show();
        }
    }
}

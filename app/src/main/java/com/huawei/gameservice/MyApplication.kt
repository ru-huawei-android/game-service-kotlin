package com.huawei.gameservice

import android.app.Application
import com.huawei.hms.api.HuaweiMobileServicesUtil


class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        var instance:MyApplication = this
        HuaweiMobileServicesUtil.setApplication(this)
    }

    companion object {
        @JvmStatic
        var instance: MyApplication? = null
            private set
    }
}
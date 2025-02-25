package org.odk.collect.selfiecamera.support

import android.app.Application
import org.odk.collect.androidshared.ui.ToastUtils
import org.odk.collect.selfiecamera.SelfieCameraDependencyComponent
import org.odk.collect.selfiecamera.SelfieCameraDependencyComponentProvider

class RobolectricApplication : Application(), SelfieCameraDependencyComponentProvider {

    override lateinit var selfieCameraDependencyComponent: SelfieCameraDependencyComponent

    override fun onCreate() {
        super.onCreate()
        ToastUtils.setApplication(this)
    }
}

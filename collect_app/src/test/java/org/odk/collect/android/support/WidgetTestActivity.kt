package org.odk.collect.android.support

import android.view.View
import androidx.appcompat.app.AppCompatActivity

class WidgetTestActivity : AppCompatActivity() {
    @JvmField
    val viewsRegisterForContextMenu = mutableListOf<View>()

    override fun registerForContextMenu(view: View) {
        super.registerForContextMenu(view)
        viewsRegisterForContextMenu.add(view)
    }
}

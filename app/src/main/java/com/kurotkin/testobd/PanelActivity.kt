package com.kurotkin.testobd

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.kurotkin.testobd.ui.main.PanelFragment

class PanelActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.panel_activity)
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.container, PanelFragment.newInstance())
                .commitNow()
        }
    }
}
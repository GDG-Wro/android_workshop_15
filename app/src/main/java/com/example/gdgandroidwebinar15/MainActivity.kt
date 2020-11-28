package com.example.gdgandroidwebinar15

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.gdgandroidwebinar15.ui.main.MainFragment
import com.google.android.gms.common.GoogleApiAvailability
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.ktx.messaging
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfigSettings
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        GoogleApiAvailability.getInstance().makeGooglePlayServicesAvailable(this)
        setContentView(R.layout.main_activity)
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.container, MainFragment.newInstance())
                .commitNow()
        }
        lifecycleScope.launch {
            Log.i("FCM", "Current token: ${Firebase.messaging.token.await()}")
        }
        lifecycleScope.launch {
            Firebase.remoteConfig.apply {
                if (BuildConfig.DEBUG) {
                    setConfigSettingsAsync(
                        remoteConfigSettings { minimumFetchIntervalInSeconds = 100 }
                    ).await()
                }
                setDefaultsAsync(R.xml.remote_config_defaults).await()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        GoogleApiAvailability.getInstance().makeGooglePlayServicesAvailable(this)
    }
}

package org.ensodai.avalonmediacard

import android.app.UiModeManager
import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.core.view.WindowCompat
import org.ensodai.avalonmediacard.presentation.App
import org.ensodai.avalonmediacard.presentation.screens.commonComponents.DeviceTarget
import org.ensodai.avalonmediacard.presentation.screens.commonComponents.LocalDeviceTarget

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
            window.isStatusBarContrastEnforced = false
        }
        
        val uiModeManager = getSystemService(Context.UI_MODE_SERVICE) as? UiModeManager
        val isTv = uiModeManager?.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION
        val isTablet = resources.configuration.smallestScreenWidthDp >= 600
        val target = when {
            isTv -> DeviceTarget.ANDROID_TV
            isTablet -> DeviceTarget.TABLET
            else -> DeviceTarget.ANDROID_MOBILE
        }
        
        setContent {
            CompositionLocalProvider(LocalDeviceTarget provides target) {
                App()
            }
        }
    }
}

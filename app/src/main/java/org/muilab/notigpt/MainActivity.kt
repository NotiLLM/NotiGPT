package org.muilab.notigpt

import android.app.ActivityManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.net.Uri
import android.os.Bundle
import android.os.IBinder
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import org.muilab.notigpt.database.room.DrawerDatabase
import org.muilab.notigpt.paging.NotiRepository
import org.muilab.notigpt.service.GeminiService
import org.muilab.notigpt.service.NotiListenerService
import org.muilab.notigpt.ui.theme.NotiTaskTheme
import org.muilab.notigpt.view.screen.MainScreen
import org.muilab.notigpt.viewModel.DrawerViewModel
import org.muilab.notigpt.viewModel.DrawerViewModelFactory
import org.muilab.notigpt.viewModel.GeminiViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!NotificationManagerCompat.from(applicationContext).areNotificationsEnabled()) {
            val intent = Intent().apply {
                action = Settings.ACTION_APP_NOTIFICATION_SETTINGS
                putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
            }
            startActivity(intent)
        }

        if (!isBatteryOptimizationsIgnored()) {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:$packageName")
            }
            try {
                if (intent.resolveActivity(packageManager) != null)
                    startActivity(intent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }


        if (isNotiListenerEnabled()) {
            val notiListenerIntent = Intent(this@MainActivity, NotiListenerService::class.java)
            startService(notiListenerIntent)
        } else {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }

        val geminiServiceIntent = Intent(this, GeminiService::class.java)
        if (!isServiceRunning(this, GeminiService::class.java)) {
            startService(geminiServiceIntent)
        }
        bindService(geminiServiceIntent, gptServiceConnection, Context.BIND_AUTO_CREATE)

        setContent {
            NotiTaskTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen(applicationContext, drawerViewModel, geminiViewModel)
                }
            }
        }
    }

    private val drawerViewModel: DrawerViewModel by viewModels {
        val drawerDatabase = DrawerDatabase.getInstance(applicationContext)
        val drawerDao = drawerDatabase.drawerDao()
        DrawerViewModelFactory(this.application, NotiRepository(drawerDao))
    }

    private val gptServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            val binder = service as GeminiService.GPTBinder
            geminiService = binder.getService()
            geminiViewModel.setService(geminiService)
        }

        override fun onServiceDisconnected(className: ComponentName) {}
    }

    private lateinit var geminiService: GeminiService
    private val geminiViewModel by viewModels<GeminiViewModel>()

    private fun isNotiListenerEnabled(): Boolean {
        val cn = ComponentName(this, NotiListenerService::class.java)
        val flat: String? =
            Settings.Secure.getString(this.contentResolver, "enabled_notification_listeners")
        return (flat != null) && (cn.flattenToString() in flat)
    }

    fun isBatteryOptimizationsIgnored(): Boolean {
        val pm = getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
        return pm.isIgnoringBatteryOptimizations(packageName)
    }

    private fun isServiceRunning(context: Context, serviceClass: Class<*>): Boolean {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val services = activityManager.getRunningServices(Integer.MAX_VALUE)
        for (serviceInfo in services)
            if (serviceClass.name == serviceInfo.service.className)
                return true
        return false
    }
}

@Composable
fun TestCard(geminiViewModel: GeminiViewModel) {
    val result by geminiViewModel.response.observeAsState("")
    Card (
        Modifier
            .fillMaxHeight(0.4f)
            .fillMaxWidth()
            .padding(8.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 12.dp
        )
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                result,
                Modifier
                    .fillMaxHeight(0.8f)
                    .padding(10.dp)
                    .verticalScroll(rememberScrollState())
            )
        }
    }
}

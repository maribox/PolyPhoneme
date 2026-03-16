package it.bosler.polyphoneme

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import it.bosler.polyphoneme.ui.about.BuildInfo
import it.bosler.polyphoneme.ui.library.rememberEpubPicker
import kotlinx.coroutines.flow.MutableStateFlow

class MainActivity : ComponentActivity() {

    companion object {
        val pendingEpubUri = MutableStateFlow<String?>(null)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        handleIntent(intent)

        val buildInfo = BuildInfo(
            versionName = BuildConfig.VERSION_NAME,
            versionCode = BuildConfig.VERSION_CODE,
            buildDate = BuildConfig.BUILD_DATE,
            buildType = BuildConfig.BUILD_TYPE,
        )

        setContent {
            App(
                filePicker = { onResult -> rememberEpubPicker(onResult) },
                pendingEpubUri = pendingEpubUri,
                buildInfo = buildInfo,
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        val uri = when (intent.action) {
            Intent.ACTION_VIEW -> intent.data
            Intent.ACTION_SEND -> intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
                ?: intent.data
            else -> null
        }
        if (uri != null) {
            try {
                contentResolver.takePersistableUriPermission(
                    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) { }
            pendingEpubUri.value = uri.toString()
        }
    }
}

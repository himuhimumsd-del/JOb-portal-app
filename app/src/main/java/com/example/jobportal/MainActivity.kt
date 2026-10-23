package com.example.jobportal

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.jobportal.ui.MainScreen
import com.example.jobportal.ui.MainViewModel
import com.example.jobportal.ui.theme.TalentNexusTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Handle referral deep link if present: https://talentnexus.app/join?ref=...
        handleReferralIntent(intent)

        setContent {
            TalentNexusTheme {
                MainScreen(viewModel = viewModel)
            }
        }

        // Post live Cloud Firestore connection and auto-sign-in after first layout/render pass
        // to prevent main-thread cold-start freeze and SurfaceSyncGroup 1000ms timeout
        window.decorView.post {
            val databaseId = getString(R.string.firestore_database_id)
            viewModel.connectToLiveFirestore(databaseId)
            viewModel.attemptAutoSignIn(this)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleReferralIntent(intent)
    }

    private fun handleReferralIntent(intent: Intent?) {
        val uri = intent?.data ?: return
        val refCode = uri.getQueryParameter("ref")
        if (!refCode.isNullOrBlank()) {
            viewModel.handleIncomingReferralCode(refCode)
        }
    }
}

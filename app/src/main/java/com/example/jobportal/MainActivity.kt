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

        // Initialize live Cloud Firestore connection using named database ID from firebase_applet_config.xml
        val databaseId = getString(R.string.firestore_database_id)
        viewModel.connectToLiveFirestore(databaseId)

        // Attempt non-blocking silent auto-sign-in for authorized Google accounts
        viewModel.attemptAutoSignIn(this)

        setContent {
            TalentNexusTheme {
                MainScreen(viewModel = viewModel)
            }
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

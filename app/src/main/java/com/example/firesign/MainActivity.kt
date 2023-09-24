package com.example.firesign

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.firesign.ui.sign_in.FireAuthUIClient
import com.example.firesign.ui.sign_in.SignInScreen
import com.example.firesign.ui.sign_in.SignInViewModel
import com.example.firesign.ui.theme.FireSIgnTheme
import com.google.android.gms.auth.api.identity.Identity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {

        val fireClient by lazy {
            FireAuthUIClient(
                context = applicationContext,
                oneTapClient = Identity.getSignInClient(applicationContext)
            )
        }
        super.onCreate(savedInstanceState)
        setContent {
            FireSIgnTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    NavHost(navController = navController, startDestination = "sign-in") {
                        composable(route = "sign-in") {
                            val viewModel = viewModel<SignInViewModel>()
                            val state = viewModel.state.collectAsState()

                            val launcher = rememberLauncherForActivityResult(
                                contract = ActivityResultContracts.StartIntentSenderForResult(),
                                onResult = { result ->
                                    if (result.resultCode == RESULT_OK) {
                                        lifecycleScope.launch {
                                            val signInResult = fireClient.signInWithIntent(
                                                intent = result.data ?: return@launch
                                            )
                                            viewModel.onSignInResult(signInResult)
                                        }
                                    }  // end IF
                                }  // end ON RESULT
                            )  // end LAUNCHER

                            LaunchedEffect(key1 = state.value.isSignInSuccessful) {
                                Toast.makeText(
                                    applicationContext,
                                    "Signed in user!",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            SignInScreen(
                                signInState = state.value,
                                onSignInClick = {
                                    lifecycleScope.launch {
                                        launcher.launch(
                                            IntentSenderRequest.Builder(
                                                fireClient.signIn() ?: return@launch
                                            ).build()
                                        )
                                    }
                                }
                            )
                        }  //  end COMPOSABLE SIGN-IN
                    }  // end NAV HOST
                }  // end SURFACE
            }  // end FIRESIGN THEME
        }  // end SET CONTENT
    }  // end ONCREATE
}  // end

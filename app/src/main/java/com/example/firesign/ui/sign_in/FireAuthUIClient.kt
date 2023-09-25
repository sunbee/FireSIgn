package com.example.firesign.ui.sign_in

import android.content.Context
import android.content.Intent
import android.content.IntentSender
import com.example.firesign.R
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.identity.BeginSignInRequest.GoogleIdTokenRequestOptions
import com.google.android.gms.auth.api.identity.SignInClient
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await
import java.util.concurrent.CancellationException

class FireAuthUIClient(
    private val context: Context,
    private val oneTapClient: SignInClient
) {
    private val auth = Firebase.auth

    /*
    * There are two steps:
    * Step 1: Define launcher for sign-in activity with `signInWithIntent()` as callback to handle resulting Intent
    * Step 2: Launch sign-in flow with `signIn()` that returns IntentSender for IntentSenderRequest construction
    * We want to use the external Google sign-in activity within our app, so we need the following:
    * - Intent: Result of launching sign-in flow and has credentials
    * - IntentSender: Wraps around PendingIntent and encapsulates Google sign-in flow for launch
    * - IntentSenderRequest: Constructed from IntentSender for launching sign-in activity.
    *
    * Step 1:
    * Use `rememberLauncherForActivityResult` that registers an activity for result as follows:
    * - contract: `ActivityResultContract.StartIntentSenderForResult()`.
    * - onResult: `signInWithIntent(intent: Intent)` callback
    * Here, we retrieve Google account credentials resulting from the sign-in flow via Intent
    * in the callback and proceed to authenticate for Firebase.
    *
    * Step 2:
    * Call `launcher.launch()` with the IntentSenderRequest constructed from IntentSender
    * returned by `signIn()`.
    *
    * The function `signIn()` initiates the one-tap sign-in client and returns an IntentSender.
    * It follows the recommended steps for using the client API as follows:
    * 1.
    * Get a new API client instance by calling `Identity.getSignInClient`. Here, we have passed
    * the sign-in client obtained using `Identity.getSignInClient(context)` in MainActivity.
    * 2.
    * Call `SignInClient.beginSignIn()`, supplying the constructed BeginSignInRequest as an input.
    * Here, we have used the builder pattern with `buildSignInRequest()` for this purpose.
    * 3.
    * If the request is successful, at least one matching credential is available.
    * Here, we have `await()`-ed the async process in try-catch block.
    * 4.
    * Launch the PendingIntent from the result of the operation to display the UI
    * that guides the user through sign-in. The result of sign-in will be returned
    * in Activity.onActivityResult; calling SignInClient.getSignInCredentialFromIntent
    * will either return the SignInCredential if the operation was successful,
    * or throw an ApiException that indicates the reason for failure. If the request is unsuccessful,
    * no matching credential was found on the device that can be used to sign the user in.
    * No further action needs to be taken.
    * Here, we obtain the IntentSender that wraps the sender of PendingIntent as return value
    * and use that to construct IntentSenderRequest from IntentSender when launcher is invoked.
    * The recommendation to use Activity.onActivityResult is not relevant to Jetpack Compose,
    * where we use the activity launcher with contract instead.
    * 5.
    * When the user signs out of your application, please make sure to call SignInClient.signOut
    * Here, we have function `signOut()` for the user.
    *
    * */
    suspend fun signIn(): IntentSender? {
        val result = try {
            oneTapClient.beginSignIn(
                buildSignInRequest()
            ).await()
        } catch (e: Exception) {
            e.printStackTrace()
            if (e is CancellationException) throw e
            null
        }
        return result?.pendingIntent?.intentSender
    }

    suspend fun signInWithIntent(intent: Intent): SignInResult {
        val credential = oneTapClient.getSignInCredentialFromIntent(intent)
        val googleIdToken = credential.googleIdToken
        val googleCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
        return try {
            val user = auth.signInWithCredential(googleCredential).await().user
            SignInResult(
                data = user?.run {
                    UserData(
                        userId = uid,
                        username = displayName,
                        profilePictureUrl = photoUrl?.toString()
                    )
                },
                errorMessage = null
            )

        } catch (e: Exception) {
            e.printStackTrace()
            if (e is CancellationException) throw e
            SignInResult(
                data = null,
                errorMessage = e.message
            )
        }

    }

    suspend fun signOut() {
        try {
            oneTapClient.signOut()
            auth.signOut()
        } catch (e: Exception) {
            e.printStackTrace()
            if (e is CancellationException) throw e
        }
    }

    fun getSignedInUser(): UserData? {
        return auth.currentUser?.run {
            UserData(
                userId = uid,
                username = displayName,
                profilePictureUrl = photoUrl?.toString()
            )
        }

    }
    private fun buildSignInRequest(): BeginSignInRequest {
        return BeginSignInRequest.Builder()
            .setGoogleIdTokenRequestOptions(
                GoogleIdTokenRequestOptions.builder()
                    .setSupported(true)
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(context.getString(R.string.web_client_id))
                    .build()
            )
            .setAutoSelectEnabled(true)
            .build()
    }
}
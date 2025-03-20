package com.example.enjoybook.auth

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import kotlin.coroutines.cancellation.CancellationException

class GoogleSignInClient(
    private val context: Context,
) {

    /*private val tag = "GoogleSignInClient: "

    private val credentialManager = CredentialManager.create(context)
    private val firebaseAuth = FirebaseAuth.getInstance()

    fun isSignedIn(): Boolean{

    }

    suspend fun signIn(): Boolean {
        if(isSignedIn()){
            return true
        }

        try{

            val result = buildCredentialResponse()
            return handleSignIt(result)

        } catch(e: Exception){
            e.printStackTrace()
            if (e is CancellationException) throw e
            println(tag + "sinIn error: ${e.message}")
            return false
        }
    }

    private suspend fun handleSignIn(result: GetCredentialResponse): Boolean{
        val credential = result.credential
        if(
            credential is CustomCredential &&
            credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ){

        } else{
            println(tag + "credential is not GoogleIdTokenC")
        }
    }

    private suspend fun buildCredentialResponse() : GetCredentialResponse {
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(
                GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(
                        "593084232172-fag3l40r5oeugov6a59m4arsukdhpmj2.apps.googleusercontent.com"
                    )
                    .setAutoSelectEnabled(false)
                    .build()
            )
            .build()
        return credentialManager.getCredential(
            request = request, context = context
        )
    }

    fun signOut(){

    }*/

}
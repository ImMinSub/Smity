package com.example.capstone.firebase

import android.content.Context
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import javax.inject.Inject

class FirebaseUtil @Inject constructor() {
    private val firebaseAuth: FirebaseAuth by lazy {
        FirebaseAuth.getInstance()
    }

    private val firestore: FirebaseFirestore by lazy {
        Firebase.firestore
    }

    private val tokenCollection = firestore.collection("tokens")

    fun updateToken(token: String) {
        val currentUser = firebaseAuth.currentUser ?: return

        val map = hashMapOf("token" to token)
        tokenCollection.document(currentUser.uid).set(map)
    }

    fun showToast(context: Context, message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
}
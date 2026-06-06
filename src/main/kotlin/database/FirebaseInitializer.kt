package com.database

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseToken
import java.io.FileInputStream

object FirebaseInitializer {

    fun init() {

        val serviceAccount =
            FileInputStream("src/main/resources/firebase-admin.json")

        val options = FirebaseOptions.builder()
            .setCredentials(GoogleCredentials.fromStream(serviceAccount))
            .build()

        if (FirebaseApp.getApps().isEmpty()) {
            FirebaseApp.initializeApp(options)
        }
    }
    fun verifyToken(idToken: String): FirebaseToken {
        val decodedToken = FirebaseAuth.getInstance().verifyIdToken(idToken)

        return decodedToken
    }
}

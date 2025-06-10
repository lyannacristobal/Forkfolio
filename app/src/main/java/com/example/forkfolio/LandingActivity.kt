package com.example.forkfolio

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class LandingActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize Firebase Auth (this will happen once)
        auth = FirebaseAuth.getInstance()

        // Check if the user is logged in. If yes, redirect to MainActivity
        auth.currentUser?.let {
            // If user is logged in, start MainActivity and clear the activity stack
            startActivity(Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
            finish() // Close the current activity
            return
        }

        // If not logged in, set content view and proceed with the rest of the logic
        setContentView(R.layout.activity_landing)

        // Find the Get Started button and set click listener
        findViewById<Button>(R.id.getStartedButton).setOnClickListener {
            // Start RegisterActivity and finish the current activity
            startActivity(Intent(this, RegisterActivity::class.java))
            finish()
        }
    }
}

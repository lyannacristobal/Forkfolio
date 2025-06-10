package com.example.forkfolio

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await

class RegisterActivity : AppCompatActivity() {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()

    private lateinit var firstNameEditText: EditText
    private lateinit var surnameEditText: EditText
    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var registerButton: Button
    private lateinit var loginButton: Button
    private lateinit var togglePasswordVisibilityButton: ImageButton

    private var isPasswordVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        // Bind views to their respective IDs
        firstNameEditText = findViewById(R.id.firstNameEditText)
        surnameEditText = findViewById(R.id.surnameEditText)
        emailEditText = findViewById(R.id.emailEditText)
        passwordEditText = findViewById(R.id.passwordEditText)
        registerButton = findViewById(R.id.registerButton)
        loginButton = findViewById(R.id.loginButton)
        togglePasswordVisibilityButton = findViewById(R.id.togglePasswordVisibilityButton)

        // Handle register button click
        registerButton.setOnClickListener {
            val firstName = firstNameEditText.text.toString().trim()
            val surname = surnameEditText.text.toString().trim()
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()

            if (isInputValid(firstName, surname, email, password)) {
                registerUser(firstName, surname, email, password)
            } else {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            }
        }

        // Handle login button click
        loginButton.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish() // Close RegisterActivity
        }

        // Toggle password visibility
        togglePasswordVisibilityButton.setOnClickListener {
            togglePasswordVisibility()
        }
    }

    private fun isInputValid(firstName: String, surname: String, email: String, password: String): Boolean {
        return firstName.isNotEmpty() && surname.isNotEmpty() && email.isNotEmpty() && password.isNotEmpty()
    }

    private fun togglePasswordVisibility() {
        isPasswordVisible = !isPasswordVisible
        passwordEditText.inputType = if (isPasswordVisible) {
            android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
        } else {
            android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        togglePasswordVisibilityButton.setImageResource(
            if (isPasswordVisible) R.drawable.ic_toggle else R.drawable.ic_toggle_off
        )
        passwordEditText.setSelection(passwordEditText.text.length) // Keep the cursor at the end
    }

    private fun registerUser(firstName: String, surname: String, email: String, password: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val result = auth.createUserWithEmailAndPassword(email, password).await()
                val user = result.user
                if (user != null) {
                    saveUserData(user, firstName, surname)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@RegisterActivity, "Registration failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private suspend fun saveUserData(user: FirebaseUser, firstName: String, surname: String) {
        val userData = hashMapOf(
            "firstName" to firstName,
            "surname" to surname,
            "email" to user.email,
            "createdAt" to FieldValue.serverTimestamp(), // Automatically sets createdAt
            "updatedAt" to FieldValue.serverTimestamp()  // Initially the same as createdAt
        )

        try {
            db.collection("users").document(user.uid).set(userData).await()
            withContext(Dispatchers.Main) {
                Toast.makeText(this@RegisterActivity, "User registered successfully", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this@RegisterActivity, LoginActivity::class.java))
                finish()
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(this@RegisterActivity, "Error saving user data: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

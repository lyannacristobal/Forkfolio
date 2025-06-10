package com.example.forkfolio

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ProfileActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var fullNameTextView: TextView
    private lateinit var emailTextView: TextView
    private lateinit var firstNameEditText: EditText
    private lateinit var surnameEditText: EditText
    private lateinit var emailEditText: EditText
    private lateinit var saveButton: Button
    private lateinit var changePasswordButton: Button
    private lateinit var deactivateAccountButton: Button
    private lateinit var editProfileButton: Button
    private lateinit var bottomNavigationView: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Initialize Views
        fullNameTextView = findViewById(R.id.fullNameTextView)
        emailTextView = findViewById(R.id.emailTextView)
        firstNameEditText = findViewById(R.id.firstNameEditText)
        surnameEditText = findViewById(R.id.surnameEditText)
        emailEditText = findViewById(R.id.emailEditText)
        saveButton = findViewById(R.id.saveButton)
        changePasswordButton = findViewById(R.id.changePasswordButton)
        deactivateAccountButton = findViewById(R.id.deactivateAccountButton)
        editProfileButton = findViewById(R.id.editProfileButton)
        bottomNavigationView = findViewById(R.id.bottomNavigationView)

        val user = auth.currentUser
        if (user != null) {
            loadUserProfile(user.uid)
        } else {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            finish()
        }

        // Set click listeners
        editProfileButton.setOnClickListener { enableEditing() }

        saveButton.setOnClickListener {
            val firstName = firstNameEditText.text.toString().trim()
            val surname = surnameEditText.text.toString().trim()
            val email = emailEditText.text.toString().trim()

            if (firstName.isNotEmpty() && surname.isNotEmpty() && email.isNotEmpty()) {
                saveUserProfile(user!!.uid, firstName, surname, email)
            } else {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            }
        }

        changePasswordButton.setOnClickListener {
            showChangePasswordDialog()
        }

        deactivateAccountButton.setOnClickListener {
            confirmDeactivateAccount(user!!.uid)
        }

        setupBottomNavigationView()
    }

    private fun loadUserProfile(userId: String) {
        db.collection("users").document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val firstName = document.getString("firstName") ?: ""
                    val surname = document.getString("surname") ?: ""
                    val email = document.getString("email") ?: ""

                    fullNameTextView.text = "$firstName $surname"
                    emailTextView.text = email
                } else {
                    Toast.makeText(this, "User data not found", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error loading profile: $e", Toast.LENGTH_SHORT).show()
            }
    }

    private fun saveUserProfile(userId: String, firstName: String, surname: String, email: String) {
        val userMap = hashMapOf(
            "firstName" to firstName,
            "surname" to surname,
            "email" to email
        )

        db.collection("users").document(userId)
            .set(userMap)
            .addOnSuccessListener {
                Toast.makeText(this, "Profile updated successfully", Toast.LENGTH_SHORT).show()
                disableEditing()
                loadUserProfile(userId)
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error saving profile: $e", Toast.LENGTH_SHORT).show()
            }
    }

    private fun enableEditing() {
        firstNameEditText.visibility = EditText.VISIBLE
        surnameEditText.visibility = EditText.VISIBLE
        emailEditText.visibility = EditText.VISIBLE
        saveButton.visibility = Button.VISIBLE
        changePasswordButton.visibility = Button.VISIBLE

        val nameParts = fullNameTextView.text.split(" ")
        firstNameEditText.setText(nameParts.getOrElse(0) { "" })
        surnameEditText.setText(nameParts.getOrElse(1) { "" })
        emailEditText.setText(emailTextView.text)

        editProfileButton.visibility = Button.GONE
    }

    private fun disableEditing() {
        firstNameEditText.visibility = EditText.GONE
        surnameEditText.visibility = EditText.GONE
        emailEditText.visibility = EditText.GONE
        saveButton.visibility = Button.GONE
        changePasswordButton.visibility = Button.GONE
        editProfileButton.visibility = Button.VISIBLE
    }

    private fun showChangePasswordDialog() {
        val dialog = AlertDialog.Builder(this)
        val input = EditText(this)
        input.hint = "Enter new password"
        input.inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD

        dialog.setTitle("Change Password")
            .setView(input)
            .setPositiveButton("Change") { _, _ ->
                val newPassword = input.text.toString()
                changePassword(newPassword)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun changePassword(newPassword: String) {
        val user = auth.currentUser
        user?.updatePassword(newPassword)
            ?.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "Password updated successfully", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Error changing password", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun confirmDeactivateAccount(userId: String) {
        val dialog = AlertDialog.Builder(this)
        dialog.setTitle("Deactivate Account")
            .setMessage("Are you sure you want to deactivate your account?")
            .setPositiveButton("Yes") { _, _ ->
                deactivateAccount(userId)
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun deactivateAccount(userId: String) {
        val user = auth.currentUser
        user?.delete()
            ?.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    db.collection("users").document(userId).delete()
                    Toast.makeText(this, "Account deactivated successfully", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, LoginActivity::class.java))
                    finish()
                } else {
                    Toast.makeText(this, "Error deactivating account", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun setupBottomNavigationView() {
        // Select the Profile menu item
        bottomNavigationView.selectedItemId = R.id.profile

        // Set up navigation item click listeners
        bottomNavigationView.setOnNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.home -> {
                    startActivity(Intent(this, MainActivity::class.java))
                    true
                }
                R.id.addRecipe -> {
                    startActivity(Intent(this, RecipeActivity::class.java))
                    true
                }
                R.id.profile -> true // Stay on the Profile screen
                R.id.logout -> {
                    logoutUser()
                    true
                }
                else -> false
            }
        }
    }

    private fun logoutUser() {
        auth.signOut()
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }
}

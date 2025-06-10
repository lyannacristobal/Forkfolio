package com.example.forkfolio

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class DisplayRecipeActivity : AppCompatActivity() {

    private val conn = FirebaseFirestore.getInstance()
    private lateinit var recipeNameTextView: TextView
    private lateinit var descriptionTextView: TextView
    private lateinit var ingredientsTextView: TextView
    private lateinit var instructionsTextView: TextView
    private lateinit var prepTimeTextView: TextView
    private lateinit var cookingTimeTextView: TextView
    private lateinit var categoryTextView: TextView
    private lateinit var editButton: Button
    private lateinit var deleteButton: Button
    private lateinit var bottomNavigationView: BottomNavigationView

    private var recipeId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_display_recipe)

        // Bind views to their respective IDs
        recipeNameTextView = findViewById(R.id.recipeNameTextView)
        descriptionTextView = findViewById(R.id.descriptionTextView)
        ingredientsTextView = findViewById(R.id.ingredientsTextView)
        instructionsTextView = findViewById(R.id.instructionsTextView)
        prepTimeTextView = findViewById(R.id.prepTimeTextView)
        cookingTimeTextView = findViewById(R.id.cookingTimeTextView)
        categoryTextView = findViewById(R.id.categoryTextView)
        editButton = findViewById(R.id.editButton)
        deleteButton = findViewById(R.id.deleteButton)
        bottomNavigationView = findViewById(R.id.bottomNavigationView)

        // Get recipeId from intent
        recipeId = intent.getStringExtra("recipeId")

        if (recipeId.isNullOrEmpty()) {
            Log.e("DisplayRecipeActivity", "Recipe ID is missing!")
            Toast.makeText(this, "Error: Recipe ID is missing.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        Log.d("DisplayRecipeActivity", "Received Recipe ID: $recipeId")

        // Set up real-time listener for recipe data
        loadRecipeDataRealtime(recipeId!!)

        // Edit button click handler
        editButton.setOnClickListener {
            Log.d("DisplayRecipeActivity", "Edit button clicked for Recipe ID: $recipeId")
            val intent = Intent(this, EditRecipeActivity::class.java)
            intent.putExtra("recipeId", recipeId)
            startActivity(intent)
        }

        // Delete button click handler
        deleteButton.setOnClickListener {
            Log.d("DisplayRecipeActivity", "Delete button clicked for Recipe ID: $recipeId")
            deleteRecipe(recipeId!!)
        }

        // Configure BottomNavigationView
        configureBottomNavigation()
    }

    private fun loadRecipeDataRealtime(recipeId: String) {
        Log.d("DisplayRecipeActivity", "Setting up real-time listener for ID: $recipeId")

        conn.collection("recipes").document(recipeId)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("DisplayRecipeActivity", "Error listening for changes: ${e.message}")
                    Toast.makeText(this, "Error listening for changes: ${e.message}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    Log.d("DisplayRecipeActivity", "Document changed: ${snapshot.data}")
                    val recipe = Recipe(
                        recipeName = snapshot.getString("recipeName") ?: "N/A",
                        description = snapshot.getString("description") ?: "N/A",
                        mealCategory = snapshot.getString("mealCategory") ?: "N/A",
                        ingredients = snapshot.get("ingredients") as? List<String> ?: emptyList(),
                        instructions = snapshot.getString("instructions") ?: "N/A",
                        prepTime = snapshot.getString("prepTime") ?: "N/A",
                        cookingTime = snapshot.getString("cookingTime") ?: "N/A"
                    )
                    displayRecipeData(recipe)
                } else {
                    Log.e("DisplayRecipeActivity", "Document snapshot is null or does not exist.")
                    Toast.makeText(this, "Recipe not found.", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
    }

    private fun displayRecipeData(recipe: Recipe) {
        Log.d("DisplayRecipeActivity", "Displaying recipe data")
        recipeNameTextView.text = recipe.recipeName
        descriptionTextView.text = recipe.description
        ingredientsTextView.text = recipe.ingredients.joinToString(separator = "\n") { "- $it" }
        instructionsTextView.text = recipe.instructions
        prepTimeTextView.text = "Prep Time: ${recipe.prepTime} Minutes"
        cookingTimeTextView.text = "Cooking Time: ${recipe.cookingTime} Minutes"
        categoryTextView.text = "Category: ${recipe.mealCategory}"
    }

    private fun deleteRecipe(recipeId: String) {
        // Show a confirmation dialog
        val alertDialog = android.app.AlertDialog.Builder(this)
            .setTitle("Delete Recipe")
            .setMessage("Are you sure you want to delete this recipe? This action cannot be undone.")
            .setPositiveButton("Yes") { _, _ ->
                // Proceed with deletion
                conn.collection("recipes").document(recipeId)
                    .delete()
                    .addOnSuccessListener {
                        Log.d("DisplayRecipeActivity", "Recipe deleted successfully for ID: $recipeId")
                        Toast.makeText(this, "Recipe deleted successfully.", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                    .addOnFailureListener { e ->
                        Log.e("DisplayRecipeActivity", "Error deleting recipe: ${e.message}")
                        Toast.makeText(this, "Error deleting recipe: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("No", null) // Dismiss the dialog
            .create()
        alertDialog.show()
    }

    private fun configureBottomNavigation() {
        bottomNavigationView.labelVisibilityMode = BottomNavigationView.LABEL_VISIBILITY_LABELED

        bottomNavigationView.setOnNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.home -> {
                    val intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)
                    overridePendingTransition(0, 0) // Optional: No animation
                    true
                }
                R.id.addRecipe -> {
                    val intent = Intent(this, RecipeActivity::class.java)
                    startActivity(intent)
                    overridePendingTransition(0, 0)
                    true
                }
                R.id.profile -> {
                    val intent = Intent(this, ProfileActivity::class.java)
                    startActivity(intent)
                    overridePendingTransition(0, 0)
                    true
                }
                R.id.logout -> {
                    logoutUser()
                    true
                }
                else -> false
            }
        }
    }


    private fun logoutUser() {
        FirebaseAuth.getInstance().signOut()
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }
}

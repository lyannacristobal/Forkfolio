package com.example.forkfolio

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.FirebaseApp
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.bottomnavigation.LabelVisibilityMode
import com.google.android.material.navigation.NavigationBarView

class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var searchView: SearchView
    private lateinit var recipeRecyclerView: RecyclerView
    private lateinit var emptyPlaceholderTextView: TextView
    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var categoriesRecyclerView: RecyclerView

    private val recipeList = mutableListOf<Recipe>()
    private val conn = FirebaseFirestore.getInstance()
    private lateinit var recipeAdapter: RecipeAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)
        setContentView(R.layout.activity_main)

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()

        // Initialize Views
        searchView = findViewById(R.id.searchView)
        recipeRecyclerView = findViewById(R.id.recipeRecyclerView)
        emptyPlaceholderTextView = findViewById(R.id.emptyPlaceholderTextView)
        bottomNavigationView = findViewById(R.id.bottomNavigationView)
        categoriesRecyclerView = findViewById(R.id.categoriesRecyclerView)

        // Set up RecyclerView
        recipeRecyclerView.layoutManager = LinearLayoutManager(this)
        recipeAdapter = RecipeAdapter(recipeList)
        recipeRecyclerView.adapter = recipeAdapter

        // Set up real-time updates
        setupRealTimeUpdates()

        // Configure Bottom Navigation to always show labels
        bottomNavigationView.labelVisibilityMode = NavigationBarView.LABEL_VISIBILITY_LABELED

        // Bottom Navigation Item Selection
        bottomNavigationView.setOnNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.home -> {
                    return@setOnNavigationItemSelectedListener true
                }
                R.id.addRecipe -> {
                    val intent = Intent(this, RecipeActivity::class.java)
                    startActivity(intent)
                    return@setOnNavigationItemSelectedListener true
                }
                R.id.profile -> {
                    val intent = Intent(this, ProfileActivity::class.java)
                    startActivity(intent)
                    return@setOnNavigationItemSelectedListener true
                }
                R.id.logout -> {
                    logoutUser()
                    return@setOnNavigationItemSelectedListener true
                }
                else -> false
            }
        }

        // Search functionality
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                filterRecipes(newText)
                return true
            }
        })

        // Define categories
        val categories = listOf(
            Category("All Recipes", R.drawable.book),
            Category("Breakfast", R.drawable.breakfast),
            Category("Lunch", R.drawable.lunch),
            Category("Dinner", R.drawable.dinner),
            Category("Dessert", R.drawable.dessert),
        )

        // Set up Categories RecyclerView
        categoriesRecyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        val categoryAdapter = CategoryAdapter(categories) { categoryName ->
            filterByCategory(categoryName)
        }
        categoriesRecyclerView.adapter = categoryAdapter
    }

    private fun filterByCategory(categoryName: String) {
        val filteredRecipes = if (categoryName == "All Recipes") {
            recipeList // Show all recipes
        } else {
            recipeList.filter { it.mealCategory.equals(categoryName, ignoreCase = true) }
        }

        recipeAdapter.updateList(filteredRecipes)

        // Toggle visibility of RecyclerView and Placeholder
        togglePlaceholderVisibility(filteredRecipes.isEmpty())
    }

    // Log out the user
    private fun logoutUser() {
        FirebaseAuth.getInstance().signOut()
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }

    // Real-time updates for recipes
    private fun setupRealTimeUpdates() {
        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            conn.collection("recipes")
                .whereEqualTo("userId", user.uid)
                .addSnapshotListener { snapshot, e ->
                    if (e != null) {
                        Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                        return@addSnapshotListener
                    }

                    if (snapshot != null) {
                        val newRecipeList = mutableListOf<Recipe>()
                        for (document in snapshot.documents) {
                            val recipe = Recipe(
                                recipeName = document.getString("recipeName") ?: "",
                                description = document.getString("description") ?: "",
                                mealCategory = document.getString("mealCategory") ?: "",
                                ingredients = document.get("ingredients") as? List<String> ?: emptyList(),
                                prepTime = document.getString("prepTime") ?: "",
                                cookingTime = document.getString("cookingTime") ?: "",
                                createdAt = document.get("createdAt"),
                                userId = document.getString("userId") ?: "",
                                recipeId = document.id
                            )
                            newRecipeList.add(recipe)
                        }
                        recipeList.clear()
                        recipeList.addAll(newRecipeList)
                        recipeAdapter.updateList(recipeList)

                        // Toggle visibility of RecyclerView and Placeholder
                        togglePlaceholderVisibility(recipeList.isEmpty())
                    }
                }
        } else {
            Toast.makeText(this, "User not logged in.", Toast.LENGTH_SHORT).show()
        }
    }

    // Filter recipes based on search input
    private fun filterRecipes(query: String?) {
        val filteredRecipes = recipeList.filter {
            it.recipeName.contains(query ?: "", ignoreCase = true)
        }
        recipeAdapter.updateList(filteredRecipes)

        // Toggle visibility of RecyclerView and Placeholder
        togglePlaceholderVisibility(filteredRecipes.isEmpty())
    }

    // Toggle visibility of RecyclerView and Placeholder
    private fun togglePlaceholderVisibility(isEmpty: Boolean) {
        if (isEmpty) {
            emptyPlaceholderTextView.visibility = View.VISIBLE
            recipeRecyclerView.visibility = View.GONE
        } else {
            emptyPlaceholderTextView.visibility = View.GONE
            recipeRecyclerView.visibility = View.VISIBLE
        }
    }

    override fun onResume() {
        super.onResume()
        fetchRecipes() // Fallback to fetch the latest recipes when resuming
    }

    // Fallback to fetch recipes (Optional)
    private fun fetchRecipes() {
        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            conn.collection("recipes")
                .whereEqualTo("userId", user.uid)
                .get()
                .addOnSuccessListener { documents ->
                    val newRecipeList = mutableListOf<Recipe>()
                    for (document in documents) {
                        val recipe = Recipe(
                            recipeName = document.getString("recipeName") ?: "",
                            description = document.getString("description") ?: "",
                            mealCategory = document.getString("mealCategory") ?: "",
                            ingredients = document.get("ingredients") as? List<String> ?: emptyList(),
                            prepTime = document.getString("prepTime") ?: "",
                            cookingTime = document.getString("cookingTime") ?: "",
                            createdAt = document.get("createdAt"),
                            userId = document.getString("userId") ?: "",
                            recipeId = document.id
                        )
                        newRecipeList.add(recipe)
                    }
                    recipeList.clear()
                    recipeList.addAll(newRecipeList)
                    recipeAdapter.updateList(recipeList)

                    // Toggle visibility of RecyclerView and Placeholder
                    togglePlaceholderVisibility(recipeList.isEmpty())
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error fetching recipes: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        } else {
            Toast.makeText(this, "User not logged in.", Toast.LENGTH_SHORT).show()
        }
    }
}

package com.example.forkfolio

import android.content.Intent
import android.os.Bundle
import android.util.TypedValue
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.android.material.bottomnavigation.BottomNavigationView

class RecipeActivity : AppCompatActivity() {

    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
    private lateinit var auth: FirebaseAuth
    private lateinit var bottomNavigationView: BottomNavigationView

    private lateinit var recipeNameEditText: EditText
    private lateinit var descriptionEditText: EditText
    private lateinit var mealCategorySpinner: Spinner
    private lateinit var ingredientsLayout: LinearLayout
    private lateinit var addIngredientButton: Button
    private lateinit var instructionsEditText: EditText
    private lateinit var prepTimeEditText: EditText
    private lateinit var cookingTimeEditText: EditText
    private lateinit var saveRecipeButton: Button

    private val ingredientList = mutableListOf<LinearLayout>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recipe)

        // Initialize FirebaseAuth
        auth = FirebaseAuth.getInstance()

        // Bind views
        recipeNameEditText = findViewById(R.id.recipeNameEditText)
        descriptionEditText = findViewById(R.id.descriptionEditText)
        mealCategorySpinner = findViewById(R.id.mealCategorySpinner)
        ingredientsLayout = findViewById(R.id.ingredientsLayout)
        addIngredientButton = findViewById(R.id.addIngredientButton)
        instructionsEditText = findViewById(R.id.instructionsEditText)
        prepTimeEditText = findViewById(R.id.prepTimeEditText)
        cookingTimeEditText = findViewById(R.id.cookingTimeEditText)
        saveRecipeButton = findViewById(R.id.saveRecipeButton)
        bottomNavigationView = findViewById(R.id.bottomNavigationView)

        // Set up Meal Category Spinner
        val categories = arrayOf("Breakfast", "Lunch", "Dinner", "Dessert")
        val adapter = ArrayAdapter(this, R.layout.spinner_item, categories)
        adapter.setDropDownViewResource(R.layout.spinner_item)
        mealCategorySpinner.adapter = adapter

        // Add ingredient field when the user presses the "Add Ingredient" button
        addIngredientButton.setOnClickListener {
            addIngredientField(true)
        }

        // Save Recipe
        saveRecipeButton.setOnClickListener {
            saveRecipeToDatabase()
        }

        // Configure Bottom Navigation
        configureBottomNavigation()
    }

    private fun configureBottomNavigation() {
        // Always display labels
        bottomNavigationView.labelVisibilityMode = BottomNavigationView.LABEL_VISIBILITY_LABELED

        bottomNavigationView.selectedItemId = R.id.addRecipe

        // Handle navigation clicks
        bottomNavigationView.setOnItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.home -> {
                    val intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)
                    overridePendingTransition(0, 0)
                    return@setOnItemSelectedListener true
                }
                R.id.addRecipe -> return@setOnItemSelectedListener true // Stay on this page
                R.id.profile -> {
                    val intent = Intent(this, ProfileActivity::class.java)
                    startActivity(intent)
                    overridePendingTransition(0, 0)
                    return@setOnItemSelectedListener true
                }
                R.id.logout -> {
                    logoutUser()
                    true
                }
                else -> false
            }
        }

        // Prevent any item from appearing selected by default
        bottomNavigationView.menu.setGroupCheckable(0, false, true)
    }

    private fun addIngredientField(withDeleteButton: Boolean) {
        val ingredientLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 8, 0, 8) // Optional: Add margin between fields
            }
        }

        val ingredientEditText = EditText(this).apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            hint = "Enter ingredient"
            setBackgroundResource(R.drawable.circular_edittext_bg)
            setPadding(12, 12, 12, 12)
            typeface = ResourcesCompat.getFont(this@RecipeActivity, R.font.poppins_light)
            inputType = android.text.InputType.TYPE_CLASS_TEXT
        }

        val deleteButton = Button(this).apply {
            text = "Delete"
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setTextColor(resources.getColor(android.R.color.white, null)) // White text
            backgroundTintList = getColorStateList(R.color.red)
            typeface = ResourcesCompat.getFont(this@RecipeActivity, R.font.poppins_medium)
            setOnClickListener {
                ingredientsLayout.removeView(ingredientLayout)
                ingredientList.remove(ingredientLayout)
            }
        }

        // Add views to the ingredient layout
        ingredientLayout.addView(ingredientEditText)
        if (withDeleteButton) {
            ingredientLayout.addView(deleteButton)
        }

        // Add the ingredient layout to the parent layout
        ingredientsLayout.addView(ingredientLayout)
        ingredientList.add(ingredientLayout)
    }

    private fun saveRecipeToDatabase() {
        val recipeName = recipeNameEditText.text.toString()
        val description = descriptionEditText.text.toString()
        val mealCategory = mealCategorySpinner.selectedItem.toString()
        val instructions = instructionsEditText.text.toString()
        val prepTime = prepTimeEditText.text.toString()
        val cookingTime = cookingTimeEditText.text.toString()

        if (recipeName.isEmpty() || description.isEmpty() || instructions.isEmpty()) {
            Toast.makeText(this, "All fields must be filled", Toast.LENGTH_SHORT).show()
            return
        }

        val ingredients = mutableListOf<String>()
        for (ingredientLayout in ingredientList) {
            val ingredientEditText = ingredientLayout.getChildAt(0) as EditText
            val ingredient = ingredientEditText.text.toString()
            if (ingredient.isEmpty()) {
                Toast.makeText(this, "Please enter all ingredients", Toast.LENGTH_SHORT).show()
                return
            }
            ingredients.add(ingredient)
        }

        if (!isValidTime(prepTime) || !isValidTime(cookingTime)) {
            Toast.makeText(this, "Invalid preparation or cooking time", Toast.LENGTH_SHORT).show()
            return
        }

        val userId = auth.currentUser?.uid ?: return

        val recipeData = hashMapOf(
            "recipeName" to recipeName,
            "description" to description,
            "mealCategory" to mealCategory,
            "ingredients" to ingredients,
            "instructions" to instructions,
            "prepTime" to prepTime,
            "cookingTime" to cookingTime,
            "createdAt" to FieldValue.serverTimestamp(),
            "userId" to userId
        )

        db.collection("recipes")
            .add(recipeData)
            .addOnSuccessListener {
                Toast.makeText(this, "Recipe saved successfully", Toast.LENGTH_SHORT).show()
                redirectToMainActivity()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error saving recipe: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun isValidTime(time: String): Boolean {
        return time.toIntOrNull()?.let { it > 0 } == true
    }

    private fun redirectToMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun logoutUser() {
        FirebaseAuth.getInstance().signOut()
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }
}

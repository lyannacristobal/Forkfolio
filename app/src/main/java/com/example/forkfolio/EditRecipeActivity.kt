package com.example.forkfolio

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

class EditRecipeActivity : AppCompatActivity() {

    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
    private lateinit var auth: FirebaseAuth
    private lateinit var bottomNavigationView: BottomNavigationView

    private lateinit var recipeNameEditText: EditText
    private lateinit var descriptionEditText: EditText
    private lateinit var instructionsEditText: EditText
    private lateinit var mealCategorySpinner: Spinner
    private lateinit var ingredientsLayout: LinearLayout
    private lateinit var addIngredientButton: Button
    private lateinit var prepTimeEditText: EditText
    private lateinit var cookingTimeEditText: EditText
    private lateinit var saveButton: Button
    private lateinit var deleteButton: Button

    private val ingredientList = mutableListOf<LinearLayout>()
    private var recipeId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_recipe)

        auth = FirebaseAuth.getInstance()

        // Initialize Views
        recipeNameEditText = findViewById(R.id.recipeNameEditText)
        descriptionEditText = findViewById(R.id.descriptionEditText)
        instructionsEditText = findViewById(R.id.instructionsEditText)
        mealCategorySpinner = findViewById(R.id.mealCategorySpinner)
        ingredientsLayout = findViewById(R.id.ingredientsLayout)
        addIngredientButton = findViewById(R.id.addIngredientButton)
        prepTimeEditText = findViewById(R.id.prepTimeEditText)
        cookingTimeEditText = findViewById(R.id.cookingTimeEditText)
        saveButton = findViewById(R.id.saveRecipeButton)
        deleteButton = findViewById(R.id.deleteRecipeButton)
        bottomNavigationView = findViewById(R.id.bottomNavigationView)

        recipeId = intent.getStringExtra("recipeId")

        if (recipeId.isNullOrEmpty()) {
            Toast.makeText(this, "Recipe ID is missing.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupSpinner()
        loadRecipeData()
        configureBottomNavigation()

        addIngredientButton.setOnClickListener { addIngredientField(true) }
        saveButton.setOnClickListener { updateRecipe() }
        deleteButton.setOnClickListener { confirmDeleteRecipe() }
    }

    private fun setupSpinner() {
        val categories = resources.getStringArray(R.array.categories)
        val adapter = ArrayAdapter(this, R.layout.spinner_item, categories)
        mealCategorySpinner.adapter = adapter
    }

    private fun loadRecipeData() {
        recipeId?.let { id ->
            db.collection("recipes").document(id).get().addOnSuccessListener { document ->
                if (document.exists()) {
                    recipeNameEditText.setText(document.getString("recipeName"))
                    descriptionEditText.setText(document.getString("description"))
                    instructionsEditText.setText(document.getString("instructions"))
                    prepTimeEditText.setText(document.getString("prepTime"))
                    cookingTimeEditText.setText(document.getString("cookingTime"))

                    val mealCategory = document.getString("mealCategory")
                    val categories = resources.getStringArray(R.array.categories)
                    mealCategory?.let {
                        mealCategorySpinner.setSelection(categories.indexOf(it))
                    }

                    val ingredients = document.get("ingredients") as? List<String> ?: emptyList()
                    ingredients.forEach { ingredient ->
                        addIngredientField(false).apply {
                            (getChildAt(0) as EditText).setText(ingredient)
                        }
                    }
                } else {
                    Toast.makeText(this, "Recipe not found.", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }.addOnFailureListener { e ->
                Toast.makeText(this, "Error loading recipe: ${e.message}", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun addIngredientField(withDeleteButton: Boolean): LinearLayout {
        // Create a horizontal LinearLayout for the ingredient field and button
        val ingredientLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 8, 0, 8) // Add margin between ingredient fields
            }
        }

        // Create the EditText for entering the ingredient
        val ingredientEditText = EditText(this).apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            hint = "Enter ingredient"
            setBackgroundResource(R.drawable.circular_edittext_bg) // Background drawable
            setPadding(12, 12, 12, 12) // Padding inside the EditText
            typeface = ResourcesCompat.getFont(this@EditRecipeActivity, R.font.poppins_light) // Custom font
            inputType = android.text.InputType.TYPE_CLASS_TEXT // Set text input type
        }

        // Add the EditText to the LinearLayout
        ingredientLayout.addView(ingredientEditText)

        // Optionally add the Delete button
        if (withDeleteButton) {
            val deleteButton = Button(this).apply {
                text = "Delete"
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                setTextColor(resources.getColor(android.R.color.white, null)) // White text color
                backgroundTintList = getColorStateList(R.color.red) // Use red color from colors.xml
                typeface = ResourcesCompat.getFont(this@EditRecipeActivity, R.font.poppins_medium) // Custom font
                setPadding(12, 12, 12, 12) // Padding for the button
                setOnClickListener {
                    ingredientsLayout.removeView(ingredientLayout)
                    ingredientList.remove(ingredientLayout)
                }
            }

            // Add the Delete button to the LinearLayout
            ingredientLayout.addView(deleteButton)
        }

        // Add the entire ingredient layout to the parent LinearLayout
        ingredientsLayout.addView(ingredientLayout)
        ingredientList.add(ingredientLayout)

        return ingredientLayout
    }


    private fun updateRecipe() {
        val recipeName = recipeNameEditText.text.toString().trim()
        val description = descriptionEditText.text.toString().trim()
        val instructions = instructionsEditText.text.toString().trim()
        val mealCategory = mealCategorySpinner.selectedItem.toString()
        val prepTime = prepTimeEditText.text.toString().trim()
        val cookingTime = cookingTimeEditText.text.toString().trim()

        if (recipeName.isEmpty() || description.isEmpty() || prepTime.isEmpty() || cookingTime.isEmpty() || instructions.isEmpty()) {
            Toast.makeText(this, "Please fill out all fields.", Toast.LENGTH_SHORT).show()
            return
        }

        val ingredients = ingredientList.map { layout ->
            (layout.getChildAt(0) as EditText).text.toString().trim()
        }.filter { it.isNotEmpty() }

        if (ingredients.isEmpty()) {
            Toast.makeText(this, "Please add at least one ingredient.", Toast.LENGTH_SHORT).show()
            return
        }

        val recipeData = hashMapOf(
            "recipeName" to recipeName,
            "description" to description,
            "instructions" to instructions,
            "mealCategory" to mealCategory,
            "prepTime" to prepTime,
            "cookingTime" to cookingTime,
            "ingredients" to ingredients,
            "updatedAt" to FieldValue.serverTimestamp()
        )

        recipeId?.let { id ->
            db.collection("recipes").document(id).set(recipeData, SetOptions.merge()).addOnSuccessListener {
                Toast.makeText(this, "Recipe updated successfully.", Toast.LENGTH_SHORT).show()
                finish()
            }.addOnFailureListener {
                Toast.makeText(this, "Failed to update recipe. Please try again.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun confirmDeleteRecipe() {
        recipeId?.let { id ->
            AlertDialog.Builder(this).apply {
                setTitle("Confirm Delete")
                setMessage("Are you sure you want to delete this recipe?")
                setPositiveButton("Yes") { _, _ ->
                    db.collection("recipes").document(id).delete().addOnSuccessListener {
                        Toast.makeText(this@EditRecipeActivity, "Recipe deleted successfully.", Toast.LENGTH_SHORT).show()
                        finish()
                    }.addOnFailureListener {
                        Toast.makeText(this@EditRecipeActivity, "Failed to delete recipe. Please try again.", Toast.LENGTH_SHORT).show()
                    }
                }
                setNegativeButton("No", null)
            }.show()
        }
    }

    private fun configureBottomNavigation() {
        bottomNavigationView.labelVisibilityMode = BottomNavigationView.LABEL_VISIBILITY_LABELED

        bottomNavigationView.selectedItemId = R.id.addRecipe


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

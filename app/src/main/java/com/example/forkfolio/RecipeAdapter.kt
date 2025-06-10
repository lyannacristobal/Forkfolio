package com.example.forkfolio

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore

class RecipeAdapter(private var recipes: List<Recipe>) : RecyclerView.Adapter<RecipeAdapter.RecipeViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecipeViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_recipe, parent, false)
        return RecipeViewHolder(view)
    }

    override fun onBindViewHolder(holder: RecipeViewHolder, position: Int) {
        val recipe = recipes[position]
        holder.titleTextView.text = recipe.recipeName
        holder.descriptionTextView.text = recipe.description
        holder.categoryTextView.text = recipe.mealCategory // Add this line to display the category

        // Open DisplayRecipeActivity when card is clicked
        holder.itemView.setOnClickListener {
            val context = holder.itemView.context
            val intent = Intent(context, DisplayRecipeActivity::class.java)
            intent.putExtra("recipeId", recipe.recipeId)
            context.startActivity(intent)
        }

        // Edit button click
        holder.editButton.setOnClickListener {
            val context = holder.itemView.context
            val intent = Intent(context, EditRecipeActivity::class.java)
            intent.putExtra("recipeId", recipe.recipeId)
            context.startActivity(intent)
        }

        // Delete button click
        holder.deleteButton.setOnClickListener {
            showDeleteConfirmationDialog(holder.itemView.context, recipe.recipeId, position)
        }
    }

    override fun getItemCount(): Int = recipes.size

    fun updateList(newRecipes: List<Recipe>) {
        recipes = newRecipes
        notifyDataSetChanged()
    }

    private fun showDeleteConfirmationDialog(context: Context, recipeId: String, position: Int) {
        val alertDialog = AlertDialog.Builder(context)
            .setTitle("Delete Recipe")
            .setMessage("Are you sure you want to delete this recipe? This action cannot be undone.")
            .setPositiveButton("Yes") { _, _ ->
                deleteRecipeFromDatabase(context, recipeId, position)
            }
            .setNegativeButton("No", null)
            .create()
        alertDialog.show()
    }

    private fun deleteRecipeFromDatabase(context: Context, recipeId: String, position: Int) {
        val db = FirebaseFirestore.getInstance()
        db.collection("recipes").document(recipeId)
            .delete()
            .addOnSuccessListener {
                Toast.makeText(context, "Recipe deleted successfully", Toast.LENGTH_SHORT).show()
                // Remove the recipe from the list and update the adapter
                val updatedRecipes = recipes.toMutableList()
                updatedRecipes.removeAt(position)
                updateList(updatedRecipes)
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Error deleting recipe: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    class RecipeViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val titleTextView: TextView = view.findViewById(R.id.recipeTitleTextView)
        val categoryTextView: TextView = view.findViewById(R.id.categoryTextView) // Bind the category TextView
        val descriptionTextView: TextView = view.findViewById(R.id.DescriptionTextView)
        val editButton: Button = view.findViewById(R.id.editButton)
        val deleteButton: Button = view.findViewById(R.id.deleteButton)
    }
}

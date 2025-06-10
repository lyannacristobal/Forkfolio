package com.example.forkfolio

data class Recipe(
    val recipeName: String = "",
    val description: String = "",
    val mealCategory: String = "",
    val ingredients: List<String> = emptyList(),
    val instructions: String = "",
    val prepTime: String = "",
    val cookingTime: String = "",
    val createdAt: Any? = null, // Using Any? because it's a timestamp
    val userId: String = "", // New field for userId
    val recipeId: String = "",
    )

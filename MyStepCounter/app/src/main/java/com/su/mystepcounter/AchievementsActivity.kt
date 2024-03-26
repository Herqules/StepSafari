package com.su.mystepcounter


import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity

class AchievementsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_achievements)

        // Sample data for the list
        val achievementsList = listOf(
            "Reached 10,000 steps",
            "Completed 5 miles",
            "10-day streak",
            // Add more achievements as desired
        )

        // Find the ListView
        val listView = findViewById<ListView>(R.id.listViewAchievements)

        // Create an ArrayAdapter
        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_list_item_1,
            achievementsList
        )

        // Set the adapter for the ListView
        listView.adapter = adapter
    }
}

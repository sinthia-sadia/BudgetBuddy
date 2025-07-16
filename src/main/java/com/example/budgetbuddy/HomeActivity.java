package com.example.budgetbuddy;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class HomeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        // Initialize BottomNavigationView
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);

        // Set up NavHostFragment and NavController
        NavHostFragment navHostFragment =
                (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);

        // Check if navHostFragment is null to avoid NullPointerException
        if (navHostFragment != null) {
            NavController navController = navHostFragment.getNavController();
            // Set up NavigationUI with BottomNavigationView
            NavigationUI.setupWithNavController(bottomNav, navController);
        } else {
            // Handle the case where the navHostFragment is not found
            throw new IllegalStateException("NavHostFragment is null. Make sure it's included in the layout.");
        }
    }
}

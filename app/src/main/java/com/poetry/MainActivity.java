package com.poetry;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import com.poetry.data.LearningDatabase;
import com.poetry.data.UserProfile;
import com.poetry.domain.LearningEngine;
import com.poetry.ui.widget.ConfettiView;

import java.time.LocalDate;

public class MainActivity extends AppCompatActivity {

    private ConfettiView confettiView;
    private NavController navController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        confettiView = findViewById(R.id.confetti_view);

        // 设置 Navigation Component + 底部导航
        NavHostFragment navHost = (NavHostFragment) getSupportFragmentManager()
            .findFragmentById(R.id.nav_host_fragment);
        if (navHost != null) {
            navController = navHost.getNavController();
            BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
            NavigationUI.setupWithNavController(bottomNav, navController);
        }

        initDatabase();
    }

    private void initDatabase() {
        LearningDatabase db = LearningDatabase.getInstance(this);
        new Thread(() -> {
            UserProfile profile = db.poemDao().getUserProfileSync();
            if (profile == null) {
                profile = new UserProfile();
                String today = LocalDate.now().toString();
                profile.lastActiveDate = today;
                profile.streak = 1;
                db.poemDao().insertUserProfile(profile);
            } else {
                String today = LocalDate.now().toString();
                if (!today.equals(profile.lastActiveDate)) {
                    profile.streak = LearningEngine.calcStreak(
                        profile.lastActiveDate, profile.streak);
                    profile.lastActiveDate = today;
                    db.poemDao().updateStreak(profile.streak, today);

                    final int streak = profile.streak;
                    if (streak >= 7 && streak % 7 == 0) {
                        runOnUiThread(() -> Toast.makeText(this,
                            "🎉 你已经连续学习 " + streak + " 天啦！太棒了！",
                            Toast.LENGTH_LONG).show());
                    }
                }
            }
        }).start();
    }

    public void celebrate() {
        if (confettiView != null) {
            confettiView.celebrate();
        }
    }
}

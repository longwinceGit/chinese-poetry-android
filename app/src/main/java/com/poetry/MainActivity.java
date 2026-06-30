package com.poetry;

import android.app.AlertDialog;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.poetry.data.LearningDatabase;
import com.poetry.data.UserProfile;
import com.poetry.data.model.Poem;
import com.poetry.domain.LearningEngine;
import com.poetry.ui.detail.DetailFragment;
import com.poetry.ui.game.CoupletGameFragment;
import com.poetry.ui.game.MatchGameFragment;
import com.poetry.ui.home.HomeFragment;
import com.poetry.ui.profile.ProfileFragment;
import com.poetry.ui.quiz.QuizFragment;
import com.poetry.ui.widget.ConfettiView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements HomeFragment.OnPoemClickListener, HomeFragment.OnNavigateListener {

    private ConfettiView confettiView;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        confettiView = findViewById(R.id.confetti_view);

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, new HomeFragment())
                .commit();
        }

        initDatabase();
    }

    private void initDatabase() {
        LearningDatabase db = LearningDatabase.getInstance(this);
        new Thread(() -> {
            UserProfile profile = db.poemDao().getUserProfileSync();
            if (profile == null) {
                profile = new UserProfile();
                String today = dateFormat.format(new Date());
                profile.lastActiveDate = today;
                profile.streak = 1;
                db.poemDao().insertUserProfile(profile);
            } else {
                String today = dateFormat.format(new Date());
                if (!today.equals(profile.lastActiveDate)) {
                    profile.streak = LearningEngine.calcStreak(profile.lastActiveDate, profile.streak);
                    profile.lastActiveDate = today;
                    db.poemDao().updateStreak(profile.streak, today);

                    final int streak = profile.streak;
                    if (streak >= 7 && streak % 7 == 0) {
                        runOnUiThread(() ->
                            Toast.makeText(this, "🎉 你已经连续学习 " + streak + " 天啦！太棒了！", Toast.LENGTH_LONG).show()
                        );
                    }
                }
            }
        }).start();
    }

    @Override
    public void onPoemClick(Poem poem) {
        DetailFragment fragment = DetailFragment.newInstance(poem);
        getSupportFragmentManager().beginTransaction()
            .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left,
                                 R.anim.slide_in_left, R.anim.slide_out_right)
            .replace(R.id.fragment_container, fragment)
            .addToBackStack(null)
            .commit();
    }

    private void navigateTo(Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
            .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left,
                                 R.anim.slide_in_left, R.anim.slide_out_right)
            .replace(R.id.fragment_container, fragment)
            .addToBackStack(null)
            .commit();
    }

    @Override
    public void openLearning() {
        navigateTo(new ProfileFragment());
    }

    @Override
    public void openGameMenu() {
        String[] games = {"诗词接龙", "翻翻卡配对", "填空背诵"};
        new AlertDialog.Builder(this)
            .setTitle("🎮 选择游戏")
            .setItems(games, (dialog, which) -> {
                Fragment f;
                switch (which) {
                    case 0: f = new CoupletGameFragment(); break;
                    case 1: f = new MatchGameFragment(); break;
                    case 2: f = new QuizFragment(); break;
                    default: return;
                }
                navigateTo(f);
            })
            .show();
    }

    public void celebrate() {
        if (confettiView != null) {
            confettiView.celebrate();
        }
    }
}

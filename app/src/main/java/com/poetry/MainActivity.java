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

/**
 * 主 Activity（诗词乐园入口）。
 * <p>
 * 负责应用启动时的核心初始化工作，包括：
 * <ul>
 *     <li>设置 Navigation Component 与底部导航栏绑定，实现 Fragment 切换；</li>
 *     <li>初始化用户学习数据库，创建或更新用户档案；</li>
 *     <li>计算连续学习天数（streak），并在达到里程碑时弹出庆祝提示；</li>
 *     <li>提供全屏撒花（confetti）庆祝动画的触发入口。</li>
 * </ul>
 */
public class MainActivity extends AppCompatActivity {

    /** 撒花动画视图，用于里程碑庆祝效果 */
    private ConfettiView confettiView;

    /** Navigation Component 控制器，管理底部导航的 Fragment 切换 */
    private NavController navController;

    /**
     * Activity 生命周期入口。
     * <p>
     * 主要完成：
     * <ol>
     *     <li>加载布局并获取撒花视图引用；</li>
     *     <li>设置 Navigation Component：找到 NavHostFragment，绑定底部导航栏，
     *         实现 Fragment 间自动切换与回退栈管理；</li>
     *     <li>调用 {@link #initDatabase()} 完成用户数据库初始化。</li>
     * </ol>
     *
     * @param savedInstanceState 上次销毁时保存的状态数据，首次创建时为 null
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 获取撒花动画视图引用，供 celebrate() 方法使用
        confettiView = findViewById(R.id.confetti_view);

        // 设置 Navigation Component + 底部导航

        // 1. 从 FragmentManager 中找到 NavHostFragment（导航宿主）
        NavHostFragment navHost = (NavHostFragment) getSupportFragmentManager()
            .findFragmentById(R.id.nav_host_fragment);
        if (navHost != null) {
            // 2. 获取 NavController，用于管理 Fragment 间的导航跳转
            navController = navHost.getNavController();

            // 3. 获取底部导航控件并绑定 NavController
            //    setupWithNavController 会自动处理：
            //    - 菜单项 id 与 destination id 的匹配
            //    - 选中状态的同步
            //    - 回退栈的自动处理
            BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
            NavigationUI.setupWithNavController(bottomNav, navController);
        }

        // 初始化用户学习数据库（在后台线程异步执行）
        initDatabase();
    }

    /**
     * 初始化用户学习数据库，管理用户档案与连续学习天数。
     * <p>
     * 在后台线程执行，逻辑如下：
     * <ol>
     *     <li>查询已有的用户档案；</li>
     *     <li>若不存在（首次使用），创建新档案，设置当天日期为上次活跃日期，
     *         streak（连续学习天数）初始化为 1；</li>
     *     <li>若已存在，计算自上次活跃日期至今的 streak：</li>
     *     <ul>
     *         <li>若上次活跃日期为昨天，streak 加 1（连续学习）；</li>
     *         <li>若超过一天未学习，streak 重置为 1（间断后重新计数）；</li>
     *     </ul>
     *     <li>当 streak 达到 7 的倍数（7、14、21、28……）时，
     *         在主线程弹出 Toast 里程碑庆祝提示。</li>
     * </ol>
     */
    private void initDatabase() {
        // 获取数据库单例
        LearningDatabase db = LearningDatabase.getInstance(this);

        // 在后台线程执行数据库操作，避免阻塞 UI 线程
        new Thread(() -> {
            // 查询用户档案（同步操作）
            UserProfile profile = db.poemDao().getUserProfileSync();
            if (profile == null) {
                // 首次使用：创建新档案
                profile = new UserProfile();
                String today = LocalDate.now().toString();
                profile.lastActiveDate = today;
                profile.streak = 1;
                db.poemDao().insertUserProfile(profile);
            } else {
                // 老用户：检查并更新 streak
                String today = LocalDate.now().toString();
                if (!today.equals(profile.lastActiveDate)) {
                    // 上次活跃日期不是今天，需要重新计算 streak

                    // calcStreak 逻辑：
                    // - 若 lastActiveDate 是昨天 → streak + 1（连续学习）
                    // - 若间隔超过 1 天 → streak 重置为 1（间断重新计数）
                    profile.streak = LearningEngine.calcStreak(
                        profile.lastActiveDate, profile.streak);
                    profile.lastActiveDate = today;

                    // 将更新后的 streak 持久化到数据库
                    db.poemDao().updateStreak(profile.streak, today);

                    // 里程碑判断：连续学习天数达到 7 的倍数时弹出庆祝提示
                    final int streak = profile.streak;
                    if (streak >= 7 && streak % 7 == 0) {
                        // 切回主线程显示 Toast
                        runOnUiThread(() -> Toast.makeText(this,
                            "🎉 你已经连续学习 " + streak + " 天啦！太棒了！",
                            Toast.LENGTH_LONG).show());
                    }
                }
            }
        }).start();
    }

    /**
     * 触发全屏撒花庆祝动画。
     * <p>
     * 当用户完成学习或达到里程碑时，由外部 Fragment 或 ViewModel 调用此方法，
     * 通过 {@link ConfettiView} 播放 confetti 粒子动画效果。
     */
    public void celebrate() {
        if (confettiView != null) {
            confettiView.celebrate();
        }
    }
}

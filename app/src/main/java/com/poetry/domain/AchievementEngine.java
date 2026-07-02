package com.poetry.domain;

import com.poetry.data.LearningDatabase;
import com.poetry.data.LearningRecord;
import com.poetry.data.UserProfile;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

/**
 * 成就引擎 —— 检测并解锁用户成就。
 *
 * 定义了 12 种成就，通过 {@link #checkAndUnlock(LearningDatabase, AchievementListener)}
 * 在积分/学习/游戏等关键节点被调用，检测条件满足后自动解锁。
 *
 * 用户档案中的成就以 JSON 数组（["first_poem","poem_10"]）存储。
 */
public class AchievementEngine {

    /** 成就定义（静态常量，全部 12 种） */
    public static class AchievementDef {
        /** 唯一标识 */
        public String id;
        /** 名称 */
        public String name;
        /** 描述 */
        public String desc;
        /** 图标 emoji */
        public String icon;
        public AchievementDef(String id, String name, String desc, String icon) {
            this.id = id; this.name = name; this.desc = desc; this.icon = icon;
        }
    }

    /** 全部 12 种成就定义 */
    public static final List<AchievementDef> ALL_ACHIEVEMENTS = new ArrayList<>();
    static {
        ALL_ACHIEVEMENTS.add(new AchievementDef("first_poem", "初出茅庐", "学习第1首诗", "🌱"));
        ALL_ACHIEVEMENTS.add(new AchievementDef("poem_10", "小有积累", "学习10首诗", "📚"));
        ALL_ACHIEVEMENTS.add(new AchievementDef("poem_50", "学富五车", "学习50首诗", "🧠"));
        ALL_ACHIEVEMENTS.add(new AchievementDef("poem_100", "诗词达人", "学习100首诗", "🏆"));
        ALL_ACHIEVEMENTS.add(new AchievementDef("streak_7", "坚持不懈", "连续学习7天", "🔥"));
        ALL_ACHIEVEMENTS.add(new AchievementDef("streak_30", "持之以恒", "连续学习30天", "💪"));
        ALL_ACHIEVEMENTS.add(new AchievementDef("favorite_10", "初代收藏家", "收藏10首诗", "⭐"));
        ALL_ACHIEVEMENTS.add(new AchievementDef("favorite_20", "收藏达人", "收藏20首诗", "💎"));
        ALL_ACHIEVEMENTS.add(new AchievementDef("quiz_perfect_5", "满分达人", "在填空游戏中获得5次满分", "🎯"));
        ALL_ACHIEVEMENTS.add(new AchievementDef("game_10", "游戏高手", "完成10次游戏", "🎮"));
        ALL_ACHIEVEMENTS.add(new AchievementDef("level_5", "小有名气", "达到等级5", "🌟"));
        ALL_ACHIEVEMENTS.add(new AchievementDef("level_9", "千古诗圣", "达到满级9", "👑"));
    }

    /** 成就解锁回调接口 */
    public interface AchievementListener {
        /** 当一项成就被解锁时回调 */
        void onAchievementUnlocked(AchievementDef def);
    }

    /**
     * 检测并解锁成就（核心入口）。
     * 在积分变更、答题完成、签到等关键节点调用。
     *
     * @param db       数据库实例
     * @param listener 解锁回调（用于 UI 通知）
     */
    public static void checkAndUnlock(LearningDatabase db, AchievementListener listener) {
        UserProfile profile = db.poemDao().getUserProfileSync();
        if (profile == null) return;

        List<String> unlocked = parseIds(profile.achievements);
        List<String> newlyUnlocked = new ArrayList<>();
        int learned = db.poemDao().getLearnedCountSync();
        int favCount = getFavCount(db);
        int perfectQuizCount = db.poemDao().getPerfectQuizCountSync(10);
        int gameCount = getGameCount(db);

        for (AchievementDef def : ALL_ACHIEVEMENTS) {
            if (unlocked.contains(def.id)) continue;
            if (matches(def.id, learned, favCount, perfectQuizCount, gameCount, profile)) {
                newlyUnlocked.add(def.id);
                if (listener != null) {
                    listener.onAchievementUnlocked(def);
                }
            }
        }

        if (!newlyUnlocked.isEmpty()) {
            unlocked.addAll(newlyUnlocked);
            db.poemDao().updateAchievements(toJson(unlocked));
        }
    }

    /** 根据成就 ID 判断条件是否满足 */
    private static boolean matches(String id, int learned, int favCount,
                                   int perfectQuizCount, int gameCount, UserProfile profile) {
        switch (id) {
            case "first_poem": return learned >= 1;
            case "poem_10": return learned >= 10;
            case "poem_50": return learned >= 50;
            case "poem_100": return learned >= 100;
            case "streak_7": return profile.streak >= 7;
            case "streak_30": return profile.streak >= 30;
            case "favorite_10": return favCount >= 10;
            case "favorite_20": return favCount >= 20;
            case "quiz_perfect_5": return perfectQuizCount >= 5;
            case "game_10": return gameCount >= 10;
            case "level_5": return profile.level >= 5;
            case "level_9": return profile.level >= 9;
            default: return false;
        }
    }

    /**
     * 获取用户已解锁的成就 ID 列表。
     * @param profile 用户档案
     * @return 成就 ID 列表
     */
    public static List<String> getUnlockedIds(UserProfile profile) {
        if (profile == null) return new ArrayList<>();
        return parseIds(profile.achievements);
    }

    /** 从 JSON 数组字符串解析 ID 列表 */
    private static List<String> parseIds(String json) {
        List<String> ids = new ArrayList<>();
        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) ids.add(arr.getString(i));
        } catch (JSONException e) {
            // JSON 数据损坏或版本不兼容，降级返回空列表
            android.util.Log.w("AchievementEngine", "Failed to parse achievement JSON", e);
        }
        return ids;
    }

    /** 将 ID 列表序列化为 JSON 数组字符串 */
    private static String toJson(List<String> ids) {
        JSONArray arr = new JSONArray(ids);
        return arr.toString();
    }

    /** 查询已收藏诗词数量 */
    private static int getFavCount(LearningDatabase db) {
        return db.poemDao().getFavCountSync();
    }

    /** 查询游戏总次数（所有诗词的 gamePlayed 累加） */
    private static int getGameCount(LearningDatabase db) {
        List<LearningRecord> records = db.poemDao().getGameRecords();
        int total = 0;
        for (LearningRecord r : records) total += r.gamePlayed;
        return total;
    }
}

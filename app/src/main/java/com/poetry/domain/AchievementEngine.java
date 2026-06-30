package com.poetry.domain;

import com.poetry.data.LearningDatabase;
import com.poetry.data.LearningRecord;
import com.poetry.data.UserProfile;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

public class AchievementEngine {

    public static class AchievementDef {
        public String id;
        public String name;
        public String desc;
        public String icon;
        public AchievementDef(String id, String name, String desc, String icon) {
            this.id = id; this.name = name; this.desc = desc; this.icon = icon;
        }
    }

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

    public interface AchievementListener {
        void onAchievementUnlocked(AchievementDef def);
    }

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

    public static List<String> getUnlockedIds(UserProfile profile) {
        if (profile == null) return new ArrayList<>();
        return parseIds(profile.achievements);
    }

    private static List<String> parseIds(String json) {
        List<String> ids = new ArrayList<>();
        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) ids.add(arr.getString(i));
        } catch (JSONException ignored) {}
        return ids;
    }

    private static String toJson(List<String> ids) {
        JSONArray arr = new JSONArray(ids);
        return arr.toString();
    }

    private static int getFavCount(LearningDatabase db) {
        return db.poemDao().getFavCountSync();
    }

    private static int getGameCount(LearningDatabase db) {
        List<LearningRecord> records = db.poemDao().getGameRecords();
        int total = 0;
        for (LearningRecord r : records) total += r.gamePlayed;
        return total;
    }
}

package com.poetry.domain;

import com.poetry.data.DailyStats;
import com.poetry.data.LearningDatabase;
import com.poetry.data.LearningRecord;
import com.poetry.data.UserProfile;

import org.json.JSONArray;
import org.json.JSONException;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 成就引擎 —— 检测并解锁用户成就。
 *
 * 定义了 19 种成就（12 项原有 + 7 项 M7 新增），通过 {@link #checkAndUnlock(LearningDatabase, AchievementListener)}
 * 在积分/学习/游戏等关键节点被调用，检测条件满足后自动解锁。
 *
 * <p>M7 新增成就分两类：
 * <ul>
 *   <li><b>累计型</b>（flyflower_20 / star_30 / play_3_days / play_7_days / month_poem_collect）：
 *       由 {@link #checkAndUnlock} 在结算时通过数据库标量判定；</li>
 *   <li><b>触发型</b>（star_first_3 / couplet_first_perfect）：仅由 {@link GameSettlement}
 *       在满足触发条件后经 {@link #unlockDirect} 解锁，标量判定恒为 false。</li>
 * </ul>
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
        // ===== M7 新增 7 项成就（仅追加，不改动上方 12 项）=====
        ALL_ACHIEVEMENTS.add(new AchievementDef("flyflower_20", "飞花令主", "飞花令累计命中20句", "🎤"));
        ALL_ACHIEVEMENTS.add(new AchievementDef("star_30", "三星集邮家", "累计获得30颗星(跨全部游戏)", "⭐"));
        ALL_ACHIEVEMENTS.add(new AchievementDef("couplet_first_perfect", "一气呵成", "对诗一局全对", "💯"));
        ALL_ACHIEVEMENTS.add(new AchievementDef("play_3_days", "天天报到", "连续3天各玩≥1局游戏", "📅"));
        ALL_ACHIEVEMENTS.add(new AchievementDef("play_7_days", "七日诗虫", "连续7天各玩≥1局", "🗓️"));
        ALL_ACHIEVEMENTS.add(new AchievementDef("star_first_3", "三连星", "任一游戏首次获得3星", "🌟"));
        ALL_ACHIEVEMENTS.add(new AchievementDef("month_poem_collect", "收藏达人·进阶", "累计从游戏结算收藏10句", "🏷️"));
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
        UserProfile profile = db.userProfileDao().getUserProfileSync();
        if (profile == null) return;

        List<String> unlocked = parseIds(profile.achievements);
        List<String> newlyUnlocked = new ArrayList<>();
        int learned = db.learningRecordDao().getLearnedCountSync();
        int favCount = getFavCount(db);
        int perfectQuizCount = db.learningRecordDao().getPerfectQuizCountSync(10);
        int gameCount = getGameCount(db);

        // M7：累计型新成就所需的额外标量（一次查询，循环内复用）
        long flyflowerHits = db.gameHistoryDao().getFlyflowerHitSum();
        long starSum = db.gameHistoryDao().getStarSum();
        int collectCount = db.gameHistoryDao().getCollectCount();
        int consecutivePlayDays = countConsecutivePlayDays(db);

        for (AchievementDef def : ALL_ACHIEVEMENTS) {
            if (unlocked.contains(def.id)) continue;
            if (matchesWithDb(def.id, db, learned, favCount, perfectQuizCount, gameCount,
                    profile, flyflowerHits, starSum, collectCount, consecutivePlayDays)) {
                newlyUnlocked.add(def.id);
                if (listener != null) {
                    listener.onAchievementUnlocked(def);
                }
            }
        }

        if (!newlyUnlocked.isEmpty()) {
            unlocked.addAll(newlyUnlocked);
            db.userProfileDao().updateAchievements(toJson(unlocked));
        }
    }

    /**
     * 直接解锁一项触发型成就（幂等：已解锁则无操作）。
     *
     * <p>用于 {@code star_first_3} / {@code couplet_first_perfect} 这类"本局触发"型成就，
     * 由 {@link GameSettlement} 在满足触发条件后调用。这些成就在标量 {@code matches} 中恒为
     * false（避免在任意结算时被 {@code checkAndUnlock} 误解锁），仅通过本方法按触发条件解锁。
     *
     * @param db       数据库实例
     * @param id       成就 ID
     * @param listener 解锁回调（新解锁时触发，可为 null）
     */
    public static void unlockDirect(LearningDatabase db, String id, AchievementListener listener) {
        if (db == null || id == null) return;
        UserProfile profile = db.userProfileDao().getUserProfileSync();
        if (profile == null) return;

        List<String> unlocked = parseIds(profile.achievements);
        if (unlocked.contains(id)) return; // 幂等：已解锁

        if (!matchesTrigger(id)) return; // 仅触发型成就可经此解锁

        unlocked.add(id);
        db.userProfileDao().updateAchievements(toJson(unlocked));
        if (listener != null) {
            for (AchievementDef def : ALL_ACHIEVEMENTS) {
                if (id.equals(def.id)) {
                    listener.onAchievementUnlocked(def);
                    break;
                }
            }
        }
    }

    /**
     * 触发型成就判定：仅 {@code star_first_3} / {@code couplet_first_perfect} 返回 true。
     *
     * <p>这两个成就不依赖任何数据库标量，只由 {@link GameSettlement} 在满足触发条件后
     * 通过 {@link #unlockDirect} 解锁，故在标量 {@code matches} 中恒为 false。
     */
    private static boolean matchesTrigger(String id) {
        return "star_first_3".equals(id) || "couplet_first_perfect".equals(id);
    }

    /**
     * 带数据库访问的成就判定（M7 扩展）。
     *
     * <p>原有 12 项沿用 {@link #matches} 的标量判定；新增累计型成就（flyflower_20 / star_30 /
     * play_3_days / play_7_days / month_poem_collect）需要数据库查询，在此处理。
     * 触发型成就（star_first_3 / couplet_first_perfect）在此恒为 false，仅经
     * {@link #unlockDirect} + {@link #matchesTrigger} 解锁。
     */
    private static boolean matchesWithDb(String id, LearningDatabase db,
                                         int learned, int favCount, int perfectQuizCount,
                                         int gameCount, UserProfile profile,
                                         long flyflowerHits, long starSum,
                                         int collectCount, int consecutivePlayDays) {
        switch (id) {
            case "flyflower_20": return flyflowerHits >= 20;
            case "star_30": return starSum >= 30;
            case "play_3_days": return consecutivePlayDays >= 3;
            case "play_7_days": return consecutivePlayDays >= 7;
            case "month_poem_collect": return collectCount >= 10;
            case "star_first_3":
            case "couplet_first_perfect":
                // 触发型成就：标量判定恒 false，仅经 unlockDirect 解锁
                return false;
            default:
                return matches(id, learned, favCount, perfectQuizCount, gameCount, profile);
        }
    }

    /**
     * 统计最近连续"玩过游戏"的天数（成就 play_3_days / play_7_days）。
     *
     * <p>从今天（若今天无游戏则从昨天）开始，向前逐日回溯，统计连续满足
     * {@code gamesPlayed > 0} 的天数。查询最近 7 天数据即可覆盖 7 天连续判定。
     */
    private static int countConsecutivePlayDays(LearningDatabase db) {
        LocalDate today = LocalDate.now();
        List<DailyStats> recent = db.dailyStatsDao()
                .getRecentDailyStats(today.minusDays(7).toString(), 7);
        return countConsecutivePlayDays(recent, today);
    }

    /** 从今天（或昨天）向前回溯统计连续游戏天数。 */
    private static int countConsecutivePlayDays(List<DailyStats> recent, LocalDate today) {
        if (recent == null || recent.isEmpty()) return 0;

        // 建立 date -> gamesPlayed 映射
        java.util.Map<String, Integer> byDate = new java.util.HashMap<>();
        for (DailyStats s : recent) {
            if (s != null && s.date != null) {
                byDate.put(s.date, s.gamesPlayed);
            }
        }

        // 起点：今天有游戏则从今天开始，否则从昨天开始
        LocalDate cursor = today;
        if (byDate.getOrDefault(today.toString(), 0) <= 0) {
            cursor = today.minusDays(1);
        }

        int count = 0;
        while (count < 7) {
            Integer played = byDate.get(cursor.toString());
            if (played == null || played <= 0) break;
            count++;
            cursor = cursor.minusDays(1);
        }
        return count;
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
            // domain 层不依赖 Android Log，静默降级
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
        return db.learningRecordDao().getFavCountSync();
    }

    /** 查询游戏总次数（所有诗词的 gamePlayed 累加） */
    private static int getGameCount(LearningDatabase db) {
        List<LearningRecord> records = db.learningRecordDao().getGameRecords();
        int total = 0;
        for (LearningRecord r : records) total += r.gamePlayed;
        return total;
    }
}

package com.poetry.domain;

import com.poetry.data.LearningDatabase;
import com.poetry.data.UserProfile;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

/**
 * 主题管理器 —— 管理 9 套诗词主题的解锁状态。
 *
 * 每套主题有解锁条件（等级/连续天数），默认解锁「墨韵」主题。
 * 等级提升或签到后调用 {@link #syncUnlockedThemes(LearningDatabase)} 更新解锁列表。
 *
 * 用户档案中的主题以 JSON 数组（["default","spring"]）存储。
 */
public class ThemeManager {

    /** 主题定义 */
    public static class ThemeDef {
        /** 主题 ID */
        public String id;
        /** 名称 */
        public String name;
        /** 图标 emoji */
        public String icon;
        /** 描述 */
        public String desc;
        /** 默认解锁（无需条件） */
        public boolean defaultUnlocked;
        /** 需要的最低等级 */
        public int requireLevel;
        /** 需要的最低连续天数 */
        public int requireStreak;
        /** 是否要求学完所有诗词 */
        public boolean requireAllPoems;

        public ThemeDef(String id, String name, String icon, String desc) {
            this.id = id; this.name = name; this.icon = icon; this.desc = desc;
            this.defaultUnlocked = false;
            this.requireLevel = 0;
            this.requireStreak = 0;
            this.requireAllPoems = false;
        }
        /** 标记为默认解锁主题 */
        public ThemeDef setDefault() { this.defaultUnlocked = true; return this; }
        /** 设置所需等级 */
        public ThemeDef level(int lv) { this.requireLevel = lv; return this; }
        /** 设置所需连续天数 */
        public ThemeDef streak(int s) { this.requireStreak = s; return this; }
    }

    /** 全部 9 套主题定义 */
    public static final List<ThemeDef> ALL_THEMES = new ArrayList<>();
    static {
        ALL_THEMES.add(new ThemeDef("default",  "墨韵", "🖋️", "古典墨色主题").setDefault());
        ALL_THEMES.add(new ThemeDef("spring",   "春意", "🌸", "春暖花开").level(2));
        ALL_THEMES.add(new ThemeDef("summer",   "夏荷", "🌻", "夏日清凉").level(3).streak(3));
        ALL_THEMES.add(new ThemeDef("autumn",   "秋月", "🌙", "秋风明月").level(4).streak(7));
        ALL_THEMES.add(new ThemeDef("winter",   "冬雪", "❄️", "雪落无声").level(5).streak(14));
        ALL_THEMES.add(new ThemeDef("bamboo",   "竹韵", "🎋", "君子如竹").level(6));
        ALL_THEMES.add(new ThemeDef("lotus",    "莲心", "🪷", "出淤泥而不染").level(7));
        ALL_THEMES.add(new ThemeDef("golden",   "金榜", "🏅", "金榜题名").level(8));
        ALL_THEMES.add(new ThemeDef("legend",   "传奇", "👑", "千古流传").level(9));
    }

    /**
     * 判断某个主题对当前用户是否已解锁。
     *
     * @param theme   主题定义
     * @param profile 用户档案
     * @return true 如果已解锁
     */
    public static boolean isUnlocked(ThemeDef theme, UserProfile profile) {
        if (theme.defaultUnlocked) return true;
        if (profile.level < theme.requireLevel) return false;
        if (profile.streak < theme.requireStreak) return false;
        return true;
    }

    /**
     * 获取当前用户已解锁的主题 ID 列表。
     * @param profile 用户档案
     * @return 已解锁主题 ID 列表
     */
    public static List<String> getCurrentUnlockedIds(UserProfile profile) {
        List<String> ids = new ArrayList<>();
        for (ThemeDef theme : ALL_THEMES) {
            if (isUnlocked(theme, profile)) {
                ids.add(theme.id);
            }
        }
        return ids;
    }

    /**
     * 同步主题解锁状态到数据库。
     * 在等级提升、连续天数更新后调用。
     *
     * @param db 数据库实例
     */
    public static void syncUnlockedThemes(LearningDatabase db) {
        UserProfile profile = db.poemDao().getUserProfileSync();
        if (profile == null) return;
        List<String> available = getCurrentUnlockedIds(profile);
        profile.unlockedThemes = new JSONArray(available).toString();
        db.poemDao().insertUserProfile(profile);
    }

    /**
     * 根据 ID 查找主题定义。
     * @param id 主题 ID
     * @return 主题定义，找不到时返回第一个（默认主题）
     */
    public static ThemeDef getThemeById(String id) {
        for (ThemeDef t : ALL_THEMES) if (t.id.equals(id)) return t;
        return ALL_THEMES.get(0);
    }
}

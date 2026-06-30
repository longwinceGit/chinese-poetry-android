package com.poetry.domain;

import com.poetry.data.LearningDatabase;
import com.poetry.data.UserProfile;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

public class ThemeManager {

    public static class ThemeDef {
        public String id;
        public String name;
        public String icon;
        public String desc;
        public boolean defaultUnlocked;
        public int requireLevel;
        public int requireStreak;
        public boolean requireAllPoems;
        public ThemeDef(String id, String name, String icon, String desc) {
            this.id = id; this.name = name; this.icon = icon; this.desc = desc;
            this.defaultUnlocked = false;
            this.requireLevel = 0;
            this.requireStreak = 0;
            this.requireAllPoems = false;
        }
        public ThemeDef setDefault() { this.defaultUnlocked = true; return this; }
        public ThemeDef level(int lv) { this.requireLevel = lv; return this; }
        public ThemeDef streak(int s) { this.requireStreak = s; return this; }
    }

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

    public static boolean isUnlocked(ThemeDef theme, UserProfile profile) {
        if (theme.defaultUnlocked) return true;
        if (profile.level < theme.requireLevel) return false;
        if (profile.streak < theme.requireStreak) return false;
        return true;
    }

    public static List<String> getCurrentUnlockedIds(UserProfile profile) {
        List<String> ids = new ArrayList<>();
        for (ThemeDef theme : ALL_THEMES) {
            if (isUnlocked(theme, profile)) {
                ids.add(theme.id);
            }
        }
        return ids;
    }

    public static void syncUnlockedThemes(LearningDatabase db) {
        UserProfile profile = db.poemDao().getUserProfileSync();
        if (profile == null) return;
        List<String> available = getCurrentUnlockedIds(profile);
        profile.unlockedThemes = new JSONArray(available).toString();
        db.poemDao().insertUserProfile(profile);
    }

    public static ThemeDef getThemeById(String id) {
        for (ThemeDef t : ALL_THEMES) if (t.id.equals(id)) return t;
        return ALL_THEMES.get(0);
    }
}

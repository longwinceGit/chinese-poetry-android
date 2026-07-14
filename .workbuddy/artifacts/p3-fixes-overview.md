# P3 遗留债务清理 — 完成报告

> 2026-07-01 15:30 | BUILD SUCCESSFUL (clean rebuild)

---

## 修复清单

| # | 问题 | 修复方式 | 文件 |
|---|------|---------|------|
| P3-1 | MatchCardAdapter 5处硬编码颜色 | `Color.parseColor()` → `ContextCompat.getColor(R.color.xxx)` | `MatchCardAdapter.java`, `colors.xml` |
| P3-2 | 数据库升级丢数据 | `fallbackToDestructiveMigration()` → `addMigrations(MIGRATION_1_2)` | `LearningDatabase.java` |
| P3-3 | PoemAdapter 颜色复查 | 无硬编码，仅布局引用 ✅ | — |

---

## 详细变更

### P3-1: MatchCardAdapter 颜色映射

| 硬编码值 | 语义 | 替换为 |
|----------|------|--------|
| `#E0E0E0` | 卡片边框 | `R.color.divider` |
| `#C8E6C9` | 配对成功背景 | `R.color.match_card_success_bg` *(新增)* |
| `#FFFFFF` | 默认卡片背景 | `R.color.surface` |
| `#2E7D32` | 配对成功文字 | `R.color.answer_correct` |
| `#333333` | 默认文字 | `R.color.on_surface` |

### P3-2: 数据库迁移

```java
static final Migration MIGRATION_1_2 = new Migration(1, 2) {
    public void migrate(SupportSQLiteDatabase database) {
        database.execSQL("CREATE TABLE IF NOT EXISTS daily_stats (...)");
    }
};
// 替换 fallbackToDestructiveMigration() → addMigrations(MIGRATION_1_2)
```

**影响**：v1 用户升级到 v2 时不再丢失学习记录和积分数据。

---

## 编译结果

```
BUILD SUCCESSFUL in 38s
36 actionable tasks: 35 executed, 1 up-to-date
```

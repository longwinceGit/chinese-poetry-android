# 设计令牌系统 v1.0 — 代码落地总结

## 完成内容

### 1. 新建 `values/dimens.xml`（令牌系统核心）
- **间距**: 9级原始(2/4/8/12/16/20/24/32/48dp) + 9级语义(spacing_page_horizontal 等)
- **字号**: 14级原始(10~48sp) + 3级行高(compact/body/relaxed)
- **圆角**: 5级(0/8/12/16/24dp)
- **阴影**: 4级(none/sm/md/lg)
- **描边**: 3级(none/thin/medium)
- **组件尺寸**: 13项(avatar/icon/button/divider 等)
- **触摸目标**: 2级(48dp WCAG 标准 / 36dp 紧凑)
- **动效时长**: 3级(fast/normal/slow)

### 2. `values/colors.xml` 追加 17 个语义色
- 文字: text_title / text_body / text_body_secondary / text_caption / text_accent / text_on_primary / text_gold
- 背景: bg_page / bg_card / bg_card_elevated
- 分隔线: divider_default / divider_decorative
- 状态: state_success / state_error / state_warning / state_disabled

### 3. `values/styles.xml` 全量重写
- 4 个 ShapeAppearance（Small/Medium/Large/Pill）
- 10 个 TextAppearance（Display/PageTitle/SectionTitle/CardTitle/Headline/Title/Body/BodySecondary/Label/Explanation/StatNumber/DailyLabel）
- 3 个 Card 样式（标准/Raised/Search）
- 3 个 Button 样式（标准/Outlined/Small）
- 2 个 Chip 样式（筛选 Chip/朝代标签 Tag）
- 所有硬编码值→@dimen/@color 引用

### 4. 13 个布局文件全量迁移
| 文件 | 关键变更 |
|------|----------|
| fragment_home | 搜索栏→Card.Search, 推荐卡→Card.Raised |
| fragment_detail | 装饰条/按钮高度→令牌, TextAppearance 引用 |
| fragment_learning | 统计数字→text_size_stat, 分隔线→令牌 |
| fragment_profile | 头像→size_avatar, 经验条→bg_card_elevated |
| fragment_favorites | 空状态→text_size_emoji |
| fragment_game_hub | 游戏卡→Card.Raised, 图标→size_icon_lg |
| fragment_game_match | padding/颜色→令牌 |
| fragment_quiz | 标题/得分→text_size_headline |
| fragment_game_couplet | 问题卡→Card.Raised, padding→spacing_xxl |
| item_poem_card | 修复 MissingConstraints, Chip→Chip.Tag |
| item_achievement | padding→spacing_lg, 标题→CardTitle |
| item_favorite | 圆角→radius_medium, 取消按钮→touch_target_small |
| activity_main | 背景→bg_page |

### 5. 预存 Lint 修复
- `windowLightStatusBar` 需 API 23 → 加 `tools:targetApi="23"`（values + values-night 两处）

## 验证结果
- `assembleDebug`: BUILD SUCCESSFUL in 17s
- `lintDebug`: BUILD SUCCESSFUL (0 errors)

## 设计约定
1. 所有布局禁止硬编码 textSize/padding/margin/color
2. 新增组件必须引用 @dimen/@color 令牌
3. values-night 无需额外定义语义色（原始色已有 night 覆盖）
4. 新增 TextAppearance 优先继承 Material3 对应级别

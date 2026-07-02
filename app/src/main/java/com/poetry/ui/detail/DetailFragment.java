package com.poetry.ui.detail;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.poetry.R;
import com.poetry.util.PinyinLineView;
import com.poetry.util.TtsManager;

import java.io.File;
import java.io.FileOutputStream;

/**
 * 诗词详情 Fragment。
 * <p>
 * 负责展示单首诗词的完整信息，包括标题、作者、朝代、标签、诗句（支持拼音切换）、
 * 释义等。同时提供 TTS 语音朗读、收藏/取消收藏、标记已学、生成并分享古风诗词卡片等交互功能。
 * </p>
 */
public class DetailFragment extends Fragment {

    public static final String ARG_ID = "poem_id";
    public static final String ARG_TITLE = "poem_title";
    public static final String ARG_AUTHOR = "poem_author";
    public static final String ARG_DYNASTY = "poem_dynasty";
    public static final String ARG_CATEGORY = "poem_category";
    public static final String ARG_TAG = "poem_tag";
    public static final String ARG_EMOJI = "poem_emoji";
    public static final String ARG_LINES = "poem_lines";
    public static final String ARG_EXPLANATION = "poem_explanation";

    private TextView tvTitle, tvAuthor, tvDynasty, tvEmoji;
    private TextView tvExplanationTitle, tvExplanation;
    private Chip chipTag;
    private LinearLayout llPoemLines;
    private MaterialButton btnFavorite, btnRead, btnShare, btnLearn, btnPinyin;
    private DetailViewModel viewModel;
    private TtsManager ttsManager;
    private boolean pinyinVisible = false;
    private String ttsErrorMsg = null;

    private String poemId, poemTitle, poemAuthor, poemDynasty, poemCategory, poemTag, poemEmoji;
    private String[] poemLines;
    private String poemExplanation;

    /**
     * 创建 Fragment 视图，inflate 详情页布局。
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_detail, container, false);
    }

    /**
     * 视图创建完成后，依次执行：读取 Bundle 参数、绑定控件、初始化 ViewModel 与 TTS 引擎、
     * 填充数据、注册监听器、查询收藏/已学状态。
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        readArgs();
        initViews(view);
        viewModel = new ViewModelProvider(this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(
                requireActivity().getApplication())).get(DetailViewModel.class);
        ttsManager = new TtsManager(requireContext(), new TtsManager.OnInitListener() {
            @Override
            public void onReady() {
                ttsErrorMsg = null;
            }

            @Override
            public void onError(String reason) {
                ttsErrorMsg = reason;
            }
        });
        setupData();
        setupListeners();
        observeViewModel();
        viewModel.checkStatus(poemId);
    }

    /**
     * 销毁视图时关闭 TTS 引擎，释放语音资源。
     */
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (ttsManager != null) {
            ttsManager.shutdown();
        }
    }

    /**
     * 从 Fragment arguments 中解析诗词基本数据（ID、标题、作者、朝代、分类、标签、
     * Emoji、诗句数组、释义）。
     */
    private void readArgs() {
        Bundle args = getArguments();
        if (args != null) {
            poemId = args.getString(ARG_ID, "");
            poemTitle = args.getString(ARG_TITLE, "");
            poemAuthor = args.getString(ARG_AUTHOR, "");
            poemDynasty = args.getString(ARG_DYNASTY, "");
            poemCategory = args.getString(ARG_CATEGORY, "");
            poemTag = args.getString(ARG_TAG, "");
            poemEmoji = args.getString(ARG_EMOJI, "📖");
            poemLines = args.getStringArray(ARG_LINES);
            poemExplanation = args.getString(ARG_EXPLANATION, "");
        }
    }

    /**
     * 绑定布局中的各控件引用（标题、作者、朝代、Emoji、标签 Chip、诗句容器、
     * 释义区域、操作按钮）。
     *
     * @param v 根视图
     */
    private void initViews(View v) {
        tvTitle = v.findViewById(R.id.tv_title);
        tvAuthor = v.findViewById(R.id.tv_author);
        tvDynasty = v.findViewById(R.id.tv_dynasty);
        tvEmoji = v.findViewById(R.id.tv_emoji);
        chipTag = v.findViewById(R.id.chip_tag);
        llPoemLines = v.findViewById(R.id.ll_poem_lines);
        tvExplanationTitle = v.findViewById(R.id.tv_explanation_title);
        tvExplanation = v.findViewById(R.id.tv_explanation);
        btnFavorite = v.findViewById(R.id.btn_favorite);
        btnRead = v.findViewById(R.id.btn_read);
        btnShare = v.findViewById(R.id.btn_share);
        btnLearn = v.findViewById(R.id.btn_learn);
        btnPinyin = v.findViewById(R.id.btn_pinyin);
    }

    /**
     * 向界面控件填充诗词数据：标题、Emoji、作者、朝代、标签 Chip、
     * 诗句（默认无拼音模式）及释义。
     */
    private void setupData() {
        tvTitle.setText(poemTitle);
        tvEmoji.setText(poemEmoji);
        tvAuthor.setText(poemAuthor);
        tvDynasty.setText(poemDynasty);

        // 设置标签
        String tagText = !poemCategory.isEmpty() ? poemCategory : poemDynasty;
        chipTag.setText(tagText);
        chipTag.setChipBackgroundColorResource(getTagColorRes(poemTag));

        // 渲染诗句
        renderPoemLines(false);

        // 渲染释义
        renderExplanation();
    }

    /**
     * 根据朝代标签返回对应的颜色资源 ID。
     *
     * @param tag 朝代标签（如 "tang"、"song"、"yuan"、"ming"、"modern" 等）
     * @return 对应的颜色资源 ID，未知标签默认返回中性灰色
     */
    private int getTagColorRes(String tag) {
        if (tag == null) return R.color.tag_other;
        switch (tag) {
            case "tang": return R.color.tag_tang;
            case "song": return R.color.tag_song;
            case "qin": return R.color.tag_qin;
            case "wei": return R.color.tag_wei;
            case "yuan": return R.color.tag_yuan;
            case "qing": return R.color.tag_qing;
            case "wu": return R.color.tag_wu;
            case "ming": return R.color.tag_ming;
            case "modern": return R.color.tag_modern;
            default: return R.color.tag_other;
        }
    }

    /** 拼音逐字模式下每列最小宽度（dp），用于计算每行最多可容纳的字符数 */
    private static final int MIN_CELL_DP = 22;

    /**
     * 渲染诗句行。
     * <p>
     * 根据 {@code showPinyin} 决定使用普通 {@link TextView} 还是带逐字拼音的
     * {@link PinyinLineView}。同时依据屏幕宽度自动计算每行最大字符数，
     * 以保证列宽不低于 {@link #MIN_CELL_DP}。
     * </p>
     *
     * @param showPinyin 是否显示拼音逐字模式
     */
    private void renderPoemLines(boolean showPinyin) {
        llPoemLines.removeAllViews();
        if (poemLines == null) return;

        int lineSpacing = dp2px(showPinyin ? 12 : 6);

        // 计算可用宽度 → 每行最多几个字
        float density = getResources().getDisplayMetrics().density;
        int screenWidthDp = (int) (getResources().getDisplayMetrics().widthPixels / density);
        int availableDp = screenWidthDp - 48; // 左右 margin 各 24dp
        int maxCharsPerRow = Math.max(5, availableDp / MIN_CELL_DP);

        for (String line : poemLines) {
            if (showPinyin) {
                PinyinLineView pinyinLine = new PinyinLineView(requireContext(), line, maxCharsPerRow);
                pinyinLine.setPadding(0, lineSpacing / 2, 0, lineSpacing / 2);
                llPoemLines.addView(pinyinLine);
            } else {
                llPoemLines.addView(createLineTextView(line));
            }
        }
    }

    /**
     * dp 转 px 工具方法。
     *
     * @param dp 设计尺寸（dp）
     * @return 对应的像素值
     */
    private int dp2px(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }

    /**
     * 创建普通诗句 {@link TextView}（无拼音模式），设置居中、字号、颜色等样式。
     *
     * @param text 诗句文本
     * @return 已配置样式的 TextView
     */
    private TextView createLineTextView(String text) {
        TextView tv = new TextView(requireContext());
        tv.setText(text);
        tv.setTextSize(18);
        tv.setTextColor(ContextCompat.getColor(requireContext(), R.color.on_surface));
        tv.setLineSpacing(8, 1);
        tv.setGravity(android.view.Gravity.CENTER);
        tv.setPadding(0, 4, 0, 4);
        return tv;
    }

    /**
     * 为各操作按钮注册点击监听（业务逻辑委托给 {@link DetailViewModel}）：
     * <ul>
     *   <li>朗读按钮 → TTS 朗读/暂停</li>
     *   <li>收藏按钮 → viewModel.toggleFavorite()</li>
     *   <li>分享按钮 → {@link #sharePoem()}</li>
     *   <li>已学按钮 → viewModel.markAsLearned()</li>
     *   <li>拼音按钮 → 切换拼音显示模式</li>
     * </ul>
     */
    private void setupListeners() {
        btnRead.setOnClickListener(v -> {
            if (!ttsManager.isReady()) {
                Toast.makeText(requireContext(),
                    ttsErrorMsg != null ? ttsErrorMsg : "语音引擎尚未就绪，请稍后再试",
                    Toast.LENGTH_SHORT).show();
                return;
            }
            if (ttsManager.isSpeaking()) {
                ttsManager.stop();
                btnRead.setText(R.string.detail_read);
            } else {
                String fullText = poemTitle + "。" + String.join("，", poemLines != null ? poemLines : new String[0]);
                ttsManager.speak(fullText, "detail_" + System.currentTimeMillis());
                btnRead.setText(R.string.detail_pause);
            }
        });

        btnFavorite.setOnClickListener(v -> viewModel.toggleFavorite());
        btnShare.setOnClickListener(v -> sharePoem());
        btnLearn.setOnClickListener(v -> {
            Boolean learned = viewModel.getIsLearned().getValue();
            if (learned == null || !learned) {
                viewModel.markAsLearned();
                Toast.makeText(requireContext(), "已标记为已学 ✓", Toast.LENGTH_SHORT).show();
            }
        });

        btnPinyin.setOnClickListener(v -> {
            pinyinVisible = !pinyinVisible;
            btnPinyin.setText(pinyinVisible ? "隐藏拼音" : "拼音");
            renderPoemLines(pinyinVisible);
        });
    }

    /**
     * 观察 ViewModel 中的收藏和已学状态，自动更新按钮 UI。
     */
    private void observeViewModel() {
        viewModel.getIsFavorite().observe(getViewLifecycleOwner(), fav -> {
            btnFavorite.setText(fav != null && fav ? R.string.detail_unfavorite : R.string.detail_favorite);
            btnFavorite.setIconResource(fav != null && fav ? R.drawable.ic_favorite_filled : R.drawable.ic_favorite);
            if (fav != null && fav) {
                btnFavorite.setIconTintResource(R.color.favorite_active);
            }
        });

        viewModel.getIsLearned().observe(getViewLifecycleOwner(), learned -> {
            boolean isLearned = learned != null && learned;
            btnLearn.setText(isLearned ? R.string.detail_learned : R.string.detail_learn);
            btnLearn.setEnabled(!isLearned);
            btnLearn.setAlpha(isLearned ? 0.6f : 1.0f);
        });
    }

    /**
     * 生成古风分享卡片，保存为 PNG 文件到缓存目录，然后通过系统分享 Intent 发送。
     * <p>
     * 整个过程在子线程中执行，分享前将图片通过 FileProvider 转为 URI。
     * 失败时在主线程弹出 Toast 提示。
     * </p>
     */
    private void sharePoem() {
        new Thread(() -> {
            try {
                Bitmap card = generateShareCard();
                if (card == null) {
                    requireActivity().runOnUiThread(() ->
                        Toast.makeText(requireContext(), "生成分享卡片失败", Toast.LENGTH_SHORT).show());
                    return;
                }

                // 保存到缓存目录
                File cacheDir = new File(requireContext().getCacheDir(), "images");
                if (!cacheDir.exists()) cacheDir.mkdirs();
                File file = new File(cacheDir, "poem_share_" + poemId + ".png");
                try (FileOutputStream fos = new FileOutputStream(file)) {
                    card.compress(Bitmap.CompressFormat.PNG, 95, fos);
                }

                Uri uri = FileProvider.getUriForFile(requireContext(),
                    requireContext().getPackageName() + ".fileprovider", file);

                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("image/png");
                shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
                shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                requireActivity().runOnUiThread(() -> {
                    String text = "《" + poemTitle + "》—— " + poemAuthor;
                    Intent chooser = Intent.createChooser(shareIntent, text);
                    startActivity(chooser);
                });
            } catch (Exception e) {
                requireActivity().runOnUiThread(() ->
                    Toast.makeText(requireContext(), "分享失败：" + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    /**
     * 使用 Canvas 绘制古风诗词分享卡片 Bitmap。
     * <p>
     * 卡片宽 750px、高度自适应内容，使用宣纸色背景、墨色文字、朱红装饰线的
     * 传统风格配色。包含顶部装饰线、Emoji、标题、作者･朝代、分割线、诗句正文、
     * 底部水印及装饰线等元素。
     * </p>
     *
     * @return 生成好的分享卡片 Bitmap，失败返回 null
     */
    private Bitmap generateShareCard() {
        int width = 750;
        int padding = 48;
        int paddingSmall = 32;
        int contentWidth = width - padding * 2;

        // 颜色
        int bgColor = 0xFFFAF7F0;       // 宣纸色
        int inkColor = 0xFF5D4037;      // 墨色
        int subColor = 0xFF795548;      // 赭石
        int accentColor = 0xFFC62828;   // 朱红
        int waterColor = 0xFF7A7570;    // 水印灰
        int dividerColor = 0xFFD7CCC8;  // 分割线

        // 文本画笔
        Paint titlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        titlePaint.setColor(inkColor);
        titlePaint.setTextSize(40f);
        titlePaint.setFakeBoldText(true);
        titlePaint.setTextAlign(Paint.Align.CENTER);

        Paint authorPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        authorPaint.setColor(subColor);
        authorPaint.setTextSize(26f);
        authorPaint.setTextAlign(Paint.Align.CENTER);

        Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        linePaint.setColor(inkColor);
        linePaint.setTextSize(32f);
        linePaint.setTextAlign(Paint.Align.CENTER);

        Paint footerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        footerPaint.setColor(waterColor);
        footerPaint.setTextSize(22f);
        footerPaint.setTextAlign(Paint.Align.CENTER);

        Paint accentPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        accentPaint.setColor(accentColor);
        accentPaint.setStrokeWidth(3f);
        accentPaint.setStyle(Paint.Style.STROKE);

        Paint dividerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        dividerPaint.setColor(dividerColor);
        dividerPaint.setStrokeWidth(1.5f);
        dividerPaint.setStyle(Paint.Style.STROKE);

        // 计算尺子
        Rect titleBounds = new Rect();
        float titleY = 0;
        if (poemTitle != null && !poemTitle.isEmpty()) {
            titlePaint.getTextBounds(poemTitle, 0, poemTitle.length(), titleBounds);
            titleY = 80 + titleBounds.height();
        }

        Rect authorBounds = new Rect();
        String authorText = poemAuthor + " · " + poemDynasty;
        authorPaint.getTextBounds(authorText, 0, authorText.length(), authorBounds);

        // 计算诗句总高度
        float linesHeight = 0;
        float[] lineWidths = null;
        if (poemLines != null && poemLines.length > 0) {
            lineWidths = new float[poemLines.length];
            for (int i = 0; i < poemLines.length; i++) {
                lineWidths[i] = linePaint.measureText(poemLines[i]);
                linesHeight += 46; // 行间距
            }
        }

        float footerY = titleY + 40 + authorBounds.height() + 48 + linesHeight + 60;
        float totalHeight = footerY + 60;

        // 创建 Bitmap
        Bitmap bitmap = Bitmap.createBitmap(width, (int) totalHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(bgColor);

        // 顶部装饰线
        float lineY = padding;
        canvas.drawLine(padding, lineY, width - padding, lineY, accentPaint);

        // Emoji
        if (poemEmoji != null && !poemEmoji.isEmpty() && !poemEmoji.equals("📖")) {
            Paint emojiPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            emojiPaint.setTextSize(36f);
            emojiPaint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText(poemEmoji, width / 2f, lineY + 48, emojiPaint);
            lineY += 56;
        }

        // 标题
        float curY = lineY + paddingSmall + titleBounds.height();
        canvas.drawText(poemTitle, width / 2f, curY, titlePaint);

        // 作者朝代
        curY += paddingSmall + authorBounds.height();
        canvas.drawText(authorText, width / 2f, curY, authorPaint);

        // 分割线
        curY += paddingSmall;
        canvas.drawLine(padding, curY, width - padding, curY, dividerPaint);

        // 诗句
        curY += 40;
        if (poemLines != null) {
            // 计算合适的内容宽度（取最长行）
            float maxLineWidth = contentWidth * 0.8f;
            for (int i = 0; i < poemLines.length; i++) {
                String line = poemLines[i];
                // 如果一行太长，适当缩小字号绘制
                Paint drawPaint = linePaint;
                if (lineWidths != null && lineWidths[i] > contentWidth) {
                    drawPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
                    drawPaint.setColor(inkColor);
                    drawPaint.setTextSize(28f);
                    drawPaint.setTextAlign(Paint.Align.CENTER);
                }
                canvas.drawText(line, width / 2f, curY, drawPaint);
                curY += 46;
            }
        }

        // 底部水印
        curY += 12;
        canvas.drawText("—— 来自「诗词乐园」", width / 2f, curY, footerPaint);

        // 底部装饰线
        canvas.drawLine(padding, totalHeight - padding, width - padding, totalHeight - padding, accentPaint);

        return bitmap;
    }

    /**
     * 渲染诗词释义区域：有释义时显示内容并同步显示分隔线，无释义时整个区域隐藏。
     */
    private void renderExplanation() {
        boolean hasExplanation = poemExplanation != null && !poemExplanation.isEmpty();
        tvExplanationTitle.setVisibility(hasExplanation ? View.VISIBLE : View.GONE);
        tvExplanation.setVisibility(hasExplanation ? View.VISIBLE : View.GONE);
        // divider 也跟着隐藏
        View divider = getView() != null ? getView().findViewById(R.id.divider_explanation) : null;
        if (divider != null) {
            divider.setVisibility(hasExplanation ? View.VISIBLE : View.GONE);
        }
        if (hasExplanation) {
            tvExplanation.setText(poemExplanation);
        }
    }

}

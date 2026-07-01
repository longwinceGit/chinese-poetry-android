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
import com.poetry.data.LearningDatabase;
import com.poetry.data.model.Poem;
import com.poetry.util.PinyinLineView;
import com.poetry.util.TtsManager;

import java.io.File;
import java.io.FileOutputStream;

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
    private boolean isFavorite = false;
    private boolean isLearned = false;
    private boolean pinyinVisible = false;
    private String ttsErrorMsg = null;

    private String poemId, poemTitle, poemAuthor, poemDynasty, poemCategory, poemTag, poemEmoji;
    private String[] poemLines;
    private String poemExplanation;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        readArgs();
        initViews(view);
        viewModel = new ViewModelProvider(this).get(DetailViewModel.class);
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
        checkStatus();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (ttsManager != null) {
            ttsManager.shutdown();
        }
    }

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

    private int getTagColorRes(String tag) {
        if (tag == null) return R.color.tag_tang;
        switch (tag) {
            case "song": return R.color.tag_song;
            case "qin": return R.color.tag_qin;
            case "wei": return R.color.tag_wei;
            case "yuan": return R.color.tag_yuan;
            case "qing": return R.color.tag_qing;
            case "wu": return R.color.tag_wu;
            default: return R.color.tag_tang;
        }
    }

    /** 拼音逐字模式下每列最小宽度（dp），用于计算每行最大字符数 */
    private static final int MIN_CELL_DP = 22;

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

    private int dp2px(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }

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

        btnFavorite.setOnClickListener(v -> toggleFavorite());
        btnShare.setOnClickListener(v -> sharePoem());
        btnLearn.setOnClickListener(v -> markAsLearned());

        btnPinyin.setOnClickListener(v -> {
            pinyinVisible = !pinyinVisible;
            btnPinyin.setText(pinyinVisible ? "隐藏拼音" : "拼音");
            renderPoemLines(pinyinVisible);
        });
    }

    private void checkStatus() {
        LearningDatabase db = LearningDatabase.getInstance(requireContext());
        new Thread(() -> {
            boolean fav = db.poemDao().isFavorite(poemId);
            boolean learned = db.poemDao().isLearned(poemId);
            requireActivity().runOnUiThread(() -> {
                isFavorite = fav;
                isLearned = learned;
                updateButtons();
            });
        }).start();
    }

    private void toggleFavorite() {
        isFavorite = !isFavorite;
        LearningDatabase db = LearningDatabase.getInstance(requireContext());
        new Thread(() -> {
            db.poemDao().ensureRecordExists(poemId);
            if (isFavorite) {
                db.poemDao().addFavorite(poemId);
            } else {
                db.poemDao().removeFavorite(poemId);
            }
            requireActivity().runOnUiThread(this::updateButtons);
        }).start();
    }

    private void markAsLearned() {
        if (isLearned) return;
        isLearned = true;
        LearningDatabase db = LearningDatabase.getInstance(requireContext());
        new Thread(() -> {
            db.poemDao().ensureRecordExists(poemId);
            db.poemDao().markLearned(poemId, System.currentTimeMillis());
            requireActivity().runOnUiThread(() -> {
                updateButtons();
                Toast.makeText(requireContext(), "已标记为已学 ✓", Toast.LENGTH_SHORT).show();
            });
        }).start();
    }

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
     * 生成古风诗词分享卡片
     * 尺寸: 750xN px, 适配主流社交平台分享预览
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

    private void updateButtons() {
        btnFavorite.setText(isFavorite ? R.string.detail_unfavorite : R.string.detail_favorite);
        btnFavorite.setIconResource(isFavorite ? R.drawable.ic_favorite_filled : R.drawable.ic_favorite);
        if (isFavorite) {
            btnFavorite.setIconTintResource(R.color.favorite_active);
        }

        btnLearn.setText(isLearned ? R.string.detail_learned : R.string.detail_learn);
        btnLearn.setEnabled(!isLearned);
        btnLearn.setAlpha(isLearned ? 0.6f : 1.0f);
    }
}

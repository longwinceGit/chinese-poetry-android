package com.poetry.ui.detail;

import android.content.Intent;
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
import com.poetry.R;
import com.poetry.ui.widget.ShareCardGenerator;
import com.poetry.util.AppExecutors;
import com.poetry.util.PinyinLineView;
import com.poetry.util.PoemArgs;
import com.poetry.util.TtsManager;

import java.io.File;
import java.io.FileOutputStream;

/**
 * 诗词详情 Fragment。
 * <p>
 * 负责展示单首诗词的完整信息，包括标题、作者、朝代、诗句（支持拼音切换）、
 * 释义等。同时提供 TTS 语音朗读、收藏/取消收藏、标记已学、生成并分享古风诗词卡片等交互功能。
 * </p>
 */
public class DetailFragment extends Fragment {

    private TextView tvTitle, tvAuthor, tvDynasty;
    private TextView tvExplanationTitle, tvExplanation;
    private LinearLayout llPoemLines;
    private MaterialButton btnFavorite, btnRead, btnShare, btnLearn, btnPinyin;
    private DetailViewModel viewModel;
    private TtsManager ttsManager;
    private boolean pinyinVisible = false;
    private String ttsErrorMsg = null;

    private PoemArgs poemArgs;

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
        poemArgs = PoemArgs.fromBundle(getArguments());
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
        viewModel.checkStatus(poemArgs.getId(), poemArgs.getTitle(), poemArgs.getAuthor(), poemArgs.getDynasty());
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
     * 绑定布局中的各控件引用（标题、作者、朝代、诗句容器、
     * 释义区域、操作按钮）。
     *
     * @param v 根视图
     */
    private void initViews(View v) {
        tvTitle = v.findViewById(R.id.tv_title);
        tvAuthor = v.findViewById(R.id.tv_author);
        tvDynasty = v.findViewById(R.id.tv_dynasty);
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
     * 向界面控件填充诗词数据：标题、作者、朝代、
     * 诗句（默认无拼音模式）及释义。
     */
    private void setupData() {
        tvTitle.setText(poemArgs.getTitle());
        tvAuthor.setText(poemArgs.getAuthor());
        tvDynasty.setText(poemArgs.getDynasty());

        // 渲染诗句
        renderPoemLines(false);

        // 渲染释义
        renderExplanation();
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
        String[] poemLines = poemArgs.getLines();
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
                String dynasty = poemArgs.getDynasty();
                String author = poemArgs.getAuthor();
                String title = poemArgs.getTitle();
                String[] lines = poemArgs.getLines();
                String authorLine = dynasty != null && !dynasty.isEmpty()
                        ? author + "（" + dynasty + "）" : author;
                ttsManager.speakPoemStructured(title, authorLine,
                        lines != null ? lines : new String[0],
                        new TtsManager.LineReadListener() {
                            @Override
                            public void onLineComplete(int index, int total) {}

                            @Override
                            public void onAllComplete() {
                                btnRead.post(() -> btnRead.setText(R.string.detail_read));
                            }
                        });
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
     *
     * <h3>P0-A A2 修复</h3>
     * <ul>
     *   <li>捕获 OutOfMemoryError（长文 Bitmap 渲染可能 OOM）</li>
     *   <li>用 ApplicationContext 而非 requireContext()，避免后台线程持有 Fragment 引用导致泄漏</li>
     * </ul>
     */
    private void sharePoem() {
        final android.content.Context appCtx = requireContext().getApplicationContext();
        final String packageName = appCtx.getPackageName();
        final String poemId = poemArgs.getId();
        final String title = poemArgs.getTitle();
        final String author = poemArgs.getAuthor();
        final String dynasty = poemArgs.getDynasty();
        final String[] lines = poemArgs.getLines();

        AppExecutors.io(() -> {
            android.graphics.Bitmap card = null;
            try {
                ShareCardGenerator generator = new ShareCardGenerator(appCtx, title, author, dynasty, lines);
                card = generator.generate();
                if (card == null) {
                    AppExecutors.main(() ->
                        Toast.makeText(appCtx, "生成分享卡片失败", Toast.LENGTH_SHORT).show());
                    return;
                }

                // 保存到缓存目录
                File cacheDir = new File(appCtx.getCacheDir(), "images");
                if (!cacheDir.exists()) cacheDir.mkdirs();
                File file = new File(cacheDir, "poem_share_" + poemId + ".png");
                try (FileOutputStream fos = new FileOutputStream(file)) {
                    card.compress(android.graphics.Bitmap.CompressFormat.PNG, 95, fos);
                }

                Uri uri = FileProvider.getUriForFile(appCtx,
                    packageName + ".fileprovider", file);

                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("image/png");
                shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
                shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                final String text = "《" + title + "》—— " + author;
                final Intent finalIntent = shareIntent;
                AppExecutors.main(() -> {
                    Intent chooser = Intent.createChooser(finalIntent, text);
                    startActivity(chooser);
                });
            } catch (OutOfMemoryError oom) {
                AppExecutors.main(() ->
                    Toast.makeText(appCtx, "墨未干，稍后再试", Toast.LENGTH_SHORT).show());
            } catch (Exception e) {
                AppExecutors.main(() ->
                    Toast.makeText(appCtx, "分享失败：" + e.getMessage(), Toast.LENGTH_SHORT).show());
            } finally {
                if (card != null && !card.isRecycled()) {
                    card.recycle();
                }
            }
        });
    }

    /**
     * 渲染诗词释义区域：有释义时显示内容并同步显示分隔线，无释义时整个区域隐藏。
     */
    private void renderExplanation() {
        String explanation = poemArgs.getExplanation();
        boolean hasExplanation = explanation != null && !explanation.isEmpty();
        tvExplanationTitle.setVisibility(hasExplanation ? View.VISIBLE : View.GONE);
        tvExplanation.setVisibility(hasExplanation ? View.VISIBLE : View.GONE);
        // divider 也跟着隐藏
        View divider = getView() != null ? getView().findViewById(R.id.divider_explanation) : null;
        if (divider != null) {
            divider.setVisibility(hasExplanation ? View.VISIBLE : View.GONE);
        }
        if (hasExplanation) {
            tvExplanation.setText(explanation);
        }
    }

}

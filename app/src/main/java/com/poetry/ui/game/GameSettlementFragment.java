package com.poetry.ui.game;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.google.android.material.button.MaterialButton;
import com.poetry.R;
import com.poetry.data.LearningDatabase;
import com.poetry.data.PoemRepository;
import com.poetry.data.model.Poem;
import com.poetry.ui.widget.SettlementView;
import com.poetry.ui.widget.ShareCardGenerator;
import com.poetry.util.AppExecutors;
import com.poetry.util.TtsManager;

import java.io.File;
import java.io.FileOutputStream;

/**
 * 游戏结算 Fragment。
 * <p>
 * 承载 {@link SettlementView}，从导航参数读取 {@code game_type}/{@code score}/{@code stars}，
 * 展示大星弹跳、滚动得分与本局最美一句卡片。
 * 提供「再来一局」「回大厅」导航，以及收藏/再听/分享的最小可工作实现。
 * </p>
 * <p>
 * 导航目标 {@code nav_game_settlement} 已在 nav_graph.xml 预置，
 * 参数：game_type(string)、score(integer, 默认 0)、stars(integer, 默认 1)。
 * </p>
 */
public class GameSettlementFragment extends Fragment {

    /** 对诗模式 */
    private static final String TYPE_COUPLET = "couplet";
    /** 消消乐模式 */
    private static final String TYPE_MATCH = "match";
    /** 填空模式 */
    private static final String TYPE_QUIZ = "quiz";
    /** 飞花令模式 */
    private static final String TYPE_FLYFLOWER = "flyflower";
    /** 作者连线模式 */
    private static final String TYPE_AUTHOR = "author";

    private SettlementView settlementView;
    private TtsManager ttsManager;

    private String gameType;
    private int score;
    private int stars;
    private String poemId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_game_settlement, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        readArguments();
        initViews(view);
        setupListeners(view);
        ttsManager = new TtsManager(requireContext(), new TtsManager.OnInitListener() {
            @Override
            public void onReady() {
            }

            @Override
            public void onError(String reason) {
            }
        });
    }

    /**
     * 从导航参数读取 game_type / score / stars（防御式读取，int 带默认值）。
     */
    private void readArguments() {
        Bundle args = getArguments();
        if (args == null) {
            gameType = "";
            score = 0;
            stars = 1;
            return;
        }
        gameType = args.getString("game_type", "");
        score = args.getInt("score", 0);
        stars = args.getInt("stars", 1);
        poemId = args.getString("poem_id", null);
    }

    /**
     * 绑定控件并填充结算数据。
     */
    private void initViews(View view) {
        settlementView = view.findViewById(R.id.settlement_view);
        // 骨架阶段：bestLine 暂用 poemId 关联的诗句，无 poemId 时传 null 显示通用文案
        String bestLine = resolveBestLine();
        settlementView.setResult(score, stars, bestLine);
        // M10 名句彩卡：本局诗词属 88 首释义名篇时额外弹卡（可收藏/TTS/分享）
        bindFamousCard(view);
    }

    /**
     * M10：绑定名句彩卡（§7.3）。
     * <p>当本局最美一句所属诗词是 88 首释义名篇（{@link Poem#hasExplanation()}）时，
     * 显示彩卡（默认 GONE）。卡片展示第一句 + 标题 + 作者；收藏/再听/分享复用下方按钮
     * （操作对象同为该 poemId 对应诗词）。
     */
    private void bindFamousCard(View view) {
        View cardFamous = view.findViewById(R.id.card_famous);
        if (poemId == null || poemId.isEmpty()) return;
        Poem poem = PoemRepository.getInstance().findPoemById(poemId);
        if (poem == null || !poem.hasExplanation()) return;
        if (poem.lines == null || poem.lines.length == 0) return;

        TextView tvFamousPoem = view.findViewById(R.id.tv_famous_poem);
        tvFamousPoem.setText(getString(R.string.settlement_famous_poem_fmt,
                poem.lines[0],
                poem.title != null ? poem.title : "",
                poem.author != null ? poem.author : ""));
        cardFamous.setVisibility(View.VISIBLE);
        cardFamous.setOnClickListener(v -> {
            // 点击彩卡 → 收藏本局名句（复用 onFavorite，操作对象即该 poemId）
            onFavorite();
        });
    }

    /**
     * 尝试通过 poemId 解析本局最美一句。
     * <p>
     * 结算参数暂不传 poemId 时返回 null，由 SettlementView 显示通用文案。
     * </p>
     *
     * @return 最美一句诗句，无法解析时返回 null
     */
    @Nullable
    private String resolveBestLine() {
        if (poemId == null || poemId.isEmpty()) return null;
        Poem poem = PoemRepository.getInstance().findPoemById(poemId);
        if (poem == null || poem.lines == null || poem.lines.length == 0) return null;
        return poem.lines[0];
    }

    /**
     * 注册结算视图交互与底部按钮导航。
     */
    private void setupListeners(View view) {
        settlementView.setListeners(
                this::onFavorite,
                this::onListen,
                this::onShare,
                this::onReplay,
                this::onBackHome);

        MaterialButton btnReplay = view.findViewById(R.id.btn_settlement_replay);
        btnReplay.setOnClickListener(v -> onReplay());

        MaterialButton btnBackHome = view.findViewById(R.id.btn_settlement_back_home);
        btnBackHome.setOnClickListener(v -> onBackHome());
    }

    /**
     * 收藏本句：poemId 可用时写库，否则仅 Toast 提示。
     * <p>
     * 收藏成功后额外写入一条 {@code gameType='settlement_collect'} 的合成 GameHistory 行，
     * 作为 {@code month_poem_collect} 成就的纯领域层累计计数器（不参与任何展示）。
     * </p>
     */
    private void onFavorite() {
        if (poemId == null || poemId.isEmpty()) {
            Toast.makeText(requireContext(), "收藏功能将在后续版本开放", Toast.LENGTH_SHORT).show();
            return;
        }
        final String id = poemId;
        AppExecutors.io(() -> {
            LearningDatabase db = LearningDatabase.getInstance(requireContext().getApplicationContext());
            db.learningRecordDao().ensureRecordExists(id);
            db.learningRecordDao().addFavorite(id);
            // 记录结算收藏计数（month_poem_collect 成就判定依据）
            db.gameHistoryDao().insert(new com.poetry.data.GameHistory(
                    "settlement_collect", 1, 0, 0, 0,
                    System.currentTimeMillis(), 0));
            AppExecutors.main(() -> {
                settlementView.markCollected();
                Toast.makeText(requireContext(), R.string.settlement_collected, Toast.LENGTH_SHORT).show();
            });
        });
    }

    /**
     * 再听一遍：用 TTS 朗读本局最美一句。
     */
    private void onListen() {
        String line = settlementView.getBestLine();
        if (line == null || line.isEmpty()) {
            Toast.makeText(requireContext(), "暂无诗句可朗读", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!ttsManager.isReady()) {
            Toast.makeText(requireContext(), "语音引擎尚未就绪，请稍后再试", Toast.LENGTH_SHORT).show();
            return;
        }
        ttsManager.speakPoem("", "", new String[]{line});
    }

    /**
     * 分享本局最美一句：生成古风分享卡片并走系统分享（镜像 DetailFragment.sharePoem）。
     *
     * <p>优先用 poemId 反查完整诗词（标题/作者/朝代/诗句）生成卡片；无 poemId 时退化为
     * 仅用本局最美一句文本生成卡片。整个过程在 IO 线程执行，FileProvider 转 URI 后
     * 通过 {@code Intent.ACTION_SEND} 分享。
     */
    private void onShare() {
        final android.content.Context appCtx = requireContext().getApplicationContext();
        final String packageName = appCtx.getPackageName();
        final String bestLine = settlementView.getBestLine();

        // 解析诗词元数据（标题/作者/朝代/诗句）
        String title = "";
        String author = "";
        String dynasty = "";
        String[] lines;
        if (poemId != null && !poemId.isEmpty()) {
            Poem poem = PoemRepository.getInstance().findPoemById(poemId);
            if (poem != null) {
                title = poem.title != null ? poem.title : "";
                author = poem.author != null ? poem.author : "";
                dynasty = poem.dynasty != null ? poem.dynasty : "";
                lines = poem.lines;
            } else {
                lines = bestLine != null ? new String[]{bestLine} : new String[]{};
            }
        } else {
            lines = bestLine != null ? new String[]{bestLine} : new String[]{};
        }
        if (lines == null || lines.length == 0) {
            Toast.makeText(requireContext(), R.string.settlement_share_failed, Toast.LENGTH_SHORT).show();
            return;
        }

        final String fTitle = title;
        final String fAuthor = author;
        final String fDynasty = dynasty;
        final String[] fLines = lines;

        AppExecutors.io(() -> {
            android.graphics.Bitmap card = null;
            try {
                ShareCardGenerator generator = new ShareCardGenerator(appCtx, fTitle, fAuthor, fDynasty, fLines);
                card = generator.generate();
                if (card == null) {
                    AppExecutors.main(() ->
                            Toast.makeText(appCtx, R.string.settlement_share_failed, Toast.LENGTH_SHORT).show());
                    return;
                }

                // 保存到缓存目录
                File cacheDir = new File(appCtx.getCacheDir(), "images");
                if (!cacheDir.exists()) cacheDir.mkdirs();
                String fileKey = (poemId != null && !poemId.isEmpty()) ? poemId : "settlement";
                File file = new File(cacheDir, "poem_share_" + fileKey + ".png");
                try (FileOutputStream fos = new FileOutputStream(file)) {
                    card.compress(android.graphics.Bitmap.CompressFormat.PNG, 95, fos);
                }

                Uri uri = FileProvider.getUriForFile(appCtx,
                        packageName + ".fileprovider", file);

                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("image/png");
                shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
                shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                final String text = fTitle.isEmpty() ? "本局最美一句" : "《" + fTitle + "》—— " + fAuthor;
                final Intent finalIntent = shareIntent;
                AppExecutors.main(() -> {
                    Intent chooser = Intent.createChooser(finalIntent, text);
                    startActivity(chooser);
                });
            } catch (OutOfMemoryError oom) {
                AppExecutors.main(() ->
                        Toast.makeText(appCtx, R.string.settlement_share_failed, Toast.LENGTH_SHORT).show());
            } catch (Exception e) {
                AppExecutors.main(() ->
                        Toast.makeText(appCtx, R.string.settlement_share_failed, Toast.LENGTH_SHORT).show());
            } finally {
                if (card != null && !card.isRecycled()) {
                    card.recycle();
                }
            }
        });
    }

    /**
     * 再来一局：按 game_type 导航回对应游戏 Fragment。
     */
    private void onReplay() {
        NavController nav = Navigation.findNavController(requireView());
        switch (gameType) {
            case TYPE_COUPLET:
                nav.popBackStack(R.id.nav_game_couplet, false);
                break;
            case TYPE_MATCH:
                nav.popBackStack(R.id.nav_game_match, false);
                break;
            case TYPE_QUIZ:
                nav.popBackStack(R.id.nav_quiz, false);
                break;
            case TYPE_FLYFLOWER:
                nav.popBackStack(R.id.nav_game_flyflower, false);
                break;
            case TYPE_AUTHOR:
                nav.popBackStack(R.id.nav_game_author, false);
                break;
            default:
                // 未知类型：直接返回上一页
                nav.popBackStack();
                break;
        }
    }

    /**
     * 回大厅：多次 pop 到游戏大厅 nav_game。
     */
    private void onBackHome() {
        NavController nav = Navigation.findNavController(requireView());
        nav.popBackStack(R.id.nav_game, false);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (ttsManager != null) {
            ttsManager.shutdown();
        }
    }
}

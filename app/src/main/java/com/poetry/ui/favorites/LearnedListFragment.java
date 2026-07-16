package com.poetry.ui.favorites;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.poetry.R;
import com.poetry.data.LearningDatabase;
import com.poetry.data.LearningRecord;
import com.poetry.data.PoemRepository;
import com.poetry.data.model.Poem;
import com.poetry.util.AppExecutors;
import com.poetry.util.PoemArgs;

import java.util.List;

/**
 * 已学诗词列表 Fragment。
 * <p>
 * 以线性列表形式展示用户已学习的诗词，按学习时间降序排列。
 * 点击卡片跳转详情页。无已学记录时显示空状态提示。
 * 利用 Room LiveData 自动刷新，标记新诗词为已学后自动更新列表。
 * </p>
 */
public class LearnedListFragment extends Fragment {

    private RecyclerView rvLearned;
    private LinearLayout layoutEmpty;
    private FavoriteAdapter adapter;
    private LearnedListViewModel viewModel;
    private NavController navController;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_learned, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        navController = Navigation.findNavController(view);
        rvLearned = view.findViewById(R.id.rv_learned);
        layoutEmpty = view.findViewById(R.id.layout_empty);

        viewModel = new ViewModelProvider(this,
                ViewModelProvider.AndroidViewModelFactory.getInstance(
                        requireActivity().getApplication())).get(LearnedListViewModel.class);

        setupRecyclerView();
        observeData();
    }

    /**
     * 初始化 RecyclerView，设置 LinearLayoutManager 和适配器。
     */
    private void setupRecyclerView() {
        rvLearned.setLayoutManager(new LinearLayoutManager(requireContext()));

        adapter = new FavoriteAdapter(new FavoriteAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(LearningRecord record, int position) {
                navigateToDetail(record);
            }

            @Override
            public void onUnfavoriteClick(LearningRecord record, int position) {
                // 已学列表不支持取消收藏操作，无操作
            }
        });
        adapter.setShowUnfavButton(false);

        rvLearned.setAdapter(adapter);
    }

    /**
     * 观察 ViewModel 中的已学诗词列表数据，自动更新 UI。
     * 自动修复 title/author/dynasty 为 null 的旧记录（首次加载时修复一次）。
     */
    private void observeData() {
        viewModel.getLearnedPoems().observe(getViewLifecycleOwner(), records -> {
            boolean isEmpty = records == null || records.isEmpty();
            layoutEmpty.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
            rvLearned.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
            if (records != null) adapter.setRecords(records);

            // 修复旧记录：补齐缺失的 title/author/dynasty
            if (records != null && !records.isEmpty()) {
                boolean needsRepair = false;
                for (LearningRecord r : records) {
                    if (r.title == null || r.author == null) {
                        needsRepair = true;
                        break;
                    }
                }
                if (needsRepair) {
                    AppExecutors.io(() -> repairRecords(records));
                }
            }
        });
    }

    /**
     * 在后台线程中修复缺失诗词元数据的旧记录。
     */
    private void repairRecords(List<LearningRecord> records) {
        PoemRepository repo = PoemRepository.getInstance();
        com.poetry.data.LearningRecordDao dao =
                LearningDatabase.getInstance(requireContext()).learningRecordDao();
        for (LearningRecord r : records) {
            if (r.title == null || r.author == null) {
                Poem poem = repo.findPoemById(r.poemId);
                if (poem != null) {
                    r.title = poem.title;
                    r.author = poem.author;
                    r.dynasty = poem.dynasty;
                    dao.insertLearningRecord(r);
                }
            }
        }
    }

    /**
     * 跳转到诗词详情页。
     *
     * @param record 学习记录（含 poemId、title、author、dynasty）
     */
    private void navigateToDetail(LearningRecord record) {
        Poem poem = PoemRepository.getInstance().findPoemById(record.poemId);
        if (poem == null) {
            Toast.makeText(requireContext(), "诗词数据加载失败", Toast.LENGTH_SHORT).show();
            return;
        }

        navController.navigate(R.id.nav_detail, PoemArgs.fromPoem(poem).toBundle());
    }
}

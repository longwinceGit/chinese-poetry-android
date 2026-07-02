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
import com.poetry.data.LearningRecord;
import com.poetry.data.PoemRepository;
import com.poetry.data.model.Poem;

/**
 * 收藏列表 Fragment。
 * <p>
 * 以线性列表形式展示用户收藏的诗词，支持点击跳转详情页和取消收藏操作。
 * 无收藏时显示空状态提示。利用 Room LiveData 自动刷新，无需手动刷新。
 * </p>
 */
public class FavoritesFragment extends Fragment {

    private RecyclerView rvFavorites;
    private LinearLayout layoutEmpty;
    private FavoriteAdapter adapter;
    private FavoritesViewModel viewModel;
    private NavController navController;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_favorites, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        navController = Navigation.findNavController(view);
        rvFavorites = view.findViewById(R.id.rv_favorites);
        layoutEmpty = view.findViewById(R.id.layout_empty);

        viewModel = new ViewModelProvider(this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(
                requireActivity().getApplication())).get(FavoritesViewModel.class);

        setupRecyclerView();
        observeData();
    }

    /**
     * 初始化 RecyclerView，设置 LinearLayoutManager 和适配器。
     */
    private void setupRecyclerView() {
        rvFavorites.setLayoutManager(new LinearLayoutManager(requireContext()));

        adapter = new FavoriteAdapter(new FavoriteAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(LearningRecord record, int position) {
                navigateToDetail(record);
            }

            @Override
            public void onUnfavoriteClick(LearningRecord record, int position) {
                viewModel.removeFavorite(record.poemId);
                Toast.makeText(requireContext(), "已取消收藏", Toast.LENGTH_SHORT).show();
            }
        });

        rvFavorites.setAdapter(adapter);
    }

    /**
     * 观察 ViewModel 中的收藏列表数据，自动更新 UI。
     */
    private void observeData() {
        viewModel.getFavorites().observe(getViewLifecycleOwner(), records -> {
            boolean isEmpty = records == null || records.isEmpty();
            layoutEmpty.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
            rvFavorites.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
            if (records != null) adapter.setRecords(records);
        });
    }

    /**
     * 跳转到诗词详情页。
     * <p>
     * 通过 PoemRepository 根据 poemId 查找完整诗词数据，
     * 然后以 Bundle 形式传递至 DetailFragment。
     * </p>
     *
     * @param record 收藏记录（含 poemId、title、author、dynasty）
     */
    private void navigateToDetail(LearningRecord record) {
        Poem poem = PoemRepository.getInstance().findPoemById(record.poemId);
        if (poem == null) {
            Toast.makeText(requireContext(), "诗词数据加载失败", Toast.LENGTH_SHORT).show();
            return;
        }

        Bundle args = new Bundle();
        args.putString("poem_id", poem.id);
        args.putString("poem_title", poem.title);
        args.putString("poem_author", poem.author);
        args.putString("poem_dynasty", poem.dynasty);
        args.putString("poem_category", poem.category != null ? poem.category : "");
        args.putString("poem_tag", poem.tag != null ? poem.tag : "");
        args.putString("poem_emoji", poem.emoji != null ? poem.emoji : "");
        args.putStringArray("poem_lines", poem.lines);
        args.putString("poem_explanation", poem.explanation != null ? poem.explanation : "");
        navController.navigate(R.id.nav_detail, args);
    }
}

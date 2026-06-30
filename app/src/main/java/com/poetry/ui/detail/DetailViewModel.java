package com.poetry.ui.detail;

import android.app.Application;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.poetry.data.LearningDatabase;
import com.poetry.data.LearningRecord;
import com.poetry.data.model.Poem;
import com.poetry.domain.LearningEngine;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DetailViewModel extends AndroidViewModel {

    private LearningDatabase db;
    private MutableLiveData<Poem> poem = new MutableLiveData<>();
    private MutableLiveData<Boolean> isFavorite = new MutableLiveData<>(false);
    private MutableLiveData<Boolean> isLearned = new MutableLiveData<>(false);
    private MutableLiveData<Boolean> showPinyin = new MutableLiveData<>(false);
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    public DetailViewModel(Application app) {
        super(app);
        db = LearningDatabase.getInstance(app);
    }

    public void loadPoem(Poem p) {
        poem.setValue(p);
        new Thread(() -> {
            LearningRecord record = db.poemDao().getLearningRecord(p.id);
            if (record != null) {
                isFavorite.postValue(record.favorite);
                isLearned.postValue(true);
            }
        }).start();
    }

    public void toggleFavorite() {
        Poem p = poem.getValue();
        if (p == null) return;
        Boolean current = isFavorite.getValue();
        boolean newFav = current == null || !current;
        isFavorite.setValue(newFav);
        new Thread(() -> {
            LearningRecord record = db.poemDao().getLearningRecord(p.id);
            if (record == null) {
                record = new LearningRecord(p.id, p.title, p.dynasty, p.author);
            }
            record.favorite = newFav;
            record.learnedAt = System.currentTimeMillis();
            db.poemDao().insertLearningRecord(record);
        }).start();
    }

    public void markLearned() {
        Poem p = poem.getValue();
        if (p == null) return;
        isLearned.setValue(true);
        new Thread(() -> {
            LearningRecord record = db.poemDao().getLearningRecord(p.id);
            if (record == null) {
                record = new LearningRecord(p.id, p.title, p.dynasty, p.author);
            }
            record.learnedAt = System.currentTimeMillis();
            db.poemDao().insertLearningRecord(record);

            String today = dateFormat.format(new Date());
            LearningEngine.calcStreak(today, 0);
        }).start();
    }

    public void togglePinyin() {
        Boolean current = showPinyin.getValue();
        boolean newVal = current == null || !current;
        showPinyin.setValue(newVal);
    }

    public LiveData<Poem> getPoem() { return poem; }
    public LiveData<Boolean> getIsFavorite() { return isFavorite; }
    public LiveData<Boolean> getIsLearned() { return isLearned; }
    public LiveData<Boolean> getShowPinyin() { return showPinyin; }
}

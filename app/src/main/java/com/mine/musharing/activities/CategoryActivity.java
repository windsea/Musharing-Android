package com.mine.musharing.activities;

import android.content.Intent;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.transition.TransitionSet;
import android.util.Log;
import android.view.MenuItem;

import com.mine.musharing.R;
import com.mine.musharing.models.User;
import com.mine.musharing.fragments.CategoryFragment;
import com.mine.musharing.utils.Utility;

/**
 * <h1>Category 活动</h1>
 *
 * 查看所有可选的播放列表；
 * 并可以点击某一播放列表来启动其 PlaylistActivity 以查看详情
 */
public class CategoryActivity extends AppCompatActivity {

    private static final String TAG = "CategotyActivity";
    private User user;

    private CategoryFragment categoryFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_category);

        Intent intent = getIntent();
        user = (User) intent.getBundleExtra("data").get("user");
        Log.d(TAG, "onCreate: user: " + user);

        // Show Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle("播放列表");
        }

        // 打包要传递给 fragments 的 user 数据
        Bundle bundle = new Bundle();
        bundle.putSerializable("user", user);

        // categoryFragment
        categoryFragment = new CategoryFragment();
        categoryFragment.setArguments(bundle);

        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.add(R.id.category_fragment_frame, categoryFragment);
        transaction.addToBackStack(null);
        transaction.commit();

        setupTransition();
    }

    /**
     * 设置 Activity 的转场动画
     */
    private void setupTransition() {
        TransitionSet transitionSet1 = Utility.getRandomTransitionSet();
        TransitionSet transitionSet2 = Utility.getRandomTransitionSet();
        TransitionSet transitionSet3 = Utility.getRandomTransitionSet();

        getWindow().setEnterTransition(transitionSet1);
        getWindow().setExitTransition(transitionSet2);
        getWindow().setReenterTransition(transitionSet3);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}

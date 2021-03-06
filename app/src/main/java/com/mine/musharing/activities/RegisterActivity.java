package com.mine.musharing.activities;

import android.content.Intent;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.transition.TransitionSet;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;


import com.bumptech.glide.Glide;
import com.mine.musharing.R;
import com.mine.musharing.models.User;
import com.mine.musharing.requestTasks.RegisterTask;
import com.mine.musharing.requestTasks.RequestTaskListener;
import com.mine.musharing.utils.ParseUtil;
import com.mine.musharing.utils.SensitiveWordsUtils;
import com.mine.musharing.utils.UserUtil;
import com.mine.musharing.utils.Utility;

import de.hdodenhof.circleimageview.CircleImageView;

/**
 * <h1>注册活动</h1>
 * 提供头像、用户名、密码尝试注册，成功后转到LoginActivity
 */
public class RegisterActivity extends AppCompatActivity {

    private LinearLayout registerLayout;

    private ProgressBar progressBar;

    private Button registerButton;

    private CircleImageView imageView;

    // TODO(b03) 实现让用户可选则头像
    private String imgUrl = "https://cdn.pixabay.com/photo/2014/12/21/23/59/block-576506_1280.png";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        registerLayout = findViewById(R.id.register_layout);

        progressBar = findViewById(R.id.register_progress_bar);
        progressBar.setIndeterminate(true);
        progressBar.setVisibility(View.GONE);

        registerButton = findViewById(R.id.register_button);

        imageView = findViewById(R.id.register_img);
        Glide.with(this).load(imgUrl).into(imageView);

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

    /**
     * 尝试注册
     */
    public void  registerOnClick(View view) {
        EditText userNameText = findViewById(R.id.register_user_name);
        EditText passwordText = findViewById(R.id.register_password);
        EditText passwordAgainText = findViewById(R.id.register_password_again);

        String userName = userNameText.getText().toString();
        String password = passwordText.getText().toString();
        String passwordAgain = passwordAgainText.getText().toString();

        if (TextUtils.isEmpty(imgUrl) || TextUtils.isEmpty(userName) || TextUtils.isEmpty(password)) {
            Toast.makeText(this, "请正确输入用户名和密码", Toast.LENGTH_SHORT).show();
            return;
        }

        if (userName.length() < 3) {
            Snackbar.make(registerLayout, "昵称不能少于3个字符。", Snackbar.LENGTH_LONG).show();
            return;
        }

        if (password.length() < 6) {
            Snackbar.make(registerLayout, "请设置至少6位密码。", Snackbar.LENGTH_LONG).show();
            return;
        }

        if (!TextUtils.equals(password, passwordAgain)) {
            Toast.makeText(this, "两次输入密码不一致", Toast.LENGTH_SHORT).show();
            return;
        }

        // 敏感词检测
        if(SensitiveWordsUtils.contains(userName, SensitiveWordsUtils.MinMatchTYpe)) {
            Snackbar.make(registerLayout, "昵称不能包含敏感词汇。", Snackbar.LENGTH_LONG).show();
            return;
        }

        String nameEncoded = UserUtil.encodeName(userName);
        String passwordEncrypted = UserUtil.encryptPassword(nameEncoded, password);

        new RegisterTask(new RequestTaskListener<User>() {
            // TODO [b01](todo://LoginActivity/b01)
            @Override
            public void onStart() {
                registerButton.setVisibility(View.GONE);
                progressBar.setVisibility(View.VISIBLE);
            }

            @Override
            public void onSuccess(User user) {
                runOnUiThread(() -> {
                    Toast.makeText(RegisterActivity.this, "注册成功，请登录\nWelcome, " + user.getName(), Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                    Bundle translateBundle = ActivityOptionsCompat.makeSceneTransitionAnimation(RegisterActivity.this).toBundle();
                    startActivity(intent, translateBundle);
                    // finish();
                });
            }

            @Override
            public void onFailed(String error) {
                runOnUiThread(() -> {
                    String readableError;
                    switch (error) {
                        case ParseUtil.ResponseError.NAME_OCCUPIED:
                            readableError = "用户名被占用了，换一个吧。"; break;
                        case ParseUtil.ResponseError.FROM_NOT_IN_ROOM:
                            readableError = "请加入房间"; break;
                        case ParseUtil.ResponseError.FROM_NOT_LOGIN:
                            readableError = "请登录。"; break;
                        case ParseUtil.ResponseError.FROM_NOT_EXIST:
                            readableError = "错误！发起用户不存在。"; break;
                        default:
                            readableError = "出错啦，请稍后再试TAT";
                    }
                    Toast.makeText(RegisterActivity.this, readableError, Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onFinish(String s) {
                progressBar.setVisibility(View.GONE);
                registerButton.setVisibility(View.VISIBLE);
            }
        }).execute(nameEncoded, passwordEncrypted, imgUrl);
    }
}

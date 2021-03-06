package com.mine.musharing.activities;

import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.mine.musharing.R;
import com.mine.musharing.audio.HotLineRecorder;
import com.mine.musharing.audio.MusicListHolder;
import com.mine.musharing.audio.PlayAsyncer;
import com.mine.musharing.models.User;
import com.mine.musharing.requestTasks.LoginTask;
import com.mine.musharing.requestTasks.RequestTaskListener;
import com.mine.musharing.services.NoticeService;
import com.mine.musharing.utils.ParseUtil;
import com.mine.musharing.utils.UserUtil;

import android.content.Intent;
import android.text.TextUtils;
import android.transition.ChangeBounds;
import android.transition.ChangeClipBounds;
import android.transition.ChangeImageTransform;
import android.transition.ChangeScroll;
import android.transition.ChangeTransform;
import android.transition.Explode;
import android.transition.Fade;
import android.transition.Slide;
import android.transition.TransitionSet;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.mine.musharing.utils.AESUtil;
import com.mine.musharing.utils.Utility;

import com.mine.musharing.utils.ParseUtil.ResponseError;

import java.util.Random;
import java.util.UUID;

/**
 * <h1>登录活动</h1>
 * 输入用户名、密码尝试登录，成功后转到RoomPlaylistActivity; 或 忘记密码/快速注册
 */
public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";

    private ProgressBar progressBar;
    private SharedPreferences pref;

    private LinearLayout loginLayout;
    private EditText userNameText;
    private EditText passwordText;
    private CheckBox rememberAccountCheckBox;
    private ImageButton loginButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        loginLayout = findViewById(R.id.login_layout);

        pref = PreferenceManager.getDefaultSharedPreferences(this);

        rememberAccountCheckBox = findViewById(R.id.remember_account);
        userNameText = findViewById(R.id.login_user_name);
        passwordText = findViewById(R.id.login_password);
        loginButton = findViewById(R.id.login_button);

        progressBar = findViewById(R.id.login_progress_bar);
        progressBar.setIndeterminate(true);
        progressBar.setVisibility(View.GONE);

        getRememberedAccount();

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
     * 尝试登录
     */
    public void loginOnClick(View view) {

        String userName = userNameText.getText().toString();
        String password = passwordText.getText().toString();

        if (TextUtils.isEmpty(userName) || TextUtils.isEmpty(password)) {
            Toast.makeText(this, "请输入用户名和密码", Toast.LENGTH_SHORT).show();
            return;
        }

        rememberAccount();

        String nameEncoded = UserUtil.encodeName(userName);
        String passwordEncrypted = UserUtil.encryptPassword(nameEncoded, password);

        new LoginTask(new RequestTaskListener<User>() {
            /*
            # TODO(b01): 一个关于提升用户体验的试探性意见

            在layout中顶部添加一条初始时不可见的横幅
            在请求完毕时，如果onFinish参数时FAILED则左侧一个❎（SUCCESS则为✅）
            然后 onSuccess | onFailed 的提示写在 ✅ | ❎ 右边
             */
            @Override
            public void onStart() {
                loginButton.setVisibility(View.GONE);
                progressBar.setVisibility(View.VISIBLE);
            }

            @Override
            public void onSuccess(User user) {

                PlayAsyncer.getInstance().setUser(user);
                MusicListHolder.getInstance().setUser(user);
                HotLineRecorder.getInstance().setUser(user);

                runOnUiThread(() -> {
                    // 打包数据
                    Bundle bundle = new Bundle();
                    bundle.putSerializable("user", user);

                    // 开启 NoticeService
                    Intent notifyServiceIntent = new Intent(LoginActivity.this, NoticeService.class);
                    notifyServiceIntent.putExtra("data", bundle);
                    startService(notifyServiceIntent);

                    // 转到 MusicChatActivity
                    Intent musicChatActivityIntent = new Intent(LoginActivity.this, MusicChatActivity.class);
                    musicChatActivityIntent.putExtra("data", bundle);

                    Bundle translateBundle = ActivityOptionsCompat.makeSceneTransitionAnimation(LoginActivity.this).toBundle();
                    startActivity(musicChatActivityIntent, translateBundle);

                    // finish();
                });
            }

            @Override
            public void onFailed(String error) {
                runOnUiThread(() -> {
                    String readableError;
                    switch (error) {
                        case ResponseError.WRONG_NAME:
                            readableError = "用户不存在"; break;
                        case ResponseError.WRONG_PASSWORD:
                            readableError = "密码错误"; break;
                        default:
                            readableError = "出错啦，请稍后再试TAT";
                    }
                    Toast.makeText(LoginActivity.this, readableError, Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onFinish(String s) {
                progressBar.setVisibility(View.GONE);
                loginButton.setVisibility(View.VISIBLE);
            }
        }).execute(nameEncoded, passwordEncrypted);
    }

    /**
     * 转到注册界面
     */
    public void toRegisterOnClick(View view) {
        runOnUiThread(() -> {
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            Bundle translateBundle = ActivityOptionsCompat.makeSceneTransitionAnimation(LoginActivity.this).toBundle();
            startActivity(intent, translateBundle);
            // finish();
        });

    }

    /**
     * 尝试读取记住的账户，如果有已记住的则填充至输入框中
     */
    public void getRememberedAccount() {
        boolean isRemembered = pref.getBoolean("remember", false);

        if (isRemembered) {
            try {
                String randomKey = pref.getString("key", "");
                String accountEncrypted = pref.getString("account", "");
                String passwordEncrypted = pref.getString("password", "");

                String accountDecrypted = AESUtil.decrypt(randomKey, accountEncrypted);
                String passwordDecrypted = AESUtil.decrypt(accountDecrypted, passwordEncrypted);

                userNameText.setText(AESUtil.decodeBase64(accountDecrypted));
                passwordText.setText(AESUtil.decodeBase64(passwordDecrypted));

                // "续订"
                rememberAccountCheckBox.setChecked(true);
            } catch (Exception e) {
                e.printStackTrace();
                Snackbar.make(loginLayout, "读取失败", Snackbar.LENGTH_LONG).show();
            }

        }
    }

    /**
     * 尝试记住账户，只有勾选了选择框才会记住
     */
    public void rememberAccount() {
        final SharedPreferences.Editor editor = pref.edit();

        if (rememberAccountCheckBox.isChecked()) {
            String accountRaw = AESUtil.encodeBase64(userNameText.getText().toString());
            String passwordRaw = AESUtil.encodeBase64(passwordText.getText().toString());

            /*
            用 randomUUID 加密 account
            未加密的 account 加密 password
             */
            String randomKey = UUID.randomUUID().toString();
            String accountEncrypted = AESUtil.encrypt(randomKey, accountRaw);
            String passwordEncrypted = AESUtil.encrypt(accountRaw, passwordRaw);

            editor.putString("key", randomKey);
            editor.putBoolean("remember", true);
            editor.putString("account", accountEncrypted);
            editor.putString("password", passwordEncrypted);

        } else {
            editor.clear();
        }
        editor.apply();
    }
}

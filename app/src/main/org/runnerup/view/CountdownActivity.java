package org.runnerup.view;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.util.Log;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.view.animation.AnimationSet;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import org.runnerup.R;
import org.runnerup.tracker.Tracker;

public class CountdownActivity extends AppCompatActivity {

    public static final String EXTRA_RUN_NAME    = "run_name";
    public static final String EXTRA_PLAYER_NAME = "PLAYER_NAME";
    public static final String EXTRA_TOKEN       = "token";

    private TextView tvCountdown;
    private CountDownTimer countDownTimer;
    private boolean mIsBound = false;
    private Tracker mTracker = null;

        private final ServiceConnection mConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                mTracker = ((Tracker.LocalBinder) service).getService();
            }
            @Override
            public void onServiceDisconnected(ComponentName name) {
                mTracker = null;
            }
        };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String runName = getIntent().getStringExtra(EXTRA_RUN_NAME);

        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
        );
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(runName);
        }

        mIsBound = getApplicationContext().bindService(
                new Intent(this, Tracker.class),
                mConnection,
                Context.BIND_AUTO_CREATE
        );
        String token      = getIntent().getStringExtra(EXTRA_TOKEN);

        setContentView(R.layout.fragment_countdown);

        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(
                findViewById(R.id.countdown_root), (v, insets) -> {
                    int top = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars()).top;
                    v.setPadding(v.getPaddingLeft(), top, v.getPaddingRight(), v.getPaddingBottom());
                    return insets;
                });

        tvCountdown = findViewById(R.id.tv_countdown);
        ((TextView) findViewById(R.id.tv_countdown_token)).setText(
                token != null ? getString(R.string.code_placeholder, token) : "");

        startCountdown();
    }

    private void startCountdown() {
        tvCountdown.post(() -> {
            showNumber("3");
            countDownTimer = new CountDownTimer(3000, 1000) {
                int tick = 0;

                @Override
                public void onTick(long millisUntilFinished) {
                    tick++;
                    if (tick == 1) showNumber("3");
                    else if (tick == 2) showNumber("2");
                    else if (tick == 3) showNumber("1");
                }

                @Override
                public void onFinish() {
                    showNumber(getString(R.string.countdown_go));
                    tvCountdown.postDelayed(CountdownActivity.this::launchLiveRun, 600);
                }
            };
            countDownTimer.start();
        });
    }

    private void showNumber(String text) {
        tvCountdown.setText(text);

        ScaleAnimation scale = new ScaleAnimation(
                0.4f, 1.0f, 0.4f, 1.0f,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f
        );
        scale.setDuration(200);

        AlphaAnimation fade = new AlphaAnimation(0f, 1f);
        fade.setDuration(200);

        AnimationSet set = new AnimationSet(true);
        set.addAnimation(scale);
        set.addAnimation(fade);
        tvCountdown.startAnimation(set);
    }

    private void launchLiveRun() {
        if (mTracker != null) mTracker.start();

        Intent intent = new Intent(this, LiveRunActivity.class);

        intent.putExtra(LiveRunActivity.EXTRA_RUN_NAME,
                getIntent().getStringExtra(EXTRA_RUN_NAME));

        intent.putExtra("PLAYER_NAME",
                getIntent().getStringExtra(EXTRA_PLAYER_NAME));

        intent.putExtra("DISTANCE",
                getIntent().getFloatExtra("DISTANCE", 5.0f));

        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (countDownTimer != null) countDownTimer.cancel();
        if (mIsBound) {
            getApplicationContext().unbindService(mConnection);
            mIsBound = false;
        }
    }
}
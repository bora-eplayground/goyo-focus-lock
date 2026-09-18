package com.goyo.focuslock;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Space;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Locale;

public class MainActivity extends Activity {
    private static final int BG = Color.rgb(244, 248, 252);
    private static final int INK = Color.rgb(24, 50, 74);
    private static final int MUTED = Color.rgb(111, 131, 149);
    private static final int BLUE = Color.rgb(47, 101, 143);
    private static final int SOFT = Color.rgb(220, 235, 245);
    private static final int FOCUS_BG = Color.rgb(28, 64, 88);
    private static final String PREFS = "goyo_state";
    private static final String KEY_END = "focus_end";
    private static final String KEY_DURATION = "focus_duration";
    private static final String KEY_FILTER = "previous_filter";

    private FrameLayout root;
    private CountDownTimer timer;
    private int selectedMinutes = 25;
    private boolean focusing = false;
    private NotificationManager notificationManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        root = new FrameLayout(this);
        setContentView(root);

        long end = getPreferences().getLong(KEY_END, 0);
        if (end > System.currentTimeMillis()) {
            selectedMinutes = getPreferences().getInt(KEY_DURATION, 25);
            showFocus(end);
        } else {
            clearFocusState();
            if (notificationManager.isNotificationPolicyAccessGranted()) showHome();
            else showPermission();
        }
    }

    private android.content.SharedPreferences getPreferences() {
        return getSharedPreferences(PREFS, MODE_PRIVATE);
    }

    private LinearLayout base(boolean dark) {
        root.removeAllViews();
        root.setBackgroundColor(dark ? FOCUS_BG : BG);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(28), dp(46), dp(28), dp(26));
        root.addView(layout, new FrameLayout.LayoutParams(-1, -1));
        return layout;
    }

    private TextView text(String value, float sp, int color, int gravity, boolean bold) {
        TextView v = new TextView(this);
        v.setText(value);
        v.setTextSize(sp);
        v.setTextColor(color);
        v.setGravity(gravity);
        v.setTypeface(Typeface.create("sans", bold ? Typeface.BOLD : Typeface.NORMAL));
        v.setLineSpacing(0, 1.2f);
        return v;
    }

    private Button button(String label, boolean primary) {
        Button b = new Button(this);
        b.setText(label);
        b.setTextSize(16);
        b.setAllCaps(false);
        b.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        b.setTextColor(primary ? Color.WHITE : BLUE);
        b.setBackgroundColor(primary ? BLUE : Color.TRANSPARENT);
        b.setMinHeight(dp(56));
        return b;
    }

    private void brand(LinearLayout layout) {
        TextView b = text("◉  고요", 16, Color.rgb(53, 90, 118), Gravity.START, true);
        layout.addView(b, new LinearLayout.LayoutParams(-1, dp(42)));
    }

    private void showPermission() {
        focusing = false;
        normalWindow();
        LinearLayout l = base(false);
        brand(l);
        Space top = new Space(this);
        l.addView(top, new LinearLayout.LayoutParams(1, 0, 1));
        TextView icon = text("☾", 56, BLUE, Gravity.CENTER, false);
        l.addView(icon, new LinearLayout.LayoutParams(-1, dp(90)));
        l.addView(text("집중을 위해\n권한이 필요해요", 29, INK, Gravity.CENTER, true));
        TextView desc = text("집중 시간 동안 알림과 전화 벨을\n조용하게 하기 위해 필요합니다.", 16, MUTED, Gravity.CENTER, false);
        LinearLayout.LayoutParams dp = new LinearLayout.LayoutParams(-1, -2);
        dp.topMargin = dp(18);
        l.addView(desc, dp);
        Space bottom = new Space(this);
        l.addView(bottom, new LinearLayout.LayoutParams(1, 0, 1));
        Button grant = button("권한 설정하기", true);
        grant.setOnClickListener(v -> startActivity(new Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)));
        l.addView(grant, new LinearLayout.LayoutParams(-1, dp(58)));
        Button later = button("나중에", false);
        later.setOnClickListener(v -> showHome());
        l.addView(later, new LinearLayout.LayoutParams(-1, dp(50)));
        l.addView(text("권한은 언제든 다시 변경할 수 있어요", 13, MUTED, Gravity.CENTER, false));
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!focusing && root != null && notificationManager.isNotificationPolicyAccessGranted()) {
            long end = getPreferences().getLong(KEY_END, 0);
            if (end <= 0 && root.getChildCount() > 0) {
                View child = root.getChildAt(0);
                if (child.getTag() != null && "permission".equals(child.getTag())) showHome();
            }
        }
    }

    private void showHome() {
        focusing = false;
        normalWindow();
        LinearLayout l = base(false);
        brand(l);
        TextView title = text("얼마나 집중할까요?", 29, INK, Gravity.START, true);
        LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(-1, -2);
        tp.topMargin = dp(34);
        l.addView(title, tp);
        l.addView(text("1분부터 60분까지 설정할 수 있어요", 15, MUTED, Gravity.START, false));

        Space s1 = new Space(this);
        l.addView(s1, new LinearLayout.LayoutParams(1, 0, 1));

        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER);
        Button minus = button("−", false);
        Button plus = button("+", false);
        TextView value = text(selectedMinutes + "분", 62, Color.rgb(36, 77, 109), Gravity.CENTER, true);
        row.addView(minus, new LinearLayout.LayoutParams(dp(58), dp(58)));
        row.addView(value, new LinearLayout.LayoutParams(0, dp(92), 1));
        row.addView(plus, new LinearLayout.LayoutParams(dp(58), dp(58)));
        l.addView(row, new LinearLayout.LayoutParams(-1, -2));

        SeekBar seek = new SeekBar(this);
        seek.setMax(59);
        seek.setProgress(selectedMinutes - 1);
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(-1, dp(54));
        sp.topMargin = dp(22);
        l.addView(seek, sp);

        LinearLayout labels = new LinearLayout(this);
        TextView one = text("1분", 12, MUTED, Gravity.START, false);
        TextView sixty = text("60분", 12, MUTED, Gravity.END, false);
        labels.addView(one, new LinearLayout.LayoutParams(0, -2, 1));
        labels.addView(sixty, new LinearLayout.LayoutParams(0, -2, 1));
        l.addView(labels);

        minus.setOnClickListener(v -> { selectedMinutes = Math.max(1, selectedMinutes - 1); value.setText(selectedMinutes + "분"); seek.setProgress(selectedMinutes - 1); });
        plus.setOnClickListener(v -> { selectedMinutes = Math.min(60, selectedMinutes + 1); value.setText(selectedMinutes + "분"); seek.setProgress(selectedMinutes - 1); });
        seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) { selectedMinutes = progress + 1; value.setText(selectedMinutes + "분"); }
            public void onStartTrackingTouch(SeekBar bar) {}
            public void onStopTrackingTouch(SeekBar bar) {}
        });

        Space s2 = new Space(this);
        l.addView(s2, new LinearLayout.LayoutParams(1, 0, 1));
        Button start = button("집중 시작", true);
        start.setOnClickListener(v -> showConfirm());
        l.addView(start, new LinearLayout.LayoutParams(-1, dp(58)));
        TextView hint = text("집중 중에는 알림과 전화 벨이 조용해집니다", 13, MUTED, Gravity.CENTER, false);
        LinearLayout.LayoutParams hp = new LinearLayout.LayoutParams(-1, -2);
        hp.topMargin = dp(14);
        l.addView(hint, hp);
    }

    private void showConfirm() {
        LinearLayout l = base(false);
        Space top = new Space(this);
        l.addView(top, new LinearLayout.LayoutParams(1, 0, 1));
        l.addView(text("▣", 44, BLUE, Gravity.CENTER, false), new LinearLayout.LayoutParams(-1, dp(82)));
        l.addView(text("집중을 시작할까요?", 29, INK, Gravity.CENTER, true));
        TextView duration = text(selectedMinutes + "분 집중", 31, Color.rgb(36, 79, 110), Gravity.CENTER, true);
        LinearLayout.LayoutParams durp = new LinearLayout.LayoutParams(-1, -2);
        durp.topMargin = dp(34);
        l.addView(duration, durp);
        TextView copy = text("집중을 시작하면 설정한 시간이 끝날 때까지\n앱 내부에서 종료할 수 없습니다.\n\n시간이 끝나면 자동으로 해제됩니다.", 15, MUTED, Gravity.CENTER, false);
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(-1, -2);
        cp.topMargin = dp(25);
        l.addView(copy, cp);
        Space bottom = new Space(this);
        l.addView(bottom, new LinearLayout.LayoutParams(1, 0, 1));
        Button start = button("시작하기", true);
        start.setOnClickListener(v -> beginFocus());
        l.addView(start, new LinearLayout.LayoutParams(-1, dp(58)));
        Button cancel = button("취소", false);
        cancel.setOnClickListener(v -> showHome());
        l.addView(cancel, new LinearLayout.LayoutParams(-1, dp(52)));
    }

    private void beginFocus() {
        if (!notificationManager.isNotificationPolicyAccessGranted()) {
            Toast.makeText(this, "방해금지 권한을 먼저 허용해 주세요.", Toast.LENGTH_LONG).show();
            startActivity(new Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS));
            return;
        }
        int previous = notificationManager.getCurrentInterruptionFilter();
        long end = System.currentTimeMillis() + selectedMinutes * 60_000L;
        getPreferences().edit()
                .putLong(KEY_END, end)
                .putInt(KEY_DURATION, selectedMinutes)
                .putInt(KEY_FILTER, previous)
                .apply();
        notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_NONE);
        showFocus(end);
        try { startLockTask(); } catch (Exception ignored) {}
    }

    private void showFocus(long endTime) {
        focusing = true;
        focusWindow();
        LinearLayout l = base(true);
        l.setGravity(Gravity.CENTER_HORIZONTAL);
        l.addView(text("집중 중", 15, Color.rgb(192, 211, 225), Gravity.CENTER, true), new LinearLayout.LayoutParams(-1, dp(46)));
        Space top = new Space(this);
        l.addView(top, new LinearLayout.LayoutParams(1, 0, 1));

        TextView countdown = text("00:00", 64, Color.rgb(245, 249, 252), Gravity.CENTER, true);
        l.addView(countdown, new LinearLayout.LayoutParams(-1, dp(126)));
        l.addView(text("지금은 나에게 집중하는 시간", 18, Color.WHITE, Gravity.CENTER, true));
        TextView sub = text("잠시 휴대폰을 내려놓으세요", 14, Color.rgb(169, 191, 206), Gravity.CENTER, false);
        LinearLayout.LayoutParams subp = new LinearLayout.LayoutParams(-1, -2);
        subp.topMargin = dp(10);
        l.addView(sub, subp);

        Space bottom = new Space(this);
        l.addView(bottom, new LinearLayout.LayoutParams(1, 0, 1));
        l.addView(text("설정한 시간이 끝나면 자동으로 해제됩니다", 12, Color.rgb(130, 157, 173), Gravity.CENTER, false));
        Button emergency = button("☎  긴급 전화", false);
        emergency.setTextColor(Color.rgb(215, 230, 239));
        emergency.setOnClickListener(v -> openEmergencyDialer());
        LinearLayout.LayoutParams ep = new LinearLayout.LayoutParams(-1, dp(52));
        ep.topMargin = dp(8);
        l.addView(emergency, ep);

        if (timer != null) timer.cancel();
        long remaining = Math.max(0, endTime - System.currentTimeMillis());
        timer = new CountDownTimer(remaining, 1000) {
            public void onTick(long ms) { countdown.setText(formatTime(ms)); }
            public void onFinish() { completeFocus(); }
        }.start();
        countdown.setText(formatTime(remaining));
    }

    private String formatTime(long ms) {
        long total = Math.max(0, (ms + 999) / 1000);
        return String.format(Locale.KOREA, "%02d:%02d", total / 60, total % 60);
    }

    private void openEmergencyDialer() {
        try { stopLockTask(); } catch (Exception ignored) {}
        Intent dial = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:112"));
        startActivity(dial);
    }

    private void completeFocus() {
        focusing = false;
        try { stopLockTask(); } catch (Exception ignored) {}
        restoreDnd();
        clearFocusState();
        normalWindow();

        LinearLayout l = base(false);
        Space top = new Space(this);
        l.addView(top, new LinearLayout.LayoutParams(1, 0, 1));
        l.addView(text("✓", 52, BLUE, Gravity.CENTER, true), new LinearLayout.LayoutParams(-1, dp(90)));
        l.addView(text("집중 완료", 33, INK, Gravity.CENTER, true));
        l.addView(text("잘하셨어요", 17, MUTED, Gravity.CENTER, false));
        TextView summary = text(selectedMinutes + "분 동안 집중했어요", 20, Color.rgb(45, 86, 117), Gravity.CENTER, true);
        LinearLayout.LayoutParams sump = new LinearLayout.LayoutParams(-1, -2);
        sump.topMargin = dp(42);
        l.addView(summary, sump);
        l.addView(text("잠깐의 집중이 하루를 바꿉니다", 14, MUTED, Gravity.CENTER, false));
        Space bottom = new Space(this);
        l.addView(bottom, new LinearLayout.LayoutParams(1, 0, 1));
        Button again = button("다시 시작하기", true);
        again.setOnClickListener(v -> showConfirm());
        l.addView(again, new LinearLayout.LayoutParams(-1, dp(58)));
        Button home = button("홈으로", false);
        home.setOnClickListener(v -> showHome());
        l.addView(home, new LinearLayout.LayoutParams(-1, dp(50)));
    }

    private void restoreDnd() {
        if (notificationManager.isNotificationPolicyAccessGranted()) {
            int previous = getPreferences().getInt(KEY_FILTER, NotificationManager.INTERRUPTION_FILTER_ALL);
            notificationManager.setInterruptionFilter(previous);
        }
    }

    private void clearFocusState() {
        getPreferences().edit().remove(KEY_END).remove(KEY_DURATION).remove(KEY_FILTER).apply();
    }

    private void focusWindow() {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().setStatusBarColor(FOCUS_BG);
        getWindow().setNavigationBarColor(FOCUS_BG);
        hideSystemUi();
    }

    private void normalWindow() {
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
    }

    private void hideSystemUi() {
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY |
                View.SYSTEM_UI_FLAG_FULLSCREEN |
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus && focusing) hideSystemUi();
    }

    @Override
    public void onBackPressed() {
        if (!focusing) super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        if (timer != null) timer.cancel();
        super.onDestroy();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}

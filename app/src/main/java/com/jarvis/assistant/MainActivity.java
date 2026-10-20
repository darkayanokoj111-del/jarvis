package com.jarvis.assistant;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER);
        box.setBackgroundColor(0xFF02060D);
        box.setPadding(60, 60, 60, 60);

        TextView t = new TextView(this);
        t.setText("J A R V I S");
        t.setTextColor(0xFF38E8FF);
        t.setTextSize(28);
        t.setGravity(Gravity.CENTER);
        box.addView(t);

        Button go = new Button(this);
        go.setText("Wake Jarvis");
        go.setOnClickListener(v -> start());
        box.addView(go);
        setContentView(box);
    }

    private void start() {
        if (!Settings.canDrawOverlays(this)) {
            startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName())));
            return;
        }
        requestPermissions(new String[]{
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.READ_CONTACTS,
                Manifest.permission.CALL_PHONE,
                Manifest.permission.POST_NOTIFICATIONS}, 1);
    }

    @Override
    public void onRequestPermissionsResult(int code, String[] p, int[] r) {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED) {
            startForegroundService(new Intent(this, JarvisService.class));
            finish();
        }
    }
}

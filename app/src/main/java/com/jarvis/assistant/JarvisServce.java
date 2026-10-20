package com.jarvis.assistant;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.ContactsContract;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.view.Gravity;
import android.view.WindowManager;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public class JarvisService extends Service {
    private SpeechRecognizer sr;
    private TextToSpeech tts;
    private boolean ttsReady = false;
    private boolean speaking = false;
    private long awakeUntil = 0;
    private WindowManager wm;
    private OrbView orb;
    private final Handler h = new Handler(Looper.getMainLooper());
    private final String[] jokes = {
            "Why did the phone go to school? To get smarter, sir.",
            "Why was the computer cold? It left its Windows open.",
            "I would tell you a joke about the cloud, but it might go over your head."
    };

    @Override
    public IBinder onBind(Intent i) { return null; }

    @Override
    public int onStartCommand(Intent i, int f, int id) {
        NotificationChannel ch = new NotificationChannel("jarvis", "Jarvis",
                NotificationManager.IMPORTANCE_LOW);
        getSystemService(NotificationManager.class).createNotificationChannel(ch);
        Notification n = new Notification.Builder(this, "jarvis")
                .setContentTitle("Jarvis is listening")
                .setSmallIcon(android.R.drawable.ic_btn_speak_now).build();
        startForeground(1, n);
        wm = (WindowManager) getSystemService(WINDOW_SERVICE);

        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                tts.setLanguage(Locale.UK);
                tts.setPitch(0.7f);
                tts.setSpeechRate(1.25f);
                tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                    @Override public void onStart(String s) { }
                    @Override public void onError(String s) { done(); }
                    @Override public void onDone(String s) { done(); }
                });
                ttsReady = true;
                showOrb();
                speak(greeting() + ", sir. Jarvis is ready.");
            }
        });
        return START_STICKY;
    }

    private String greeting() {
        int hr = Integer.parseInt(new SimpleDateFormat("H", Locale.getDefault()).format(new Date()));
        return hr < 12 ? "Good morning" : hr < 17 ? "Good afternoon" : "Good evening";
    }

    private void done() {
        h.post(() -> {
            speaking = false;
            if (System.currentTimeMillis() >= awakeUntil) hideOrb();
            startListening();
        });
    }

    private void speak(String s) {
        speaking = true;
        try { sr.cancel(); } catch (Exception e) { }
        if (!ttsReady) { done(); return; }
        tts.speak(s, TextToSpeech.QUEUE_FLUSH, null, "j");
    }

    private void showOrb() {
        if (orb != null) return;
        orb = new OrbView(this);
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT, 1000,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                PixelFormat.TRANSLUCENT);
        lp.gravity = Gravity.BOTTOM;
        try { wm.addView(orb, lp); } catch (Exception e) { orb = null; }
    }

    private void hideOrb() {
        if (orb == null) return;
        try { wm.removeView(orb); } catch (Exception e) { }
        orb = null;
    }

    private void startListening() {
        if (speaking) return;
        h.post(() -> {
            if (sr == null) {
                sr = SpeechRecognizer.createSpeechRecognizer(this);
                sr.setRecognitionListener(new RecognitionListener() {
                    @Override public void onReadyForSpeech(Bundle b) { }
                    @Override public void onBeginningOfSpeech() { }
                    @Override public void onRmsChanged(float rms) {
                        if (orb != null) orb.setLevel(1f + Math.max(0f, rms) / 4f);
                    }
                    @Override public void onBufferReceived(byte[] b) { }
                    @Override public void onEndOfSpeech() { }
                    @Override public void onPartialResults(Bundle b) { }
                    @Override public void onEvent(int t, Bundle b) { }
                    @Override public void onError(int e) {
                        if (!speaking) h.postDelayed(() -> startListening(), 700);
                    }
                    @Override public void onResults(Bundle b) { onHeard(b); }
                });
            }
            Intent i = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            i.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-IN");
            i.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 2500L);
            i.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 2500L);
            try { sr.startListening(i); } catch (Exception e) { }
        });
    }

    private void onHeard(Bundle b) {
        ArrayList<String> list = b.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        if (list == null || list.isEmpty()) { startListening(); return; }
        String t = list.get(0).toLowerCase(Locale.ROOT).trim();
        boolean wake = t.contains("jarvis") || t.contains("jar vis") || t.contains("jarvish");
        boolean awake = System.currentTimeMillis() < awakeUntil;
        if (!wake && !awake) { startListening(); return; }
        String cmd = t.replaceAll("(hey |hi |hello |ok )?(jarvis|jar vis|jarvish)[,.]?\\s*", "").trim();
        showOrb();
        if (cmd.isEmpty()) {
            awakeUntil = System.currentTimeMillis() + 10000;
            speak("Yes, sir?");
            return;
        }
        awakeUntil = 0;
        handle(cmd);
    }

    private void go(Intent i) {
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        h.postDelayed(() -> {
            try { startActivity(i); } catch (Exception e) { }
        }, 1500);
    }

    private void handle(String cmd) {
        if (cmd.contains("time")) {
            speak("It is " + new SimpleDateFormat("h:mm a", Locale.getDefault()).format(new Date()) + ", sir.");
        } else if (cmd.contains("date") || cmd.contains("today")) {
            speak("Today is " + new SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()).format(new Date()) + ", sir.");
        } else if (cmd.startsWith("call ")) {
            String name = cmd.substring(5).trim();
            String num = findNumber(name);
            if (num == null) {
                speak("I could not find " + name + " in your contacts, sir.");
            } else {
                speak("Calling " + name + ", sir.");
                go(new Intent(Intent.ACTION_CALL, Uri.parse("tel:" + Uri.encode(num))));
            }
        } else if (cmd.startsWith("play ")) {
            String q = cmd.substring(5).replace(" on youtube", "").trim();
            speak("Playing " + q + " on YouTube, sir.");
            go(new Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://www.youtube.com/results?search_query=" + Uri.encode(q))));
        } else if (cmd.startsWith("search ") || cmd.startsWith("google ")) {
            String q = cmd.substring(cmd.indexOf(' ') + 1).replaceFirst("^for ", "").trim();
            speak("Searching for " + q + ", sir.");
            go(new Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://www.google.com/search?q=" + Uri.encode(q))));
        } else if (cmd.startsWith("open ")) {
            String name = cmd.substring(5).trim();
            Intent app = findApp(name);
            if (app == null) {
                speak("I could not find " + name + " on your phone, sir.");
            } else {
                speak("Opening " + name + ", sir.");
                go(app);
            }
        } else if (cmd.contains("joke")) {
            speak(jokes[new Random().nextInt(jokes.length)]);
        } else if (cmd.contains("who are you") || cmd.contains("your name")) {
            speak("I am Jarvis, your personal assistant, sir.");
        } else if (cmd.contains("thank")) {
            speak("Always a pleasure, sir.");
        } else {
            speak("I am still learning that one, sir.");
        }
    }

    private String findNumber(String name) {
        try (Cursor c = getContentResolver().query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                new String[]{ContactsContract.CommonDataKinds.Phone.NUMBER},
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " LIKE ?",
                new String[]{"%" + name + "%"}, null)) {
            if (c != null && c.moveToFirst()) return c.getString(0);
        } catch (Exception e) { }
        return null;
    }

    private Intent findApp(String name) {
        PackageManager pm = getPackageManager();
        Intent main = new Intent(Intent.ACTION_MAIN);
        main.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> apps = pm.queryIntentActivities(main, 0);
        for (ResolveInfo r : apps) {
            String label = r.loadLabel(pm).toString().toLowerCase(Locale.ROOT);
            if (label.equals(name) || label.contains(name)) {
                return pm.getLaunchIntentForPackage(r.activityInfo.packageName);
            }
        }
        return null;
    }

    @Override
    public void onDestroy() {
        hideOrb();
        if (sr != null) sr.destroy();
        if (tts != null) tts.shutdown();
        super.onDestroy();
    }
            }

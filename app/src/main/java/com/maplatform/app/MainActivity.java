package com.maplatform.app;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintManager;
import android.provider.MediaStore;
import android.speech.tts.TextToSpeech;
import android.speech.tts.Voice;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity implements TextToSpeech.OnInitListener {

    private static final String TAG = "MA_Platform";
    private WebView webView;
    private TextToSpeech tts;
    private boolean ttsReady = false;
    private Voice arabicFemaleVoice;
    private Vibrator vibrator;
    private long lastBackPressTime = 0;
    private FrameLayout rootContainer;
    private LinearLayout splashContainer;
    private boolean splashShown = false;

    @SuppressLint({"SetJavaScriptEnabled", "AddJavascriptInterface"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 🌙 الوضع الليلي التلقائي حسب النظام
        AppCompatDelegate.setDefaultNightMode(
            AppCompatDelegate.DEFAULT_NIGHT_MODE != 0
                ? AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                : AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);

        // 💡 إبقاء الشاشة مضاءة
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // 🔒 منع لقطات الشاشة
        getWindow().setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE);

        // تهيئة الجذر
        rootContainer = new FrameLayout(this);
        rootContainer.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));
        setContentView(rootContainer);

        // 1) عرض Splash أولاً
        buildSplash();

        // 2) تجهيز WebView في الخلفية
        buildWebView();

        // 3) تهيئة الاهتزاز
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        // 4) تهيئة محرك النطق
        tts = new TextToSpeech(this, this);

        // 5) جدولة الإشعارات اليومية
        try { NotificationHelper.scheduleDailyReminder(this); } catch (Exception ignored) {}
    }

    /** 🎨 بناء شاشة Splash */
    private void buildSplash() {
        splashContainer = new LinearLayout(this);
        splashContainer.setOrientation(LinearLayout.VERTICAL);
        splashContainer.setGravity(android.view.Gravity.CENTER);
        splashContainer.setBackgroundColor(0xFF0F172A);
        splashContainer.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));

        // الشعار
        TextView logo = new TextView(this);
        logo.setText("M.A");
        logo.setTextSize(64);
        logo.setTextColor(0xFFFBBF24);
        logo.setTypeface(null, android.graphics.Typeface.BOLD);
        logo.setGravity(android.view.Gravity.CENTER);
        logo.setShadowLayer(20, 0, 0, 0x66FBBF24);

        // اسم المنصة
        TextView title = new TextView(this);
        title.setText("منصة M.A للتدريب والتأهيل المهني");
        title.setTextSize(18);
        title.setTextColor(0xFFF8FAFC);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        title.setGravity(android.view.Gravity.CENTER);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        titleParams.topMargin = 30;

        // الوصف
        TextView subtitle = new TextView(this);
        subtitle.setText("البيئة التفاعلية المتطورة لإعداد الكوادر");
        subtitle.setTextSize(14);
        subtitle.setTextColor(0xFF94A3B8);
        subtitle.setGravity(android.view.Gravity.CENTER);
        LinearLayout.LayoutParams subParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        subParams.topMargin = 12;

        // مؤشر التحميل
        android.widget.ProgressBar spinner = new android.widget.ProgressBar(
                this, null, android.R.attr.progressBarStyle);
        spinner.setIndeterminate(true);
        LinearLayout.LayoutParams spinnerParams = new LinearLayout.LayoutParams(80, 80);
        spinnerParams.topMargin = 50;

        // الاسم
        TextView credit = new TextView(this);
        credit.setText("إعداد: أ. مصطفى علي اكر");
        credit.setTextSize(12);
        credit.setTextColor(0xFFFBBF24);
        credit.setGravity(android.view.Gravity.CENTER);
        LinearLayout.LayoutParams credParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        credParams.topMargin = 60;

        splashContainer.addView(logo);
        splashContainer.addView(title, titleParams);
        splashContainer.addView(subtitle, subParams);
        splashContainer.addView(spinner, spinnerParams);
        splashContainer.addView(credit, credParams);

        rootContainer.addView(splashContainer);

        // أنيميشن fade in
        splashContainer.setAlpha(0f);
        splashContainer.animate().alpha(1f).setDuration(600).start();

        // إخفاء بعد 2.2 ثانية
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() { hideSplash(); }
        }, 2200);
    }

    private void hideSplash() {
        if (splashShown) return;
        splashShown = true;
        if (splashContainer == null) return;
        splashContainer.animate().alpha(0f).setDuration(450).withEndAction(new Runnable() {
            @Override
            public void run() {
                if (splashContainer != null && splashContainer.getParent() != null) {
                    rootContainer.removeView(splashContainer);
                }
            }
        }).start();
    }

    /** 🌐 بناء WebView */
    @SuppressLint({"SetJavaScriptEnabled", "AddJavascriptInterface"})
    private void buildWebView() {
        webView = new WebView(this);
        webView.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));

        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setAllowFileAccess(true);
        s.setAllowContentAccess(true);
        s.setAllowFileAccessFromFileURLs(true);
        s.setAllowUniversalAccessFromFileURLs(true);
        s.setDefaultTextEncodingName("utf-8");
        s.setLoadWithOverviewMode(true);
        s.setUseWideViewPort(true);
        s.setBuiltInZoomControls(false);
        s.setDisplayZoomControls(false);
        s.setMediaPlaybackRequiresUserGesture(false);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            s.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        }

        webView.setWebChromeClient(new WebChromeClient());

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                injectAllFixes(view);
            }
        });

        webView.addJavascriptInterface(new AndroidBridge(), "AndroidAPI");

        rootContainer.addView(webView);

        webView.loadUrl("file:///android_asset/index.html");
    }

    /** حقن كل الإصلاحات + زر الصوت */
    private void injectAllFixes(final WebView view) {
        String js =
            "(function(){" +
            "  if(window.__ma_all_fixes) return;" +
            "  window.__ma_all_fixes = true;" +
            "  window.confirm = function(msg){" +
            "    try{ if(window.AndroidAPI && window.AndroidAPI.nativeConfirm)" +
            "      return window.AndroidAPI.nativeConfirm(String(msg || '')); }catch(e){}" +
            "    return true;" +
            "  };" +
            "  window.alert = function(msg){" +
            "    try{ if(window.AndroidAPI && window.AndroidAPI.nativeAlert)" +
            "      window.AndroidAPI.nativeAlert(String(msg || '')); }catch(e){}" +
            "  };" +
            "  window.print = function(){" +
            "    try{ if(window.AndroidAPI && window.AndroidAPI.print)" +
            "      window.AndroidAPI.print(); }catch(e){}" +
            "  };" +
            "  document.addEventListener('click', function(ev){" +
            "    try{" +
            "      var t = ev.target; if(!t) return;" +
            "      var tag = (t.tagName || '').toLowerCase();" +
            "      var cls = (t.className || '') + '';" +
            "      if(tag === 'button' || /option|btn|pal|mode-card|card-btn/i.test(cls)){" +
            "        if(window.AndroidAPI && window.AndroidAPI.hapticLight)" +
            "          window.AndroidAPI.hapticLight();" +
            "      }" +
            "    }catch(e){}" +
            "  }, true);" +
            "})();";
        view.evaluateJavascript(js, null);

        view.postDelayed(new Runnable() {
            @Override
            public void run() { injectVoiceButton(view); }
        }, 400);
    }

    private void injectVoiceButton(final WebView view) {
        String js =
            "(function(){" +
            "  var old = document.getElementById('ma-voice-fab');" +
            "  if(old) old.remove();" +
            "  var btn = document.createElement('button');" +
            "  btn.id = 'ma-voice-fab';" +
            "  btn.type = 'button';" +
            "  btn.innerHTML = '🔊';" +
            "  btn.title = 'قراءة السؤال بصوت عالٍ';" +
            "  btn.style.cssText = 'position:fixed;bottom:96px;left:16px;width:56px;height:56px;" +
            "    border-radius:50%;background:linear-gradient(135deg,#0284c7,#1e3a8a);color:#fff;" +
            "    border:2px solid rgba(255,255,255,0.4);font-size:1.5rem;" +
            "    box-shadow:0 8px 24px rgba(30,58,138,.55);" +
            "    cursor:pointer;z-index:2147483000;display:flex;align-items:center;justify-content:center;" +
            "    transition:transform .2s;font-family:inherit;';" +
            "  var speaking = false; var timerId = null;" +
            "  btn.onclick = function(){" +
            "    if(speaking){" +
            "      speaking = false; btn.innerHTML='🔊';" +
            "      if(timerId) clearTimeout(timerId);" +
            "      try{ if(window.AndroidAPI && window.AndroidAPI.stopSpeak)" +
            "        window.AndroidAPI.stopSpeak(); }catch(e){}" +
            "      return;" +
            "    }" +
            "    var text='';" +
            "    var sel = document.getElementById('q-text') ||" +
            "              document.querySelector('.question-text') ||" +
            "              document.querySelector('.qtext');" +
            "    if(sel) text = (sel.textContent||sel.innerText||'').trim();" +
            "    if(!text){" +
            "      var h = document.querySelector('.main-title, .start-title, .card-title, h1, h2');" +
            "      if(h) text = (h.textContent||h.innerText||'').trim();" +
            "    }" +
            "    if(!text) return;" +
            "    speaking = true; btn.innerHTML='⏹';" +
            "    try{ if(window.AndroidAPI && window.AndroidAPI.speak)" +
            "      window.AndroidAPI.speak(text, 1); }catch(e){}" +
            "    timerId = setTimeout(function(){ speaking=false; btn.innerHTML='🔊'; }," +
            "      Math.min(90000, 1500 + text.length * 95));" +
            "  };" +
            "  document.body.appendChild(btn);" +
            "})();";
        view.evaluateJavascript(js, null);
    }

    @Override
    public void onInit(int status) {
        if (status != TextToSpeech.SUCCESS) return;
        int r = tts.setLanguage(new Locale("ar", "SA"));
        if (r == TextToSpeech.LANG_MISSING_DATA || r == TextToSpeech.LANG_NOT_SUPPORTED) {
            r = tts.setLanguage(new Locale("ar"));
        }
        if (r == TextToSpeech.LANG_MISSING_DATA || r == TextToSpeech.LANG_NOT_SUPPORTED) {
            Toast.makeText(this, "⚠️ يرجى تثبيت بيانات اللغة العربية لمحرك النطق",
                    Toast.LENGTH_LONG).show();
            r = tts.setLanguage(Locale.getDefault());
        }
        if (r != TextToSpeech.LANG_MISSING_DATA && r != TextToSpeech.LANG_NOT_SUPPORTED) {
            ttsReady = true;
            pickArabicFemaleVoice();
            tts.setSpeechRate(0.92f);
            tts.setPitch(1.35f);
        }
    }

    private void pickArabicFemaleVoice() {
        if (tts == null) return;
        try {
            Set<Voice> voices = tts.getVoices();
            if (voices == null || voices.isEmpty()) return;
            Voice best = null, fallback = null;
            for (Voice v : voices) {
                Locale loc = v.getLocale();
                if (loc == null || !"ar".equals(loc.getLanguage())) continue;
                String name = v.getName() == null ? "" : v.getName().toLowerCase(Locale.ROOT);
                boolean femaleHint = name.contains("female") || name.contains("-f-")
                        || name.contains("-f0") || name.contains("wavenet-f")
                        || name.contains("#female");
                if (femaleHint) {
                    best = v;
                    if (!v.isNetworkConnectionRequired()) break;
                }
                if (fallback == null && !v.isNetworkConnectionRequired()) fallback = v;
            }
            if (best == null) best = fallback;
            if (best != null) {
                arabicFemaleVoice = best;
                tts.setVoice(best);
            }
        } catch (Exception e) { Log.w(TAG, "voice pick failed", e); }
    }

    private void vibrateLight() {
        if (vibrator == null || !vibrator.hasVibrator()) return;
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(18,
                        VibrationEffect.DEFAULT_AMPLITUDE));
            } else vibrator.vibrate(18);
        } catch (Exception ignored) {}
    }

    private void vibrateHeavy() {
        if (vibrator == null || !vibrator.hasVibrator()) return;
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(45,
                        VibrationEffect.DEFAULT_AMPLITUDE));
            } else vibrator.vibrate(45);
        } catch (Exception ignored) {}
    }

    @Override
    protected void onDestroy() {
        if (tts != null) {
            try { tts.stop(); tts.shutdown(); } catch (Exception ignored) {}
        }
        super.onDestroy();
    }

    /** ⚠️ زر الرجوع الذكي */
    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
            return;
        }
        long now = System.currentTimeMillis();
        if (now - lastBackPressTime < 2200) super.onBackPressed();
        else {
            lastBackPressTime = now;
            Toast.makeText(this, "اضغط مرة أخرى للخروج", Toast.LENGTH_SHORT).show();
            vibrateLight();
        }
    }

    public class AndroidBridge {

        @JavascriptInterface
        public boolean nativeConfirm(final String msg) {
            final boolean[] result = { true };
            final CountDownLatch latch = new CountDownLatch(1);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        new AlertDialog.Builder(MainActivity.this)
                            .setTitle("تأكيد")
                            .setMessage(msg == null ? "" : msg)
                            .setPositiveButton("موافق", (d, w) -> { result[0]=true; latch.countDown(); })
                            .setNegativeButton("إلغاء", (d, w) -> { result[0]=false; latch.countDown(); })
                            .setCancelable(false)
                            .show();
                    } catch (Exception e) { result[0]=true; latch.countDown(); }
                }
            });
            try { if (!latch.await(120, TimeUnit.SECONDS)) result[0] = true; }
            catch (InterruptedException e) { result[0] = true; }
            return result[0];
        }

        @JavascriptInterface
        public void nativeAlert(final String msg) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        new AlertDialog.Builder(MainActivity.this)
                            .setTitle("تنبيه")
                            .setMessage(msg == null ? "" : msg)
                            .setPositiveButton("حسناً", null)
                            .show();
                    } catch (Exception ignored) {}
                }
            });
        }

        @JavascriptInterface
        public void print() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        PrintManager pm = (PrintManager) getSystemService(PRINT_SERVICE);
                        if (pm == null) return;
                        String job = "M.A - مستند";
                        try { job = getString(R.string.app_name); } catch (Exception ignored) {}
                        PrintDocumentAdapter adapter = webView.createPrintDocumentAdapter(job);
                        PrintAttributes attrs = new PrintAttributes.Builder()
                                .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                                .setResolution(new PrintAttributes.Resolution("pdf","pdf",600,600))
                                .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
                                .build();
                        pm.print(job, adapter, attrs);
                    } catch (Exception e) { Log.e(TAG, "print error", e); }
                }
            });
        }

        @JavascriptInterface
        public void speak(final String text, final int isFemale) {
            if (text == null || text.trim().isEmpty()) return;
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        if (!ttsReady || tts == null) return;
                        tts.stop();
                        if (isFemale == 1 && arabicFemaleVoice != null) tts.setVoice(arabicFemaleVoice);
                        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null,
                                "ma_" + System.currentTimeMillis());
                    } catch (Exception e) { Log.e(TAG, "speak error", e); }
                }
            });
        }

        @JavascriptInterface
        public void stopSpeak() {
            if (tts != null) { try { tts.stop(); } catch (Exception ignored) {} }
        }

        @JavascriptInterface
        public void hapticLight() {
            runOnUiThread(new Runnable() {
                @Override public void run() { vibrateLight(); }
            });
        }

        @JavascriptInterface
        public void hapticHeavy() {
            runOnUiThread(new Runnable() {
                @Override public void run() { vibrateHeavy(); }
            });
        }

        @JavascriptInterface
        public void shareText(final String title, final String text) {
            if (text == null || text.trim().isEmpty()) return;
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Intent share = new Intent(Intent.ACTION_SEND);
                        share.setType("text/plain");
                        share.putExtra(Intent.EXTRA_SUBJECT, title == null ? "" : title);
                        share.putExtra(Intent.EXTRA_TEXT, text);
                        startActivity(Intent.createChooser(share, "مشاركة عبر"));
                    } catch (Exception e) { Log.e(TAG, "shareText error", e); }
                }
            });
        }

        /** 📥 حفظ الشهادة كصورة */
        @JavascriptInterface
        public void saveImageToGallery(final String dataUrl, final String fileName) {
            if (dataUrl == null || !dataUrl.startsWith("data:image")) {
                Toast.makeText(MainActivity.this, "بيانات الصورة غير صحيحة",
                        Toast.LENGTH_SHORT).show();
                return;
            }
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        int commaIdx = dataUrl.indexOf(',');
                        if (commaIdx < 0) return;
                        String base64 = dataUrl.substring(commaIdx + 1);
                        byte[] bytes = android.util.Base64.decode(base64,
                                android.util.Base64.DEFAULT);
                        String name = (fileName == null || fileName.trim().isEmpty())
                                ? ("MA_" + System.currentTimeMillis())
                                : fileName.trim();
                        if (!name.toLowerCase().endsWith(".png")) name += ".png";

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            ContentValues values = new ContentValues();
                            values.put(MediaStore.Images.Media.DISPLAY_NAME, name);
                            values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
                            values.put(MediaStore.Images.Media.RELATIVE_PATH,
                                    Environment.DIRECTORY_PICTURES + "/M.A_Platform");
                            Uri uri = getContentResolver().insert(
                                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
                            if (uri != null) {
                                OutputStream os = getContentResolver().openOutputStream(uri);
                                if (os != null) { os.write(bytes); os.close(); }
                                Toast.makeText(MainActivity.this,
                                    "✅ تم حفظ الشهادة في المعرض", Toast.LENGTH_LONG).show();
                            }
                        } else {
                            File dir = new File(Environment.getExternalStoragePublicDirectory(
                                    Environment.DIRECTORY_PICTURES), "M.A_Platform");
                            if (!dir.exists()) dir.mkdirs();
                            File file = new File(dir, name);
                            FileOutputStream fos = new FileOutputStream(file);
                            fos.write(bytes); fos.close();
                            Intent scan = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                            scan.setData(Uri.fromFile(file));
                            sendBroadcast(scan);
                            Toast.makeText(MainActivity.this,
                                "✅ تم حفظ الشهادة في المعرض", Toast.LENGTH_LONG).show();
                        }
                        vibrateHeavy();
                    } catch (Exception e) {
                        Log.e(TAG, "saveImage error", e);
                        Toast.makeText(MainActivity.this, "تعذّر الحفظ",
                                Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }

        /** 📄 حفظ PDF من HTML */
        @JavascriptInterface
        public void savePdf(final String htmlContent, final String fileName) {
            if (htmlContent == null || htmlContent.trim().isEmpty()) return;
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                            android.print.PrintManager pm =
                                (android.print.PrintManager) getSystemService(PRINT_SERVICE);
                            if (pm == null) return;
                            // نحول إلى مستند نصي قابل للطباعة كـ PDF
                            WebView pdfWv = new WebView(MainActivity.this);
                            pdfWv.loadDataWithBaseURL(null, htmlContent,
                                    "text/HTML", "UTF-8", null);
                            final String jobName = (fileName == null || fileName.trim().isEmpty())
                                    ? "M.A_Document"
                                    : fileName.trim();
                            pdfWv.setWebViewClient(new WebViewClient() {
                                @Override
                                public void onPageFinished(WebView v, String url) {
                                    try {
                                        PrintDocumentAdapter adapter =
                                            v.createPrintDocumentAdapter(jobName);
                                        PrintAttributes attrs = new PrintAttributes.Builder()
                                            .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                                            .build();
                                        PrintManager pm2 =
                                            (PrintManager) getSystemService(PRINT_SERVICE);
                                        if (pm2 != null)
                                            pm2.print(jobName, adapter, attrs);
                                    } catch (Exception e) { Log.e(TAG, "pdf print", e); }
                                }
                            });
                        }
                    } catch (Exception e) { Log.e(TAG, "savePdf error", e); }
                }
            });
        }

        @JavascriptInterface
        public void setNightMode(final int mode) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        if (mode == 1) AppCompatDelegate.setDefaultNightMode(
                                AppCompatDelegate.MODE_NIGHT_YES);
                        else if (mode == 0) AppCompatDelegate.setDefaultNightMode(
                                AppCompatDelegate.MODE_NIGHT_NO);
                        else AppCompatDelegate.setDefaultNightMode(
                                AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                    } catch (Exception ignored) {}
                }
            });
        }

        @JavascriptInterface
        public int getNightMode() {
            try {
                int current = getResources().getConfiguration().uiMode
                        & Configuration.UI_MODE_NIGHT_MASK;
                return (current == Configuration.UI_MODE_NIGHT_YES) ? 1 : 0;
            } catch (Exception e) { return 0; }
        }

        /** 🔔 إشعار تجريبي */
        @JavascriptInterface
        public void showTestNotification() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try { NotificationHelper.showNow(MainActivity.this); }
                    catch (Exception ignored) {}
                }
            });
        }

        /** ⏰ تفعيل / إلغاء الإشعارات اليومية */
        @JavascriptInterface
        public void setDailyReminder(boolean enable) {
            try {
                if (enable) NotificationHelper.scheduleDailyReminder(MainActivity.this);
                else NotificationHelper.cancelDailyReminder(MainActivity.this);
            } catch (Exception ignored) {}
        }

        @JavascriptInterface
        public boolean isNative() { return true; }

        @JavascriptInterface
        public String getAppName() {
            try { return getString(R.string.app_name); }
            catch (Exception e) { return "M.A Platform"; }
        }

        @JavascriptInterface
        public String getDeviceInfo() {
            try {
                return "Android " + Build.VERSION.RELEASE
                        + " (SDK " + Build.VERSION.SDK_INT + ") / "
                        + Build.MANUFACTURER + " " + Build.MODEL;
            } catch (Exception e) { return "unknown"; }
        }
    }
}

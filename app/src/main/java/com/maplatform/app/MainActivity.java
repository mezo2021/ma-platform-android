package com.maplatform.app;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintManager;
import android.speech.tts.TextToSpeech;
import android.speech.tts.Voice;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

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

    @SuppressLint({"SetJavaScriptEnabled", "AddJavascriptInterface"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // إبقاء الشاشة مضاءة
        getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // منع لقطات الشاشة
        getWindow().setFlags(
            android.view.WindowManager.LayoutParams.FLAG_SECURE,
            android.view.WindowManager.LayoutParams.FLAG_SECURE);

        webView = new WebView(this);
        setContentView(webView);

        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

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
                injectFixes(view);
            }
        });

        webView.addJavascriptInterface(new AndroidBridge(), "AndroidAPI");

        tts = new TextToSpeech(this, this);

        webView.loadUrl("file:///android_asset/index.html");
    }

    private void injectFixes(final WebView view) {
        String js =
            "(function(){" +
            "  if(window.__ma_fixes) return;" +
            "  window.__ma_fixes = true;" +
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
            "})();";
        view.evaluateJavascript(js, null);

        view.postDelayed(new Runnable() {
            @Override
            public void run() { injectVoiceButton(view); }
        }, 500);
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
            "  btn.style.cssText = 'position:fixed;bottom:96px;left:16px;width:56px;height:56px;" +
            "    border-radius:50%;background:linear-gradient(135deg,#0284c7,#1e3a8a);color:#fff;" +
            "    border:2px solid rgba(255,255,255,0.4);font-size:1.5rem;" +
            "    box-shadow:0 8px 24px rgba(30,58,138,.55);" +
            "    cursor:pointer;z-index:2147483000;display:flex;align-items:center;justify-content:center;" +
            "    font-family:inherit;';" +
            "  var speaking = false;" +
            "  btn.onclick = function(){" +
            "    if(speaking){ speaking = false; btn.innerHTML='🔊';" +
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
            "    setTimeout(function(){ speaking=false; btn.innerHTML='🔊'; }," +
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
                        || name.contains("-f0") || name.contains("wavenet-f");
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

    @Override
    protected void onDestroy() {
        if (tts != null) {
            try { tts.stop(); tts.shutdown(); } catch (Exception ignored) {}
        }
        super.onDestroy();
    }

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
                            .setPositiveButton("موافق", (d, w) -> {
                                result[0]=true; latch.countDown();
                            })
                            .setNegativeButton("إلغاء", (d, w) -> {
                                result[0]=false; latch.countDown();
                            })
                            .setCancelable(false)
                            .show();
                    } catch (Exception e) {
                        result[0]=true; latch.countDown();
                    }
                }
            });
            try {
                if (!latch.await(120, TimeUnit.SECONDS)) result[0] = true;
            } catch (InterruptedException e) { result[0] = true; }
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
                        PrintDocumentAdapter adapter = webView.createPrintDocumentAdapter(job);
                        PrintAttributes attrs = new PrintAttributes.Builder()
                                .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
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
                        if (isFemale == 1 && arabicFemaleVoice != null)
                            tts.setVoice(arabicFemaleVoice);
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
                    } catch (Exception e) { Log.e(TAG, "share error", e); }
                }
            });
        }

        @JavascriptInterface
        public boolean isNative() { return true; }
    }
}

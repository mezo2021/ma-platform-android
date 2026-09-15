package com.maplatform.app;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintManager;
import android.speech.tts.TextToSpeech;
import android.speech.tts.Voice;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Locale;
import java.util.Set;

public class MainActivity extends AppCompatActivity implements TextToSpeech.OnInitListener {

    private WebView webView;
    private TextToSpeech tts;
    private boolean ttsReady = false;
    private Voice femaleVoice;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        webView = new WebView(this);
        setContentView(webView);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowFileAccessFromFileURLs(true);
        settings.setAllowUniversalAccessFromFileURLs(true);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        settings.setDefaultTextEncodingName("utf-8");

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
            }
        });

        // جسر Android API للطباعة والصوت والدوال الأخرى
        webView.addJavascriptInterface(new AndroidBridge(), "AndroidAPI");

        // تهيئة محرك النطق
        tts = new TextToSpeech(this, this);

        // تحميل الصفحة الرئيسية
        webView.loadUrl("file:///android_asset/index.html");
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            // اضبط اللغة على العربية (السعودية)
            int result = tts.setLanguage(new Locale("ar", "SA"));
            
            if (result != TextToSpeech.LANG_MISSING_DATA && 
                result != TextToSpeech.LANG_NOT_SUPPORTED) {
                ttsReady = true;
                
                // ابحث عن صوت أنثى
                Set<Voice> voices = tts.getVoices();
                if (voices != null) {
                    for (Voice voice : voices) {
                        // البحث عن صوت عربي أنثى
                        if (voice.getLocale().getLanguage().equals("ar") &&
                            !voice.isNetworkConnectionRequired() &&
                            voice.getName().toLowerCase().contains("female")) {
                            femaleVoice = voice;
                            tts.setVoice(femaleVoice);
                            break;
                        }
                    }
                }
                
                tts.setSpeechRate(0.9f);
                tts.setPitch(1.2f); // تعديل طفيف للنبرة
            }
        }
    }

    @Override
    protected void onDestroy() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    /**
     * جسر JavaScript ↔ Android
     * يسمح لصفحات HTML باستدعاء دوال أندرويد الأصلية
     */
    public class AndroidBridge {
        
        @JavascriptInterface
        public void print() {
            runOnUiThread(() -> {
                try {
                    PrintManager printManager = 
                        (PrintManager) getSystemService(PRINT_SERVICE);
                    if (printManager == null) {
                        showToast("خدمة الطباعة غير متاحة");
                        return;
                    }

                    String jobName = getString(R.string.app_name) + " - مستند";
                    PrintDocumentAdapter printAdapter = 
                        webView.createPrintDocumentAdapter(jobName);

                    printManager.print(
                        jobName,
                        printAdapter,
                        new PrintAttributes.Builder().build()
                    );
                } catch (Exception e) {
                    showToast("خطأ في الطباعة: " + e.getMessage());
                }
            });
        }

        @JavascriptInterface
        public void speak(String text, int isFemale) {
            if (text == null || text.trim().isEmpty()) return;
            
            runOnUiThread(() -> {
                try {
                    if (!ttsReady || tts == null) {
                        showToast("محرك النطق غير جاهز بعد");
                        return;
                    }
                    
                    tts.stop();
                    
                    // إذا طُلب صوت أنثى واستطعنا العثور على صوت أنثى، استخدمه
                    if (isFemale == 1 && femaleVoice != null) {
                        tts.setVoice(femaleVoice);
                    }
                    
                    tts.speak(text, TextToSpeech.QUEUE_FLUSH, null);
                } catch (Exception e) {
                    showToast("خطأ في القراءة الصوتية");
                }
            });
        }

        @JavascriptInterface
        public void stopSpeak() {
            if (tts != null) {
                tts.stop();
            }
        }

        @JavascriptInterface
        public boolean isNative() {
            return true; // للتحقق من أن التطبيق أصلي (ليس متصفح)
        }

        @JavascriptInterface
        public String getAppName() {
            return getString(R.string.app_name);
        }
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}


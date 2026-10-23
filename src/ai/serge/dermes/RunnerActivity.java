package ai.serge.dermes;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.JavascriptInterface;

/** Plunger Run (ex Neon Runner) — three.js runner shipped in assets/web; WebView renders it.
 *  Pure procedural scene (no image assets): index.html + game.js + lib/three r147. */
public final class RunnerActivity extends Activity {

    private WebView web;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        web = new WebView(this);
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);              // best score lives in localStorage
        s.setMediaPlaybackRequiresUserGesture(false); // game music autoplay; SFX unlock still gated in JS
        web.setBackgroundColor(UI.BACKGROUND);
        web.setWebViewClient(new WebViewClient());
        web.addJavascriptInterface(new Object() {
            @JavascriptInterface public void run()   { ActivityLog.onRun(RunnerActivity.this); }
            @JavascriptInterface public void swipe() { ActivityLog.onSwipe(RunnerActivity.this, 1); }
        }, "dlog");
        web.setSystemUiVisibility(View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        web.loadUrl("file:///android_asset/web/index.html");
        setContentView(web);
    }

    // pause/resume: WebView handles rAF throttling itself via window visibility — no public API needed.
}

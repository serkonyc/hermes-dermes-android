package ai.serge.dermes;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

/** Neon Runner v2 — HTML5 canvas game shipped in assets/web; WebView renders it.
 *  All game logic is JS: index.html + game.js + assets/*.png. */
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
        web.setBackgroundColor(UI.BACKGROUND);
        web.setWebViewClient(new WebViewClient());
        web.setSystemUiVisibility(View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        web.loadUrl("file:///android_asset/web/index.html");
        setContentView(web);
    }

    // pause/resume: WebView handles rAF throttling itself via window visibility — no public API needed.
}

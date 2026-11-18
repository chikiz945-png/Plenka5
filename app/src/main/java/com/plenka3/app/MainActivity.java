package com.plenka3.app;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.JavascriptInterface;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.webkit.WebViewAssetLoader;

import java.io.ByteArrayInputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class MainActivity extends Activity {
    private static final int CREATE_BACKUP = 41;
    private static final String ASSET_HOST = "https://appassets.androidplatform.net";
    private WebView webView;
    private String pendingBackup = "";

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(android.graphics.Color.rgb(20, 23, 27));
        getWindow().setNavigationBarColor(android.graphics.Color.rgb(20, 23, 27));
        webView = new WebView(this);
        setContentView(webView);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);
        settings.setSupportZoom(false);
        settings.setBuiltInZoomControls(false);
        settings.setDisplayZoomControls(false);
        webView.addJavascriptInterface(new PlenkaBridge(), "PlenkaAndroid");

        WebViewAssetLoader loader = new WebViewAssetLoader.Builder()
                .addPathHandler("/assets/", new WebViewAssetLoader.AssetsPathHandler(this))
                .build();
        webView.setWebViewClient(new WebViewClient() {
            @Override public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                return loader.shouldInterceptRequest(request.getUrl());
            }
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri uri = request.getUrl();
                return !ASSET_HOST.equals(uri.getScheme() + "://" + uri.getHost());
            }
        });
        webView.loadUrl(ASSET_HOST + "/assets/index.html#admin");
    }

    private class PlenkaBridge {
        @JavascriptInterface public void saveBackup(String json) {
            pendingBackup = "window.PLENKA_DATA=" + json + ";\n";
            runOnUiThread(() -> {
                Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("text/javascript");
                intent.putExtra(Intent.EXTRA_TITLE, "data.js");
                startActivityForResult(intent, CREATE_BACKUP);
            });
        }
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CREATE_BACKUP && resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            try (OutputStream out = getContentResolver().openOutputStream(uri)) {
                if (out == null) throw new IllegalStateException("Не удалось открыть файл");
                out.write(pendingBackup.getBytes(StandardCharsets.UTF_8));
                Toast.makeText(this, "Резервная копия сохранена", Toast.LENGTH_LONG).show();
            } catch (Exception e) {
                Toast.makeText(this, "Ошибка сохранения копии: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }
        pendingBackup = "";
    }

    @Override public void onBackPressed() {
        if (webView != null && webView.canGoBack()) webView.goBack();
        else super.onBackPressed();
    }

    @Override protected void onDestroy() {
        if (webView != null) { webView.removeJavascriptInterface("PlenkaAndroid"); webView.destroy(); }
        super.onDestroy();
    }
}

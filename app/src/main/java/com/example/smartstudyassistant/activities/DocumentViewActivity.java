// DocumentViewActivity.java
package com.example.smartstudyassistant.activities;

import android.os.Bundle;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import androidx.appcompat.app.AppCompatActivity;

import com.example.smartstudyassistant.R;

import java.io.File;

public class DocumentViewActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_document_view);

        WebView webView = findViewById(R.id.webView);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.setWebViewClient(new WebViewClient());

        String filePath = getIntent().getStringExtra("FILE_PATH");
        if (filePath != null) {
            File file = new File(filePath);
            if (file.exists()) {
                webView.loadUrl("file://" + filePath);
            }
        }
    }
}
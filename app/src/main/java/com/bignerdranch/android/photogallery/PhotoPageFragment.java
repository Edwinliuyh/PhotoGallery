package com.bignerdranch.android.photogallery;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.TextView;

public class PhotoPageFragment extends VisibleFragment {
    private String mUrl;
    private WebView mWebView;

    /**
     * 在onCreate，获得保存的fragment，和intent传入的url数据
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);

        mUrl = getActivity().getIntent().getData().toString();
    }

    /**
     * 在onCreateView，引入inflate布局，获得控件，配置WebView
     */
    @SuppressLint("SetJavaScriptEnabled") //Flickr网站需要启用JavaScript
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_photo_page, parent, false);

        final ProgressBar progressBar = (ProgressBar)v.findViewById(R.id.progressBar);
        progressBar.setMax(100); // WebChromeClient reports in range 0-100
        final TextView titleTextView = (TextView)v.findViewById(R.id.titleTextView);

        mWebView = (WebView)v.findViewById(R.id.webView);
        mWebView.getSettings().setJavaScriptEnabled(true); //Flickr网站需要启用JavaScript

        //不使用用户的默认浏览器
        mWebView.setWebViewClient(new WebViewClient() {
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return false; 
            }
        });
        //添加一个进度条
        mWebView.setWebChromeClient(new WebChromeClient() {
            public void onProgressChanged(WebView webView, int progress) {
                if (progress == 100) {
                    progressBar.setVisibility(View.INVISIBLE);
                } else {
                    progressBar.setVisibility(View.VISIBLE);
                    progressBar.setProgress(progress);
                }
            }

            //添加一个标题视图
            public void onReceivedTitle(WebView webView, String title) {
                titleTextView.setText(title);
            }
        });
        
        //webView打开URL
        mWebView.loadUrl(mUrl);

        return v;
    }
}

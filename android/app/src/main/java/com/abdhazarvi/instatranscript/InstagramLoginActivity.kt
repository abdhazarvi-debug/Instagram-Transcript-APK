package com.abdhazarvi.instatranscript

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class InstagramLoginActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        CookieManager.getInstance().setAcceptCookie(true)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(20, 20, 20, 12)
        }

        val title = TextView(this).apply {
            text = "Connect Instagram"
            textSize = 20f
            setTextColor(Color.BLACK)
        }

        val hint = TextView(this).apply {
            text = "Sign in here once. If this screen opened from Transcribe, the app will automatically continue your transcript when you tap Continue. Your session stays on this phone."
            textSize = 14f
            setTextColor(Color.DKGRAY)
            setPadding(0, 6, 0, 10)
        }

        val done = Button(this).apply {
            text = "Continue"
            setOnClickListener {
                val saved = InstagramCookieStore.saveFromWebView(this@InstagramLoginActivity)
                if (saved) {
                    setResult(RESULT_OK)
                    finish()
                } else {
                    hint.text =
                        "An authenticated Instagram session was not detected. Finish logging in, wait a moment, then tap Continue."
                }
            }
        }

        header.addView(title)
        header.addView(hint)
        header.addView(done)

        val webView = WebView(this).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.loadsImagesAutomatically = true
            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    CookieManager.getInstance().flush()
                    if (InstagramCookieStore.sessionDetectedInWebView()) {
                        hint.text =
                            "Instagram login detected. Tap Continue to save the session and return to your transcript."
                    }
                }
            }
            loadUrl("https://www.instagram.com/accounts/login/")
        }

        root.addView(header)
        root.addView(
            webView,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        setContentView(root)
    }
}

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
            text = "Instagram Login"
            textSize = 20f
            setTextColor(Color.BLACK)
        }

        val hint = TextView(this).apply {
            text = "Log in normally, then tap Done. Your session stays on this device and is never uploaded."
            textSize = 14f
            setTextColor(Color.DKGRAY)
            setPadding(0, 6, 0, 10)
        }

        val done = Button(this).apply {
            text = "Done"
            setOnClickListener {
                val saved = InstagramCookieStore.saveFromWebView(this@InstagramLoginActivity)
                if (saved) {
                    setResult(RESULT_OK)
                    finish()
                } else {
                    hint.text =
                        "Instagram session was not detected yet. Finish logging in, wait a moment, then tap Done."
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
            webViewClient = WebViewClient()
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

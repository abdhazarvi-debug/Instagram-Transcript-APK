package com.abdhazarvi.instatranscript

import android.app.Activity
import android.os.Bundle
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.graphics.Color
import android.view.Gravity

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
            text = "Log in normally, then tap Done. Your session stays on this device and is never sent to our server."
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
                    hint.text = "Instagram session was not detected yet. Finish logging in, wait a moment, then tap Done."
                }
            }
        }

        header.addView(title)
        header.addView(hint)
        header.addView(done)

        val webView = WebView(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0
            )
            webView.layoutParams = webView.layoutParams.apply {
                height = 0
            }
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.loadsImagesAutomatically = true
            settings.userAgentString =
                "Mozilla/5.0 (Linux; Android 16) AppleWebKit/537.36 " +
                    "(KHTML, like Gecko) Chrome/140.0.0.0 Mobile Safari/537.36"
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

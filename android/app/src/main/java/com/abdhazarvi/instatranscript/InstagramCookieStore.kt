package com.abdhazarvi.instatranscript

import android.content.Context
import android.webkit.CookieManager
import java.io.File

object InstagramCookieStore {
    private const val FILE_NAME = "instagram_cookies.txt"

    fun cookieFile(context: Context): File =
        File(context.filesDir, FILE_NAME)

    fun hasCookies(context: Context): Boolean =
        cookieFile(context).let { it.exists() && it.length() > 40L }

    fun saveFromWebView(context: Context): Boolean {
        val manager = CookieManager.getInstance()
        manager.flush()

        val cookieHeader = manager.getCookie("https://www.instagram.com/").orEmpty()
        if (cookieHeader.isBlank()) return false

        val lines = cookieHeader
            .split(';')
            .mapNotNull { raw ->
                val item = raw.trim()
                val eq = item.indexOf('=')
                if (eq <= 0) return@mapNotNull null
                val name = item.substring(0, eq).trim()
                val value = item.substring(eq + 1).trim()
                if (name.isBlank()) null else "$name\t$value"
            }

        if (lines.isEmpty()) return false

        val text = buildString {
            append("# Netscape HTTP Cookie File\n")
            append("# Stored locally by InstaTranscript. Never uploaded.\n")
            for (line in lines) {
                val tab = line.indexOf('\t')
                val name = line.substring(0, tab)
                val value = line.substring(tab + 1)
                append(".instagram.com\tTRUE\t/\tTRUE\t0\t")
                append(name)
                append('\t')
                append(value)
                append('\n')
            }
        }

        val target = cookieFile(context)
        val temp = File(target.parentFile, target.name + ".part")
        temp.writeText(text, Charsets.UTF_8)
        if (target.exists()) target.delete()
        check(temp.renameTo(target)) { "Could not save Instagram session." }
        return true
    }

    fun clear(context: Context) {
        cookieFile(context).delete()
    }
}

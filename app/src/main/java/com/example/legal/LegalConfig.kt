package com.example.legal

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.components.GlassButton
import com.example.ui.theme.DesignTokens
import com.example.ui.theme.LocalDyneTheme
import com.example.ui.theme.LocalDyneType

/**
 * Single source of truth for Dyne's legal identity, GitHub Pages Privacy Policy URL,
 * and offline bundled HTML document.
 */
object LegalConfig {
    const val DEVELOPER_NAME = "Sibora"
    const val CONTACT_EMAIL = "sibnathbairagi94@gmail.com"
    const val PRIVACY_POLICY_URL = "https://sibuuuu.github.io/dyne-app/"
    const val PRIVACY_POLICY_BASE_URL = "https://sibuuuu.github.io/dyne-app/"
    const val PRIVACY_POLICY_ASSET = "privacy_policy.html"
    const val TERMS_SUMMARY_URL = "https://sibuuuu.github.io/dyne-app/#overview"

    fun loadPrivacyPolicyHtml(context: Context): String {
        return runCatching {
            context.assets.open(PRIVACY_POLICY_ASSET).bufferedReader(Charsets.UTF_8).use { it.readText() }
        }.getOrElse {
            FALLBACK_PRIVACY_HTML
        }
    }

    fun openPrivacyPolicyInBrowser(context: Context): Boolean {
        return openExternalUri(context, Uri.parse(PRIVACY_POLICY_URL))
    }

    fun openExternalUri(context: Context, uri: Uri): Boolean {
        return runCatching {
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        }.getOrDefault(false)
    }

    private val FALLBACK_PRIVACY_HTML = """
        <!DOCTYPE html>
        <html lang="en">
        <head><meta charset="UTF-8"><title>Dyne Privacy Policy</title></head>
        <body style="font-family:sans-serif;padding:20px;line-height:1.6;">
          <h1>Dyne Privacy Policy</h1>
          <p><strong>Developer:</strong> Sibora &bull; <strong>Contact:</strong> sibnathbairagi94@gmail.com</p>
          <p>Dyne operates without user accounts or developer-owned cloud servers. Documents, PDFs, scans, notes, drawings, and preferences stay locally on your device. AdMob/UMP (Free tier), Google Play Billing, ML Kit, and optional Open-Meteo / Frankfurter requests are disclosed in full at $PRIVACY_POLICY_URL.</p>
        </body>
        </html>
    """.trimIndent()
}

/**
 * Full-screen glass dialog rendering the bundled offline HTML5 Privacy Policy safely inside
 * a locked-down WebView (JS disabled, no file/content access), plus a direct Open in Browser
 * action to launch the public GitHub Pages HTTPS URL.
 */
@Composable
fun PrivacyPolicyViewerDialog(
    onDismiss: () -> Unit,
    onBrowserLaunchError: ((String) -> Unit)? = null
) {
    val context = LocalContext.current
    val theme = LocalDyneTheme.current
    val type = LocalDyneType.current
    val policyHtml = remember(context) { LegalConfig.loadPrivacyPolicyHtml(context) }
    val borderColor = if (theme.isDark) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.10f)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(theme.background)
                .statusBarsPadding()
                .navigationBarsPadding()
                .testTag("privacy_policy_viewer_dialog")
        ) {
            // Top bar matching DesignTokens
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(DesignTokens.Space12),
                modifier = Modifier
                    .fillMaxWidth()
                    .background(theme.surface)
                    .border(1.dp, borderColor)
                    .padding(horizontal = DesignTokens.Space16, vertical = DesignTokens.Space12)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(theme.accent.copy(alpha = 0.16f))
                ) {
                    Icon(
                        imageVector = Icons.Default.PrivacyTip,
                        contentDescription = null,
                        tint = theme.accent,
                        modifier = Modifier.size(DesignTokens.IconSize)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Privacy Policy",
                        style = type.title.copy(
                            fontSize = DesignTokens.TitleTextSize,
                            fontWeight = DesignTokens.TitleWeight
                        ),
                        color = theme.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = LegalConfig.PRIVACY_POLICY_URL,
                        style = type.caption.copy(fontSize = DesignTokens.CaptionTextSize),
                        color = theme.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(DesignTokens.MinTouchTarget)
                        .testTag("privacy_viewer_close_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close Privacy Policy",
                        tint = theme.textPrimary,
                        modifier = Modifier.size(DesignTokens.IconSize)
                    )
                }
            }

            // Embedded safe static HTML renderer
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = DesignTokens.Space12, vertical = DesignTokens.Space12)
                    .clip(DesignTokens.CardShape)
                    .background(theme.surface)
                    .border(1.dp, borderColor, DesignTokens.CardShape)
            ) {
                AndroidView(
                    factory = { ctx ->
                        WebView(ctx).apply {
                            setBackgroundColor(android.graphics.Color.TRANSPARENT)
                            settings.apply {
                                javaScriptEnabled = false
                                allowFileAccess = false
                                allowContentAccess = false
                                builtInZoomControls = false
                                displayZoomControls = false
                                cacheMode = WebSettings.LOAD_NO_CACHE
                                loadWithOverviewMode = true
                                useWideViewPort = true
                            }
                            webViewClient = object : WebViewClient() {
                                override fun shouldOverrideUrlLoading(
                                    view: WebView?,
                                    request: WebResourceRequest?
                                ): Boolean {
                                    val url = request?.url ?: return false
                                    val scheme = url.scheme?.lowercase()
                                    // Keep in-page #section anchor jumps inside the document
                                    if (url.toString().startsWith(LegalConfig.PRIVACY_POLICY_BASE_URL) &&
                                        !url.fragment.isNullOrBlank()
                                    ) {
                                        return false
                                    }
                                    if (scheme == "https" || scheme == "http" || scheme == "mailto") {
                                        LegalConfig.openExternalUri(ctx, url)
                                    }
                                    return true
                                }
                            }
                            loadDataWithBaseURL(
                                LegalConfig.PRIVACY_POLICY_BASE_URL,
                                policyHtml,
                                "text/html",
                                "UTF-8",
                                null
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .fillMaxHeight()
                )
            }

            // Sticky action bar with 52dp buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(DesignTokens.Space12),
                modifier = Modifier
                    .fillMaxWidth()
                    .background(theme.surface)
                    .border(1.dp, borderColor)
                    .padding(horizontal = DesignTokens.ScreenMargin, vertical = DesignTokens.Space12)
            ) {
                GlassButton(
                    text = "Open in Browser",
                    icon = Icons.AutoMirrored.Filled.OpenInNew,
                    accentFill = theme.accent,
                    textColor = theme.onAccent,
                    onClick = {
                        val opened = LegalConfig.openPrivacyPolicyInBrowser(context)
                        if (!opened) {
                            onBrowserLaunchError?.invoke(
                                "No web browser found. URL: ${LegalConfig.PRIVACY_POLICY_URL}"
                            )
                        }
                    },
                    modifier = Modifier
                        .weight(1.4f)
                        .height(DesignTokens.ButtonHeight),
                    testTag = "privacy_viewer_open_browser_btn"
                )
                GlassButton(
                    text = "Done",
                    onClick = onDismiss,
                    modifier = Modifier
                        .weight(1f)
                        .height(DesignTokens.ButtonHeight),
                    testTag = "privacy_viewer_done_btn"
                )
            }
        }
    }
}

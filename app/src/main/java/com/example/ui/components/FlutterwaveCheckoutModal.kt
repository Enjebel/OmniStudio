package com.example.ui.components

import android.annotation.SuppressLint
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.repository.CheckoutSessionInit
import com.example.data.repository.PaymentGatewayRepository
import com.example.ui.theme.CyberGreen
import com.example.ui.theme.DangerRed
import com.example.ui.theme.OmniTheme
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.StudioSurfaceElevated
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import org.json.JSONObject
import java.util.Locale

private class OmniFlutterwaveBridge(
    private val onSuccess: (status: String, transactionId: String, txRef: String) -> Unit,
    private val onClosed: () -> Unit,
    private val onError: (String) -> Unit
) {
    private val mainHandler = Handler(Looper.getMainLooper())

    @JavascriptInterface
    fun onPaymentSuccess(status: String, transactionId: String, txRef: String) {
        mainHandler.post {
            onSuccess(status, transactionId, txRef)
        }
    }

    @JavascriptInterface
    fun onCheckoutClosed() {
        mainHandler.post {
            onClosed()
        }
    }

    @JavascriptInterface
    fun onCheckoutError(message: String) {
        mainHandler.post {
            onError(message)
        }
    }
}

private fun buildFlutterwaveCheckoutHtml(session: CheckoutSessionInit): String {
    val redirectUrl =
        "${PaymentGatewayRepository.CALLBACK_DEEP_LINK}?status=successful" +
            "&plan=${Uri.encode(session.plan.id)}" +
            "&cycle=${Uri.encode(session.billingCycle.id)}" +
            "&provider=FLUTTERWAVE" +
            "&tx_ref=${Uri.encode(session.sessionId)}"

    val configJson = JSONObject().apply {
        put("public_key", session.inlinePublicKey)
        put("tx_ref", session.sessionId)
        put("amount", session.amountUsd)
        put("currency", "USD")
        put("payment_options", "card, banktransfer, ussd, mobilemoney")
        put("redirect_url", redirectUrl)
        put("customer", JSONObject().apply {
            put("email", session.billingEmail)
            put("name", session.customerName.ifBlank { "OmniStudio Creator" })
        })
        put("meta", JSONObject().apply {
            put("plan_tier", session.plan.id)
            put("billing_cycle", session.billingCycle.id)
        })
        put("customizations", JSONObject().apply {
            put("title", "OmniStudio AI")
            put("description", "${session.plan.displayName} (${session.billingCycle.label})")
        })
    }.toString()

    return """
        <!DOCTYPE html>
        <html lang="en">
        <head>
          <meta charset="utf-8" />
          <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
          <title>Flutterwave Secure Checkout</title>
          <script src="https://checkout.flutterwave.com/v3.js" onerror="window.OmniBridge && window.OmniBridge.onCheckoutError('Could not load Flutterwave v3 checkout script. Check internet connectivity.')"></script>
          <style>
            body {
              margin: 0;
              padding: 24px 16px;
              font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
              background: #0B0F19;
              color: #F8FAFC;
              display: flex;
              flex-direction: column;
              align-items: center;
              justify-content: center;
              min-height: 85vh;
              text-align: center;
            }
            .card {
              background: #111827;
              border: 1px solid #1E293B;
              border-radius: 16px;
              padding: 20px;
              max-width: 360px;
              width: 100%;
              box-sizing: border-box;
            }
            .btn {
              margin-top: 16px;
              width: 100%;
              padding: 14px 16px;
              border: none;
              border-radius: 12px;
              background: #F59E0B;
              color: #0B0F19;
              font-weight: 700;
              font-size: 14px;
              cursor: pointer;
            }
            .muted {
              color: #94A3B8;
              font-size: 13px;
              line-height: 1.45;
            }
          </style>
        </head>
        <body>
          <div class="card">
            <h3 style="margin:0 0 8px 0;">Connecting to Flutterwave...</h3>
            <p class="muted">Opening encrypted payment modal for ${session.plan.displayName} (${'$'}${String.format(Locale.US, "%.2f", session.amountUsd)} USD).</p>
            <button class="btn" type="button" onclick="launchCheckout()">Open Payment Form</button>
          </div>
          <script>
            var baseConfig = $configJson;
            var paymentCompleted = false;
            function launchCheckout() {
              try {
                if (typeof FlutterwaveCheckout !== 'function') {
                  if (window.OmniBridge) {
                    window.OmniBridge.onCheckoutError('FlutterwaveCheckout SDK is unavailable. Verify your connection and FLUTTERWAVE_PUBLIC_KEY.');
                  }
                  return;
                }
                var payload = Object.assign({}, baseConfig, {
                  callback: function(data) {
                    paymentCompleted = true;
                    var st = (data && data.status) ? String(data.status) : 'successful';
                    var txId = (data && (data.transaction_id || data.id)) ? String(data.transaction_id || data.id) : '';
                    var ref = (data && data.tx_ref) ? String(data.tx_ref) : String(baseConfig.tx_ref || '');
                    if (window.OmniBridge) {
                      window.OmniBridge.onPaymentSuccess(st, txId, ref);
                    }
                  },
                  onclose: function(incomplete) {
                    if (!paymentCompleted && window.OmniBridge) {
                      window.OmniBridge.onCheckoutClosed();
                    }
                  }
                });
                FlutterwaveCheckout(payload);
              } catch (err) {
                if (window.OmniBridge) {
                  window.OmniBridge.onCheckoutError(err && err.message ? err.message : 'Unable to launch Flutterwave checkout.');
                }
              }
            }
            window.addEventListener('load', function() {
              setTimeout(launchCheckout, 150);
            });
          </script>
        </body>
        </html>
    """.trimIndent()
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun FlutterwaveCheckoutModal(
    session: CheckoutSessionInit,
    onPaymentCompleted: (status: String, transactionId: String, txRef: String) -> Unit,
    onDeepLinkIntercepted: (Uri) -> Unit,
    onDismiss: () -> Unit
) {
    val palette = OmniTheme.colors
    var isLoadingPage by remember(session.sessionId) { mutableStateOf(true) }
    var pageError by remember(session.sessionId) { mutableStateOf<String?>(null) }
    var reloadNonce by remember(session.sessionId) { mutableIntStateOf(0) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    val checkoutHtml = remember(session.sessionId, session.inlinePublicKey, session.amountUsd) {
        buildFlutterwaveCheckoutHtml(session)
    }

    DisposableEffect(Unit) {
        onDispose {
            webViewRef?.stopLoading()
            webViewRef?.removeJavascriptInterface("OmniBridge")
            webViewRef?.destroy()
            webViewRef = null
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnClickOutside = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .height(660.dp)
                .clip(RoundedCornerShape(22.dp))
                .border(1.dp, StudioBorder, RoundedCornerShape(22.dp))
                .testTag("flutterwave_inline_checkout_modal"),
            color = StudioSurface
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top Security Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(CyberGreen.copy(alpha = 0.14f))
                                .border(1.dp, CyberGreen.copy(alpha = 0.4f), RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Secured by Flutterwave",
                                tint = CyberGreen,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Flutterwave Secure Checkout",
                                color = TextPrimary,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${session.plan.displayName} • $${String.format(Locale.US, "%.2f", session.amountUsd)} USD (${session.billingCycle.label})",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("close_flutterwave_checkout_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close Flutterwave Checkout",
                            tint = TextPrimary
                        )
                    }
                }

                if (isLoadingPage && pageError == null) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        color = palette.accentCyan,
                        trackColor = StudioSurfaceElevated
                    )
                } else {
                    HorizontalDivider(color = StudioBorder)
                }

                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    if (pageError != null) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ErrorOutline,
                                contentDescription = null,
                                tint = DangerRed,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Checkout Could Not Load",
                                color = TextPrimary,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = pageError.orEmpty(),
                                color = TextSecondary,
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                OutlinedButton(
                                    onClick = onDismiss,
                                    modifier = Modifier.height(48.dp),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Cancel")
                                }
                                Button(
                                    onClick = {
                                        pageError = null
                                        isLoadingPage = true
                                        reloadNonce += 1
                                    },
                                    modifier = Modifier.height(48.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = palette.accentPurple)
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Retry", color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    } else {
                        AndroidView(
                            modifier = Modifier.fillMaxSize(),
                            factory = { ctx ->
                                WebView(ctx).apply {
                                    layoutParams = ViewGroup.LayoutParams(
                                        ViewGroup.LayoutParams.MATCH_PARENT,
                                        ViewGroup.LayoutParams.MATCH_PARENT
                                    )
                                    settings.javaScriptEnabled = true
                                    settings.domStorageEnabled = true
                                    settings.javaScriptCanOpenWindowsAutomatically = true
                                    settings.setSupportMultipleWindows(false)
                                    addJavascriptInterface(
                                        OmniFlutterwaveBridge(
                                            onSuccess = onPaymentCompleted,
                                            onClosed = onDismiss,
                                            onError = { err ->
                                                isLoadingPage = false
                                                pageError = err
                                            }
                                        ),
                                        "OmniBridge"
                                    )
                                    webChromeClient = object : WebChromeClient() {
                                        override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                            if (newProgress >= 95) {
                                                isLoadingPage = false
                                            }
                                        }
                                    }
                                    webViewClient = object : WebViewClient() {
                                        override fun shouldOverrideUrlLoading(
                                            view: WebView?,
                                            request: WebResourceRequest?
                                        ): Boolean {
                                            val uri = request?.url ?: return false
                                            if (uri.scheme == "omnistudio" && uri.host == "checkout") {
                                                onDeepLinkIntercepted(uri)
                                                return true
                                            }
                                            return false
                                        }

                                        override fun onPageFinished(view: WebView?, url: String?) {
                                            isLoadingPage = false
                                        }

                                        override fun onReceivedError(
                                            view: WebView?,
                                            request: WebResourceRequest?,
                                            error: WebResourceError?
                                        ) {
                                            if (request?.isForMainFrame == true) {
                                                isLoadingPage = false
                                                pageError = error?.description?.toString()
                                                    ?: "Network error loading Flutterwave checkout."
                                            }
                                        }
                                    }
                                    webViewRef = this
                                    tag = reloadNonce
                                    loadDataWithBaseURL(
                                        "https://omnistudio.app",
                                        checkoutHtml,
                                        "text/html",
                                        "UTF-8",
                                        null
                                    )
                                }
                            },
                            update = { view ->
                                if (view.tag != reloadNonce) {
                                    view.tag = reloadNonce
                                    view.loadDataWithBaseURL(
                                        "https://omnistudio.app",
                                        checkoutHtml,
                                        "text/html",
                                        "UTF-8",
                                        null
                                    )
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

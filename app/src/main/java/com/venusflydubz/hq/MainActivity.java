package com.venusflydubz.hq;

import android.app.Activity;
import android.app.DownloadManager;
import android.content.ActivityNotFoundException;
import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Message;
import android.provider.MediaStore;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.DownloadListener;
import android.webkit.URLUtil;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import java.util.ArrayList;

/**
 * Venus Fly HQ: the private dashboard (venusflydubz.com/dashboard) as a full-screen Android app.
 * Everything runs on the live dashboard, so the app always has the newest features and data.
 */
public class MainActivity extends Activity {
    static final String HOME = "https://venusflydubz.com/dashboard";
    static final int FILE_REQ = 41;

    WebView web;
    ValueCallback<Uri[]> pendingFiles;
    Uri cameraUri;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(Color.BLACK);
        getWindow().setNavigationBarColor(Color.BLACK);

        web = new WebView(this);
        web.setBackgroundColor(Color.BLACK);
        web.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        setContentView(web);

        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setAllowFileAccess(false);
        s.setAllowContentAccess(true);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setSupportMultipleWindows(false);
        s.setJavaScriptCanOpenWindowsAutomatically(false);
        s.setUserAgentString(s.getUserAgentString() + " VFHQApp/1.0");

        CookieManager cm = CookieManager.getInstance();
        cm.setAcceptCookie(true);
        cm.setAcceptThirdPartyCookies(web, true); // Instagram profile embed

        web.setWebViewClient(new Client());
        web.setWebChromeClient(new Chrome());
        web.setDownloadListener(new DownloadListener() {
            @Override
            public void onDownloadStart(String url, String userAgent, String disposition, String mime, long length) {
                download(url, userAgent, disposition, mime);
            }
        });

        if (state != null) web.restoreState(state);
        else web.loadUrl(startUrl(getIntent()));
    }

    static String startUrl(Intent i) {
        Uri d = i == null ? null : i.getData();
        if (d != null && isDashboard(d)) return d.toString();
        return HOME;
    }

    static boolean isDashboard(Uri u) {
        String h = u.getHost(), p = u.getPath();
        return "https".equals(u.getScheme()) && h != null
                && (h.equals("venusflydubz.com") || h.equals("www.venusflydubz.com"))
                && p != null && (p.equals("/dashboard") || p.startsWith("/dashboard/"));
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (intent.getData() != null) web.loadUrl(startUrl(intent));
    }

    @Override
    protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        web.saveState(out);
    }

    @Override
    protected void onPause() {
        super.onPause();
        CookieManager.getInstance().flush();
        web.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        web.onResume();
    }

    @Override
    public void onBackPressed() {
        if (web.canGoBack()) web.goBack();
        else super.onBackPressed();
    }

    void openOutside(Uri u) {
        try {
            Intent i = "mailto".equals(u.getScheme()) ? new Intent(Intent.ACTION_SENDTO, u) : new Intent(Intent.ACTION_VIEW, u);
            i.addCategory(Intent.CATEGORY_BROWSABLE);
            startActivity(i);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "No app can open that link.", Toast.LENGTH_SHORT).show();
        }
    }

    // Documents and zip exports: save to Downloads with the login cookie attached.
    void download(String url, String ua, String disposition, String mime) {
        try {
            String name = URLUtil.guessFileName(url, disposition, mime);
            DownloadManager.Request r = new DownloadManager.Request(Uri.parse(url));
            String cookie = CookieManager.getInstance().getCookie(url);
            if (cookie != null) r.addRequestHeader("Cookie", cookie);
            r.addRequestHeader("User-Agent", ua == null ? web.getSettings().getUserAgentString() : ua);
            if (mime != null) r.setMimeType(mime);
            r.setTitle(name);
            r.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            r.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "Venus Fly HQ/" + name);
            ((DownloadManager) getSystemService(DOWNLOAD_SERVICE)).enqueue(r);
            Toast.makeText(this, "Downloading " + name, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Download failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    class Client extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest req) {
            Uri u = req.getUrl();
            if (!req.isForMainFrame()) return false;
            if (isDashboard(u)) {
                String p = u.getPath();
                // Stored documents open with the phone's own viewer (PDFs can't show inside the app).
                if (p.startsWith("/dashboard/api/docs/") && p.endsWith("/file")) {
                    download(u.toString(), null, null, null);
                    return true;
                }
                return false;
            }
            openOutside(u);
            return true;
        }

        @Override
        public void onReceivedError(WebView view, WebResourceRequest req, WebResourceError err) {
            if (!req.isForMainFrame()) return;
            String retry = req.getUrl().toString().replace("\"", "%22");
            view.loadDataWithBaseURL(null,
                    "<!doctype html><html><head><meta name=viewport content='width=device-width,initial-scale=1'>"
                            + "<style>html,body{margin:0;height:100%;background:#000;color:#f4eeee;font:15px/1.5 sans-serif}"
                            + "body{display:grid;place-items:center;text-align:center;padding:24px}h1{font-size:18px;letter-spacing:.08em}"
                            + "h1 span{color:#e10600}p{color:#9a8f8f}a{display:inline-block;margin-top:14px;background:#e10600;color:#fff;"
                            + "padding:12px 22px;font-weight:700;letter-spacing:.1em;text-decoration:none}</style></head><body><div>"
                            + "<h1>VENUS FLY <span>HQ</span></h1><p>Can't reach the dashboard. Check your connection.</p>"
                            + "<a href=\"" + retry + "\">TRY AGAIN</a></div></body></html>",
                    "text/html", "utf-8", null);
        }
    }

    class Chrome extends WebChromeClient {
        @Override
        public boolean onCreateWindow(WebView view, boolean dialog, boolean userGesture, Message msg) {
            return false;
        }

        // Upload buttons: offer the camera as well as files/gallery.
        @Override
        public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> cb, FileChooserParams params) {
            if (pendingFiles != null) pendingFiles.onReceiveValue(null);
            pendingFiles = cb;
            cameraUri = null;

            Intent pick = new Intent(Intent.ACTION_GET_CONTENT);
            pick.addCategory(Intent.CATEGORY_OPENABLE);
            String[] types = params.getAcceptTypes();
            String accept = types == null ? "" : android.text.TextUtils.join(",", types).trim();
            boolean imagesOnly = accept.startsWith("image/") && !accept.contains(",");
            boolean onlyData = accept.contains(".csv") || accept.contains(".xls");
            pick.setType(imagesOnly ? "image/*" : "*/*");
            if (onlyData) pick.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"text/csv", "text/comma-separated-values", "application/vnd.ms-excel",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "application/octet-stream"});
            if (params.getMode() == FileChooserParams.MODE_OPEN_MULTIPLE) pick.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);

            ArrayList<Intent> extra = new ArrayList<Intent>();
            if (!onlyData) {
                try {
                    ContentValues v = new ContentValues();
                    v.put(MediaStore.Images.Media.DISPLAY_NAME, "VF_" + System.currentTimeMillis() + ".jpg");
                    v.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
                    v.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Venus Fly HQ");
                    cameraUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, v);
                    if (cameraUri != null) {
                        Intent cam = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                        cam.putExtra(MediaStore.EXTRA_OUTPUT, cameraUri);
                        cam.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        extra.add(cam);
                    }
                } catch (Exception ignored) {
                    cameraUri = null;
                }
            }
            Intent chooser = Intent.createChooser(pick, imagesOnly ? "Add photos" : "Add files");
            if (!extra.isEmpty()) chooser.putExtra(Intent.EXTRA_INITIAL_INTENTS, extra.toArray(new Intent[0]));
            try {
                startActivityForResult(chooser, FILE_REQ);
            } catch (ActivityNotFoundException e) {
                pendingFiles = null;
                cleanupCamera();
                Toast.makeText(MainActivity.this, "No file picker found.", Toast.LENGTH_SHORT).show();
                return false;
            }
            return true;
        }
    }

    void cleanupCamera() {
        if (cameraUri != null) {
            try { getContentResolver().delete(cameraUri, null, null); } catch (Exception ignored) { }
            cameraUri = null;
        }
    }

    @Override
    protected void onActivityResult(int req, int code, Intent data) {
        super.onActivityResult(req, code, data);
        if (req != FILE_REQ || pendingFiles == null) return;
        Uri[] result = null;
        if (code == RESULT_OK) {
            if (data != null && data.getClipData() != null) {
                int n = data.getClipData().getItemCount();
                result = new Uri[n];
                for (int i = 0; i < n; i++) result[i] = data.getClipData().getItemAt(i).getUri();
            } else if (data != null && data.getData() != null) {
                result = new Uri[]{data.getData()};
            } else if (cameraUri != null) {
                result = new Uri[]{cameraUri};
                cameraUri = null; // keep the photo
            }
        }
        if (result != null && cameraUri != null) cleanupCamera(); // picked a file instead of taking a photo
        if (result == null) cleanupCamera();
        pendingFiles.onReceiveValue(result);
        pendingFiles = null;
    }
}

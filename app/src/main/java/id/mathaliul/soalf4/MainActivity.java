package id.mathaliul.soalf4;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintManager;
import android.util.Base64;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Pembungkus WebView untuk Generator Soal F4.
 * - memuat assets/index.html (satu berkas, offline penuh)
 * - AndroidBridge.saveBase64  : simpan DOCX / JSON lewat pemilih "Simpan sebagai" (tanpa izin penyimpanan)
 * - AndroidBridge.printPage   : cetak halaman ke PDF/printer memakai ukuran kertas dokumen (F4 dst.)
 * - onShowFileChooser         : <input type=file> untuk logo, font, dan proyek .json
 */
public class MainActivity extends Activity {

    private static final int REQ_FILE = 101;
    private static final int REQ_SAVE = 102;

    private WebView web;
    private ValueCallback<Uri[]> filePathCallback;
    private byte[] pendingBytes;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        web = new WebView(this);
        setContentView(web);

        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);          // localStorage + IndexedDB (simpan otomatis & font)
        s.setAllowFileAccess(true);
        s.setAllowContentAccess(true);
        s.setTextZoom(100);                    // abaikan ukuran font sistem agar tata letak F4 tidak bergeser
        s.setBuiltInZoomControls(false);
        s.setSupportZoom(false);

        web.addJavascriptInterface(new Bridge(), "AndroidBridge");

        web.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (url != null && (url.startsWith("http://") || url.startsWith("https://"))) {
                    try {
                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
                    } catch (ActivityNotFoundException ignored) { }
                    return true;
                }
                return false;
            }
        });

        web.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback, FileChooserParams params) {
                if (filePathCallback != null) filePathCallback.onReceiveValue(null);
                filePathCallback = callback;

                String type = "*/*";
                String[] accept = params != null ? params.getAcceptTypes() : null;
                if (accept != null && accept.length > 0 && accept[0] != null && accept[0].startsWith("image/")) {
                    type = "image/*";
                }
                Intent i = new Intent(Intent.ACTION_GET_CONTENT);
                i.addCategory(Intent.CATEGORY_OPENABLE);
                i.setType(type);
                try {
                    startActivityForResult(Intent.createChooser(i, "Pilih berkas"), REQ_FILE);
                } catch (ActivityNotFoundException e) {
                    filePathCallback.onReceiveValue(null);
                    filePathCallback = null;
                    return false;
                }
                return true;
            }
        });

        if (savedInstanceState != null) {
            web.restoreState(savedInstanceState);
        } else {
            web.loadUrl("file:///android_asset/index.html");
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (web != null) web.saveState(outState);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (web != null) web.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (web != null) web.onResume();
    }

    @Override
    protected void onDestroy() {
        if (web != null) {
            web.removeJavascriptInterface("AndroidBridge");
            web.destroy();
            web = null;
        }
        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQ_FILE) {
            if (filePathCallback != null) {
                Uri[] result = null;
                if (resultCode == RESULT_OK && data != null && data.getData() != null) {
                    result = new Uri[]{data.getData()};
                }
                filePathCallback.onReceiveValue(result);
                filePathCallback = null;
            }
            return;
        }

        if (requestCode == REQ_SAVE) {
            byte[] bytes = pendingBytes;
            pendingBytes = null;
            if (resultCode != RESULT_OK || data == null || data.getData() == null || bytes == null) {
                toast("Penyimpanan dibatalkan");
                return;
            }
            OutputStream os = null;
            try {
                os = getContentResolver().openOutputStream(data.getData());
                if (os == null) throw new IOException("Tidak bisa membuka berkas tujuan");
                os.write(bytes);
                os.flush();
                toast("Tersimpan");
            } catch (IOException e) {
                toast("Gagal menyimpan: " + e.getMessage());
            } finally {
                if (os != null) {
                    try { os.close(); } catch (IOException ignored) { }
                }
            }
        }
    }

    @Override
    public void onBackPressed() {
        // Data sudah tersimpan otomatis di perangkat; tombol kembali menutup aplikasi.
        super.onBackPressed();
    }

    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    /** Dipanggil dari JavaScript (thread terpisah) — semua urusan UI dialihkan ke thread utama. */
    private class Bridge {

        @JavascriptInterface
        public void saveBase64(final String base64, final String fileName, final String mime) {
            final byte[] bytes;
            try {
                bytes = Base64.decode(base64, Base64.DEFAULT);
            } catch (IllegalArgumentException e) {
                runOnUiThread(new Runnable() {
                    @Override public void run() { toast("Data berkas tidak valid"); }
                });
                return;
            }
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    pendingBytes = bytes;
                    Intent i = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                    i.addCategory(Intent.CATEGORY_OPENABLE);
                    i.setType((mime == null || mime.length() == 0) ? "application/octet-stream" : mime);
                    i.putExtra(Intent.EXTRA_TITLE, fileName);
                    try {
                        startActivityForResult(i, REQ_SAVE);
                    } catch (ActivityNotFoundException e) {
                        pendingBytes = null;
                        toast("Tidak ada aplikasi penyimpan berkas di perangkat ini");
                    }
                }
            });
        }

        @JavascriptInterface
        public void printPage(final double widthMm, final double heightMm) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (web == null) return;
                    PrintManager pm = (PrintManager) getSystemService(PRINT_SERVICE);
                    if (pm == null) { toast("Cetak tidak tersedia"); return; }
                    // 1 mm = 39.3701 mil (1/1000 inci)
                    int wMil = (int) Math.round(widthMm * 39.3701);
                    int hMil = (int) Math.round(heightMm * 39.3701);
                    PrintAttributes.MediaSize size = new PrintAttributes.MediaSize("custom", "Kertas soal", wMil, hMil);
                    PrintAttributes attrs = new PrintAttributes.Builder()
                            .setMediaSize(size)
                            .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
                            .build();
                    PrintDocumentAdapter adapter = web.createPrintDocumentAdapter("Soal Ujian");
                    pm.print("Soal Ujian", adapter, attrs);
                }
            });
        }
    }
}

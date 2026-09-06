package cn.pan2411.uploader;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.widget.*;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class MainActivity extends Activity {
    private static final String API = "https://pan.2411092.xyz/api.php";
    private static final String BG_URL = "https://tc.ll521.cn/api/upload/2609062319191510.jpg";
    private static final int PICK_FILE = 1001;

    private Uri selectedUri;
    private TextView fileText, statusText, downloadText, viewText;
    private EditText passwordInput;
    private Switch passwordSwitch;
    private ProgressBar progressBar;
    private Button uploadButton;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Window w = getWindow();
        w.setStatusBarColor(Color.TRANSPARENT);
        w.setNavigationBarColor(Color.rgb(12, 12, 16));
        setContentView(buildUi());
    }

    private View buildUi() {
        FrameLayout root = new FrameLayout(this);
        root.setBackground(makeGradient(new int[]{0xFF16131B, 0xFF24151E, 0xFF101014}, 0));

        ImageView bg = new ImageView(this);
        bg.setScaleType(ImageView.ScaleType.CENTER_CROP);
        root.addView(bg, new FrameLayout.LayoutParams(-1, -1));
        loadBackground(bg);

        View dim = new View(this);
        dim.setBackgroundColor(Color.argb(135, 0, 0, 0));
        root.addView(dim, new FrameLayout.LayoutParams(-1, -1));

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setClipToPadding(false);
        scroll.setPadding(dp(18), dp(48), dp(18), dp(28));

        LinearLayout column = new LinearLayout(this);
        column.setOrientation(LinearLayout.VERTICAL);
        column.setGravity(Gravity.CENTER_HORIZONTAL);

        TextView title = text("2411盘 · 外链网盘", 30, Color.WHITE, true);
        title.setGravity(Gravity.CENTER);
        column.addView(title, lp(-1, -2, 0, 0, 0, 6));

        TextView sub = text("永久不跑路 · 稳定运行多年\n2411092.xyz · QQ频道 637168564 · TG LLNP6", 13, 0xFFE8E8EA, false);
        sub.setGravity(Gravity.CENTER);
        sub.setLineSpacing(0, 1.2f);
        column.addView(sub, lp(-1, -2, 0, 0, 0, 20));

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(18), dp(18), dp(18), dp(18));
        card.setBackground(makeRounded(0xD51A1A22, 24, 0x55FFFFFF, 1));

        card.addView(text("上传文件", 18, Color.WHITE, true));
        TextView desc = text("选择本地文件，可选设置下载密码", 12, 0xFFBFC0C7, false);
        card.addView(desc, lp(-1, -2, 0, 3, 0, 10));

        Button choose = button("选择文件");
        choose.setOnClickListener(v -> pickFile());
        card.addView(choose, lp(-1, dp(52), 0, 0, 0, 10));

        fileText = text("尚未选择文件", 13, 0xFFD0D0D5, false);
        fileText.setPadding(dp(6), 0, dp(6), dp(8));
        card.addView(fileText);

        passwordSwitch = new Switch(this);
        passwordSwitch.setText("设置下载密码");
        passwordSwitch.setTextColor(Color.WHITE);
        passwordSwitch.setTextSize(15);
        passwordSwitch.setPadding(0, dp(7), 0, dp(7));
        card.addView(passwordSwitch);

        passwordInput = new EditText(this);
        passwordInput.setHint("输入下载密码");
        passwordInput.setHintTextColor(0xFF92929A);
        passwordInput.setTextColor(Color.WHITE);
        passwordInput.setSingleLine(true);
        passwordInput.setTextSize(15);
        passwordInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        passwordInput.setBackground(makeRounded(0x99101016, 14, 0x40FFFFFF, 1));
        passwordInput.setPadding(dp(14), 0, dp(14), 0);
        passwordInput.setVisibility(View.GONE);
        card.addView(passwordInput, lp(-1, dp(52), 0, 7, 0, 7));
        passwordSwitch.setOnCheckedChangeListener((b, checked) -> passwordInput.setVisibility(checked ? View.VISIBLE : View.GONE));

        uploadButton = button("开始上传");
        uploadButton.setTypeface(Typeface.DEFAULT_BOLD);
        uploadButton.setOnClickListener(v -> upload());
        card.addView(uploadButton, lp(-1, dp(56), 0, 14, 0, 10));

        progressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progressBar.setMax(100);
        progressBar.setVisibility(View.GONE);
        card.addView(progressBar, lp(-1, dp(8), 0, 2, 0, 8));

        statusText = text("等待上传", 13, 0xFFCBCBD0, false);
        statusText.setGravity(Gravity.CENTER);
        card.addView(statusText, lp(-1, -2, 0, 4, 0, 4));

        downloadText = resultText();
        viewText = resultText();
        card.addView(downloadText, lp(-1, -2, 0, 10, 0, 0));
        card.addView(viewText, lp(-1, -2, 0, 8, 0, 0));

        column.addView(card, lp(-1, -2, 0, 0, 0, 18));

        TextView tip = text("点击结果链接可打开，长按可复制", 12, 0xFFD0D0D4, false);
        tip.setGravity(Gravity.CENTER);
        column.addView(tip);

        scroll.addView(column, new ScrollView.LayoutParams(-1, -2));
        root.addView(scroll, new FrameLayout.LayoutParams(-1, -1));
        return root;
    }

    private void loadBackground(ImageView imageView) {
        new Thread(() -> {
            HttpURLConnection c = null;
            try {
                c = (HttpURLConnection) new URL(BG_URL).openConnection();
                c.setConnectTimeout(10000);
                c.setReadTimeout(15000);
                c.setDoInput(true);
                c.connect();
                try (InputStream in = c.getInputStream()) {
                    Bitmap bmp = BitmapFactory.decodeStream(in);
                    if (bmp != null) runOnUiThread(() -> imageView.setImageBitmap(bmp));
                }
            } catch (Exception ignored) {
            } finally {
                if (c != null) c.disconnect();
            }
        }).start();
    }

    private Button button(String s) {
        Button b = new Button(this);
        b.setText(s);
        b.setTextColor(Color.WHITE);
        b.setTextSize(15);
        b.setAllCaps(false);
        b.setBackground(makeGradient(new int[]{0xFFFF6A9B, 0xFFE13F72}, 18));
        return b;
    }

    private TextView resultText() {
        TextView t = text("", 13, 0xFFFFB0C9, false);
        t.setVisibility(View.GONE);
        t.setPadding(dp(12), dp(10), dp(12), dp(10));
        t.setBackground(makeRounded(0x99101016, 14, 0x35FFFFFF, 1));
        return t;
    }

    private void pickFile() {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("*/*");
        startActivityForResult(i, PICK_FILE);
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_FILE && resultCode == RESULT_OK && data != null) {
            selectedUri = data.getData();
            if (selectedUri != null) {
                try { getContentResolver().takePersistableUriPermission(selectedUri, Intent.FLAG_GRANT_READ_URI_PERMISSION); } catch (Exception ignored) {}
                fileText.setText("已选择：" + getFileName(selectedUri));
            }
        }
    }

    private void upload() {
        if (selectedUri == null) { toast("请先选择文件"); return; }
        String pass = passwordSwitch.isChecked() ? passwordInput.getText().toString().trim() : "";
        if (passwordSwitch.isChecked() && pass.isEmpty()) { toast("请输入下载密码"); return; }

        uploadButton.setEnabled(false);
        progressBar.setVisibility(View.VISIBLE);
        progressBar.setProgress(0);
        statusText.setText("正在上传…");
        downloadText.setVisibility(View.GONE);
        viewText.setVisibility(View.GONE);

        new Thread(() -> {
            try {
                String response = multipartUpload(selectedUri, pass);
                JSONObject obj = new JSONObject(response);
                int code = obj.optInt("code", -999);
                if (code == 0) {
                    String down = cleanUrl(obj.optString("Downurl", obj.optString("downurl", "")));
                    String view = cleanUrl(obj.optString("viewurl", obj.optString("Viewurl", "")));
                    runOnUiThread(() -> showSuccess(down, view));
                } else {
                    String msg = obj.optString("msg", response);
                    runOnUiThread(() -> showError("上传失败：" + msg));
                }
            } catch (Exception e) {
                runOnUiThread(() -> showError("请求失败：" + e.getMessage()));
            }
        }).start();
    }

    private String multipartUpload(Uri uri, String pass) throws Exception {
        String boundary = "----Pan2411" + System.currentTimeMillis();
        HttpURLConnection conn = (HttpURLConnection) new URL(API).openConnection();
        conn.setConnectTimeout(20000);
        conn.setReadTimeout(120000);
        conn.setDoOutput(true);
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

        String fileName = getFileName(uri);
        long total = getFileSize(uri);
        try (DataOutputStream out = new DataOutputStream(conn.getOutputStream());
             InputStream in = getContentResolver().openInputStream(uri)) {
            // 按你原 Shell 的逻辑推断密码字段名为 pass；如接口实际为 pwd/password，只改这里即可。
            if (!pass.isEmpty()) writeField(out, boundary, "pass", pass);

            out.writeBytes("--" + boundary + "\r\n");
            out.writeBytes("Content-Disposition: form-data; name=\"file\"; filename=\"" + safeFileName(fileName) + "\"\r\n");
            out.writeBytes("Content-Type: application/octet-stream\r\n\r\n");

            byte[] buf = new byte[64 * 1024];
            long sent = 0;
            int n;
            while (in != null && (n = in.read(buf)) != -1) {
                out.write(buf, 0, n);
                sent += n;
                if (total > 0) {
                    int p = (int)Math.min(99, sent * 100 / total);
                    runOnUiThread(() -> progressBar.setProgress(p));
                }
            }
            out.writeBytes("\r\n--" + boundary + "--\r\n");
            out.flush();
        }

        int http = conn.getResponseCode();
        InputStream rs = http >= 200 && http < 400 ? conn.getInputStream() : conn.getErrorStream();
        String body = readAll(rs);
        conn.disconnect();
        if (body.trim().isEmpty()) throw new IOException("服务器返回空内容，HTTP " + http);
        return body;
    }

    private void writeField(DataOutputStream out, String boundary, String name, String value) throws IOException {
        out.writeBytes("--" + boundary + "\r\n");
        out.writeBytes("Content-Disposition: form-data; name=\"" + name + "\"\r\n\r\n");
        out.write(value.getBytes(StandardCharsets.UTF_8));
        out.writeBytes("\r\n");
    }

    private String readAll(InputStream in) throws IOException {
        if (in == null) return "";
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] b = new byte[8192];
        int n;
        while ((n = in.read(b)) != -1) bos.write(b, 0, n);
        return bos.toString("UTF-8");
    }

    private void showSuccess(String down, String view) {
        uploadButton.setEnabled(true);
        progressBar.setProgress(100);
        statusText.setText("✓ 上传成功");
        if (!down.isEmpty()) bindLink(downloadText, "下载链接：", down);
        if (!view.isEmpty()) bindLink(viewText, "查看链接：", view);
    }

    private void bindLink(TextView tv, String prefix, String url) {
        tv.setVisibility(View.VISIBLE);
        tv.setText(prefix + url);
        tv.setOnClickListener(v -> {
            try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url))); }
            catch (Exception e) { copyText(url); }
        });
        tv.setOnLongClickListener(v -> { copyText(url); return true; });
    }

    private void showError(String msg) {
        uploadButton.setEnabled(true);
        progressBar.setVisibility(View.GONE);
        statusText.setText(msg);
    }

    private String getFileName(Uri uri) {
        String result = "upload.bin";
        Cursor c = getContentResolver().query(uri, null, null, null, null);
        if (c != null) {
            try {
                int idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (idx >= 0 && c.moveToFirst()) result = c.getString(idx);
            } finally { c.close(); }
        }
        return result == null ? "upload.bin" : result;
    }

    private long getFileSize(Uri uri) {
        Cursor c = getContentResolver().query(uri, null, null, null, null);
        if (c != null) {
            try {
                int idx = c.getColumnIndex(OpenableColumns.SIZE);
                if (idx >= 0 && c.moveToFirst() && !c.isNull(idx)) return c.getLong(idx);
            } finally { c.close(); }
        }
        return -1;
    }

    private String safeFileName(String s) { return s.replace("\"", "_").replace("\r", "_").replace("\n", "_"); }
    private String cleanUrl(String s) { return s == null ? "" : s.replace("\\/", "/"); }

    private void copyText(String text) {
        ClipboardManager cm = (ClipboardManager)getSystemService(Context.CLIPBOARD_SERVICE);
        cm.setPrimaryClip(ClipData.newPlainText("2411盘链接", text));
        toast("已复制");
    }

    private void toast(String s) { Toast.makeText(this, s, Toast.LENGTH_SHORT).show(); }
    private int dp(int v) { return (int)(v * getResources().getDisplayMetrics().density + 0.5f); }

    private TextView text(String s, int sp, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(s); t.setTextSize(sp); t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.DEFAULT_BOLD);
        return t;
    }

    private LinearLayout.LayoutParams lp(int w, int h, int l, int t, int r, int b) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(w, h);
        p.setMargins(dp(l), dp(t), dp(r), dp(b));
        return p;
    }

    private GradientDrawable makeRounded(int fill, int radiusDp, int stroke, int strokeDp) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(fill);
        g.setCornerRadius(dp(radiusDp));
        g.setStroke(dp(strokeDp), stroke);
        return g;
    }

    private GradientDrawable makeGradient(int[] colors, int radiusDp) {
        GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, colors);
        g.setCornerRadius(dp(radiusDp));
        return g;
    }
}

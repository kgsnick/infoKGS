package asdk.kgs.go.infokgs;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.PowerManager;
import android.support.v7.app.AlertDialog;
import android.view.Gravity;
import android.view.View;
import android.webkit.WebSettings;
import android.widget.TextView;
import android.widget.Toast;

import com.github.lzyzsd.jsbridge.BridgeWebView;
import com.github.lzyzsd.jsbridge.DefaultHandler;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class GobanActivity extends Activity {

    String SgfFile, White, Black, Result;
    BridgeWebView myWebView; // библиотека https://github.com/lzyzsd/JsBridge
    static TextView txt;
    ProgressDialog mProgrDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.goban_activity);

        txt =(TextView)findViewById(R.id.sgfFile);
        TextView b =(TextView)findViewById(R.id.black);
        TextView w =(TextView)findViewById(R.id.white);
        TextView r =(TextView)findViewById(R.id.r);

        Bundle extras = getIntent().getExtras();

        SgfFile = extras.getString("SgfFile");
        White = extras.getString("White");
        Black = extras.getString("Black");
        Result = extras.getString("Result");

        txt.setText(SgfFile);
        b.setText(Black);
        w.setText(White);
        r.setText(Result);

        myWebView = (BridgeWebView) findViewById(R.id.webview);
        WebSettings webSettings = myWebView.getSettings();
        webSettings.setJavaScriptCanOpenWindowsAutomatically(true);
        webSettings.setJavaScriptEnabled(true);
        myWebView.setDefaultHandler(new DefaultHandler());
        myWebView.loadUrl("file:///android_asset/WebSDK/Page/SGF.html"); // js просмотрщик https://github.com/IlyaKirillov/GoProject

        new SGFThread().execute();
    }

    public void onClick(View v) {
        onBackPressed();
    }

    public void gameSaveClick(View v) {
        AlertDialog.Builder builder = new AlertDialog.Builder(GobanActivity.this);
        builder.setMessage(R.string.archive_game)
                .setNegativeButton(R.string.yes,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                // declare the dialog as a member field of your activity
                                ProgressDialog mProgressDialog;

                                // instantiate it within the onCreate method
                                mProgressDialog = new ProgressDialog(GobanActivity.this);
                                mProgressDialog.setIndeterminate(true);
                                mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                                mProgressDialog.setCancelable(true);

                                // execute this when the downloader must be fired
                                final DownloadTask downloadTask = new DownloadTask(GobanActivity.this);
                                downloadTask.execute(txt.getText().toString());

                                mProgressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                                    @Override
                                    public void onCancel(DialogInterface dialog) {
                                        downloadTask.cancel(true);
                                    }
                                });
                            }
                        })
                .setPositiveButton(R.string.no,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        });
        AlertDialog alert = builder.create();
        alert.show();
    }

    public class SGFThread extends AsyncTask<String, Void, String> {

        protected String getASCIIContentFromEntity(HttpEntity entity) throws IllegalStateException, IOException {
            InputStream in = entity.getContent();
            StringBuffer out = new StringBuffer();
            int n = 1;
            while (n > 0) {
                byte[] b = new byte[4096];
                n = in.read(b);
                if (n > 0) out.append(new String(b, 0, n));
            }
            return out.toString();
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected String doInBackground(String... arg) {
            HttpClient httpClient = new DefaultHttpClient();
            HttpContext localContext = new BasicHttpContext();
            HttpGet httpGet = new HttpGet(SgfFile);
            String text;
            try {
                HttpResponse response = httpClient.execute(httpGet, localContext);
                HttpEntity entity = response.getEntity();
                text = getASCIIContentFromEntity(entity);

            } catch (Exception e) {
                return e.getLocalizedMessage();
            }
            return text;
        }

        @Override
        protected void onPostExecute(String result) {
            String s = result.replaceAll("'", ""); // удаляем все знаки ' из чата текстового sgf по причине бага в парсере
            String p = s.replaceAll("\\n", ""); // удаляем все переносы строк, так как string для sgf должен быть в одну строку
            myWebView.send(p); // отправляем string с sgf в JS
        }
    }

    private class DownloadTask extends AsyncTask<String, Integer, String> {

        private Context context;
        private PowerManager.WakeLock mWakeLock;

        String SgfURL = GobanActivity.txt.getText().toString();
        String domain = SgfURL.substring(7);

        public DownloadTask(Context context) {
            this.context = context;
        }

        @Override
        protected String doInBackground(String... sUrl) {
            InputStream input = null;
            OutputStream output = null;
            HttpURLConnection connection = null;
            try {
                URL url = new URL(sUrl[0]);
                connection = (HttpURLConnection) url.openConnection();
                connection.connect();

                // expect HTTP 200 OK, so we don't mistakenly save error report
                // instead of the file
                if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                    return "Server returned HTTP " + connection.getResponseCode()
                            + " " + connection.getResponseMessage();
                }

                // this will be useful to display download percentage
                // might be -1: server did not report the length
                int fileLength = connection.getContentLength();

                // download the file
                input = connection.getInputStream();
                output = new FileOutputStream("/sdcard/" + White + "-" + Black + ".sgf");

                byte data[] = new byte[4096];
                long total = 0;
                int count;
                while ((count = input.read(data)) != -1) {
                    // allow canceling with back button
                    if (isCancelled()) {
                        input.close();
                        return null;
                    }
                    total += count;
                    // publishing the progress....
                    if (fileLength > 0) // only if total length is known
                        publishProgress((int) (total * 100 / fileLength));
                    output.write(data, 0, count);
                }
            } catch (Exception e) {
                return e.toString();
            } finally {
                try {
                    if (output != null)
                        output.close();
                    if (input != null)
                        input.close();
                } catch (IOException ignored) {
                }

                if (connection != null)
                    connection.disconnect();
            }
            return null;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            // take CPU lock to prevent CPU from going off if the user
            // presses the power button during download
            PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, getClass().getName());
            mWakeLock.acquire();
//            mProgrDialog.show();
        }

        @Override
        protected void onProgressUpdate(Integer... progress) {
            super.onProgressUpdate(progress);
            // if we get here, length is known, now set indeterminate to false
//            mProgrDialog.setIndeterminate(false);
//            mProgrDialog.setMax(100);
//            mProgrDialog.setProgress(progress[0]);
        }

        @Override
        protected void onPostExecute(String result) {
            mWakeLock.release();
//            mProgrDialog.dismiss();
            if (result != null) {
                Toast toast = Toast.makeText(getApplicationContext(), R.string.download_error + result, Toast.LENGTH_SHORT);
                toast.setGravity(Gravity.CENTER, 0, 0);
                toast.show();
            }
            else {
                Toast toast = Toast.makeText(getApplicationContext(),R.string.game_downloaded, Toast.LENGTH_SHORT);
                toast.setGravity(Gravity.CENTER, 0, 0);
                toast.show();
            }
        }
    }

}


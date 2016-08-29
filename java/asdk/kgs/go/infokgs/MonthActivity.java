package asdk.kgs.go.infokgs;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.PowerManager;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class MonthActivity extends Activity {

    ListView listview;
    ProgressDialog mProgressDialog;

    public ArrayList<Map<String, String>> titleList = new ArrayList<Map<String, String>>();
    Map<String, String> datum;
    private SimpleAdapter adapter;

    String url, link, month, sMounth, sYear;
    static TextView zip;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.month_activity);

        zip =(TextView)findViewById(R.id.linkZip);

        TextView txt =(TextView)findViewById(R.id.linkId);
        Bundle extras = getIntent().getExtras();
        link = extras.getString("link");
        month = extras.getString("month");
        txt.setText(link);

        if(month.contains("Jan"))sMounth = getResources().getString(R.string.jan);   if(month.contains("Feb"))sMounth = getResources().getString(R.string.feb);  if(month.contains("Mar"))sMounth = getResources().getString(R.string.mar);
        if(month.contains("Apr"))sMounth = getResources().getString(R.string.apr);   if(month.contains("May"))sMounth = getResources().getString(R.string.may);  if(month.contains("Jun"))sMounth = getResources().getString(R.string.jun);
        if(month.contains("Jul"))sMounth = getResources().getString(R.string.jul);   if(month.contains("Aug"))sMounth = getResources().getString(R.string.aug);  if(month.contains("Sep"))sMounth = getResources().getString(R.string.sep);
        if(month.contains("Oct"))sMounth = getResources().getString(R.string.oct);   if(month.contains("Nov"))sMounth = getResources().getString(R.string.nov);  if(month.contains("Dec"))sMounth = getResources().getString(R.string.dec);

        url = "https://www.gokgs.com/" + link;

        // Execute DownloadJSON AsyncTask
        new JsoupListView().execute();

        adapter = new SimpleAdapter(this, titleList, R.layout.list_item_sgf,
                new String[] {"white", "black", "setup", "time", "type", "result", "viewable", "link"},
                new int[] {R.id.white, R.id.black, R.id.setup, R.id.time, R.id.type, R.id.result, R.id.viewable, R.id.link});

        listview = (ListView) findViewById(R.id.listview);
        listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View itemClicked, int position, long id) {
                datum = (Map<String, String>) parent.getItemAtPosition(position);
                String SgfFile = datum.get("link");
                String White = datum.get("white");
                String Black = datum.get("black");
                String Result = datum.get("result");

                if((SgfFile.isEmpty())||(SgfFile.contains("gameArchives.jsp?"))){
                    Toast toast = Toast.makeText(getApplicationContext(),R.string.noviewable, Toast.LENGTH_SHORT);
                    toast.setGravity(Gravity.CENTER, 0, 0);
                    toast.show();
                    return;
                } else {
                    Intent intent = new Intent(MonthActivity.this, GobanActivity.class);
                    intent.putExtra("SgfFile", SgfFile );
                    intent.putExtra("White", White );
                    intent.putExtra("Black", Black );
                    intent.putExtra("Result", Result );
                    startActivity(intent);
                }
            }
        });
    }

    // Title AsyncTask
    private class JsoupListView extends AsyncTask<String, Void, String> {

        String href, hrefR;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mProgressDialog = new ProgressDialog(MonthActivity.this);
            mProgressDialog.setMessage(getResources().getString(R.string.loading));
            mProgressDialog.setIndeterminate(false);
            mProgressDialog.show();
        }

        @Override
        protected String doInBackground(String... params) {
            String isStop;

            try {
                // Connect to the Website URL
                Document doc = Jsoup.connect(url).get();
              //  Log.i("html code", String.valueOf(doc));

                if (String.valueOf(doc).contains("did not play any games during")) {
                    isStop = "nogames";
                    return isStop;
                }
                else {
                    Elements links = doc.select("table.grid ~ p");
                    for (Element link : links.select("a")) {
                        href = link.attr("abs:href");
                        hrefR = href.replace("tar.gz", "zip");
                    }
//                    Log.i("DOC", String.valueOf(doc));
//                    Log.i("links", hrefR);

                    Element tableVerb = doc.select("h2 ~ table.grid").first();
                    Elements rowsVerb = tableVerb.select("tr");

                    // чистим наш аррей лист для того что бы заполнить
                    titleList.clear();

                    for (int i = 1; i < rowsVerb.size(); i++) { // int i = 1 чтоб убрать верхнюю строку из таблицы
                        Element row = rowsVerb.get(i);
                        Elements cols = row.select("td");

                        String url = cols.select("a").attr("href");

                        datum = new HashMap<String, String>();

                        for (Element e: cols) {
                            if (cols.hasAttr("colspan")) {
                                String viewable = cols.get(0).text();
                                String white = cols.get(1).text();
                                String setup = cols.get(2).text();
                                String time = cols.get(3).text();
                                String type = cols.get(4).text();
                                String result = cols.get(5).text();

                                int colspan = Integer.valueOf(cols.attr("colspan"));

                                for (int y = 0; y < colspan; y++) {
                                    datum.put("white", white);
                                    datum.put("black", "-");
                                    datum.put("setup", setup);
                                    datum.put("time", time);
                                    datum.put("type", type);
                                    datum.put("result", result);
                                    datum.put("viewable", viewable);
                                    datum.put("link", url);
                                }
                            }
                            else {
                                String viewable = cols.get(0).text();
                                String white = cols.get(1).text();
                                String black = cols.get(2).text();
                                String setup = cols.get(3).text();
                                String time = cols.get(4).text();
                                String type = cols.get(5).text();
                                String result = cols.get(6).text();

                                datum.put("white", white);
                                datum.put("black", black);
                                datum.put("setup", setup);
                                datum.put("time", time);
                                datum.put("type", type);
                                datum.put("result", result);
                                datum.put("viewable", viewable);
                                datum.put("link", url);
                            }
                        }
                        titleList.add(datum);
                    }
                }
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(String result) {

            if(result == "nogames" ){

                AlertDialog.Builder builder = new AlertDialog.Builder(MonthActivity.this);
                builder.setMessage(R.string.nogames)
                        .setIcon(R.mipmap.ic_launcher)
                        .setCancelable(false)
                        .setNegativeButton(R.string.close,
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                       // dialog.cancel();
                                        onBackPressed();
                                    }
                                });
                AlertDialog alert = builder.create();
                alert.show();
            }
            else {
                if(datum.get("time").contains("/90 ")){sYear = "1990";} if(datum.get("time").contains("/91 ")){sYear = "1991";} if(datum.get("time").contains("/92 ")){sYear = "1992";} if(datum.get("time").contains("/93 ")){sYear = "1993";} if(datum.get("time").contains("/94 ")){sYear = "1994";} if(datum.get("time").contains("/95 ")){sYear = "1995";} if(datum.get("time").contains("/96 ")){sYear = "1996";} if(datum.get("time").contains("/97 ")){sYear = "1997";} if(datum.get("time").contains("/98 ")){sYear = "1998";} if(datum.get("time").contains("/99 ")){sYear = "1999";}
                if(datum.get("time").contains("/00 ")){sYear = "2000";} if(datum.get("time").contains("/01 ")){sYear = "2001";} if(datum.get("time").contains("/02 ")){sYear = "2002";} if(datum.get("time").contains("/03 ")){sYear = "2003";} if(datum.get("time").contains("/04 ")){sYear = "2004";} if(datum.get("time").contains("/05 ")){sYear = "2005";} if(datum.get("time").contains("/06 ")){sYear = "2006";} if(datum.get("time").contains("/07 ")){sYear = "2007";} if(datum.get("time").contains("/08 ")){sYear = "2008";} if(datum.get("time").contains("/09 ")){sYear = "2009";}
                if(datum.get("time").contains("/10 ")){sYear = "2010";} if(datum.get("time").contains("/11 ")){sYear = "2011";} if(datum.get("time").contains("/12 ")){sYear = "2012";} if(datum.get("time").contains("/13 ")){sYear = "2013";} if(datum.get("time").contains("/14 ")){sYear = "2014";} if(datum.get("time").contains("/15 ")){sYear = "2015";} if(datum.get("time").contains("/16 ")){sYear = "2016";} if(datum.get("time").contains("/17 ")){sYear = "2017";} if(datum.get("time").contains("/18 ")){sYear = "2018";} if(datum.get("time").contains("/19 ")){sYear = "2019";}
                if(datum.get("time").contains("/20 ")){sYear = "2020";} if(datum.get("time").contains("/21 ")){sYear = "2021";} if(datum.get("time").contains("/22 ")){sYear = "2022";} if(datum.get("time").contains("/23 ")){sYear = "2023";} if(datum.get("time").contains("/24 ")){sYear = "2024";} if(datum.get("time").contains("/25 ")){sYear = "2025";} if(datum.get("time").contains("/26 ")){sYear = "2026";} if(datum.get("time").contains("/27 ")){sYear = "2027";} if(datum.get("time").contains("/28 ")){sYear = "2028";} if(datum.get("time").contains("/29 ")){sYear = "2029";} if(datum.get("time").contains("/30 ")){sYear = "2030";}

                TextView mounthYear =(TextView)findViewById(R.id.mounthYear);
                String MounthYear = sMounth + " " + sYear;
                mounthYear.setText(MounthYear);

                zip.setText(hrefR);
                listview = (ListView) findViewById(R.id.listview);
                // Set the adapter to the ListView
                listview.setAdapter(adapter);
                // Close the progressdialog
                mProgressDialog.dismiss();
            }
        }
    }

    public void onClick(View v) {
        onBackPressed();
    }

    public void saveClick(View v) {
        AlertDialog.Builder builder = new AlertDialog.Builder(MonthActivity.this);
        builder.setMessage(R.string.archive)
                .setNegativeButton(R.string.yes,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                // declare the dialog as a member field of your activity
                                ProgressDialog mProgressDialog;

                                // instantiate it within the onCreate method
                                mProgressDialog = new ProgressDialog(MonthActivity.this);
                                mProgressDialog.setMessage("A message");
                                mProgressDialog.setIndeterminate(true);
                                mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                                mProgressDialog.setCancelable(true);

                                // execute this when the downloader must be fired
                                final DownloadTask downloadTask = new DownloadTask(MonthActivity.this);
                                downloadTask.execute(zip.getText().toString());

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

    private class DownloadTask extends AsyncTask<String, Integer, String> {

        private Context context;
        private PowerManager.WakeLock mWakeLock;

        String SgfURL = MonthActivity.zip.getText().toString();
        String domain = SgfURL.substring(45);

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
                output = new FileOutputStream("/sdcard/" + domain);

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
            mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                    getClass().getName());
            mWakeLock.acquire();
//            mProgressDialog.show();
        }

        @Override
        protected void onProgressUpdate(Integer... progress) {
            super.onProgressUpdate(progress);
            // if we get here, length is known, now set indeterminate to false
//            mProgressDialog.setIndeterminate(false);
//            mProgressDialog.setMax(100);
//            mProgressDialog.setProgress(progress[0]);
        }

        @Override
        protected void onPostExecute(String result) {
            mWakeLock.release();
//            mProgressDialog.dismiss();
            if (result != null) {
                Toast toast = Toast.makeText(getApplicationContext(), R.string.download_error + result, Toast.LENGTH_SHORT);
                toast.setGravity(Gravity.CENTER, 0, 0);
                toast.show();
            }
            else {
                Toast toast = Toast.makeText(getApplicationContext(),R.string.file_downloaded, Toast.LENGTH_SHORT);
                toast.setGravity(Gravity.CENTER, 0, 0);
                toast.show();
            }
        }
    }
}



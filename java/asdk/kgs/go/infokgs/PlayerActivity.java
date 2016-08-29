package asdk.kgs.go.infokgs;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.GridView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class PlayerActivity extends Activity {

    // то в чем будем хранить данные пока не передадим адаптеру
    public ArrayList<Map<String, String>> titleList = new ArrayList<Map<String, String>>();

    Map<String, String> datum;
    //Adapter для вывода данных
    private SimpleAdapter  adapter;

    private GridView gv;

    ProgressDialog mProgressDialog;
    String sgfgame, sPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.player_activity);

        TextView txt =(TextView)findViewById(R.id.playerID);
        Bundle extras = getIntent().getExtras();
        sgfgame = extras.getString("paramName1");
        txt.setText(sgfgame);

        gv = (GridView) findViewById(R.id.gridview);
        gv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
                datum = (Map<String, String>) parent.getItemAtPosition(position);
                String link = datum.get("link");
                String month = datum.get("month");

                Log.i("month + link", month + " " + link);

                if (  !( month.isEmpty()) && !(month.contains("20")) && (link.isEmpty())  ){
                    String linkLast = "gameArchives.jsp?user=" + sPlayer;
                    Intent intent = new Intent(PlayerActivity.this, MonthActivity.class);
                    intent.putExtra("link", linkLast );
                    intent.putExtra("month", month );
                    startActivity(intent);
                }

                if((link.isEmpty())||(month == "...")){
                    return;
                } else {
                    Intent intent = new Intent(PlayerActivity.this, MonthActivity.class);
                    intent.putExtra("link", link );
                    intent.putExtra("month", month );
                    startActivity(intent);
                }
            }
        });

        final CheckBox cb =(CheckBox)findViewById(R.id.checkBoxPlayer);

        if(cb.isChecked() == false) {
            sPlayer = sgfgame;
            new NewThread().execute();
        }

        cb.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                if(cb.isChecked()) {
                    sPlayer = sgfgame + "&oldAccounts=y";
                    // запрос к нашему отдельному поток на выборку данных
                    new NewThread().execute();
                }
                else {
                    sPlayer = sgfgame;
                    // запрос к нашему отдельному поток на выборку данных
                    new NewThread().execute();
                }
            }
        });

        // Добавляем данные для ListView
        adapter = new SimpleAdapter(this, titleList, R.layout.list_item_year,
                new String[] {"month", "link"},
                new int[] {R.id.yearText, R.id.linkitem});
    }

    public void onClick(View v) {
        onBackPressed();
    }

    public void onClickGraph(View v) {
        final String graph = sgfgame;
        Intent intent = new Intent(PlayerActivity.this, GraphActivity.class);
        intent.putExtra("graph", graph );
        startActivity(intent);
    }

    /** внутрений класс который делает запросы */
    public class NewThread extends AsyncTask<String, Void, String> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mProgressDialog = new ProgressDialog(PlayerActivity.this);
            mProgressDialog.setMessage(getResources().getString(R.string.loading));
            mProgressDialog.setIndeterminate(false);
            mProgressDialog.show();
        }

        // Метод выполняющий запрос в фоне, в версиях выше 4 андроида, запросы в главном потоке выполнять
        // нельзя, поэтому все что вам нужно выполнять - выносите в отдельный тред
        @Override
        protected String doInBackground(String... arg) {
            try {
                Document verb = Jsoup.connect("https://www.gokgs.com/gameArchives.jsp?user=" + sPlayer).get();

              //  Log.i("HTML CODE", String.valueOf(verb));

                if ( verb.select("h1").text().contains("HTTP Status 503")){
                    AlertDialog.Builder builder = new AlertDialog.Builder(PlayerActivity.this);
                    builder.setTitle(R.string.status)
                            .setMessage(R.string.request_quota)
                            .setIcon(R.mipmap.ic_launcher)
                            .setCancelable(false)
                            .setNegativeButton(R.string.ok,
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int id) {
                                            dialog.cancel();
                                        }
                                    });
                    AlertDialog alert = builder.create();
                    alert.show();
                }
                else {
                    Element tableVerb = verb.select("p ~ table.grid").first();
                    Elements rowsVerb = tableVerb.select("tr");

                    // чистим наш аррей лист для того что бы заполнить
                    titleList.clear();

                    for (int i = 0; i < rowsVerb.size(); i++) {
                        Element row = rowsVerb.get(i);
                        Elements cols = row.select("td");

                        for (Element e: cols) {
                            if (e.hasAttr("colspan")) {
                                int colspan = Integer.valueOf(e.attr("colspan"));
                                for (int y = 0; y < colspan; y++) {
                                    datum = new HashMap<String, String>(2);
                                    datum.put("month", "...");
                                    datum.put("link", "...");
                                    titleList.add(datum);
                                }
                            }
                            else {
                                String url = e.select("a").attr("href");
                                String month = e.text();
                                datum = new HashMap<String, String>(2);
                                datum.put("month", month);
                                datum.put("link", url);
                                titleList.add(datum);
                            }
                        }
                    }
                }
            }
            catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(String result) {

            // после запроса обновляем листвью
            gv.setAdapter(adapter);
            mProgressDialog.dismiss();
        }
    }

}

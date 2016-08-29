package asdk.kgs.go.infokgs;

import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends ListActivity
        implements AdapterView.OnItemClickListener, AdapterView.OnItemLongClickListener {

    EditText et;
    String listItem[]={};
    String player;
    Button btn;
    ProgressDialog mProgressDialog;
    ArrayAdapter<String> adapter;
    ArrayList values;
    ListView lv;
    SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        et = (EditText) findViewById(R.id.editText);
        player = et.getText().toString();
        btn = (Button) findViewById(R.id.addItem);
        lv = (ListView) findViewById(android.R.id.list);

        values = new ArrayList();
        for (int i = 0; i < listItem.length; i++) {
            values.add(listItem[i]);
        }

        prefs = getSharedPreferences("User", Context.MODE_PRIVATE);
        LoadPreferences();

        //адапатер для построения списка
        adapter = new ArrayAdapter<String>(this, R.layout.list_item, values);
        setListAdapter(adapter);

        lv.setOnItemClickListener(this); //обработка короткого нажатия по элементу списка
        lv.setOnItemLongClickListener(this); //обработка длинного нажатия по элементу списка
    }

    protected void SavePreferences() {
        //save the user list to preference
        SharedPreferences.Editor editor = prefs.edit();
        try {
            editor.putString("UserList", ObjectSerializer.serialize(values));
        } catch (IOException e) {
            e.printStackTrace();
        }
        editor.commit();
    }

    protected void LoadPreferences(){
        //load the user list from preference
        try {
            values = (ArrayList) ObjectSerializer.deserialize(prefs.getString("UserList", ObjectSerializer.serialize(new ArrayList())));
           // Log.i("values", String.valueOf(values));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.addItem:
                player = et.getText().toString();

                if(values.contains(player)){
                    Toast toast = Toast.makeText(getApplicationContext(), R.string.wrong_msg2, Toast.LENGTH_SHORT);
                    toast.setGravity(Gravity.CENTER, 0, 0);
                    toast.show();
                }
                else if(player.isEmpty()){
                    Toast toast = Toast.makeText(getApplicationContext(), R.string.wrong_msg, Toast.LENGTH_SHORT);
                    toast.setGravity(Gravity.CENTER, 0, 0);
                    toast.show();
                }
                else {
                    btn.setEnabled(false);
                    new GetRequest().execute();
                };
                break;
            case  R.id.info:
                Intent intent = new Intent(MainActivity.this, InfoActivity.class);
                startActivity(intent);
                break;
        }
    }

    @Override //обработка короткого нажатия по элементу списка lv
    public void onItemClick(AdapterView<?> parent, View itemClicked, int position, long id) {
        final String myStringData = adapter.getItem(position);
        Intent intent = new Intent(MainActivity.this, PlayerActivity.class);
        intent.putExtra("paramName1", myStringData );
        startActivity(intent);
    }

    @Override //обработка длинного нажатия по элементу списка lv
    public boolean onItemLongClick(final AdapterView<?> parent, View view, final int position, long id) {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setMessage(R.string.del_msg)
                .setNegativeButton(R.string.remove,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                values.remove(position);
                                SavePreferences();
                                adapter.notifyDataSetChanged();
                                dialog.cancel();
                            }
                        })
                .setPositiveButton(R.string.cancel,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        });
        AlertDialog alert = builder.create();
        alert.show();
        // Возвращает "истину", чтобы завершить событие клика, чтобы onListItemClick больше не вызывался
        return true;
    }

    private class GetRequest extends AsyncTask<Void, Void, String> {

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
            mProgressDialog = new ProgressDialog(MainActivity.this);
            mProgressDialog.setMessage(getResources().getString(R.string.loading));
            mProgressDialog.setIndeterminate(false);
            mProgressDialog.show();
        }

        protected String doInBackground(Void... params) {
            HttpClient httpClient = new DefaultHttpClient();
            HttpContext localContext = new BasicHttpContext();
                        HttpGet httpGet = new HttpGet("https://www.gokgs.com/gameArchives.jsp?user=" + player);
            String text;
            try {
                HttpResponse response = httpClient.execute(httpGet, localContext);
                HttpEntity entity = response.getEntity();
                text = getASCIIContentFromEntity(entity);
                //Log.i("Отклик; ", text);
            } catch (Exception e) {
                return e.getLocalizedMessage();
            }
            return text;
        }

        protected void onPostExecute(String results) {
            ArrayAdapter<String> adapter = (ArrayAdapter<String>) getListAdapter();

            if(results.contains("HTTP Status 503 - Request quota exceeded")){
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
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
                if(results.contains("year")) {
                    List myList = new ArrayList();
                    myList.add(player);
                    adapter.add(player);
                    et.setText("");
                    SavePreferences();
                }
                else {
                    Toast toast = Toast.makeText(getApplicationContext(),R.string.wrong_nickname, Toast.LENGTH_SHORT);
                    toast.setGravity(Gravity.CENTER, 0, 0);
                    toast.show();
                }
            }

            adapter.notifyDataSetChanged();

            btn.setEnabled(true);
            mProgressDialog.dismiss();
        }
    }

}

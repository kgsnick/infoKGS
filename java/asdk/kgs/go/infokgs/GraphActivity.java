package asdk.kgs.go.infokgs;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.InputStream;

public class GraphActivity extends Activity {

    String text;
    static ProgressBar pbCircle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.graph_activity);

        pbCircle =(ProgressBar)findViewById(R.id.pbCircle);
        TextView graphtxt =(TextView)findViewById(R.id.graphtxt);

        Bundle extras = getIntent().getExtras();
        text = extras.getString("graph");
        graphtxt.setText(text);

        // Image link from internet
        new DownloadImageFromInternet((ImageView) findViewById(R.id.graphView)).execute("https://www.gokgs.com/servlet/graph/" + text + "-en_US.png");

        pbCircle.setVisibility(View.VISIBLE);
    }

    public void onClick(View v) {
        onBackPressed();
    }
}

class DownloadImageFromInternet extends AsyncTask<String, Void, Bitmap> {

    ImageView imageView;

    public DownloadImageFromInternet(ImageView imageView) {
        this.imageView = imageView;
    }

    protected Bitmap doInBackground(String... urls) {
        String imageURL = urls[0];
        Bitmap bimage = null;
        try {
            InputStream in = new java.net.URL(imageURL).openStream();
            bimage = BitmapFactory.decodeStream(in);

        } catch (Exception e) {
           // Log.e("Error Message", e.getMessage());
            e.printStackTrace();
        }
        return bimage;
    }

    protected void onPostExecute(Bitmap result) {
        imageView.setImageBitmap(result);
        GraphActivity.pbCircle.setVisibility(View.GONE);
    }

}

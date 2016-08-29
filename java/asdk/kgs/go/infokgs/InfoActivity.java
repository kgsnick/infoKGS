package asdk.kgs.go.infokgs;

import android.app.Activity;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.text.util.Linkify;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.WebSettings;
import android.widget.TextView;

// Uncomment the following line if you want to use Plus1 WapStart Conversion SDK
// import ru.wapstart.plus1.conversion.sdk.Plus1ConversionTracker;
import com.github.lzyzsd.jsbridge.BridgeWebView;
import com.github.lzyzsd.jsbridge.DefaultHandler;

public class InfoActivity extends Activity {

    TextView infolink;
    String development, blog, google_play, github,
            libraries, jsoup, webgoboard, jsbridge;

    BridgeWebView myWebView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.info_activity);

        infolink = (TextView)findViewById(R.id.infolink);
        infolink.setMovementMethod(new ScrollingMovementMethod()); //прокрутка

        //получаем string
        development = getString(R.string.development);
        blog = getString(R.string.blog);
        google_play = getString(R.string.google_play);
        github = getString(R.string.github);
        libraries = getString(R.string.libraries);
        jsoup = getString(R.string.jsoup);
        webgoboard = getString(R.string.webgoboard);
        jsbridge = getString(R.string.jsbridge);

        String data = "" +
                development + "\n" + "\n" +
                blog + "  " + "https://kgsnick.wordpress.com" + "\n" +
                google_play + "  " + "https://play.google.com/store/apps/developer?id=asdk" + "\n" +
                github + "  " + "https://github.com/kgsnick" + "\n" + "\n" + "\n" +
                libraries + "\n" + "\n" +
                webgoboard + "  " + "https://github.com/IlyaKirillov/GoProject" + "\n" +
                jsoup + "  " + "https://jsoup.org" + "\n" +
                jsbridge + "  " + "https://github.com/lzyzsd/JsBridge";

        if(infolink != null) {
            infolink.setText(data);
            Linkify.addLinks(infolink, Linkify.ALL);
        }

        myWebView = (BridgeWebView) findViewById(R.id.advview);
        WebSettings webSettings = myWebView.getSettings();
        webSettings.setJavaScriptCanOpenWindowsAutomatically(true);
        webSettings.setJavaScriptEnabled(true);
        myWebView.setDefaultHandler(new DefaultHandler());
        myWebView.loadUrl("file:///android_asset/WebSDK/Page/Adv.html");

    }

    public void onClick(View v) {
        onBackPressed();
    }
}
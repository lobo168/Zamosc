package app.thecity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.balysv.materialripple.MaterialRippleLayout;
import com.google.ads.mediation.admob.AdMobAdapter;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.nostra13.universalimageloader.core.ImageLoader;

import java.util.ArrayList;

import app.thecity.data.AppConfig;
import app.thecity.data.Constant;
import app.thecity.data.GDPR;
import app.thecity.data.ThisApplication;
import app.thecity.model.NewsInfo;
import app.thecity.utils.Tools;

public class ActivityNewsInfoDetails extends AppCompatActivity {

    private static final String EXTRA_OBJECT = "key.EXTRA_OBJECT";
    private static final String EXTRA_FROM_NOTIF = "key.EXTRA_FROM_NOTIF";

    // activity transition
    public static void navigate(Activity activity, NewsInfo obj, Boolean from_notif) {
        Intent i = navigateBase(activity, obj, from_notif);
        activity.startActivity(i);
    }

    public static Intent navigateBase(Context context, NewsInfo obj, Boolean from_notif) {
        Intent i = new Intent(context, ActivityNewsInfoDetails.class);
        i.putExtra(EXTRA_OBJECT, obj);
        i.putExtra(EXTRA_FROM_NOTIF, from_notif);
        return i;
    }

    private Boolean from_notif;

    // extra obj
    private NewsInfo newsInfo;

    private Toolbar toolbar;
    private ActionBar actionBar;
    private View parent_view;
    private WebView webview;
    private ImageLoader imgloader = ImageLoader.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_news_info_details);

        newsInfo = (NewsInfo) getIntent().getSerializableExtra(EXTRA_OBJECT);
        from_notif = getIntent().getBooleanExtra(EXTRA_FROM_NOTIF, false);

        if (!imgloader.isInited()) Tools.initImageLoader(this);

        initComponent();
        initToolbar();
        displayData();

        // analytics tracking
        ThisApplication.getInstance().trackScreenView("View News Info : " + newsInfo.title);
    }

    private void initComponent() {
        parent_view = findViewById(android.R.id.content);
    }

    private void initToolbar() {
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeButtonEnabled(true);
        actionBar.setTitle("");
    }

    private void prepareAds() {
        if (AppConfig.ADS_NEWS_DETAILS_BANNER && Tools.cekConnection(getApplicationContext())) {
            AdView mAdView = (AdView) findViewById(R.id.ad_view);
            AdRequest adRequest = new AdRequest.Builder()
                    .addNetworkExtrasBundle(AdMobAdapter.class, GDPR.getBundleAd(this)).build();
            // Start loading the ad in the background.
            mAdView.loadAd(adRequest);
        } else {
            ((RelativeLayout) findViewById(R.id.banner_layout)).setVisibility(View.GONE);
        }
    }

    private void displayData() {
        ((TextView) findViewById(R.id.title)).setText(Html.fromHtml(newsInfo.title));

        webview = (WebView) findViewById(R.id.content);
        String html_data = "<style>img{max-width:100%;height:auto;} iframe{width:100%;}</style> ";
        html_data += newsInfo.full_content;
        webview.getSettings().setJavaScriptEnabled(true);
        webview.getSettings();
        webview.getSettings().setBuiltInZoomControls(true);
        webview.setBackgroundColor(Color.TRANSPARENT);
        webview.setWebChromeClient(new WebChromeClient());
        webview.loadData(html_data, "text/html; charset=UTF-8", null);

        // disable scroll on touch
        webview.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                return (event.getAction() == MotionEvent.ACTION_MOVE);
            }
        });

        ((TextView) findViewById(R.id.date)).setText(Tools.getFormattedDate(newsInfo.last_update));
        imgloader.displayImage(Constant.getURLimgNews(newsInfo.image), (ImageView) findViewById(R.id.image));

        ((MaterialRippleLayout) findViewById(R.id.lyt_image)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ArrayList<String> images_list = new ArrayList<>();
                images_list.add(Constant.getURLimgNews(newsInfo.image));
                Intent i = new Intent(ActivityNewsInfoDetails.this, ActivityFullScreenImage.class);
                i.putStringArrayListExtra(ActivityFullScreenImage.EXTRA_IMGS, images_list);
                startActivity(i);
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (webview != null) webview.onPause();
    }

    @Override
    protected void onResume() {
        if (!imgloader.isInited()) Tools.initImageLoader(getApplicationContext());
        if (webview != null) webview.onResume();
        prepareAds();
        super.onResume();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_activity_details, menu);
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            onBackAction();
            return true;
        } else if (id == R.id.action_share) {
            Tools.methodShareNews(this, newsInfo);
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        onBackAction();
    }

    private void onBackAction() {
        if (from_notif) {
            if (ActivityMain.active) {
                finish();
            } else {
                Intent intent = new Intent(getApplicationContext(), ActivitySplash.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                finish();
            }
        } else {
            super.onBackPressed();
        }
    }

}

package app.thecity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.ads.mediation.admob.AdMobAdapter;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.InterstitialAd;
import com.nostra13.universalimageloader.core.ImageLoader;

import app.thecity.data.AppConfig;
import app.thecity.data.Constant;
import app.thecity.data.DatabaseHandler;
import app.thecity.data.GDPR;
import app.thecity.data.SharedPref;
import app.thecity.fragment.FragmentCategory;
import app.thecity.utils.Tools;

public class ActivityMain extends AppCompatActivity {

    //for ads
    private InterstitialAd mInterstitialAd;

    private ImageLoader imgloader = ImageLoader.getInstance();

    public ActionBar actionBar;
    public Toolbar toolbar;
    private int cat[];
    private FloatingActionButton fab;
    private NavigationView navigationView;
    private DatabaseHandler db;
    private SharedPref sharedPref;
    private RelativeLayout nav_header_lyt;

    static ActivityMain activityMain;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        activityMain = this;

        if (!imgloader.isInited()) Tools.initImageLoader(this);
        fab = (FloatingActionButton) findViewById(R.id.fab);
        db = new DatabaseHandler(this);
        sharedPref = new SharedPref(this);

        prepareAds();
        initToolbar();
        initDrawerMenu();
        prepareImageLoader();
        cat = getResources().getIntArray(R.array.id_category);

        // first drawer view
        onItemSelected(R.id.nav_all, getString(R.string.title_nav_all));

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(ActivityMain.this, ActivitySearch.class);
                startActivity(i);
            }
        });

        // for system bar in lollipop
        Tools.systemBarLolipop(this);
    }

    private void initToolbar() {
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeButtonEnabled(true);
        Tools.setActionBarColor(this, actionBar);
    }

    private void initDrawerMenu() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close) {
            @Override
            public void onDrawerOpened(View drawerView) {
                updateFavoritesCounter(navigationView, R.id.nav_favorites, db.getFavoritesSize());
                showInterstitial();
                super.onDrawerOpened(drawerView);
            }
        };
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(MenuItem item) {
                return onItemSelected(item.getItemId(), item.getTitle().toString());
            }
        });
        if (!AppConfig.ENABLE_NEWS_INFO) navigationView.getMenu().removeItem(R.id.nav_news);

        // navigation header
        View nav_header = navigationView.getHeaderView(0);
        nav_header_lyt = (RelativeLayout) nav_header.findViewById(R.id.nav_header_lyt);
        nav_header_lyt.setBackgroundColor(Tools.colorBrighter(sharedPref.getThemeColorInt()));
        (nav_header.findViewById(R.id.menu_nav_setting)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(getApplicationContext(), ActivitySetting.class);
                startActivity(i);
            }
        });

        (nav_header.findViewById(R.id.menu_nav_map)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(getApplicationContext(), ActivityMaps.class);
                startActivity(i);
            }
        });
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (!drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.openDrawer(GravityCompat.START);
        } else {
            doExitApp();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_activity_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            Intent i = new Intent(getApplicationContext(), ActivitySetting.class);
            startActivity(i);
        } else if (id == R.id.action_rate) {
            Tools.rateAction(ActivityMain.this);
        } else if (id == R.id.action_about) {
            Tools.aboutAction(ActivityMain.this);
        }

        return super.onOptionsItemSelected(item);
    }

    public boolean onItemSelected(int id, String title) {
        // Handle navigation view item clicks here.
        Fragment fragment = null;
        Bundle bundle = new Bundle();
        switch (id) {
            //sub menu
            case R.id.nav_all:
                fragment = new FragmentCategory();
                bundle.putInt(FragmentCategory.TAG_CATEGORY, -1);
                actionBar.setTitle(title);
                break;
            // favorites
            case R.id.nav_favorites:
                fragment = new FragmentCategory();
                bundle.putInt(FragmentCategory.TAG_CATEGORY, -2);
                actionBar.setTitle(title);
                break;
            // news info
            case R.id.nav_news:
                Intent i = new Intent(this, ActivityNewsInfo.class);
                startActivity(i);
                break;

            case R.id.nav_featured:
                fragment = new FragmentCategory();
                bundle.putInt(FragmentCategory.TAG_CATEGORY, cat[10]);
                actionBar.setTitle(title);
                break;
            case R.id.nav_tour:
                fragment = new FragmentCategory();
                bundle.putInt(FragmentCategory.TAG_CATEGORY, cat[0]);
                actionBar.setTitle(title);
                break;
            case R.id.nav_food:
                fragment = new FragmentCategory();
                bundle.putInt(FragmentCategory.TAG_CATEGORY, cat[1]);
                actionBar.setTitle(title);
                break;
            case R.id.nav_hotels:
                fragment = new FragmentCategory();
                bundle.putInt(FragmentCategory.TAG_CATEGORY, cat[2]);
                actionBar.setTitle(title);
                break;
            case R.id.nav_ent:
                fragment = new FragmentCategory();
                bundle.putInt(FragmentCategory.TAG_CATEGORY, cat[3]);
                actionBar.setTitle(title);
                break;
            case R.id.nav_sport:
                fragment = new FragmentCategory();
                bundle.putInt(FragmentCategory.TAG_CATEGORY, cat[4]);
                actionBar.setTitle(title);
                break;
            case R.id.nav_shop:
                fragment = new FragmentCategory();
                bundle.putInt(FragmentCategory.TAG_CATEGORY, cat[5]);
                actionBar.setTitle(title);
                break;
            case R.id.nav_transport:
                fragment = new FragmentCategory();
                bundle.putInt(FragmentCategory.TAG_CATEGORY, cat[6]);
                actionBar.setTitle(title);
                break;
            case R.id.nav_religion:
                fragment = new FragmentCategory();
                bundle.putInt(FragmentCategory.TAG_CATEGORY, cat[7]);
                actionBar.setTitle(title);
                break;
            case R.id.nav_public:
                fragment = new FragmentCategory();
                bundle.putInt(FragmentCategory.TAG_CATEGORY, cat[8]);
                actionBar.setTitle(title);
                break;
            case R.id.nav_money:
                fragment = new FragmentCategory();
                bundle.putInt(FragmentCategory.TAG_CATEGORY, cat[9]);
                actionBar.setTitle(title);
                break;
            default:
                break;

            /* IMPORTANT : cat[index_array], index is start from 0
             */
        }

        if (fragment != null) {
            fragment.setArguments(bundle);
            FragmentManager fragmentManager = getSupportFragmentManager();
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            fragmentTransaction.replace(R.id.frame_content, fragment);
            fragmentTransaction.commit();
        }
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private void hideKeyboard() {
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    private long exitTime = 0;

    public void doExitApp() {
        if ((System.currentTimeMillis() - exitTime) > 2000) {
            Toast.makeText(this, R.string.press_again_exit_app, Toast.LENGTH_SHORT).show();
            exitTime = System.currentTimeMillis();
        } else {
            finish();
        }
    }

    private void prepareImageLoader() {
        Tools.initImageLoader(this);
    }


    @Override
    protected void onResume() {
        if (!imgloader.isInited()) Tools.initImageLoader(this);
        updateFavoritesCounter(navigationView, R.id.nav_favorites, db.getFavoritesSize());
        if (actionBar != null) {
            Tools.setActionBarColor(this, actionBar);
            // for system bar in lollipop
            Tools.systemBarLolipop(this);
        }
        if (nav_header_lyt != null) {
            nav_header_lyt.setBackgroundColor(Tools.colorBrighter(sharedPref.getThemeColorInt()));
        }
        super.onResume();
    }

    static boolean active = false;

    @Override
    public void onStart() {
        super.onStart();
        active = true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        active = false;
    }


    private void updateFavoritesCounter(NavigationView nav, @IdRes int itemId, int count) {
        TextView view = (TextView) nav.getMenu().findItem(itemId).getActionView().findViewById(R.id.counter);
        view.setText(String.valueOf(count));
    }

    private void prepareAds() {
        // init GDPR
        if(AppConfig.ENABLE_GDPR) GDPR.updateConsentStatus(this);

        // Create the InterstitialAd and set the adUnitId.
        mInterstitialAd = new InterstitialAd(this);
        // Defined in res/values/strings.xml
        mInterstitialAd.setAdUnitId(getString(R.string.interstitial_ad_unit_id));
        //prepare ads
        AdRequest adRequest = new AdRequest.Builder()
                .addNetworkExtrasBundle(AdMobAdapter.class, GDPR.getBundleAd(this)).build();
        mInterstitialAd.loadAd(adRequest);
    }

    /**
     * show ads
     */
    public void showInterstitial() {
        // Show the ad if it's ready
        if (AppConfig.ADS_MAIN_INTERSTITIAL && mInterstitialAd != null && mInterstitialAd.isLoaded()) {
            mInterstitialAd.show();
        }
    }


    public static ActivityMain getInstance() {
        return activityMain;
    }

    public static void animateFab(final boolean hide) {
        FloatingActionButton f_ab = (FloatingActionButton) activityMain.findViewById(R.id.fab);
        int moveY = hide ? (2 * f_ab.getHeight()) : 0;
        f_ab.animate().translationY(moveY).setStartDelay(100).setDuration(400).start();
    }
}

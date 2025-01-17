package com.dn.vpnzero.activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.FrameLayout;


import com.androidnetworking.AndroidNetworking;
import com.androidnetworking.common.Priority;
import com.androidnetworking.error.ANError;
import com.androidnetworking.interfaces.JSONArrayRequestListener;
import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.CustomEvent;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.dn.vpnzero.App;
import com.dn.vpnzero.BuildConfig;
import com.dn.vpnzero.R;
import com.dn.vpnzero.database.DBHelper;
import com.dn.vpnzero.model.Server;
import com.dn.vpnzero.util.CountriesNames;
import com.dn.vpnzero.util.PropertiesService;

import com.dn.vpnzero.util.TotalTraffic;
import com.dn.vpnzero.util.iap.IabHelper;
import com.dn.vpnzero.util.iap.IabResult;
import com.dn.vpnzero.util.iap.Inventory;
import com.dn.vpnzero.util.iap.Purchase;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

import de.blinkt.openvpn.core.VpnStatus;

import static com.dn.vpnzero.activity.ServerActivity.BROADCAST_ACTION;

/**
 * Created by Kusenko on 20.10.2016.
 */

public abstract class BaseActivity extends AppCompatActivity {

    private DrawerLayout fullLayout;
    //private Toolbar toolbar;

    static final int ADBLOCK_REQUEST = 10001;
    static final int PREMIUM_SERVERS_REQUEST = 10002;
    public static Server connectedServer = null;
    public boolean hideCurrentConnection = false;
    IabHelper iapHelper;
    public static final String IAP_TAG = "IAP";
    static final String TEST_ITEM_SKU = "android.test.purchased";
    static final String ADBLOCK_ITEM_SKU = "adblock";
    static final String MORE_SERVERS_ITEM_SKU = "more_servers";
    static String key = "";

    static boolean availableFilterAds = false;
    static boolean premiumServers = false;

    static String adblockSKU;
    static String moreServersSKU;
    static String currentSKU;

    public int widthWindow;
    public int heightWindow;

    public static DBHelper dbHelper;
    public Map<String, String> localeCountries;
    BroadcastReceiver br;

    static Tracker mTracker;

    @Override
    public void setContentView(int layoutResID)
    {
        if (BuildConfig.FLAVOR == "pro" || BuildConfig.FLAVOR == "underground") {
            availableFilterAds = true;
            premiumServers = true;
        }

        fullLayout = (DrawerLayout) getLayoutInflater().inflate(R.layout.activity_base, null);
        FrameLayout activityContainer = (FrameLayout) fullLayout.findViewById(R.id.activity_content);
        getLayoutInflater().inflate(layoutResID, activityContainer, true);
        super.setContentView(fullLayout);

        //toolbar = (Toolbar) findViewById(R.id.toolbar);

        if (useToolbar()) {
            //setSupportActionBar(toolbar);
        } else {
            //toolbar.setVisibility(View.GONE);
        }

        if (useHomeButton()) {
            if (getSupportActionBar() != null){
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setDisplayShowHomeEnabled(true);
            }
        }

        if (BuildConfig.DEBUG) {
            moreServersSKU = TEST_ITEM_SKU;
            adblockSKU = TEST_ITEM_SKU;
        } else {
            moreServersSKU = MORE_SERVERS_ITEM_SKU;
            adblockSKU = ADBLOCK_ITEM_SKU;
        }



        dbHelper = new DBHelper(this);

        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);

        widthWindow = dm.widthPixels;
        heightWindow = dm.heightPixels;

        localeCountries = CountriesNames.getCountries();

        App application = (App) getApplication();
        mTracker = application.getDefaultTracker();
    }

    @Override
    protected void onPause() {
        super.onPause();
        TotalTraffic.saveTotal();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (iapHelper != null) iapHelper.dispose();
        iapHelper = null;
    }

    private void initPurchaseHelper() {
        if (iapHelper == null) {
            iapHelper = new IabHelper(this, getString(R.string.base64EncodedPublicKey));
            iapHelper.enableDebugLogging(BuildConfig.DEBUG);

            iapHelper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
                @Override
                public void onIabSetupFinished(IabResult result) {
                    if (result.isSuccess()) {
                        // Have we been disposed of in the meantime? If so, quit.
                        if (iapHelper == null) return;

                        // IAB is fully set up. Now, let's get an inventory of stuff we own.
                        Log.d(IAP_TAG, "Setup successful. Querying inventory.");

                        checkPurchase();
                    } else {
                        Log.d(IAP_TAG, "Oh noes, there was a problem.");
                    }
                }
            });
        }
    }

    private void checkPurchase() {
        iapHelper.flagEndAsync();
        if(iapHelper.isSetupDone() && !iapHelper.isAsyncInProgress() && !iapHelper.isDisposed()) {
            iapHelper.queryInventoryAsync(mGotInventoryListener);
        }
    }

    void launchPurchase(String sku, int request) {
        currentSKU = sku;
        String base64EncodedPublicKey = getString(R.string.base64EncodedPublicKey);
        Random random = new Random();
        key = base64EncodedPublicKey.substring(random.nextInt(base64EncodedPublicKey.length() - 2));

        iapHelper.flagEndAsync();
        if (iapHelper.isSetupDone() && !iapHelper.isAsyncInProgress() && !iapHelper.isDisposed()) {
            iapHelper.launchPurchaseFlow(this,
                    sku, request,
                    mPurchaseFinishedListener,
                    key + sku);
        }
    }


    /** Verifies the developer payload of a purchase. */
    boolean verifyDeveloperPayload(Purchase p, String sku) {
        String responsePayload = p.getDeveloperPayload();
        String computedPayload = key + sku;

        return responsePayload != null && responsePayload.equals(computedPayload);
    }

    IabHelper.QueryInventoryFinishedListener mGotInventoryListener = new IabHelper.QueryInventoryFinishedListener() {
        @Override
        public void onQueryInventoryFinished(IabResult result, Inventory inventory) {
            if (result.isFailure()) {
                Log.d(IAP_TAG, "Purchase finished: " + result + ", purchase: " + inventory);
            } else {
                if (inventory.hasPurchase(adblockSKU)) {
                    availableFilterAds = true;
                }
                if (inventory.hasPurchase(moreServersSKU)) {
                    premiumServers = true;
                }
            }
        }
    };

    IabHelper.OnIabPurchaseFinishedListener mPurchaseFinishedListener = new IabHelper.OnIabPurchaseFinishedListener() {
        @Override
        public void onIabPurchaseFinished(IabResult result, Purchase purchase) {
            if (result.isFailure()) {
                Log.d(IAP_TAG, "Oh noes, there was a problem.");
                return;
            } else {
                if (purchase.getSku().equals(currentSKU) && verifyDeveloperPayload(purchase, currentSKU)) {
                    availableFilterAds = true;
                }
            }
        }
    };

    protected boolean useToolbar()
    {
        return true;
    }

    protected boolean useHomeButton()
    {
        return true;
    }

    protected boolean useMenu()
    {
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (BuildConfig.FLAVOR == "free") {
            initPurchaseHelper();
        }

        mTracker.setScreenName(getClass().getSimpleName());
        mTracker.send(new HitBuilders.ScreenViewBuilder().build());

        if (!BuildConfig.DEBUG)
            Answers.getInstance().logCustom(new CustomEvent("Viewed activity")
                    .putCustomAttribute("activity", getClass().getSimpleName()));
    }

    /*@Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);

        for (int i = 0; i < menu.size(); i++) {
            if (menu.getItem(i).getItemId() == R.id.actionCurrentServer
                    && (connectedServer == null || hideCurrentConnection || !VpnStatus.isVPNActive()))
                menu.getItem(i).setVisible(false);

            if (premiumServers && menu.getItem(i).getItemId() == R.id.actionGetMoreServers)
                menu.getItem(i).setTitle(getString(R.string.current_servers_list));

            if (BuildConfig.FLAVOR == "underground" && menu.getItem(i).getItemId() == R.id.actionShare)
                menu.getItem(i).setVisible(false);
        }

        return useMenu();
    }*/

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            case R.id.actionRefresh:
                startActivity(new Intent(getApplicationContext(), LoaderActivity.class));
                finish();
                return true;
            case R.id.actionAbout:
                startActivity(new Intent(getApplicationContext(), AboutActivity.class));
                return true;
            case R.id.actionShare:
                sendTouchButton("Share");
                Intent sendIntent = new Intent();
                sendIntent.setAction(Intent.ACTION_SEND);
                sendIntent.putExtra(Intent.EXTRA_TEXT, getString(R.string.share_text));
                sendIntent.setType("text/plain");
                startActivity(sendIntent);
                return true;
            case R.id.actionCurrentServer:
                if (connectedServer != null)
                    startActivity(new Intent(this, ServerActivity.class));
                return true;
            case R.id.actionGetMoreServers:
                if (premiumServers) {
                    startActivity(new Intent(this, ServersInfo.class));
                } else {
                    sendTouchButton("GetMoreServers");
                    launchPurchase(moreServersSKU, PREMIUM_SERVERS_REQUEST);
                }
                return true;
            case R.id.action_settings:
                sendTouchButton("Settings");
                startActivity(new Intent(this, MyPreferencesActivity.class));
                return true;
            case R.id.action_bookmarks:
                startActivity(new Intent(this, BookmarkServerListActivity.class));
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case PREMIUM_SERVERS_REQUEST:
                    Log.d(IAP_TAG, "onActivityResult(" + requestCode + "," + resultCode + "," + data);

                    if (iapHelper == null) return;

                    if (iapHelper.handleActivityResult(requestCode, resultCode, data)) {
                        Log.d(IAP_TAG, "onActivityResult handled by IABUtil.");
                        Intent intent = new Intent(getApplicationContext(), LoaderActivity.class);
                        intent.putExtra("firstPremiumLoad", true);
                        startActivity(intent);
                        finish();
                    }
                    break;
            }
        }
    }

    public Server getRandomServer() {
        Server randomServer;
        if (PropertiesService.getCountryPriority()) {
            randomServer = dbHelper.getGoodRandomServer(PropertiesService.getSelectedCountry());
        } else {
            randomServer = dbHelper.getGoodRandomServer(null);
        }
        return randomServer;
    }

    public void newConnecting(Server server, boolean fastConnection, boolean autoConnection) {
        if (server != null) {
            Intent intent = new Intent(this, ServerActivity.class);
            intent.putExtra(Server.class.getCanonicalName(), server);
            intent.putExtra("fastConnection", fastConnection);
            intent.putExtra("autoConnection", autoConnection);
            startActivity(intent);
        }
    }

    public static void sendTouchButton(String button) {
        if (!BuildConfig.DEBUG)
            Answers.getInstance().logCustom(new CustomEvent("Touches buttons")
                .putCustomAttribute("Button", button));

        mTracker.send(new HitBuilders.EventBuilder()
            .setCategory("Touches buttons")
            .setAction(button)
            .build());
    }

    protected void ipInfoResult() {}

    protected void getIpInfo(Server server) {
        List<Server> serverList = new ArrayList<Server>();
        serverList.add(server);
        getIpInfo(serverList);
    }

    protected void getIpInfo(final List<Server> serverList) {
        JSONArray jsonArray = new JSONArray();

        for (Server server : serverList) {
            JSONObject jsonObject = new JSONObject();
            try {
                jsonObject.put("query", server.getIp());
                jsonObject.put("lang", Locale.getDefault().getLanguage());

                jsonArray.put(jsonObject);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        AndroidNetworking.post(getString(R.string.url_check_ip_batch))
                .addJSONArrayBody(jsonArray)
                .setTag("getIpInfo")
                .setPriority(Priority.MEDIUM)
                .build()
                .getAsJSONArray(new JSONArrayRequestListener() {
                    @Override
                    public void onResponse(JSONArray response) {
                        if (dbHelper.setIpInfo(response, serverList))
                            ipInfoResult();
                    }
                    @Override
                    public void onError(ANError error) {

                    }
                });
    }

    public void write(String name, String value) {
        SharedPreferences.Editor e = getSharedPreferences("data", MODE_PRIVATE).edit();
        e.putString(name, value);
        e.commit();
    }

    public String read(String name, String defaultValue) {
        SharedPreferences sp = getSharedPreferences("data", MODE_PRIVATE);
        return sp.getString(name, defaultValue);
    }

    public void write(String name, boolean b) {
        SharedPreferences.Editor e = getSharedPreferences("data", MODE_PRIVATE).edit();
        e.putBoolean(name, b);
        e.commit();
    }

    public boolean read(String name, boolean defaultValue) {
        SharedPreferences sp = getSharedPreferences("data", MODE_PRIVATE);
        return sp.getBoolean(name, defaultValue);
    }

    public void write(String name, int value) {
        SharedPreferences.Editor e = getSharedPreferences("data", Context.MODE_PRIVATE).edit();
        e.putInt(name, value);
        e.commit();
    }

    public int read(String name, int defaultValue) {
        SharedPreferences sp = getSharedPreferences("data", Context.MODE_PRIVATE);
        return sp.getInt(name, defaultValue);
    }
}

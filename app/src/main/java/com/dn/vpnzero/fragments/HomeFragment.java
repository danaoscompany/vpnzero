package com.dn.vpnzero.fragments;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.net.VpnService;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomSheetBehavior;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Base64;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.CustomEvent;
import com.dn.vpnzero.Util;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.squareup.picasso.Picasso;
import com.dn.vpnzero.BuildConfig;
import com.dn.vpnzero.R;
import com.dn.vpnzero.activity.BaseActivity;
import com.dn.vpnzero.activity.HomeActivity;
import com.dn.vpnzero.activity.ServersListActivity;
import com.dn.vpnzero.adapter.CountryAdapter;
import com.dn.vpnzero.model.Country;
import com.dn.vpnzero.model.Server;
import com.dn.vpnzero.util.BitmapGenerator;
import com.dn.vpnzero.util.ConnectionQuality;
import com.dn.vpnzero.util.LoadData;
import com.dn.vpnzero.util.PropertiesService;
import com.dn.vpnzero.util.map.MapCreator;
import com.dn.vpnzero.util.map.MyMarker;
import com.startapp.android.publish.adsCommon.StartAppAd;

import org.mapsforge.core.graphics.Bitmap;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.Point;
import org.mapsforge.map.android.graphics.AndroidGraphicFactory;
import org.mapsforge.map.android.view.MapView;
import org.mapsforge.map.layer.Layers;
import org.mapsforge.map.layer.overlay.Marker;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import de.blinkt.openvpn.VpnProfile;
import de.blinkt.openvpn.core.ConfigParser;
import de.blinkt.openvpn.core.OpenVPNService;
import de.blinkt.openvpn.core.ProfileManager;
import de.blinkt.openvpn.core.VPNLaunchHelper;
import de.blinkt.openvpn.core.VpnStatus;

import static com.dn.vpnzero.activity.ServerActivity.BROADCAST_ACTION;

public class HomeFragment extends Fragment {
    private static final int START_VPN_PROFILE = 70;
    View view;
    HomeActivity activity;
    private MapView mapView;
    private PopupWindow popupWindow;
    private RelativeLayout homeContextRL;
    ProgressBar progress;
    RecyclerView countryList;
    private ArrayList<Server> countries;
    LinearLayoutCompat countriesContainer;
    CountryAdapter countryAdapter;
    ImageView countryFlagView;
    TextView countryNameView;
    RelativeLayout connect;
    ImageView connectIcon;
    RelativeLayout selectCountry;
    View overlay;
    private final String COUNTRY_FILE_NAME = "countries.json";
    Server selectedCountry = null;
    BottomSheetBehavior bottomSheetBehavior;
    private List<Country> countryLatLonList = null;
    private Layers layers;
    private List<Marker> markerList;
    TextView statusView;
    RelativeLayout openMenu;
    BroadcastReceiver br;
    BroadcastReceiver br2;
    private Server currentServer = null;
    private int connectionStatus = 0;
    private WaitConnectionAsync waitConnection;
    boolean fastConnection = false;
    boolean autoConnection = false;
    private static OpenVPNService mVPNService;
    boolean isBindedService = false;
    private VpnProfile vpnProfile;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        setRetainInstance(true);
        Util.log("onCreateView()");
        view = inflater.inflate(R.layout.fragment_home, container, false);
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Util.log("onActivityCreated()");
        Util.log("savedInstanceState is null? "+(savedInstanceState == null));
        activity = (HomeActivity)getActivity();
        homeContextRL = (RelativeLayout) view.findViewById(R.id.homeContextRL);
        connect = (RelativeLayout)view.findViewById(R.id.connect);
        connectIcon = (ImageView)view.findViewById(R.id.turn_icon);
        selectCountry = (RelativeLayout)view.findViewById(R.id.select_country);
        countries = BaseActivity.dbHelper.getUniqueCountries();
        countryFlagView = (ImageView)view.findViewById(R.id.country_flag);
        countryNameView = (TextView)view.findViewById(R.id.country_name);
        overlay = (View)view.findViewById(R.id.overlay);
        countryList = (RecyclerView)view.findViewById(R.id.countries);
        countriesContainer = (LinearLayoutCompat)view.findViewById(R.id.countries_container);
        openMenu = (RelativeLayout)view.findViewById(R.id.open_menu);
        statusView = (TextView)view.findViewById(R.id.status);
        progress = (ProgressBar)view.findViewById(R.id.progress);
        if (savedInstanceState == null) {
            openMenu.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View view) {
                    activity.openMenu();
                }
            });
            selectCountry.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View view) {
                    overlay.setVisibility(View.VISIBLE);
                    bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                }
            });
            countryList.setLayoutManager(new LinearLayoutManager(activity));
            countryList.setItemAnimator(new DefaultItemAnimator());
            bottomSheetBehavior = BottomSheetBehavior.from(countriesContainer);
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
            bottomSheetBehavior.setBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {

                @Override
                public void onStateChanged(@NonNull View bottomSheet, int newState) {
                    if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                        overlay.setVisibility(View.GONE);
                    } else {
                        overlay.setVisibility(View.VISIBLE);
                    }
                }

                @Override
                public void onSlide(@NonNull View bottomSheet, float slideOffset) {
                }
            });
            countryAdapter = new CountryAdapter(activity, countries, new CountryAdapter.Listener() {

                @Override
                public void onCountrySelected(Server server) {
                    selectedCountry = server;
                    bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
                    overlay.setVisibility(View.GONE);
                    if (!selectedCountry.getCountryShort().trim().equals("")) {
                        String flagURL = "https://www.countryflags.io/" + selectedCountry.getCountryShort().toLowerCase().trim() + "/shiny/64.png";
                        Picasso.get().load(Uri.parse(flagURL)).into(countryFlagView);
                    } else {
                        countryFlagView.setImageResource(0);
                    }
                    countryNameView.setText(selectedCountry.getCountryLong());
                }
            });
            overlay.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
                    overlay.setVisibility(View.GONE);
                }
            });
            countryList.setAdapter(countryAdapter);
            statusView.setText(R.string.text4);
            br = new BroadcastReceiver() {

                @Override
                public void onReceive(Context context, Intent intent) {
                    String status = intent.getStringExtra("status");
                    if (status != null) {
                        if (status.equals(VpnStatus.ConnectionStatus.LEVEL_CONNECTED)) {
                            connectIcon.setImageResource(R.drawable.turn_off);
                        } else if (status.equals(VpnStatus.ConnectionStatus.LEVEL_AUTH_FAILED)) {
                            connectIcon.setImageResource(R.drawable.turn);
                        } else if (status.equals(VpnStatus.ConnectionStatus.LEVEL_CONNECTING_NO_SERVER_REPLY_YET)) {
                            connectIcon.setImageResource(R.drawable.turn);
                        } else if (status.equals(VpnStatus.ConnectionStatus.LEVEL_CONNECTING_SERVER_REPLIED)) {
                            connectIcon.setImageResource(R.drawable.turn);
                        } else if (status.equals(VpnStatus.ConnectionStatus.LEVEL_NONETWORK)) {
                            connectIcon.setImageResource(R.drawable.turn);
                        } else if (status.equals(VpnStatus.ConnectionStatus.LEVEL_NOTCONNECTED)) {
                            connectIcon.setImageResource(R.drawable.turn);
                        } else if (status.equals(VpnStatus.ConnectionStatus.LEVEL_START)) {
                            connectIcon.setImageResource(R.drawable.turn);
                        } else if (status.equals(VpnStatus.ConnectionStatus.LEVEL_VPNPAUSED)) {
                            connectIcon.setImageResource(R.drawable.turn);
                        } else if (status.equals(VpnStatus.ConnectionStatus.LEVEL_WAITING_FOR_USER_INPUT)) {
                            connectIcon.setImageResource(R.drawable.turn);
                        }
                    }
                }
            };
            activity.registerReceiver(br, new IntentFilter("de.blinkt.openvpn.VPN_STATUS"));
            connect.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View view) {
                    if (!isBindedService) {
                        Util.show(activity, R.string.text26);
                        return;
                    }
                    if (selectedCountry != null) {
                        currentServer = selectedCountry;
                        mVPNService.currentServer = currentServer;
                        activity.connectedServer = currentServer;
                        Util.log("Current OpenVPN server (2): " + mVPNService.currentServer);
                        if (connectionStatus == 1 || connectionStatus == 2) {
                            //if (checkStatus()) {
                            stopVpn();
                            //}
                            connectionStatus = 0;
                            connectIcon.setImageResource(R.drawable.turn);
                            statusView.setText(R.string.text4);
                            progress.setVisibility(View.GONE);
                        } else {
                            StartAppAd.showAd(activity);
                            statusView.setText(R.string.text5);
                            progress.setVisibility(View.VISIBLE);
                            activity.sendTouchButton("homeBtnRandomConnection");
                            connect(selectedCountry, true, true);
                            connectionStatus = 1;
                        }
                    } else {
                        String randomError = getResources().getString(R.string.error_random_country);
                        Toast.makeText(activity, randomError, Toast.LENGTH_LONG).show();
                    }
                }
            });
            selectedCountry = activity.getRandomServer();
            countryNameView.setText(selectedCountry.getCountryLong());
            if (!selectedCountry.getCountryShort().toLowerCase().trim().equals("")) {
                String flagURL = "https://www.countryflags.io/" + selectedCountry.getCountryShort().toLowerCase() + "/shiny/64.png";
                Picasso.get().load(Uri.parse(flagURL)).into(countryFlagView);
            }
            long totalServ = BaseActivity.dbHelper.getCount();
            if (!BuildConfig.DEBUG)
                Answers.getInstance().logCustom(new CustomEvent("Total servers")
                        .putCustomAttribute("Total servers", totalServ));
            String totalServers = String.format(getResources().getString(R.string.total_servers), totalServ);
            ((TextView) view.findViewById(R.id.homeTotalServers)).setText(totalServers);
            initMap();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        activity.invalidateOptionsMenu();

        initDetailsServerOnMap();

        if (PropertiesService.getShowNote()) {
            /*homeContextRL.post(new Runnable() {
                @Override
                public void run() {
                    showNote();
                }
            });*/
        }
        Intent intent = new Intent(activity, OpenVPNService.class);
        intent.setAction(OpenVPNService.START_SERVICE);
        isBindedService = activity.bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
    }

    private void prepareVpn() {
        if (loadVpnProfile()) {
            Util.log("Load profile success");
            waitConnection = new WaitConnectionAsync();
            waitConnection.execute();
            startVpn();
        } else {
            Util.log("Load profile failed");
            Toast.makeText(activity, getString(R.string.server_error_loading_profile), Toast.LENGTH_SHORT).show();
        }
    }

    private void startVpn() {
        activity.connectedServer = currentServer;
        activity.hideCurrentConnection = true;
        Intent intent = VpnService.prepare(activity);
        if (intent != null) {
            VpnStatus.updateStateString("USER_VPN_PERMISSION", "", R.string.state_user_vpn_permission,
                    VpnStatus.ConnectionStatus.LEVEL_WAITING_FOR_USER_INPUT);
            try {
                startActivityForResult(intent, START_VPN_PROFILE);
            } catch (ActivityNotFoundException ane) {
                // Shame on you Sony! At least one user reported that
                // an official Sony Xperia Arc S image triggers this exception
                VpnStatus.logError(R.string.no_vpn_support_image);
            }
        } else {
            onActivityResult(START_VPN_PROFILE, Activity.RESULT_OK, null);
        }
    }

    private boolean loadVpnProfile() {
        Util.log("Current country: "+currentServer.getCountryLong());
        Util.log("Current config data: "+currentServer.getConfigData());
        byte[] data;
        try {
            data = Base64.decode(currentServer.getConfigData(), Base64.DEFAULT);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        ConfigParser cp = new ConfigParser();
        InputStreamReader isr = new InputStreamReader(new ByteArrayInputStream(data));
        try {
            cp.parseConfig(isr);
            vpnProfile = cp.convertProfile();
            vpnProfile.mName = currentServer.getCountryLong();
            /*if (filterAds) {
                vpnProfile.mOverrideDNS = true;
                vpnProfile.mDNS1 = "198.101.242.72";
                vpnProfile.mDNS2 = "23.253.163.53";
            }*/
            ProfileManager.getInstance(activity).addProfile(vpnProfile);
        } catch (IOException | ConfigParser.ConfigParseError e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    @Override
    public void onDestroy() {
        mapView.destroyAll();
        AndroidGraphicFactory.clearResourceMemoryCache();
        super.onDestroy();
    }

    private void initMap() {
        AndroidGraphicFactory.createInstance(activity.getApplication());
        mapView = new MapView(activity);
        mapView.setClickable(true);
        mapView.getMapScaleBar().setVisible(false);
        mapView.setBuiltInZoomControls(false);
        mapView.setZoomLevelMin((byte) 2);
        mapView.setZoomLevelMax((byte) 10);

        mapView.setZoomLevel((byte) 2);
        mapView.getModel().displayModel.setBackgroundColor(ContextCompat.getColor(activity, R.color.mapBackground));

        layers = mapView.getLayerManager().getLayers();

        MapCreator mapCreator = new MapCreator(activity, layers);
        mapCreator.parseGeoJson("world_map.geo.json");

        initServerOnMap(layers);

        LinearLayout map = (LinearLayout)view.findViewById(R.id.map);
        map.addView(mapView);
    }

    public void homeOnClick(View view) {
        switch (view.getId()) {
            case R.id.homeBtnChooseCountry:
                activity.sendTouchButton("homeBtnChooseCountry");
                chooseCountry();
                break;
            case R.id.homeBtnRandomConnection:
                activity.sendTouchButton("homeBtnRandomConnection");
                if (selectedCountry != null) {
                    connect(selectedCountry, true, true);
                } else {
                    String randomError = String.format(getResources().getString(R.string.error_random_country), PropertiesService.getSelectedCountry());
                    Toast.makeText(activity, randomError, Toast.LENGTH_LONG).show();
                }
                break;
        }

    }

    public void connect(Server server, boolean fastConnection, boolean autoConnection) {
        currentServer = server;
        this.fastConnection = fastConnection;
        this.autoConnection = autoConnection;
        br2 = new BroadcastReceiver() {

            @Override
            public void onReceive(Context context, Intent intent) {
                if (checkStatus()) {
                    String statusText = intent.getStringExtra("status");
                    Util.log("Status: "+statusText);
                    VpnStatus.ConnectionStatus status = VpnStatus.ConnectionStatus.valueOf(statusText);
                    switch (status) {
                        case LEVEL_CONNECTED:
                            connectionStatus = 1;
                            connectIcon.setImageResource(R.drawable.turn_off);
                            statusView.setText(R.string.text6);
                            progress.setVisibility(View.GONE);
                            break;
                        case LEVEL_NOTCONNECTED:
                            connectionStatus = 0;
                            connectIcon.setImageResource(R.drawable.turn);
                            statusView.setText(R.string.text4);
                            progress.setVisibility(View.GONE);
                            break;
                        case LEVEL_AUTH_FAILED:
                            connectionStatus = 0;
                            connectIcon.setImageResource(R.drawable.turn);
                            statusView.setText(R.string.text4);
                            progress.setVisibility(View.GONE);
                            break;
                        case LEVEL_CONNECTING_NO_SERVER_REPLY_YET:
                            connectionStatus = 1;
                            connectIcon.setImageResource(R.drawable.turn);
                            statusView.setText(R.string.text5);
                            progress.setVisibility(View.VISIBLE);
                            break;
                        case LEVEL_WAITING_FOR_USER_INPUT:
                            connectionStatus = 0;
                            connectIcon.setImageResource(R.drawable.turn);
                            progress.setVisibility(View.GONE);
                            break;
                        case LEVEL_VPNPAUSED:
                            connectionStatus = 0;
                            connectIcon.setImageResource(R.drawable.turn);
                            statusView.setText(R.string.text4);
                            progress.setVisibility(View.GONE);
                            break;
                        case UNKNOWN_LEVEL:
                            break;
                        case LEVEL_NONETWORK:
                            connectionStatus = 0;
                            connectIcon.setImageResource(R.drawable.turn);
                            statusView.setText(R.string.text4);
                            progress.setVisibility(View.GONE);
                            break;
                        case LEVEL_CONNECTING_SERVER_REPLIED:
                            connectionStatus = 1;
                            connectIcon.setImageResource(R.drawable.turn);
                            statusView.setText(R.string.text5);
                            progress.setVisibility(View.VISIBLE);
                            break;
                        case LEVEL_START:
                            connectionStatus = 0;
                            connectIcon.setImageResource(R.drawable.turn);
                            statusView.setText(R.string.text5);
                            progress.setVisibility(View.VISIBLE);
                            break;
                        default:
                            connectionStatus = 0;
                            connectIcon.setImageResource(R.drawable.turn);
                            statusView.setText(R.string.text4);
                            progress.setVisibility(View.GONE);
                            break;
                    }
                }
                if (intent.getStringExtra("detailstatus").equals("NOPROCESS")) {
                    try {
                        if (!VpnStatus.isVPNActive()) {
                            prepareStopVPN();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        };
        activity.registerReceiver(br2, new IntentFilter(BROADCAST_ACTION));
        if (currentServer == null) {
            if (activity.connectedServer != null) {
                currentServer = activity.connectedServer;
            }
        }
        if (checkStatus()) {
            Util.log("VPN is connected");
            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (!checkStatus()) {
                activity.connectedServer = null;
            }
        } else {
            Util.log("VPN is not connected, auto connection: "+autoConnection);
            if (autoConnection) {
                prepareVpn();
            }
        }
    }

    private void prepareStopVPN() {
        connectionStatus = 0;
        if (waitConnection != null) {
            waitConnection.cancel(false);
        }
        activity.connectedServer = null;
    }

    private boolean checkStatus() {
        Util.log("activity.connectedServer != null? "+(activity.connectedServer != null));
        if (activity.connectedServer != null) {
            Util.log("activity.connectedServer.getHostName().equals(currentServer.getHostName())? "+(activity.connectedServer.getHostName().equals(currentServer.getHostName())));
            if (activity.connectedServer.getHostName().equals(currentServer.getHostName())) {
                Util.log("Is VPN active: " + VpnStatus.isVPNActive());
                return VpnStatus.isVPNActive();
            }
        }
        return false;
    }

    private void chooseCountry() {
        View view = initPopUp(R.layout.pop_up_choose_country, 0.6f, 0.8f, 0.8f, 0.7f);

        final List<String> countryListName = new ArrayList<String>();
        for (Server server : countries) {
            String localeCountryName = activity.localeCountries.get(server.getCountryShort()) != null ?
                    activity.localeCountries.get(server.getCountryShort()) : server.getCountryLong();
            countryListName.add(localeCountryName);
        }

        ListView lvCountry = (ListView) view.findViewById(R.id.homeCountryList);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(activity, android.R.layout.simple_list_item_1, countryListName);
        lvCountry.setAdapter(adapter);
        lvCountry.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                popupWindow.dismiss();
                onSelectCountry(countries.get(position));
            }
        });

        popupWindow.showAtLocation(homeContextRL, Gravity.CENTER, 0, 0);
    }

    private void showNote() {
        View view = initPopUp(R.layout.pop_up_note, 0.6f, 0.5f, 0.9f, 0.4f);
        ((TextView) view.findViewById(R.id.noteLink)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent in = new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.vpngate.net/en/join.aspx"));
                startActivity(in);
            }
        });

        popupWindow.showAtLocation(homeContextRL, Gravity.CENTER, 0, 0);

        PropertiesService.setShowNote(false);
    }

    private View initPopUp(int resourse,
                           float landPercentW,
                           float landPercentH,
                           float portraitPercentW,
                           float portraitPercentH) {

        LayoutInflater inflater = (LayoutInflater) activity.getApplicationContext()
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(resourse, null);

        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            popupWindow = new PopupWindow(
                    view,
                    (int) (activity.widthWindow * landPercentW),
                    (int) (activity.heightWindow * landPercentH)
            );
        } else {
            popupWindow = new PopupWindow(
                    view,
                    (int) (activity.widthWindow * portraitPercentW),
                    (int) (activity.heightWindow * portraitPercentH)
            );
        }


        popupWindow.setOutsideTouchable(false);
        popupWindow.setFocusable(true);
        popupWindow.setBackgroundDrawable(new BitmapDrawable());

        return view;
    }

    private void onSelectCountry(Server server) {
        Intent intent = new Intent(activity.getApplicationContext(), ServersListActivity.class);
        intent.putExtra(HomeActivity.EXTRA_COUNTRY, server.getCountryShort());
        startActivity(intent);
    }

    private void initDetailsServerOnMap() {
        if (markerList != null && markerList.size() > 0) {
            for (Marker marker : markerList) {
                layers.remove(marker);
            }
        }
        List<Server> serverList = BaseActivity.dbHelper.getServersWithGPS();

        markerList = new ArrayList<Marker>();
        for (Server server : serverList) {
            LatLong position = new LatLong(server.getLat(), server.getLon());
            Bitmap bitmap = AndroidGraphicFactory.convertToBitmap(ContextCompat.getDrawable(activity,
                    getResources().getIdentifier(ConnectionQuality.getSimplePointIcon(server.getQuality()),
                            "drawable",
                            activity.getPackageName())));
            Marker serverMarker = new Marker(position, bitmap, 0, 0);
            markerList.add(serverMarker);
            layers.add(serverMarker);
        }
    }

    private void initServerOnMap(Layers layers) {
        Type listType = new TypeToken<ArrayList<Country>>() {
        }.getType();
        countryLatLonList = new Gson().fromJson(LoadData.fromFile(COUNTRY_FILE_NAME, activity), listType);

        for (Server server : countries) {
            for (Country country : countryLatLonList) {
                if (server.getCountryShort().equals(country.getCountryCode())) {
                    LatLong position = new LatLong(country.getCapitalLatitude(), country.getCapitalLongitude());
                    Bitmap bitmap = AndroidGraphicFactory.convertToBitmap(ContextCompat.getDrawable(activity,
                            getResources().getIdentifier(ConnectionQuality.getPointIcon(server.getQuality()),
                                    "drawable",
                                    activity.getPackageName())));

                    MyMarker countryMarker = new MyMarker(position, bitmap, 0, 0, server) {
                        @Override
                        public boolean onTap(LatLong geoPoint, Point viewPosition,
                                             Point tapPoint) {

                            if (contains(viewPosition, tapPoint)) {
                                onSelectCountry((Server) getRelationObject());
                                return true;
                            }
                            return false;
                        }
                    };

                    layers.add(countryMarker);


                    String localeCountryName = activity.localeCountries.get(country.getCountryCode()) != null ?
                            activity.localeCountries.get(country.getCountryCode()) : country.getCountryName();

                    Drawable drawable = new BitmapDrawable(getResources(),
                            BitmapGenerator.getTextAsBitmap(localeCountryName, 20, ContextCompat.getColor(activity, R.color.mapNameCountry)));
                    Bitmap bitmapName = AndroidGraphicFactory.convertToBitmap(drawable);

                    Marker countryNameMarker = new Marker(position, bitmapName, 0, bitmap.getHeight() / 2);

                    layers.add(countryNameMarker);
                }
            }
        }
    }

    private class WaitConnectionAsync extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {
            try {
                TimeUnit.SECONDS.sleep(PropertiesService.getAutomaticSwitchingSeconds());
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            if (connectionStatus == 0) {
                if (currentServer != null)
                    activity.dbHelper.setInactive(currentServer.getIp());
                if (fastConnection) {
                    stopVpn();
                    connect(activity.getRandomServer(), true, true);
                } else if (PropertiesService.getAutomaticSwitching()){
                }
            }
        }
    }

    private void stopVpn() {
        //prepareStopVPN();
        ProfileManager.setConntectedVpnProfileDisconnected(activity);
        if (mVPNService != null && mVPNService.getManagement() != null) {
            mVPNService.getManagement().stopVPN(false);
        }
    }

    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            OpenVPNService.LocalBinder binder = (OpenVPNService.LocalBinder) service;
            mVPNService = binder.getService();
            Util.log("Current OpenVPN server: "+mVPNService.currentServer);
            if (mVPNService.currentServer == null) {
                mVPNService.currentServer = currentServer;
            } else {
                currentServer = mVPNService.currentServer;
            }
            activity.connectedServer = currentServer;
            if (activity.connectedServer != null) {
                countryNameView.setText(activity.connectedServer.getCountryLong());
                String flagURL = "https://www.countryflags.io/" + activity.connectedServer.getCountryShort().toLowerCase().trim() + "/shiny/64.png";
                Picasso.get().load(Uri.parse(flagURL)).into(countryFlagView);
                connectionStatus = 2;
            }
            boolean status = checkStatus();
            Util.log("Status: "+status);
            if (status) {
                connectIcon.setImageResource(R.drawable.turn_off);
                statusView.setText(R.string.text6);
                try {
                    TimeUnit.SECONDS.sleep(1);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if (!checkStatus()) {
                    connectIcon.setImageResource(R.drawable.turn);
                    statusView.setText(R.string.text4);
                    activity.connectedServer = null;
                }
            } else {
                connectIcon.setImageResource(R.drawable.turn);
                statusView.setText(R.string.text4);
                if (autoConnection) {
                    prepareVpn();
                }
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mVPNService = null;
        }
    };

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            switch (requestCode) {
                case START_VPN_PROFILE:
                    VPNLaunchHelper.startOpenVpn(vpnProfile, activity);
                    break;
            }
        }
    }

    public boolean onBackPressed() {
        if (bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_HIDDEN) {
            return true;
        } else {
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
            return false;
        }
    }
}

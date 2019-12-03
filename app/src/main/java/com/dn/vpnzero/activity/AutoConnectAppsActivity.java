package com.dn.vpnzero.activity;

import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.dn.vpnzero.R;
import com.dn.vpnzero.Util;
import com.dn.vpnzero.adapter.ApplicationAdapter;
import com.dn.vpnzero.items.Application;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class AutoConnectAppsActivity extends AppCompatActivity {
    RecyclerView appList;
    ArrayList<Application> applications;
    ApplicationAdapter adapter;
    ArrayList<Application> autoConnectApps;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auto_connect_apps);
        setTitle(R.string.text24);
        Toolbar toolbar = (Toolbar)findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        appList = (RecyclerView)findViewById(R.id.apps);
        appList.setLayoutManager(new LinearLayoutManager(this));
        appList.setItemAnimator(new DefaultItemAnimator());
        applications = new ArrayList<>();
        adapter = new ApplicationAdapter(this, applications);
        appList.setAdapter(adapter);
        Gson gson = new Gson();
        autoConnectApps = gson.fromJson(Util.read(this, "auto_connect_apps", "").trim(),
                new TypeToken<List<Application>>(){}.getType());
        if (autoConnectApps == null) {
            autoConnectApps = new ArrayList<>();
        }
        getApps();
    }

    public void getApps() {
        try {
            Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
            mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
            List<ResolveInfo> packages = getPackageManager().queryIntentActivities(mainIntent, 0);
            for (int i=0; i<packages.size(); i++) {
                Application application = new Application();
                String packageName = packages.get(i).activityInfo.packageName;
                application.setName(getAppName(packageName));
                application.setIcon(getAppIcon(packageName));
                application.setPackageName(packageName);
                boolean autoStart = isAppAutoConnect(packageName);
                application.setAutoStart(autoStart);
                application.setChecked(autoStart);
                applications.add(application);
            }
            adapter.notifyDataSetChanged();
            Collections.sort(applications, new Comparator<Application>() {

                @Override
                public int compare(Application application1, Application application2) {
                    return application1.getName().compareTo(application2.getName());
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean isAppAutoConnect(String packageName) {
        for (int i=0; i<autoConnectApps.size(); i++) {
            if (autoConnectApps.get(i).getPackageName().equals(packageName)) {
                return true;
            }
        }
        return false;
    }

    private String getAppName(String packageName) {
        try {
            ApplicationInfo applicationInfo = getPackageManager().getApplicationInfo(packageName, 0);
            return getPackageManager().getApplicationLabel(applicationInfo).toString();
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return "";
    }

    private Drawable getAppIcon(String packageName) {
        try {
            ApplicationInfo applicationInfo = getPackageManager().getApplicationInfo(packageName, 0);
            return getPackageManager().getApplicationIcon(applicationInfo);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            finish();
        }
        return false;
    }
}

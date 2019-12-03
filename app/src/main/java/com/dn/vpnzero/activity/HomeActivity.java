package com.dn.vpnzero.activity;

import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.view.Gravity;
import android.view.MenuItem;

import com.dn.vpnzero.BackgroundService;
import com.dn.vpnzero.R;
import com.dn.vpnzero.fragments.HomeFragment;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class HomeActivity extends BaseActivity {
    public static final String EXTRA_COUNTRY = "country";
    HomeFragment homeFragment;
    boolean bound = false;
    BackgroundService service;
    Fragment currentFragment;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        DrawerLayout drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawerLayout, R.string.drawer_open, R.string.drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();
        homeFragment = new HomeFragment();
        NavigationView navigationView = (NavigationView) findViewById(R.id.navigation_view);
        navigationView.setItemIconTintList(null);
        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {

            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                closeMenu();
                int id = item.getItemId();
                if (id == R.id.servers) {
                    selectFragment(homeFragment);
                } else if (id == R.id.settings) {
                    startActivity(new Intent(HomeActivity.this, SettingsActivity.class));
                } else if (id == R.id.about) {
                    startActivity(new Intent(HomeActivity.this, HelpActivity.class));
                } else if (id == R.id.exit) {
                    finish();
                }
                return false;
            }
        });
        selectFragment(homeFragment);
        //checkForUpdate();
    }

    private void checkForUpdate() {
        new Handler(Looper.getMainLooper()).post(new Runnable() {

            @Override
            public void run() {
                if (read("new_version_reminder", false)) {
                    final int totalCheck = read("total_update_check", 0);
                    if ((totalCheck % 20) == 0) {
                        try {
                            PackageInfo packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
                            final int appVersionCode = packageInfo.versionCode;
                            FirebaseDatabase.getInstance().getReference("appinfo").child("versions").child("1").child("version").addListenerForSingleValueEvent(new ValueEventListener() {

                                @Override
                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                    try {
                                        int versionCode = dataSnapshot.getValue(Integer.class);
                                        if (appVersionCode != versionCode) {
                                            AlertDialog dialog = new AlertDialog.Builder(HomeActivity.this)
                                                    .setTitle(R.string.text23)
                                                    .setMessage(R.string.text22)
                                                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {

                                                        @Override
                                                        public void onClick(DialogInterface dialogInterface, int i) {
                                                            write("total_update_check", totalCheck+1);
                                                            final String appPackageName = getPackageName();
                                                            try {
                                                                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + appPackageName)));
                                                            } catch (android.content.ActivityNotFoundException anfe) {
                                                                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + appPackageName)));
                                                            }
                                                        }
                                                    })
                                                    .setNegativeButton(R.string.title_cancel, new DialogInterface.OnClickListener() {

                                                        @Override
                                                        public void onClick(DialogInterface dialogInterface, int i) {
                                                            write("total_update_check", totalCheck+1);
                                                        }
                                                    })
                                                    .create();
                                            dialog.show();
                                        }
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError databaseError) {
                                }
                            });
                        } catch (PackageManager.NameNotFoundException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        });
    }

    public void selectFragment(Fragment fr) {
        currentFragment = fr;
        getSupportFragmentManager().beginTransaction().replace(R.id.content_home, fr).commit();
    }

    public void openMenu() {
        DrawerLayout drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawerLayout.openDrawer(Gravity.LEFT);
    }

    public void closeMenu() {
        DrawerLayout drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawerLayout.closeDrawer(Gravity.LEFT);
    }

    @Override
    protected void onStart() {
        super.onStart();
        Intent i = new Intent(this, BackgroundService.class);
        if (!BackgroundService.started) {
            startService(i);
        }
        bindService(i, sc, BIND_AUTO_CREATE);
    }

    ServiceConnection sc = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder binder) {
            service = ((BackgroundService.BackgroundBinder)binder).getService();
            bound = true;
            service.setActivity(HomeActivity.this);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
        }
    };

    @Override
    protected void onStop() {
        super.onStop();
        unbindService(sc);
        bound = false;
    }

    @Override
    public void onBackPressed() {
        if (currentFragment instanceof HomeFragment) {
            if (homeFragment.onBackPressed()) {
                finish();
            }
        } else {
            finish();
        }
    }
}

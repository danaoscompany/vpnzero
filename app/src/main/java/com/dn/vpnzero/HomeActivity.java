package com.dn.vpnzero;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.view.MenuItem;

import com.dn.vpnzero.activity.HelpActivity;
import com.dn.vpnzero.activity.SettingsActivity;
import com.dn.vpnzero.fragments.HomeFragment;

public class HomeActivity extends BaseActivity {
    HomeFragment homeFragment;

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
    }

    public void selectFragment(Fragment fr) {
        getSupportFragmentManager().beginTransaction().replace(R.id.content_home, fr).commit();
    }
}

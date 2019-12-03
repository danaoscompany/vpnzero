package com.dn.vpnzero.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.dn.vpnzero.R;

public class HelpActivity extends AppCompatActivity {
    LinearLayout tos, privacyPolicy, about, license;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_help);
        setSupportActionBar((Toolbar)findViewById(R.id.toolbar));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        tos = (LinearLayout) findViewById(R.id.tos);
        privacyPolicy = (LinearLayout) findViewById(R.id.privacy_policy);
        about = (LinearLayout) findViewById(R.id.about);
        license = (LinearLayout) findViewById(R.id.license);
        tos.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                startActivity(new Intent(HelpActivity.this, TOSActivity.class));
            }
        });
        privacyPolicy.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                startActivity(new Intent(HelpActivity.this, PrivacyPolicyActivity.class));
            }
        });
        about.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                startActivity(new Intent(HelpActivity.this, AboutActivity.class));
            }
        });
        license.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                startActivity(new Intent(HelpActivity.this, LicenseActivity.class));
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            finish();
        }
        return false;
    }
}

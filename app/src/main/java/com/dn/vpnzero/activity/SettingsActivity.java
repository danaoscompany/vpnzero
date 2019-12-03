package com.dn.vpnzero.activity;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Switch;

import com.dn.vpnzero.R;

public class SettingsActivity extends BaseActivity {
    LinearLayout newVersionReminderContainer;
    Switch newVersionReminder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        setSupportActionBar((Toolbar)findViewById(R.id.toolbar));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        newVersionReminderContainer = (LinearLayout)findViewById(R.id.new_version_reminder_container);
        newVersionReminder = (Switch)findViewById(R.id.new_version_reminder);
        newVersionReminderContainer.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                newVersionReminder.setChecked(!newVersionReminder.isChecked());
            }
        });
        newVersionReminder.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                write("new_version_reminder", checked);
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

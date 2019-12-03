package com.dn.vpnzero;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.dn.vpnzero.activity.LoaderActivity;

public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Util.show(context, "Receive boot completed, action: "+action);
        if (action.equals(Intent.ACTION_BOOT_COMPLETED)) {
            Intent i = new Intent(context, LoaderActivity.class);
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(i);
        }
    }
}

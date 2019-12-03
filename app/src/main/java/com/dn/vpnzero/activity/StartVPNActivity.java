package com.dn.vpnzero.activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.widget.ImageView;

import com.dn.vpnzero.R;
import com.dn.vpnzero.model.Server;

import java.util.concurrent.TimeUnit;

import de.blinkt.openvpn.core.VpnStatus;

import static com.dn.vpnzero.activity.ServerActivity.BROADCAST_ACTION;

public class StartVPNActivity extends BaseActivity {
    Server currentServer;
    BroadcastReceiver br;
    boolean fastConnection = false;
    boolean autoConnection = false;
    int connectionStatus = 0;
    ImageView connectIcon;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start_vpn);
        connectIcon = (ImageView)findViewById(R.id.turn_icon);
        fastConnection = getIntent().getBooleanExtra("fastConnection", false);
        autoConnection = getIntent().getBooleanExtra("autoConnection", false);
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
        registerReceiver(br, new IntentFilter(BROADCAST_ACTION));

    }

    private boolean checkStatus() {
        if (connectedServer != null && connectedServer.getHostName().equals(currentServer.getHostName())) {
            return VpnStatus.isVPNActive();
        }
        return false;
    }
}

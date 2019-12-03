package com.dn.vpnzero;

import android.app.ActivityManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

import com.dn.vpnzero.activity.HomeActivity;
import com.dn.vpnzero.activity.ServerActivity;
import com.dn.vpnzero.activity.StartVPNActivity;
import com.dn.vpnzero.items.Application;
import com.dn.vpnzero.model.Server;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import de.blinkt.openvpn.VpnProfile;
import de.blinkt.openvpn.core.OpenVPNService;
import de.blinkt.openvpn.core.ProfileManager;

public class BackgroundService extends Service {
    private static final int START_VPN_PROFILE = 70;
    HomeActivity activity = null;
    public static boolean started = false;
    BackgroundBinder binder = new BackgroundBinder();
    Timer timer = new Timer();

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        started = true;
        timer.scheduleAtFixedRate(new TimerTask() {

            @Override
            public void run() {
                Gson gson = new Gson();
                ArrayList<Application> autoConnectApps = (ArrayList<Application>)gson.fromJson(Util.read(BackgroundService.this, "auto_connect_apps", "").trim(), new TypeToken<List<Application>>(){}.getRawType());
                if (autoConnectApps == null) {
                    autoConnectApps = new ArrayList<>();
                }
                for (int i=0; i<autoConnectApps.size(); i++) {
                    if (isAppRunning(autoConnectApps.get(i).getPackageName())) {
                        connect(true, true);
                        break;
                    }
                }
            }
        }, 0, 100);
        return START_STICKY;
    }

    public void connect(boolean fastConnection, boolean autoConnection) {
        Intent intent = new Intent(this, StartVPNActivity.class);
        intent.putExtra("fastConnection", fastConnection);
        intent.putExtra("autoConnection", autoConnection);
        startActivity(intent);
    }

    public void setActivity(HomeActivity activity) {
        this.activity = activity;
    }

    private boolean isAppRunning(String packageName) {
        ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> runningAppProcessInfo = am.getRunningAppProcesses();
        for (int i = 0; i < runningAppProcessInfo.size(); i++) {
            if (runningAppProcessInfo.get(i).processName.equals(packageName)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    public class BackgroundBinder extends Binder {

        public BackgroundService getService() {
            return BackgroundService.this;
        }
    }
}

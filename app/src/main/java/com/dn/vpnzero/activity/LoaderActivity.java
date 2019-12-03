package com.dn.vpnzero.activity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.View;
import android.widget.TextView;

import com.androidnetworking.AndroidNetworking;
import com.androidnetworking.common.Priority;
import com.androidnetworking.error.ANError;
import com.androidnetworking.interfaces.DownloadListener;
import com.androidnetworking.interfaces.DownloadProgressListener;

import com.dn.vpnzero.R;
import com.dn.vpnzero.Util;
import com.dn.vpnzero.model.Server;
import com.dn.vpnzero.util.PropertiesService;
import com.dn.vpnzero.util.Stopwatch;
import com.startapp.android.publish.adsCommon.StartAppAd;
import com.startapp.android.publish.adsCommon.StartAppSDK;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;

public class LoaderActivity extends BaseActivity {
    String[] permissions = {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
    };
    private Handler updateHandler;

    private final int LOAD_ERROR = 0;
    private final int DOWNLOAD_PROGRESS = 1;
    private final int PARSE_PROGRESS = 2;
    private final int LOADING_SUCCESS = 3;
    private final int SWITCH_TO_RESULT = 4;
    private final String BASE_URL = "http://www.vpngate.net/api/iphone/";
    private final String BASE_FILE_NAME = "vpngate.csv";

    private boolean premiumStage = true;

    private final String PREMIUM_URL = "http://www.vpngate.net/api/iphone/";
    private final String PREMIUM_FILE_NAME = "vpngate.csv";

    private int percentDownload = 0;
    private Stopwatch stopwatch;
    private TextView text1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_loader);
        if (Build.VERSION.SDK_INT >= 23) {
            if (checkSelfPermission(permissions[0]) != PackageManager.PERMISSION_GRANTED
                    || checkSelfPermission(permissions[1]) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(permissions, 1);
            } else {
                init();
            }
        } else {
            init();
        }
    }

    public void init() {
        StartAppSDK.init(this, "211139012", true);
        StartAppSDK.setUserConsent(this,
                "pas",
                System.currentTimeMillis(),
                true);
        StartAppAd.disableSplash();
        text1 = (TextView)findViewById(R.id.text1);
        boolean firstTime = Util.read(this, "first_time", true);
        if (firstTime) {
            text1.setVisibility(View.VISIBLE);
        } else {
            text1.setVisibility(View.GONE);
        }
        updateHandler = new Handler(new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {
                Util.log("(Line 92) Handler called: "+msg.arg1);
                switch (msg.arg1) {
                    case LOADING_SUCCESS: {
                        Message end = new Message();
                        end.arg1 = SWITCH_TO_RESULT;
                        updateHandler.sendMessageDelayed(end, 500);
                    }
                    break;
                    case SWITCH_TO_RESULT: {
                        Util.write(LoaderActivity.this, "first_time", false);
                        if (PropertiesService.getConnectOnStart()) {
                            Server randomServer = getRandomServer();
                            if (randomServer != null) {
                                newConnecting(randomServer, true, true);
                            } else {
                                startActivity(new Intent(LoaderActivity.this, HomeActivity.class));
                            }
                        } else {
                            startActivity(new Intent(LoaderActivity.this, HomeActivity.class));
                        }
                    }
                }
                return true;
            }
        });
        downloadCSVFile(BASE_URL, BASE_FILE_NAME);
    }

    @Override
    protected boolean useHomeButton() {
        return false;
    }

    @Override
    protected boolean useMenu() {
        return false;
    }

    private void downloadCSVFile(final String url, String fileName) {
        Util.log("Downloading CSV file...");
        final File file = new File(getFilesDir(), BASE_FILE_NAME);
        if (!file.exists()) {
            stopwatch = new Stopwatch();
            Util.run(new Runnable() {

                @Override
                public void run() {
                    try {
                        URLConnection c = (URLConnection) new URL(url).openConnection();
                        c.connect();
                        byte[] buffer = new byte[8192];
                        int read;
                        InputStream stream = c.getInputStream();
                        FileOutputStream fos = new FileOutputStream(file);
                        while ((read = stream.read(buffer)) != -1) {
                            fos.write(buffer, 0, read);
                        }
                        fos.flush();
                        fos.close();
                        stream.close();
                        startActivity(new Intent(LoaderActivity.this, LoaderActivity.class));
                        finish();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
            /*AndroidNetworking.download(url, getCacheDir().getPath(), fileName)
                    .setTag("downloadCSV")
                    .setPriority(Priority.MEDIUM)
                    .setOkHttpClient(okHttpClient)
                    .build()
                    .setDownloadProgressListener(new DownloadProgressListener() {
                        @Override
                        public void onProgress(long bytesDownloaded, long totalBytes) {
                            Util.log("Bytes downloaded: " + bytesDownloaded);
                            if (totalBytes <= 0) {
                                // when we dont know the file size, assume it is 1200000 bytes :)
                                totalBytes = 1200000;
                            }

                            if (!premiumServers || !premiumStage) {
                                if (percentDownload <= 90)
                                    percentDownload = percentDownload + (int) ((100 * bytesDownloaded) / totalBytes);
                            } else {
                                percentDownload = (int) ((100 * bytesDownloaded) / totalBytes);
                            }

                            Message msg = new Message();
                            msg.arg1 = DOWNLOAD_PROGRESS;
                            msg.arg2 = percentDownload;
                            updateHandler.sendMessage(msg);
                        }
                    })
                    .startDownload(new DownloadListener() {
                        @Override
                        public void onDownloadComplete() {
                            if (premiumServers && premiumStage) {
                                premiumStage = false;
                                downloadCSVFile(PREMIUM_URL, PREMIUM_FILE_NAME);
                            } else {
                                parseCSVFile(BASE_FILE_NAME);
                            }
                        }

                        @Override
                        public void onError(ANError error) {
                            Message msg = new Message();
                            msg.arg1 = LOAD_ERROR;
                            msg.arg2 = R.string.network_error;
                            updateHandler.sendMessage(msg);
                        }
                    });*/
        } else {
            parseCSVFile(BASE_FILE_NAME);
        }
    }

    private void parseCSVFile(final String fileName) {
        Util.runLater(new Runnable() {

            @Override
            public void run() {
                Util.log("Parsing CSV file: " + fileName);
                BufferedReader reader = null;
                try {
                    reader = new BufferedReader(new FileReader(new File(getFilesDir(), BASE_FILE_NAME)));
                } catch (IOException e) {
                    e.printStackTrace();
                    Message msg = new Message();
                    msg.arg1 = LOAD_ERROR;
                    msg.arg2 = R.string.csv_file_error;
                    updateHandler.sendMessage(msg);
                }
                if (reader != null) {
                    try {
                        int startLine = 2;
                        int type = 0;

                        dbHelper.clearTable();

                        int counter = 0;
                        String line = null;
                        while ((line = reader.readLine()) != null) {
                            Util.log("Read line: "+line);
                            if (counter >= startLine) {
                                dbHelper.putLine(line, type);
                            }
                            counter++;
                            Message msg = new Message();
                            msg.arg1 = PARSE_PROGRESS;
                            msg.arg2 = counter;// we know that the server returns 100 records
                            updateHandler.sendMessage(msg);
                        }

                        Message end = new Message();
                        end.arg1 = LOADING_SUCCESS;
                        updateHandler.sendMessageDelayed(end, 200);

                    } catch (Exception e) {
                        e.printStackTrace();
                        Message msg = new Message();
                        msg.arg1 = LOAD_ERROR;
                        msg.arg2 = R.string.csv_file_error_parsing;
                        updateHandler.sendMessage(msg);
                    }
                }
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1) {
            boolean granted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    granted = false;
                    break;
                }
            }
            if (granted) {
                init();
            } else {
                finish();
            }
        }
    }
}

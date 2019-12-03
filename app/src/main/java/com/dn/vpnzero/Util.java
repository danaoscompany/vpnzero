package com.dn.vpnzero;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import com.squareup.okhttp.Interceptor;
import com.squareup.okhttp.MultipartBuilder;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import com.squareup.okhttp.ResponseBody;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.concurrent.TimeUnit;

public class Util {
    private static long timeoutStart = 0L;

    public static void log(String message) {
        Log.e(Build.MODEL, message);
    }

    public static OkHttpClient getOkHttpClient() {
        OkHttpClient client = new OkHttpClient();
        client.setReadTimeout(60, TimeUnit.SECONDS);
        client.setWriteTimeout(60, TimeUnit.SECONDS);
        client.setConnectTimeout(60, TimeUnit.SECONDS);
        return client;
    }

    public static void run(Runnable runnable) {
        new Thread(runnable).start();
    }

    public static void runLater(Runnable runnable) {
        new Handler(Looper.getMainLooper()).post(runnable);
    }

    public static void show(final Context ctx, final String message) {
        if (Thread.currentThread() != Looper.getMainLooper().getThread()) {
            runLater(new Runnable() {

                @Override
                public void run() {
                    Toast.makeText(ctx, message, Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            Toast.makeText(ctx, message, Toast.LENGTH_SHORT).show();
        }
    }

    public static void show(final Context ctx, final int messageID) {
        if (Thread.currentThread() != Looper.getMainLooper().getThread()) {
            runLater(new Runnable() {

                @Override
                public void run() {
                    Toast.makeText(ctx, messageID, Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            Toast.makeText(ctx, messageID, Toast.LENGTH_SHORT).show();
        }
    }

    public static void post(final Context ctx, final Listener listener, final String url, final String... values) {
        run(new Runnable() {

            @Override
            public void run() {
                try {
                    OkHttpClient client = Util.getOkHttpClient();
                    client.interceptors().add(new Interceptor() {

                        @Override
                        public Response intercept(Chain chain) throws IOException {
                            try {
                                Response response = chain.proceed(chain.request());
                                String content = response.body().string();
                                return response.newBuilder().body(ResponseBody.create(response.body().contentType(), content)).build();
                            }
                            catch (SocketTimeoutException e) {
                                final long elapsed = System.currentTimeMillis()-timeoutStart;
                                e.printStackTrace();
                                show(ctx, R.string.text3);
                                if (listener != null) {
                                    runLater(new Runnable() {

                                        @Override
                                        public void run() {
                                            listener.onTimeout(elapsed);
                                        }
                                    });
                                }
                            }
                            return chain.proceed(chain.request());
                        }
                    });
                    MultipartBuilder builder = new MultipartBuilder();
                    builder.type(MultipartBuilder.FORM);
                    for (int i = 0; i < values.length; i += 2) {
                        String postName = values[i];
                        String postContent = values[i + 1];
                        builder.addFormDataPart(postName, postContent);
                    }
                    Request request = new Request.Builder()
                            .url(url)
                            .post(builder.build())
                            .build();
                    timeoutStart = System.currentTimeMillis();
                    final String response = client.newCall(request).execute().body().string();
                    if (listener != null) {
                        runLater(new Runnable() {

                            @Override
                            public void run() {
                                listener.onSuccess(response);
                            }
                        });
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public static void get(final Context ctx, final Listener listener, final String url, final String... headers) {
        run(new Runnable() {

            @Override
            public void run() {
                try {
                    OkHttpClient client = Util.getOkHttpClient();
                    client.interceptors().add(new Interceptor() {

                        @Override
                        public Response intercept(Chain chain) throws IOException {
                            try {
                                Response response = chain.proceed(chain.request());
                                String content = response.body().string();
                                return response.newBuilder().body(ResponseBody.create(response.body().contentType(), content)).build();
                            } catch (SocketTimeoutException e) {
                                final long elapsed = System.currentTimeMillis()-timeoutStart;
                                e.printStackTrace();
                                show(ctx, R.string.text3);
                                if (listener != null) {
                                    runLater(new Runnable() {

                                        @Override
                                        public void run() {
                                            listener.onTimeout(elapsed);
                                        }
                                    });
                                }
                            }
                            return chain.proceed(chain.request());
                        }
                    });
                    Request.Builder builder = new Request.Builder();
                    builder = builder.url(url);
                    for (int i = 0; i < headers.length; i += 2) {
                        String headerName = headers[i];
                        String headerContent = headers[i + 1];
                        builder = builder.header(headerName, headerContent);
                    }
                    final String response = client.newCall(builder.build()).execute().body().string();
                    if (listener != null) {
                        runLater(new Runnable() {

                            @Override
                            public void run() {
                                listener.onSuccess(response);
                            }
                        });
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public static interface Listener {

        void onTimeout(long elapsed);
        void onSuccess(String response);
    }

    public static void write(Context ctx, String name, String value) {
        SharedPreferences.Editor e = ctx.getSharedPreferences("data", Context.MODE_PRIVATE).edit();
        e.putString(name, value);
        e.commit();
    }

    public static String read(Context ctx, String name, String defaultValue) {
        SharedPreferences sp = ctx.getSharedPreferences("data", Context.MODE_PRIVATE);
        return sp.getString(name, defaultValue);
    }

    public static void write(Context ctx, String name, boolean b) {
        SharedPreferences.Editor e = ctx.getSharedPreferences("data", Context.MODE_PRIVATE).edit();
        e.putBoolean(name, b);
        e.commit();
    }

    public static boolean read(Context ctx, String name, boolean defaultValue) {
        SharedPreferences sp = ctx.getSharedPreferences("data", Context.MODE_PRIVATE);
        return sp.getBoolean(name, defaultValue);
    }

    public static void write(Context ctx, String name, int value) {
        SharedPreferences.Editor e = ctx.getSharedPreferences("data", Context.MODE_PRIVATE).edit();
        e.putInt(name, value);
        e.commit();
    }

    public static int read(Context ctx, String name, int defaultValue) {
        SharedPreferences sp = ctx.getSharedPreferences("data", Context.MODE_PRIVATE);
        return sp.getInt(name, defaultValue);
    }
}

package com.dn.vpnzero;

import android.support.v7.app.AppCompatActivity;

import org.json.JSONObject;

public class BaseActivity extends AppCompatActivity {

    public void log(String message) {
        Util.log(message);
    }

    public void run(Runnable runnable) {
        Util.run(runnable);
    }

    public void runLater(Runnable runnable) {
        Util.runLater(runnable);
    }

    public void post(final Util.Listener listener, final String url, final String... values) {
        Util.post(this, listener, url, values);
    }

    public void get(final Util.Listener listener, final String url, final String... headers) {
        Util.get(this, listener, url, headers);
    }

    public String getString(JSONObject obj, String name, String defaultValue) {
        try {
            String value = obj.getString(name);
            if (value != null && !value.trim().equals("null")) {
                return value;
            }
        } catch (Exception e) {
        }
        return defaultValue;
    }

    public long getLong(JSONObject obj, String name, long defaultValue) {
        try {
            String value = obj.getString(name);
            if (value != null && !value.trim().equals("null")) {
                return Long.parseLong(value);
            }
        } catch (Exception e) {
        }
        return defaultValue;
    }

    public int getInt(JSONObject obj, String name, int defaultValue) {
        try {
            String value = obj.getString(name);
            if (value != null && !value.trim().equals("null")) {
                return Integer.parseInt(value);
            }
        } catch (Exception e) {
        }
        return defaultValue;
    }

    public double getDouble(JSONObject obj, String name, double defaultValue) {
        try {
            String value = obj.getString(name);
            if (value != null && !value.trim().equals("null")) {
                return Double.parseDouble(value);
            }
        } catch (Exception e) {
        }
        return defaultValue;
    }

    public float getFloat(JSONObject obj, String name, float defaultValue) {
        try {
            String value = obj.getString(name);
            if (value != null && !value.trim().equals("null")) {
                return Float.parseFloat(value);
            }
        } catch (Exception e) {
        }
        return defaultValue;
    }
}

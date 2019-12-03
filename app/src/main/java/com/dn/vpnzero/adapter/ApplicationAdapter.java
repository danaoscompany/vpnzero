package com.dn.vpnzero.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;

import com.dn.vpnzero.App;
import com.dn.vpnzero.R;
import com.dn.vpnzero.Util;
import com.dn.vpnzero.items.Application;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.List;

public class ApplicationAdapter extends RecyclerView.Adapter<ApplicationAdapter.ViewHolder> {
    Context context;
    ArrayList<Application> applications;

    public ApplicationAdapter(Context context, ArrayList<Application> applications) {
        this.context = context;
        this.applications = applications;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup container, int viewType) {
        return new ViewHolder(LayoutInflater.from(context).inflate(R.layout.app, container, false));
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        final Application application = applications.get(position);
        holder.name.setText(application.getName());
        holder.icon.setImageDrawable(application.getIcon());
        holder.enable.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                application.setAutoStart(checked);
                application.setChecked(checked);
            }
        });
        holder.selectApp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                holder.enable.setChecked(!application.isChecked());
                if (holder.enable.isChecked()) {
                    addApp(application);
                } else {
                    removeApp(application);
                }
            }
        });
    }

    public void removeApp(Application application) {
        Gson gson = new Gson();
        ArrayList<Application> autoConnectApps = gson.fromJson(
                Util.read(context, "auto_connect_apps", "").trim(),
                new TypeToken<List<Application>>(){}.getType());
        if (autoConnectApps == null) {
            autoConnectApps = new ArrayList<>();
        }
        for (int i=0; i<autoConnectApps.size(); i++) {
            if (autoConnectApps.get(i).getPackageName().equals(application.getPackageName())) {
                autoConnectApps.remove(i);
                break;
            }
        }
        Util.write(context, "auto_connect_apps", gson.toJson(autoConnectApps));
    }

    public void addApp(Application application) {
        Gson gson = new Gson();
        ArrayList<Application> autoConnectApps = gson.fromJson(
                Util.read(context, "auto_connect_apps", "").trim(),
                new TypeToken<List<Application>>(){}.getType());
        if (autoConnectApps == null) {
            autoConnectApps = new ArrayList<>();
        }
        if (!isAppAdded(application)) {
            autoConnectApps.add(application);
        }
        Util.write(context, "auto_connect_apps", gson.toJson(autoConnectApps));
    }

    private boolean isAppAdded(Application application) {
        Gson gson = new Gson();
        ArrayList<Application> autoConnectApps = gson.fromJson(
                Util.read(context, "auto_connect_apps", "").trim(),
                new TypeToken<List<Application>>(){}.getType());
        if (autoConnectApps == null) {
            autoConnectApps = new ArrayList<>();
        }
        for (int i=0; i<autoConnectApps.size(); i++) {
            if (autoConnectApps.get(i).getPackageName().equals(application.getPackageName())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int getItemCount() {
        return applications.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public RelativeLayout selectApp;
        public ImageView icon;
        public TextView name;
        public Switch enable;

        public ViewHolder(View view) {
            super(view);
            selectApp = (RelativeLayout)view.findViewById(R.id.select_app);
            icon = (ImageView) view.findViewById(R.id.icon);
            name = (TextView) view.findViewById(R.id.name);
            enable = (Switch) view.findViewById(R.id.enable);
        }
    }
}

package com.dn.vpnzero.adapter;

import android.content.Context;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.squareup.picasso.Picasso;
import com.dn.vpnzero.R;
import com.dn.vpnzero.model.Server;

import java.util.ArrayList;

public class CountryAdapter extends RecyclerView.Adapter<CountryAdapter.ViewHolder> {
    Context context;
    ArrayList<Server> countries;
    Listener listener;

    public CountryAdapter(Context context, ArrayList<Server> countries, Listener listener) {
        this.context = context;
        this.countries = countries;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup container, int viewType) {
        return new ViewHolder(LayoutInflater.from(context).inflate(R.layout.country, container, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        final Server server = countries.get(position);
        holder.name.setText(server.getCountryLong());
        String flagURL = "https://www.countryflags.io/"+server.getCountryShort().toLowerCase()+"/shiny/64.png";
        if (!server.getCountryShort().toLowerCase().trim().equals("")) {
            Picasso.get().load(Uri.parse(flagURL)).into(holder.flag);
        } else {
            holder.flag.setImageResource(0);
        }
        holder.selectCountry.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                listener.onCountrySelected(server);
            }
        });
    }

    @Override
    public int getItemCount() {
        return countries.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public LinearLayout selectCountry;
        public ImageView flag;
        public TextView name;

        public ViewHolder(View view) {
            super(view);
            selectCountry = (LinearLayout)view.findViewById(R.id.select_country);
            flag = (ImageView)view.findViewById(R.id.flag);
            name = (TextView)view.findViewById(R.id.name);
        }
    }

    public interface Listener {

        void onCountrySelected(Server server);
    }
}

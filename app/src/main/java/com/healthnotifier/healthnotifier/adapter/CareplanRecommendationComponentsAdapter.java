package com.healthnotifier.healthnotifier.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.healthnotifier.healthnotifier.R;

import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Created by charles on 2/17/17.
 */

public class CareplanRecommendationComponentsAdapter extends RecyclerView.Adapter<CareplanRecommendationComponentsAdapter.MyViewHolder> {

    private Context mContext;
    private ArrayList<JSONObject> mItems = new ArrayList<JSONObject>();

    public CareplanRecommendationComponentsAdapter(Context context, ArrayList<JSONObject> items) {
        this.mContext = context;
        this.mItems = items;
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

    public class MyViewHolder extends RecyclerView.ViewHolder {
        public TextView body;
        public TextView title;

        public MyViewHolder(View view) {
            super(view);
            title = (TextView) view.findViewById(R.id.tvTitle);
            body = (TextView) view.findViewById(R.id.tvBody);
        }
    }

    @Override
    public void onBindViewHolder(final CareplanRecommendationComponentsAdapter.MyViewHolder holder, int position) {
        try {
            JSONObject item = mItems.get(position);
            holder.title.setText(item.getString("category"));
            holder.body.setText(item.getString("data"));
        } catch(Exception e){

        }
    }

    @Override
    public CareplanRecommendationComponentsAdapter.MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.card_careplan_recommendation_component, parent, false);
        return new CareplanRecommendationComponentsAdapter.MyViewHolder(itemView);
    }

}

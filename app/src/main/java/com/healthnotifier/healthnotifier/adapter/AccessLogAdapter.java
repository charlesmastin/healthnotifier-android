package com.healthnotifier.healthnotifier.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.healthnotifier.healthnotifier.R;
import com.healthnotifier.healthnotifier.utility.Logcat;

import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Created by charles on 2/7/17.
 */

public class AccessLogAdapter extends JSONArrayAdapter {

    private Context mContext;

    public AccessLogAdapter(Context context, ArrayList<JSONObject> node){
        mContext = context;
        mItems = node;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = convertView;
        if(view == null){
            LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = inflater.inflate(R.layout.item_collection_item, null);
            AccessLogAdapter.ViewHolder holder = new AccessLogAdapter.ViewHolder();
            holder.tvTitle = (TextView) view.findViewById(R.id.tvCollectionItemTitle);
            holder.tvDescription = (TextView) view.findViewById(R.id.tvCollectionItemDescription);
            view.setTag(holder);
        }
        try {
            JSONObject item = (JSONObject) getItem(position);
            final AccessLogAdapter.ViewHolder holder = (AccessLogAdapter.ViewHolder) view.getTag();
            // check for null
            if(!item.isNull("scanner_name")) {
                holder.tvTitle.setText(item.getString("scanner_name"));
                // if is provider
                if(item.getBoolean("is_provider")){
                    holder.tvTitle.setText(item.getString("scanner_name") + ", health care provider");
                }
            }
            if(!item.isNull("scanner_phone_number")) {
                holder.tvTitle.setText(item.getString("scanner_phone_number"));
            }
            holder.tvDescription.setText(item.getString("created_at").substring(0, 10));
            // expose geo details, naaa son

        } catch(Exception e){
            Logcat.d(e.toString());
        }
        return view;
    }

    static class ViewHolder {
        TextView tvTitle;
        TextView tvDescription;
    }
}

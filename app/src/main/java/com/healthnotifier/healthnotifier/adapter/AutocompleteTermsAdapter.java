package com.healthnotifier.healthnotifier.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;


import com.healthnotifier.healthnotifier.HealthNotifierApplication;
import com.healthnotifier.healthnotifier.R;
import com.healthnotifier.healthnotifier.utility.GenericEvent;
import com.healthnotifier.healthnotifier.utility.Logcat;
import com.squareup.otto.Bus;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

// TODO: remove this class, as we no longer need this UX (aka the dialog full picker), since we went to the widget
// but, it works, mostly so let's keep it for now

public class AutocompleteTermsAdapter extends JSONArrayAdapter {

    private Context mContext;
    private String mAutocompleteId;

    public AutocompleteTermsAdapter(Context context, ArrayList<JSONObject> node, String autocompleteid){
        mContext = context;
        mItems = node;
        mAutocompleteId = autocompleteid;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = convertView;
        final AutocompleteTermsAdapter.ViewHolder holder;
        if (view == null) {
            LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = inflater.inflate(R.layout.item_autocomplete_term, null);
            holder = new AutocompleteTermsAdapter.ViewHolder();
            holder.tvTitle = (TextView) view.findViewById(R.id.tvAutocompleteTermTitle);
            view.setTag(holder);
        } else {
            holder = (AutocompleteTermsAdapter.ViewHolder) view.getTag();
        }
        try {
            JSONObject item = getItem(position);
            holder.tvTitle.setText(item.getString("title"));

            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    handleSelection(position);
                }
            });

        } catch (Exception e) {

        }
        return view;
    }

    private void handleSelection(int position){
        try {
            JSONObject item = getItem(position);

            Bus bus = HealthNotifierApplication.bus;
            Map<String, Object> e = new HashMap<String, Object>();
            e.put("AutocompleteId", mAutocompleteId); // hopefully this is enough juice for our calling class to do it blow hard edition
            // but we probablyu aso wwant the field attribute yea son
            e.put("Title", item.getString("title"));
            e.put("Value", item.getString("code"));
            bus.post(new GenericEvent("AutocompleteChange", e));
            Logcat.d(e.toString());

        } catch(Exception e){

        }
    }

    static class ViewHolder {
        TextView tvTitle;
    }


}

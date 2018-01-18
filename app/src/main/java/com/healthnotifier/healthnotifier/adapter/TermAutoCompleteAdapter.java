package com.healthnotifier.healthnotifier.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

import com.healthnotifier.healthnotifier.network.HealthNotifierAPI;
import com.healthnotifier.healthnotifier.R;
import com.healthnotifier.healthnotifier.utility.JSONHelper;

import org.json.JSONObject;

import java.util.ArrayList;

import okhttp3.Response;

/**
 * Created by charles on 2/5/17.
 */

// http://makovkastar.github.io/blog/2014/04/12/android-autocompletetextview-with-suggestions-from-a-web-service/
    // TODO: wire in the delay manager
    // TODO: wire up a progress animation in the UI

public class TermAutoCompleteAdapter extends JSONArrayAdapter implements Filterable {

    private static final int MAX_RESULTS = 10;

    private Context mContext;
    private String mAutocompleteId;

    public TermAutoCompleteAdapter(Context context, ArrayList<JSONObject> node, String autocompleteid){
        mContext = context;
        mItems = node;
        mAutocompleteId = autocompleteid;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater) mContext
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.support_simple_spinner_dropdown_item, parent, false);
        }
        try {
            JSONObject item = getItem(position);
            ((TextView) convertView.findViewById(android.R.id.text1)).setText(item.getString("title"));
        } catch(Exception e){

        }

        return convertView;
    }

    @Override
    public Filter getFilter() {
        Filter filter = new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                FilterResults filterResults = new FilterResults();
                if (constraint != null) {
                    // List<Books> books = findBooks(mContext, constraint.toString());
                    Response response = HealthNotifierAPI.getInstance().termsSearchSync(mAutocompleteId, constraint.toString());
                    if(response.isSuccessful()){
                        try {
                            JSONObject responseJSON = new JSONObject(response.body().string());
                            mItems = JSONHelper.jsonArrayToArrayList(responseJSON.getJSONArray("combinations"));
                            // Assign the data to the FilterResults
                            // TODO: restructure data? meh
                            filterResults.values = mItems;
                            filterResults.count = mItems.size();
                        } catch(Exception e){

                        }
                    }else {

                    }


                }
                return filterResults;
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                if (results != null && results.count > 0) {
                    // resultList = (List<Books>) results.values;
                    // balls on your chin
                    // we already saved our values lol, shadow data on your shadow warrior
                    notifyDataSetChanged();
                } else {
                    notifyDataSetInvalidated();
                }
            }
        };
        return filter;
    }

}

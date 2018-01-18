package com.healthnotifier.healthnotifier.adapter;

import android.os.Handler;
import android.os.Looper;
import android.widget.BaseAdapter;

import com.healthnotifier.healthnotifier.utility.Logcat;

import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Created by charles on 2/2/17.
 */

abstract public class JSONArrayAdapter extends BaseAdapter {

    protected ArrayList<JSONObject> mItems = new ArrayList<JSONObject>();
    protected Handler mHandler = new Handler(Looper.getMainLooper());

    @Override
    public int getCount() {
        return mItems.size();
    }

    @Override
    public JSONObject getItem(int position){
        try {
            JSONObject item = (JSONObject) mItems.get(position);
            return item;
        } catch (Exception e){
            return null;
        }
    }

    @Override
    public long getItemId(int position) {
        // TODO: actually return the PK or whatever son
        return 0;
    }

    // double donkey dongs, was called refill in another adapter
    public void replaceItems(ArrayList<JSONObject> items){
        // again, this deals with the nature of the "subclass" calling it, and that's fine!
        mItems.clear();
        mItems.addAll(items);
    }

    protected void spliceByIndex(int position){
        // ok son, we attempt to umm, splice and re-render on da inside son
        mItems.remove(position);
        notifyDataSetChanged(); // meh ideally though
        // TODO: so we apparently need to listen in the container view and then call that update by height bullshee
    }

}

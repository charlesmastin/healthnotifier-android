package com.healthnotifier.healthnotifier.adapter;

import java.util.ArrayList;
import java.util.HashMap;


import com.healthnotifier.healthnotifier.R;
import android.content.Context;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.display.SimpleBitmapDisplayer;

public class CapturedFilesAdapter extends BaseAdapter {

    private Context mContext;
    private ArrayList<HashMap<String, Object>> mDataList;
    private int mSelectedPosition = 0;
    private DisplayImageOptions mDisplayImageOptions;
    private ImageLoader mImageLoader = ImageLoader.getInstance();

    public CapturedFilesAdapter(Context context, ArrayList<HashMap<String, Object>> dataList) {
        mContext = context;
        mDataList = dataList;

        mDisplayImageOptions = new DisplayImageOptions.Builder()
                .cacheInMemory(true)
                .cacheOnDisk(true)
                .displayer(new SimpleBitmapDisplayer())
                .build();
    }

    @Override
    public int getCount() {
        return mDataList.size();
    }

    @Override
    public Object getItem(int position) {
        if (mDataList != null)
            return mDataList.size();
        else
            return 0;
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    public void refill(Context context, ArrayList<HashMap<String, Object>> dataList) {
        mContext = context;
        mDataList = dataList;
        notifyDataSetChanged();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // inflate view
        View view = convertView;
        if (view == null) {
            LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            // TODO: create our own layout partial aka table cell, bla bla blabla, for now reuse the recent_lifesquare
            view = inflater.inflate(R.layout.item_captured_file, null);

            // view holder
            ViewHolder holder = new ViewHolder();
            holder.tvName = (TextView) view.findViewById(R.id.tvName);
            holder.ivPhoto = (ImageView) view.findViewById(R.id.ivPhoto);
            view.setTag(holder);
        }

        // entity
        HashMap item = (HashMap) mDataList.get(position);

        if (item != null) {
            ViewHolder holder = (ViewHolder) view.getTag();
            holder.tvName.setText("Page " + (position + 1) + " of " + getCount());
            byte[] decodedString = Base64.decode(item.get("File").toString(), Base64.DEFAULT);
            BitmapFactory.Options mBitmapOptions = new BitmapFactory.Options();
            mBitmapOptions.inSampleSize = 16;
            Bitmap imageBitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length, mBitmapOptions);
            holder.ivPhoto.setImageBitmap(imageBitmap);
        }

        return view;
    }

    public void setSelectedPosition(int position) {
        mSelectedPosition = position;
        notifyDataSetChanged();
    }

    public int getSelectedPosition() {
        return mSelectedPosition;
    }

    static class ViewHolder {
        TextView tvName;
        ImageView ivPhoto;
    }
}

package com.healthnotifier.healthnotifier.adapter;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.healthnotifier.healthnotifier.HealthNotifierConfig;
import com.healthnotifier.healthnotifier.R;
import com.healthnotifier.healthnotifier.utility.Files;
import com.healthnotifier.healthnotifier.utility.Logcat;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.display.SimpleBitmapDisplayer;
import com.nostra13.universalimageloader.core.listener.SimpleImageLoadingListener;

import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Created by charles on 2/15/17.
 */

public class DocumentFilesAdapter extends JSONArrayAdapter {
    private Context mContext;
    private DisplayImageOptions mDisplayImageOptions;
    private ImageLoader mImageLoader = ImageLoader.getInstance();

    public DocumentFilesAdapter(Context context, ArrayList<JSONObject> node){
        mContext = context;
        mItems = node;
        mDisplayImageOptions = new DisplayImageOptions.Builder()
                .cacheInMemory(true)
                .cacheOnDisk(true)
                .displayer(new SimpleBitmapDisplayer())
                .build();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = convertView;
        final PatientNetworkPendingAdapter.ViewHolder holder;
        if(view == null){
            LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = inflater.inflate(R.layout.item_document_file, null);
            holder = new PatientNetworkPendingAdapter.ViewHolder();
            holder.tvTitle = (TextView) view.findViewById(R.id.tvName);
            holder.ivPhoto = (ImageView) view.findViewById(R.id.ivPhoto);
            view.setTag(holder);
        } else {
            holder = (PatientNetworkPendingAdapter.ViewHolder) view.getTag();
        }
        try {
            JSONObject item = getItem(position);
            String photoUrl = null;
            final int size = 96;

            holder.tvTitle.setText("Page " + (position+1) + " of " + getCount());
            photoUrl = HealthNotifierConfig.API_ROOT + "files/" + item.getString("Uid") + "/?width=" + (size * 2) + "&height=" + (size * 2);

            if(photoUrl != null){
                mImageLoader.displayImage(photoUrl, holder.ivPhoto, mDisplayImageOptions, new SimpleImageLoadingListener() {
                    @Override
                    public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
                        ((ImageView) view).setImageBitmap(loadedImage);
                        //((ImageView) view).setScaleType(ImageView.ScaleType.CENTER_CROP);
                    }
                });
            }

        } catch(Exception e){
            Logcat.d(e.toString());
        }

        return view;
    }

    static class ViewHolder {
        ImageView ivPhoto;
        TextView tvTitle;
    }
}

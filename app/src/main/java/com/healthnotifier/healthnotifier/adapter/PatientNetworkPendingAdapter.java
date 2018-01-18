package com.healthnotifier.healthnotifier.adapter;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.healthnotifier.healthnotifier.Config;
import com.healthnotifier.healthnotifier.R;
import com.healthnotifier.healthnotifier.activity.EditCollectionItemActivity;
import com.healthnotifier.healthnotifier.activity.PatientActivity;
import com.healthnotifier.healthnotifier.utility.Files;
import com.healthnotifier.healthnotifier.utility.Logcat;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.display.SimpleBitmapDisplayer;
import com.nostra13.universalimageloader.core.listener.SimpleImageLoadingListener;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Created by charles on 1/30/17.
 */

public class PatientNetworkPendingAdapter extends JSONArrayAdapter {

    private Context mContext;
    private String mPatientId;
    private String mMode;

    private DisplayImageOptions mDisplayImageOptions;
    private ImageLoader mImageLoader = ImageLoader.getInstance();

    public PatientNetworkPendingAdapter(Context context, ArrayList<JSONObject> node, String patientId, String mode){
        mContext = context;
        mPatientId = patientId;
        mItems = node;
        mMode = mode;

        mDisplayImageOptions = new DisplayImageOptions.Builder()
                .cacheInMemory(true)
                .cacheOnDisk(true)
                .displayer(new SimpleBitmapDisplayer())
                .build();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = convertView;
        final ViewHolder holder;
        if(view == null){
            LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = inflater.inflate(R.layout.item_patient_network_pending, null);
            holder = new ViewHolder();
            holder.tvTitle = (TextView) view.findViewById(R.id.tvPatientNetworkPendingTitle);
            holder.tvDescription = (TextView) view.findViewById(R.id.tvPatientNetworkPendingDescription);
            holder.ivPhoto = (ImageView) view.findViewById(R.id.ivPatientNetworkPendingPhoto);
            view.setTag(holder);
        } else {
            holder = (ViewHolder) view.getTag();
        }
        try {
            JSONObject item = getItem(position);
            String photoUrl = null;
            final int size = 32;


            if(mMode.equals("inbound")){
                holder.tvTitle.setText(item.getString("granter_name"));
                if(!item.isNull("granter_photo_uuid")) {
                    photoUrl = Config.API_ROOT + "profiles/" + item.getString("granter_uuid") + "/profile-photo?photo_uuid=" + item.getString("granter_photo_uuid") + "&width=" + (size * 2) + "&height=" + (size * 2);
                }
                String description = item.getString("asked_at"); // TODO: date format that bitch
                // if they are a health care provider
                // like, you're gonna ask to see your provider's lifesquare? naa son
                /*
                if(item.getBoolean("granter_provider")) {
                    description += " | Registered health care provider";
                }
                */
                holder.tvDescription.setText(description);
            }
            if(photoUrl != null){
                mImageLoader.displayImage(photoUrl, holder.ivPhoto, mDisplayImageOptions, new SimpleImageLoadingListener() {
                    @Override
                    public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
                        Bitmap circleCroppers86 = Files.getCroppedBitmap(loadedImage, size * 2);
                        ((ImageView) view).setImageBitmap(circleCroppers86);
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
        TextView tvDescription;
    }

}

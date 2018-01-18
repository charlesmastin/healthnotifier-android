package com.healthnotifier.healthnotifier.adapter;

import android.content.Context;
import android.content.Intent;
import android.content.res.ObbInfo;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.TextView;

import com.healthnotifier.healthnotifier.HealthNotifierApplication;
import com.healthnotifier.healthnotifier.Config;
import com.healthnotifier.healthnotifier.activity.LifesquareActivity;
import com.healthnotifier.healthnotifier.model.ScanHistory;
import com.healthnotifier.healthnotifier.utility.AnalyticsEvent;
import com.healthnotifier.healthnotifier.utility.Files;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.display.SimpleBitmapDisplayer;

import com.healthnotifier.healthnotifier.R;
import com.nostra13.universalimageloader.core.listener.SimpleImageLoadingListener;
import com.squareup.otto.Bus;


import java.util.HashMap;
import java.util.Map;

import io.realm.OrderedRealmCollection;
import io.realm.RealmBaseAdapter;
import io.realm.RealmResults;

/**
 * Created by charles on 4/20/16.
 */
// extends RealmBaseAdapter<>
public class ScanHistoryAdapter extends RealmBaseAdapter<ScanHistory> {
    private Context mContext;
    private DisplayImageOptions mDisplayImageOptions;
    private ImageLoader mImageLoader = ImageLoader.getInstance();
    private LayoutInflater inflater;

    static class ViewHolder {
        TextView tvName;
        TextView tvAddress;
        ImageView ivPhoto;
    }

    public ScanHistoryAdapter(Context context, OrderedRealmCollection<ScanHistory> realmResults) {
        super(realmResults);
        mContext = context;
        mDisplayImageOptions = new DisplayImageOptions.Builder()
                .cacheInMemory(true)
                .cacheOnDisk(true)
                .displayer(new SimpleBitmapDisplayer())
                .build();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = convertView;
        final ScanHistoryAdapter.ViewHolder holder;
        if (view == null) {
            LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = inflater.inflate(R.layout.item_recent_lifesquare, parent, false);
            holder = new ViewHolder();
            holder.tvName = (TextView) view.findViewById(R.id.tvName);
            holder.tvAddress = (TextView) view.findViewById(R.id.tvAddress);
            holder.ivPhoto = (ImageView) view.findViewById(R.id.ivPhoto);
            view.setTag(holder);
        } else {
            holder = (ViewHolder) view.getTag();
        }
        final int size = 48;

        if (adapterData != null) {
            //AllJavaTypes item = adapterData.get(position);
            //viewHolder.textView.setText(item.getFieldString());
            ScanHistory item = adapterData.get(position);
            holder.tvName.setText(item.name);
            holder.tvAddress.setText(item.address);
            mImageLoader.displayImage(item.profilePhoto, holder.ivPhoto, mDisplayImageOptions, new SimpleImageLoadingListener() {
                @Override
                public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
                    Bitmap circleCroppers86 = Files.getCroppedBitmap(loadedImage, size * 2);
                    ((ImageView) view).setImageBitmap(circleCroppers86);
                }
            });
            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Bus bus = HealthNotifierApplication.bus;
                    Map<String, Object> attributes = new HashMap<String, Object>();

                    attributes.put("PatientId", item.patientId);
                    bus.post(new AnalyticsEvent("History View Patient", attributes));

                    Intent intent = new Intent(mContext, LifesquareActivity.class);
                    intent.putExtra("PATIENT_ID", item.patientId);
                    intent.putExtra("PATIENT_NAME", item.name);
                    // construct dat webview url for old times sake
                    String url = Config.API_ROOT + "lifesquares/" + item.lifesquareId + "/webview";
                    intent.putExtra("PATIENT_WEBVIEW_URL", url);
                    mContext.startActivity(intent);
                }
            });
        }


        return view;
    }
    /*
    public RealmResults<ScanHistory> getRealmResults() {
        return realmResults;
    }
    */
}

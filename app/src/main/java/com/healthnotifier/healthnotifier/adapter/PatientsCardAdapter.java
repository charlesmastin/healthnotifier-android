package com.healthnotifier.healthnotifier.adapter;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.healthnotifier.healthnotifier.R;
import com.healthnotifier.healthnotifier.activity.PatientActivity;
import com.healthnotifier.healthnotifier.utility.Files;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.display.SimpleBitmapDisplayer;
import com.nostra13.universalimageloader.core.listener.SimpleImageLoadingListener;

import org.json.JSONArray;
import org.json.JSONObject;


public class PatientsCardAdapter extends RecyclerView.Adapter<PatientsCardAdapter.MyViewHolder> {

    private Context mContext;
    // MF public access son? whtf
    public JSONArray mPatientsList;

    // TODO: swap this to the Glide loader maybe? wut?
    private DisplayImageOptions mDisplayImageOptions;
    private ImageLoader mImageLoader = ImageLoader.getInstance();

    @Override
    public int getItemCount() {
        return mPatientsList.length(); // lol on dat length vs count API diff?
    }

    public class MyViewHolder extends RecyclerView.ViewHolder {
        public TextView name, age;
        public ImageView profile_photo, overflow;

        public MyViewHolder(View view) {
            super(view);
            name = (TextView) view.findViewById(R.id.patient_name);
            age = (TextView) view.findViewById(R.id.patient_age);
            profile_photo = (ImageView) view.findViewById(R.id.patient_photo);
            //overflow = (ImageView) view.findViewById(R.id.overflow);
        }
    }

    public PatientsCardAdapter(Context mContext, JSONArray albumList) {
        this.mContext = mContext;
        this.mPatientsList = albumList;

        mDisplayImageOptions = new DisplayImageOptions.Builder()
                .cacheInMemory(true)
                .cacheOnDisk(true)
                .displayer(new SimpleBitmapDisplayer())
                .build();
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.card_patient, parent, false);

        return new MyViewHolder(itemView);
    }

    private void handleCardClick(int position) {
        try {
            JSONObject patient = (JSONObject) mPatientsList.getJSONObject(position);
            Intent intent = new Intent(mContext, PatientActivity.class);
            intent.putExtra("PATIENT_ID", patient.getString("PatientId"));
            mContext.startActivity(intent);
        } catch(Exception e){

        }
    }

    @Override
    public void onBindViewHolder(final MyViewHolder holder, int position) {
        try {
            JSONObject patient = (JSONObject) mPatientsList.getJSONObject(position);
            // TODO: hack it up son
            String name = patient.getString("Name");
            holder.name.setText(name);
            // TODO: HACK TOWN USA
            // it's currently not possible *really* to have a name saved and still have the default Jesus birthday
            holder.age.setText(patient.getString("Age"));
            // let's leave room for an update to this API, etc
            if(name.equals("New Profile")){
                holder.age.setText("");
            }

            if(!patient.getBoolean("Confirmed") || patient.isNull("Coverage")){
                holder.age.setText("Continue Setup");
            }

            // loading album cover using Glide library
            //Glide.with(mContext).load(album.getThumbnail()).into(holder.thumbnail);
            final int size = 128;
            String url = patient.getString("ProfilePhoto") + "?width=" + (size*2) + "&height=" + (size*2);
            mImageLoader.displayImage(url, holder.profile_photo, mDisplayImageOptions, new SimpleImageLoadingListener() {
                @Override
                public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
                    ((ImageView) view).setImageBitmap(loadedImage);
                    ((ImageView) view).setScaleType(ImageView.ScaleType.CENTER_CROP);
                }
            });

            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    handleCardClick(position);
                }
            });

            // this is a jimmy hack
            holder.profile_photo.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    handleCardClick(position);
                }
            });

            // actions menu handler if we have that son

        } catch (Exception e){
            // WTF SON, I CAN'T STAND THIS json.org API with all it's excessive exception handling
        }
    }
}

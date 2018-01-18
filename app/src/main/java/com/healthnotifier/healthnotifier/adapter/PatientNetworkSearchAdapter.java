package com.healthnotifier.healthnotifier.adapter;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.healthnotifier.healthnotifier.network.HealthNotifierAPI;
import com.healthnotifier.healthnotifier.Config;
import com.healthnotifier.healthnotifier.R;
import com.healthnotifier.healthnotifier.activity.PatientNetworkManageActivity;
import com.healthnotifier.healthnotifier.utility.Files;
import com.healthnotifier.healthnotifier.utility.Logcat;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.display.SimpleBitmapDisplayer;
import com.nostra13.universalimageloader.core.listener.SimpleImageLoadingListener;

import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;

import okhttp3.Call;
import okhttp3.Callback;

/**
 * Created by charles on 1/30/17.
 */

public class PatientNetworkSearchAdapter extends JSONArrayAdapter {

    private Context mContext;
    private String mPatientId;
    private String mMode;

    private DisplayImageOptions mDisplayImageOptions;
    private ImageLoader mImageLoader = ImageLoader.getInstance();

    public PatientNetworkSearchAdapter(Context context, ArrayList<JSONObject> node, String patientId, String mode){
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
            view = inflater.inflate(R.layout.item_patient_network_search_result, null);
            holder = new ViewHolder();
            holder.tvTitle = (TextView) view.findViewById(R.id.tvPatientNetworkSearchResultTitle);
            holder.tvDescription = (TextView) view.findViewById(R.id.tvPatientNetworkSearchResultDescription);
            holder.ivPhoto = (ImageView) view.findViewById(R.id.ivPatientNetworkSearchResultPhoto);
            holder.btConnect = (Button) view.findViewById(R.id.btPatientNetworkSearchResultConnect);
            view.setTag(holder);
        } else {
            holder = (ViewHolder) view.getTag();
        }
        try {
            JSONObject item = getItem(position);
            // check for null
            holder.tvTitle.setText(item.getString("Name"));

            if(mMode.equals("inbound")){
                holder.btConnect.setText("Request Access");
            }
            if(mMode.equals("outbound")){
                holder.btConnect.setText("Add to LifeCircle");
            }

            if(item.getBoolean("IsProvider")) {
                holder.tvDescription.setText("Registered health care provider");
            } else {
                holder.tvDescription.setVisibility(View.GONE);
            }

            final int size = 32;
            if(!item.isNull("PatientPhotoUuid")){
                // TODO: DRY THIS UP
                String url = Config.API_ROOT + "profiles/" + item.getString("PatientUuid") + "/profile-photo?photo_uuid=" + item.getString("PatientPhotoUuid") + "&width=" + (size*2) + "&height=" + (size*2);

                mImageLoader.displayImage(url, holder.ivPhoto, mDisplayImageOptions, new SimpleImageLoadingListener() {
                    @Override
                    public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
                        Bitmap circleCroppers86 = Files.getCroppedBitmap(loadedImage, size * 2);
                        ((ImageView) view).setImageBitmap(circleCroppers86);
                    }
                });
            }


            // TODO: view click SON, in theory to see a larger view of the person? / medical credentials details and so on

            // connection button
            holder.btConnect.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    handleConnectClick(position);
                };
            });

        } catch(Exception e){
            Logcat.d(e.toString());
        }

        return view;

    }

    private void requestAccess(int position){
        try {
            JSONObject item = getItem(position);
            // networking son
            HealthNotifierAPI.getInstance().patientNetworkRequestAccess(mPatientId, item.getString("PatientUuid"), mPatientId, new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    // we couldn't get a response or we timed out, offline, etc
                    Logcat.d( "onFailure" + e.toString());
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(mContext, "Error requesting access :(", Toast.LENGTH_SHORT).show();
                        }
                    });
                }

                @Override
                public void onResponse(Call call, okhttp3.Response response) throws IOException {
                    if (response.isSuccessful()) { // exactly what status codes determine this?
                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(mContext, "Invite sent!", Toast.LENGTH_SHORT).show();
                                Intent intent = new Intent(mContext, PatientNetworkManageActivity.class);
                                intent.putExtra("PATIENT_ID", mPatientId);
                                intent.putExtra("MODE", mMode);
                                ((Activity) mContext).startActivity(intent);
                            }
                        });
                    } else {
                        Logcat.d( "onResponseError" + response.toString());
                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(mContext, "Error requesting access :(", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }
            });
        } catch(Exception e){

        }
    }

    private void addNetwork(int position, String privacy){
        // ok, so asside from the "calling method" the signature and responding event flow implementation is 99% the same as decline, WTF SON
        // on success, splice that shit, and re-render from the INSIDE SON, if that's possible
        // ideally though
        try {
            JSONObject item = getItem(position);
            // ok, ideally though we just pass on the attributes
            HealthNotifierAPI.getInstance().patientNetworkAdd(mPatientId, mPatientId, item.getString("PatientUuid"), privacy, new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    // we couldn't get a response or we timed out, offline, etc
                    Logcat.d( "onFailure" + e.toString());
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(mContext, "Error adding :(", Toast.LENGTH_SHORT).show();
                        }
                    });
                }

                @Override
                public void onResponse(Call call, okhttp3.Response response) throws IOException {
                    if (response.isSuccessful()) { // exactly what status codes determine this?
                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(mContext, "Added to LifeCircle!", Toast.LENGTH_SHORT).show();
                                Intent intent = new Intent(mContext, PatientNetworkManageActivity.class);
                                intent.putExtra("PATIENT_ID", mPatientId);
                                intent.putExtra("MODE", mMode);
                                ((Activity) mContext).startActivity(intent);
                            }
                        });
                    } else {
                        Logcat.d( "onResponseError" + response.toString());
                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(mContext, "Error adding :(", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }
            });
        } catch(Exception e){

        }
    }

    private void handleConnectClick(int position){
        try {
            JSONObject item = getItem(position);
            if (mMode.equals("inbound")) {
                // Request access dialog
                AlertDialog alertDialog = new AlertDialog.Builder(mContext).create();
                alertDialog.setTitle("Please Confirm");
                alertDialog.setMessage("Would like to join the LifeCircle of " + item.getString("Name") + ".");
                alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "Request Access",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                requestAccess(position);
                                dialog.dismiss();
                                // consider putting this in some global scope son, but whatever

                            }
                        });
                alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, "Cancel",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        });
                alertDialog.show();
            }
            if (mMode.equals("outbound")) {
                // Grant custom dialog
                AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(mContext);
                LayoutInflater inflater = LayoutInflater.from(mContext);
                final View dialogView = inflater.inflate(R.layout.dialog_connection_privacy, null);
                dialogBuilder.setView(dialogView);

                dialogBuilder.setTitle("Share Your LifeSticker?");
                String message = "";
                message = "Allow " + item.getString("Name");
                if(item.getBoolean("IsAuditor")){
                    message += " (a registered health care provider)";
                }else {

                }
                // TODO: FML privacy terminology son
                message += " to join your LifeCircle at the privacy level:";
                dialogBuilder.setMessage(message);


                dialogBuilder.setPositiveButton("Share",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {

                                RadioGroup rg = (RadioGroup) dialogView.findViewById(R.id.rgConnectionPrivacy);
                                // perhaps we validate ahead of this, in case we don't pre-select things
                                switch(rg.getCheckedRadioButtonId()){
                                    case R.id.rbPrivacyPublic:
                                        addNetwork(position, "public");
                                        dialog.dismiss();
                                        break;

                                    case R.id.rbPrivacyProvider:
                                        addNetwork(position, "provider");
                                        dialog.dismiss();
                                        break;

                                    case R.id.rbPrivacyPrivate:
                                        addNetwork(position, "private");
                                        dialog.dismiss();
                                        break;

                                    default:
                                        // TOAST ON THESE NUTS
                                        Toast.makeText(mContext, "Please choose privacy level!", Toast.LENGTH_SHORT).show();
                                        // this an error condition, assuming we don't have one
                                        // alert your shee down
                                        dialog.cancel();// hmm not sure about this
                                        break;
                                }

                                //
                            }
                        });
                dialogBuilder.setNegativeButton("Cancel",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        });

                AlertDialog b = dialogBuilder.create();
                b.show();
            }
        } catch(Exception e){
            // not much to say here, you're dead in the water
            Logcat.d("DEATH FROM ABOVE" + e.toString());
        }
    }

    static class ViewHolder {
        ImageView ivPhoto;
        TextView tvTitle;
        TextView tvDescription;
        Button btConnect;
    }

}

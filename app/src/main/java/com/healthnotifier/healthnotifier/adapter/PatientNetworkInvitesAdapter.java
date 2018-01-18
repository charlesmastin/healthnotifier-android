package com.healthnotifier.healthnotifier.adapter;

import android.content.Context;
import android.content.DialogInterface;
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

public class PatientNetworkInvitesAdapter extends JSONArrayAdapter {

    private Context mContext;
    private String mPatientId;
    private String mMode;


    private DisplayImageOptions mDisplayImageOptions;
    private ImageLoader mImageLoader = ImageLoader.getInstance();

    public PatientNetworkInvitesAdapter(Context context, ArrayList<JSONObject> node, String patientId, String mode){
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
            view = inflater.inflate(R.layout.item_patient_network_invite, null);
            holder = new ViewHolder();
            holder.tvTitle = (TextView) view.findViewById(R.id.tvPatientNetworkInviteTitle);
            holder.tvDescription = (TextView) view.findViewById(R.id.tvPatientNetworkInviteDescription);
            holder.ivPhoto = (ImageView) view.findViewById(R.id.ivPatientNetworkInvitePhoto);
            holder.btAccept = (Button) view.findViewById(R.id.btPatientNetworkInviteAccept);
            holder.btDecline = (Button) view.findViewById(R.id.btPatientNetworkInviteDecline);
            view.setTag(holder);
        } else {
            holder = (ViewHolder) view.getTag();
        }
        try {
            JSONObject item = getItem(position);
            String photoUrl = null;
            final int size = 32;
            if(mMode.equals("inbound")){

            }
            if(mMode.equals("outbound")){
                holder.tvTitle.setText(item.getString("auditor_name"));
                if(!item.isNull("auditor_photo_uuid")) {
                    photoUrl = Config.API_ROOT + "profiles/" + item.getString("auditor_uuid") + "/profile-photo?photo_uuid=" + item.getString("auditor_photo_uuid") + "&width=" + (size * 2) + "&height=" + (size * 2);
                }
                String description = item.getString("asked_at"); // TODO: date format that bitch
                // if they are a health care provider
                if(item.getBoolean("auditor_provider")) {
                    description += " | Registered health care provider";
                }
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

            holder.btAccept.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    handleAcceptClick(position);
                };
            });

            holder.btDecline.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    handleDeclineClick(position);
                };
            });



        } catch(Exception e){
            Logcat.d(e.toString());
        }

        return view;

    }

    private void declineInvite(int position){
        // on success, splice that shit, and re-render from the INSIDE SON, if that's possible
        // ideally though
        try {
            JSONObject item = getItem(position);
            // ok, ideally though we just pass on the attributes
            HealthNotifierAPI.getInstance().patientNetworkDecline(mPatientId, item.getString("granter_uuid"), item.getString("auditor_uuid"), new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    // we couldn't get a response or we timed out, offline, etc
                    Logcat.d( "onFailure" + e.toString());
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(mContext, "Error declining invite :(", Toast.LENGTH_SHORT).show();
                        }
                    });
                }

                @Override
                public void onResponse(Call call, okhttp3.Response response) throws IOException {
                    if (response.isSuccessful()) { // exactly what status codes determine this?
                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(mContext, "Invite declined!", Toast.LENGTH_SHORT).show();
                                spliceByIndex(position);
                            }
                        });
                    } else {
                        Logcat.d( "onResponseError" + response.toString());
                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(mContext, "Error declining invite :(", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }
            });
        } catch(Exception e){

        }
    }

    private void handleDeclineClick(int position){
        // double opt out of this here buns
        // ideally though, ideally…
        try {
            JSONObject item = getItem(position);

            AlertDialog alertDialog = new AlertDialog.Builder(mContext).create();
            alertDialog.setTitle("Really Decline?");
            // alertDialog.setMessage(getString(R.string.dialog_delete_account_message));
            alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "Decline Invite",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                            // consider putting this in some global scope son, but whatever
                            declineInvite(position);
                        }
                    });
            alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, "Cancel",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });
            alertDialog.show();

        } catch(Exception e){

        }
    }

    // TODO: get value of radiogroup vs handle on click

    private void handleAcceptClick(int position){
        // TODO: toss up that custom dialog son
        // placeholder for now son
        try {
            JSONObject item = getItem(position);

            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(mContext);
            LayoutInflater inflater = LayoutInflater.from(mContext);
            final View dialogView = inflater.inflate(R.layout.dialog_connection_privacy, null);
            dialogBuilder.setView(dialogView);

            dialogBuilder.setTitle("Accept Invite?");
            String message = "";
            message = "Allow " + item.getString("auditor_name");
            if(item.getBoolean("auditor_provider")){
                message += " (a registered health care provider)";
            }else {

            }
            message += " to access your LifeSticker at the privacy level:";
            dialogBuilder.setMessage(message);


            dialogBuilder.setPositiveButton("Accept",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {

                            RadioGroup rg = (RadioGroup) dialogView.findViewById(R.id.rgConnectionPrivacy);
                            // perhaps we validate ahead of this, in case we don't pre-select things
                            switch(rg.getCheckedRadioButtonId()){
                                case R.id.rbPrivacyPublic:
                                    acceptInvite(position, "public");
                                    dialog.dismiss();
                                    break;

                                case R.id.rbPrivacyProvider:
                                    acceptInvite(position, "provider");
                                    dialog.dismiss();
                                    break;

                                case R.id.rbPrivacyPrivate:
                                    acceptInvite(position, "private");
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


        } catch(Exception e){

        }

    }

    private void acceptInvite(int position, String privacy){
        // ok, so asside from the "calling method" the signature and responding event flow implementation is 99% the same as decline, WTF SON
        // on success, splice that shit, and re-render from the INSIDE SON, if that's possible
        // ideally though
        try {
            JSONObject item = getItem(position);
            // ok, ideally though we just pass on the attributes
            HealthNotifierAPI.getInstance().patientNetworkAccept(mPatientId, item.getString("granter_uuid"), item.getString("auditor_uuid"), privacy, new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    // we couldn't get a response or we timed out, offline, etc
                    Logcat.d( "onFailure" + e.toString());
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(mContext, "Error accepting invite :(", Toast.LENGTH_SHORT).show();
                        }
                    });
                }

                @Override
                public void onResponse(Call call, okhttp3.Response response) throws IOException {
                    if (response.isSuccessful()) { // exactly what status codes determine this?
                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(mContext, "Invite accepted!", Toast.LENGTH_SHORT).show();
                                spliceByIndex(position);
                                // AND, add that top level shee ma nees hack
                                // TODO: the parent screen needs to "re-load" the data to visually indicate the profile has been moved from invite to connection, yea son
                                // PAIN IN DA SACK, but the easiest solution By-Far is to simpy requery all the shiz
                            }
                        });
                    } else {
                        Logcat.d( "onResponseError" + response.toString());
                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(mContext, "Error accepting invite :(", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }
            });
        } catch(Exception e){

        }
    }

    static class ViewHolder {
        ImageView ivPhoto;
        TextView tvTitle;
        TextView tvDescription;
        Button btAccept;
        Button btDecline;
    }

}

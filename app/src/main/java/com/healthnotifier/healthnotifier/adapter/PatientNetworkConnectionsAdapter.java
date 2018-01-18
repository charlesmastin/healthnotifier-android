package com.healthnotifier.healthnotifier.adapter;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.PopupMenu;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.healthnotifier.healthnotifier.network.HealthNotifierAPI;
import com.healthnotifier.healthnotifier.Config;
import com.healthnotifier.healthnotifier.R;
import com.healthnotifier.healthnotifier.activity.LifesquareActivity;
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

public class PatientNetworkConnectionsAdapter extends JSONArrayAdapter {

    private Context mContext;
    private String mPatientId;
    private String mMode;

    private DisplayImageOptions mDisplayImageOptions;
    private ImageLoader mImageLoader = ImageLoader.getInstance();

    public PatientNetworkConnectionsAdapter(Context context, ArrayList<JSONObject> node, String patientId, String mode){
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
            view = inflater.inflate(R.layout.item_patient_network_connection, null);
            holder = new ViewHolder();
            holder.tvTitle = (TextView) view.findViewById(R.id.tvPatientNetworkConnectionTitle);
            holder.tvDescription = (TextView) view.findViewById(R.id.tvPatientNetworkConnectionDescription);
            holder.ivPhoto = (ImageView) view.findViewById(R.id.ivPatientNetworkConnectionPhoto);
            // holder.btConnect = (Button) view.findViewById(R.id.btPatientNetworkSearchResultConnect);
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
                // if they are a health care provider
                if(item.getBoolean("granter_provider")) {
                    holder.tvDescription.setText("Registered health care provider");
                } else {
                    // hide it son
                    holder.tvDescription.setVisibility(View.INVISIBLE);
                }
            }
            if(mMode.equals("outbound")){
                // if they are a health care provider
                holder.tvTitle.setText(item.getString("auditor_name"));
                if(item.getBoolean("auditor_provider")) {
                    holder.tvTitle.setText(item.getString("auditor_name") + " (provider)");
                }
                if(!item.isNull("auditor_photo_uuid")) {
                    photoUrl = Config.API_ROOT + "profiles/" + item.getString("auditor_uuid") + "/profile-photo?photo_uuid=" + item.getString("auditor_photo_uuid") + "&width=" + (size * 2) + "&height=" + (size * 2);
                }
                String description = "Shared with privacy: " + HealthNotifierAPI.getInstance().getNameForValue("privacy", null, item.getString("privacy"));
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

            // click SON
            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    showPopup(view, position);
                };
            });

        } catch(Exception e){
            Logcat.d(e.toString());
        }

        return view;

    }

    private void showPopup(View view, int position){
        PopupMenu popup = new PopupMenu(mContext, view);
        MenuInflater inflater = popup.getMenuInflater();
        if(mMode.equals("inbound")) {
            inflater.inflate(R.menu.menu_patient_network_connection_inbound, popup.getMenu());
        }
        if(mMode.equals("outbound")) {
            inflater.inflate(R.menu.menu_patient_network_connection_outbound, popup.getMenu());
        }
        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch(item.getItemId()){
                    case R.id.action_patient_network_update:
                        handleUpdate(position);
                        break;
                    case R.id.action_patient_network_leave:
                        handleDelete(position, "leave");
                        break;
                    case R.id.action_patient_network_revoke:
                        handleDelete(position, "revoke");
                        break;
                    case R.id.action_patient_network_view_lifesquare:
                        handleViewLifesquare(position);
                        break;
                    default:
                        break;
                }
                return true;
            }
        });
        popup.show();
    }

    private void updateConnection(int position, String privacy){
        try {
            JSONObject item = getItem(position);
            HealthNotifierAPI.getInstance().patientNetworkUpdate(mPatientId, item.getString("granter_uuid"), item.getString("auditor_uuid"), privacy, new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    // we couldn't get a response or we timed out, offline, etc
                    Logcat.d( "onFailure" + e.toString());
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(mContext, "Error updating privacy :(", Toast.LENGTH_SHORT).show();
                        }
                    });
                }

                @Override
                public void onResponse(Call call, okhttp3.Response response) throws IOException {
                    if (response.isSuccessful()) { // exactly what status codes determine this?
                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(mContext, "Privacy Updated!", Toast.LENGTH_SHORT).show();
                                // spliceByIndex(position);
                                // ROUND TRIP TO THE SERVER BITCH
                                // for cheap times now

                                // "assign" back to local data
                                // and redraw
                                try {
                                    item.put("privacy", privacy);
                                    // meh
                                    notifyDataSetChanged();
                                } catch(Exception e){

                                }
                            }
                        });
                    } else {
                        Logcat.d( "onResponseError" + response.toString());
                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(mContext, "Error updating privacy :(", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }
            });
        } catch(Exception e){

        }
    }

    private void handleUpdate(int position){
        // toss that dialog up in there
        try {
            JSONObject item = getItem(position);

            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(mContext);
            LayoutInflater inflater = LayoutInflater.from(mContext);
            final View dialogView = inflater.inflate(R.layout.dialog_connection_privacy, null);
            dialogBuilder.setView(dialogView);

            dialogBuilder.setTitle("Update Sharing");
            String message = "";
            message = "Allow " + item.getString("auditor_name");
            if(item.getBoolean("auditor_provider")){
                message += " (a registered health care provider)";
            }else {

            }
            message += " to access your LifeSticker at the privacy level:";
            dialogBuilder.setMessage(message);

            switch(item.getString("privacy")){
                case "public":
                    ((RadioButton) dialogView.findViewById(R.id.rbPrivacyPublic)).setChecked(true);
                    break;
                case "provider":
                    ((RadioButton) dialogView.findViewById(R.id.rbPrivacyProvider)).setChecked(true);
                    break;
                case "private":
                    ((RadioButton) dialogView.findViewById(R.id.rbPrivacyPrivate)).setChecked(true);
                    break;
            }

            dialogBuilder.setPositiveButton("Update",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {

                            RadioGroup rg = (RadioGroup) dialogView.findViewById(R.id.rgConnectionPrivacy);
                            // perhaps we validate ahead of this, in case we don't pre-select things
                            switch(rg.getCheckedRadioButtonId()){
                                case R.id.rbPrivacyPublic:
                                    updateConnection(position, "public");
                                    dialog.dismiss();
                                    break;

                                case R.id.rbPrivacyProvider:
                                    updateConnection(position, "provider");
                                    dialog.dismiss();
                                    break;

                                case R.id.rbPrivacyPrivate:
                                    updateConnection(position, "private");
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

    private void deleteConnection(int position, String action){
        try {
            JSONObject item = getItem(position);
            Callback callback = new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    // we couldn't get a response or we timed out, offline, etc
                    Logcat.d("onFailure" + e.toString());
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(mContext, "Error removing connection :(", Toast.LENGTH_SHORT).show();
                        }
                    });
                }

                @Override
                public void onResponse(Call call, okhttp3.Response response) throws IOException {
                    if (response.isSuccessful()) { // exactly what status codes determine this?
                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(mContext, "Connection Removed!", Toast.LENGTH_SHORT).show();
                                spliceByIndex(position);
                            }
                        });
                    } else {
                        Logcat.d("onResponseError" + response.toString());
                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(mContext, "Error removing connection :(", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }
            };
            if(action.equals("leave")){
                HealthNotifierAPI.getInstance().patientNetworkLeave(mPatientId, item.getString("granter_uuid"), item.getString("auditor_uuid"), callback);
            }
            if(action.equals("revoke")){
                HealthNotifierAPI.getInstance().patientNetworkRevoke(mPatientId, item.getString("granter_uuid"), item.getString("auditor_uuid"), callback);
            }
        } catch(Exception e){

        }
    }

    private void handleDelete(int position, String action){
        // TODO: JW behind alert dialog for confirmation
        try {
            JSONObject item = getItem(position);
            AlertDialog alertDialog = new AlertDialog.Builder(mContext).create();
            String cta = "Delete";
            alertDialog.setTitle("Please Confirm");
            if(action.equals("leave")){
                cta = "Leave LifeCircle";
                alertDialog.setMessage("Leave " + item.getString("granter_name") + "‘s LifeCircle?");
            }
            if(action.equals("revoke")){
                cta = "Remove from LifeCircle";
                alertDialog.setMessage("Remove " + item.getString("auditor_name") + " from your LifeCircle?");
            }
            alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, cta,
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                            // consider putting this in some global scope son, but whatever
                            deleteConnection(position, action);
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

    private void handleViewLifesquare(int position){
        try {
            JSONObject item = getItem(position);
            Intent intent;
            intent = new Intent(mContext, LifesquareActivity.class);
            intent.putExtra("PATIENT_ID", item.getString("granter_uuid"));
            intent.putExtra("PATIENT_NAME", item.getString("granter_name"));
            // ok, yea, this isn't gonna work outta the box, we'll need a new network endpoint in rails, perhaps, most likely
            // TODO: this is minor security/privacy concern issue for Android, only minor though
            String url = Config.API_ROOT + "lifesquares/" + item.getString("granter_lifesquare") + "/webview";
            intent.putExtra("PATIENT_WEBVIEW_URL", url);
            mContext.startActivity(intent);
        } catch(Exception e){

        }
    }

    static class ViewHolder {
        ImageView ivPhoto;
        TextView tvTitle;
        TextView tvDescription;
        // Button btConnect;
    }

}

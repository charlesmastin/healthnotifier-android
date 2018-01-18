package com.healthnotifier.healthnotifier.adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.healthnotifier.healthnotifier.network.HealthNotifierAPI;
import com.healthnotifier.healthnotifier.R;
import com.healthnotifier.healthnotifier.activity.EditCollectionItemActivity;
import com.healthnotifier.healthnotifier.activity.EditDocumentActivity;

import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Created by charles on 1/30/17.
 */

public class CollectionAdapter extends JSONArrayAdapter {

    private Context mContext;
    private String mCollectionId;
    private String mPatientId;
    public Boolean mEdit = true; //GHETTO ARCHITECTURE HACK, too lazy to refactor signature of constructor
    // model instance?? template? huh what
    public CollectionAdapter(Context context, ArrayList<JSONObject> node, String collectionId, String patientId){
        mContext = context;
        mItems = node;
        mCollectionId = collectionId;
        mPatientId = patientId;
    }

    @Override
    public int getCount() {
        // if that's a thing
        if(mEdit) {
            return super.getCount() + 1;
        } else {
            return super.getCount();
        }
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if(position < mItems.size() && mItems.size() > 0){
            // aka
            View view = convertView;
            if(view == null){
                LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                view = inflater.inflate(R.layout.item_collection_item, null);
                ViewHolder holder = new ViewHolder();
                holder.tvTitle = (TextView) view.findViewById(R.id.tvCollectionItemTitle);
                holder.tvDescription = (TextView) view.findViewById(R.id.tvCollectionItemDescription);
                // holder.ivPhoto = (ImageView) view.findViewById(R.id.ivCollectionItemPhoto);
                view.setTag(holder);
            }
            try {
                JSONObject item = (JSONObject) getItem(position);
                final ViewHolder holder = (ViewHolder) view.getTag();

                // check for null
                if(!item.isNull("title")) {
                    holder.tvTitle.setText(item.getString("title"));
                }
                if(!item.isNull("description")) {
                    holder.tvDescription.setText(item.getString("description"));
                }

                // tailored - in honesty, we know these are going to move to the "document" adapter or view section that is + image
                if(mCollectionId.equals("documents")){
                    // the Title is the user "title" but we also have the Category
                    String title = HealthNotifierAPI.getInstance().getNameForValue("document", null, item.getString("category"));
                    if(!item.isNull("title")) {
                        title += ": (" + item.getString("title") + ")";
                    }
                    holder.tvTitle.setText(title);
                }

                if(mCollectionId.equals("directives")){
                    String title = HealthNotifierAPI.getInstance().getNameForValue("directive", null, item.getString("category"));
                    holder.tvTitle.setText(title);
                }
                if(mCollectionId.equals("emergency")){
                    String d = "";
                    if(!item.isNull("description")) {
                        d = item.getString("description");
                    }
                    if (item.getBoolean("power_of_attorney")) {
                        d += ", Power of Attorney";
                    }
                    if (item.getBoolean("next_of_kin")) {
                        d += ", Next of Kin";
                    }
                    holder.tvDescription.setText(d);
                }

                // TOOD: image view

                // click SON
                view.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        handleItemClick(position);
                    };
                });

            } catch(Exception e){

            }

            return view;
        } else {
            // for now jus the + Add Item button, lolzones
            View view = convertView;
            if(view == null) {
                LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                view = inflater.inflate(R.layout.item_add_collection_item, null);
                ViewHolderAdd holder = new ViewHolderAdd();
                holder.btAddItem = (Button) view.findViewById(R.id.btAddItem);
                view.setTag(holder);

                holder.btAddItem.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        handleItemClick(position);
                    };
                });

                // we will potentially disambiguate these guys

                view.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        handleItemClick(position);
                    };
                });

            }
            return view;
        }

    }

    private void handleItemClick(int position){
        // if we are documents / directives/// queue that shit up
        if(mCollectionId.equals("documents") || mCollectionId.equals("directives")){
            // yup, double up the logic up in this bitch
            if(position < mItems.size() && mItems.size() > 0){
                try {
                    JSONObject item = (JSONObject) getItem(position);
                    Intent intent = new Intent(mContext, EditDocumentActivity.class);
                    intent.putExtra("PATIENT_UUID", mPatientId);
                    intent.putExtra("COLLECTION_ID", mCollectionId);
                    intent.putExtra("COLLECTION_ITEM_JSON", item.toString());
                    intent.putExtra("CALLING_ACTIVITY_NAME", mContext.getClass().getName());
                    mContext.startActivity(intent);
                } catch (Exception e) {
                    // you really f'd it up son
                }
            } else {
                // create new
                Intent intent = new Intent(mContext, EditDocumentActivity.class);
                intent.putExtra("PATIENT_UUID", mPatientId);
                intent.putExtra("COLLECTION_ID", mCollectionId);
                intent.putExtra("CALLING_ACTIVITY_NAME", mContext.getClass().getName());
                mContext.startActivity(intent);
            }
        } else {
            if(position < mItems.size() && mItems.size() > 0){
                try {
                    JSONObject item = (JSONObject) getItem(position);
                    Intent intent = new Intent(mContext, EditCollectionItemActivity.class);
                    intent.putExtra("PATIENT_UUID", mPatientId);
                    intent.putExtra("COLLECTION_ID", mCollectionId);
                    intent.putExtra("COLLECTION_ITEM_JSON", item.toString());
                    intent.putExtra("CALLING_ACTIVITY_NAME", mContext.getClass().getName());
                    mContext.startActivity(intent);
                } catch (Exception e) {
                    // you really f'd it up son
                }
            } else {
                // this is misleading though
                // lol automatic hack to just create the stuffs
                Intent intent = new Intent(mContext, EditCollectionItemActivity.class);
                intent.putExtra("PATIENT_UUID", mPatientId);
                intent.putExtra("COLLECTION_ID", mCollectionId);
                intent.putExtra("CALLING_ACTIVITY_NAME", mContext.getClass().getName());
                mContext.startActivity(intent);
            }
        }
    }

    static class ViewHolder {
        // ImageView ivPhoto;
        TextView tvTitle;
        TextView tvDescription;
        // other stufffs
        // photo?
    }

    static class ViewHolderAdd {
        Button btAddItem;
    }
}

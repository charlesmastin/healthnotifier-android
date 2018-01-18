package com.healthnotifier.healthnotifier.adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.healthnotifier.healthnotifier.R;
import com.healthnotifier.healthnotifier.activity.CareplanQuestionGroupActivity;

import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Created by charles on 2/17/17.
 */

public class CareplansAdapter extends JSONArrayAdapter {
    private Context mContext;
    private String mPatientId;

    public CareplansAdapter(Context context, ArrayList<JSONObject> node, String patientId) {
        mContext = context;
        mPatientId = patientId;
        mItems = node;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = convertView;
        final CareplansAdapter.ViewHolder holder;
        if(view == null){
            LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = inflater.inflate(R.layout.item_careplan, null);
            holder = new CareplansAdapter.ViewHolder();
            holder.tvTitle = (TextView) view.findViewById(R.id.tvCareplanTitle); // YOLO, we should just use a basic id here son
            holder.tvDescription = (TextView) view.findViewById(R.id.tvCareplanDescription);
            view.setTag(holder);
        } else {
            holder = (CareplansAdapter.ViewHolder) view.getTag();
        }
        try {
            JSONObject item = getItem(position);
            holder.tvTitle.setText(item.getString("name"));
            holder.tvDescription.setText("");

            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    handleClick(position);
                }
            });
        } catch(Exception e){

        }
        return view;
    }

    private void handleClick(int position){
        try {
            JSONObject item = getItem(position);
            Intent intent = new Intent(mContext, CareplanQuestionGroupActivity.class);
            intent.putExtra("PATIENT_ID", mPatientId);
            intent.putExtra("CAREPLAN_ID", item.getString("uuid"));
            intent.putExtra("CAREPLAN_QUESTION_GROUP_ID", item.getString("initial_question_group_uuid"));
            mContext.startActivity(intent);
        } catch(Exception e){

        }
    }

    // TODO: wish we could straight adapt to a default single item "list" item thingy, but I digressm nothing for free here

    static class ViewHolder {
        TextView tvTitle;
        TextView tvDescription;
    }

}

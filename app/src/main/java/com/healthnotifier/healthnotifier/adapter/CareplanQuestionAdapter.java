package com.healthnotifier.healthnotifier.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.healthnotifier.healthnotifier.HealthNotifierApplication;
import com.healthnotifier.healthnotifier.R;
import com.healthnotifier.healthnotifier.utility.GenericEvent;
import com.healthnotifier.healthnotifier.utility.Logcat;
import com.squareup.otto.Bus;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by charles on 2/17/17.
 */

public class CareplanQuestionAdapter extends RecyclerView.Adapter<CareplanQuestionAdapter.MyViewHolder> {

    private Context mContext;
    private ArrayList<JSONObject> mItems = new ArrayList<JSONObject>();

    public CareplanQuestionAdapter(Context context, ArrayList<JSONObject> items) {
        this.mContext = context;
        this.mItems = items;
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

    public class MyViewHolder extends RecyclerView.ViewHolder {

        public TextView tvTitle;
        public TextView tvDescription;
        public RadioGroup rgChoices;
        // we can store the actual data structure up in this sucker, so we never have to return
        public JSONArray choices;// too lazy to do ArrayList<JSONObject>
        public String mQuestionId;

        public MyViewHolder(View view) {
            super(view);
            tvTitle = (TextView) view.findViewById(R.id.tvTitle);
            tvDescription = (TextView) view.findViewById(R.id.tvDescription);
            rgChoices = (RadioGroup) view.findViewById(R.id.rgChoices);
        }
    }

    @Override
    public void onBindViewHolder(final CareplanQuestionAdapter.MyViewHolder holder, int position) {
        try {
            JSONObject item = mItems.get(position);
            holder.tvTitle.setText(item.getString("name"));
            holder.tvDescription.setText(item.getString("description"));
            holder.mQuestionId = item.getString("uuid");
            holder.choices = item.getJSONArray("choices");
            for(int i=0;i<holder.choices.length();i++) {
                // meh
                try {
                    JSONObject cJson = (JSONObject) holder.choices.get(i);
                    RadioButton choice = new RadioButton(mContext);
                    choice.setChecked(false);
                    choice.setText(cJson.getString("name"));
                    choice.setId(i);// this is gonna be mad cheesy, just use the index, mmmkay
                    holder.rgChoices.addView(choice);
                } catch(Exception e){
                }
            }
            // handle dat onChange for dem radio buts
            holder.rgChoices.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(RadioGroup group, int checkedId) {
                    // this only works if we can access back to the holder, so let's hope the scope works as expected during this here assignment
                    // but considering that shiznit was declared as final, it should
                    // brizzle ma nizzle broadcastizzle dat check state UP DA CHAIN O COMMAND
                    Bus bus = HealthNotifierApplication.bus;
                    Map<String, Object> attributes = new HashMap<String, Object>();
                    attributes.put("QuestionId", holder.mQuestionId);
                    try {
                        attributes.put("QuestionChoiceId", ((JSONObject) holder.choices.get(checkedId)).getString("uuid"));
                    } catch(Exception e){
                        // your pants are on fire son
                    }
                    bus.post(new GenericEvent("onChoiceChange", attributes));
                }
            });

            // name, and uuid baked into that sucker
        } catch(Exception e){

        }
    }

    @Override
    public CareplanQuestionAdapter.MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.card_careplan_question, parent, false);
        return new CareplanQuestionAdapter.MyViewHolder(itemView);
    }
}

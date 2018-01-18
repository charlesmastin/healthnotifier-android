package com.healthnotifier.healthnotifier.adapter;

import java.util.ArrayList;

import com.healthnotifier.healthnotifier.R;
import com.healthnotifier.healthnotifier.model.Lifesquare;

import android.content.Context;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.json.JSONObject;

public class FindLifesquaresAdapter extends JSONArrayAdapter {

	private Context mContext;
	private int mSelectedPosition = 0;

	public FindLifesquaresAdapter(Context context, ArrayList<JSONObject> items) {
		mContext = context;
		mItems = items;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		// inflate view
		View view = convertView;
		if (view == null) {
			LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			view = inflater.inflate(R.layout.item_find_lifesquares, null);

			// view holder
			ViewHolder holder = new ViewHolder();
			holder.tvName = (TextView) view.findViewById(R.id.tvName);
			holder.tvAddress = (TextView) view.findViewById(R.id.tvAddress);
			holder.tvLifesquareLocation = (TextView) view.findViewById(R.id.tvLifesquareLocation);
			view.setTag(holder);
		}

		try {
			JSONObject item = getItem(position);
            Lifesquare lifesquare = new Lifesquare();
            lifesquare.setNode(item); // pimp it out brizzle
            // view holder
            ViewHolder holder = (ViewHolder) view.getTag();

            // content
            holder.tvName.setText(lifesquare.getName());
            String formattedAddress = lifesquare.getFormattedAddress();
            String lifesquareLocation = lifesquare.getLifesquareLocation();
            if(formattedAddress != null){
                holder.tvAddress.setVisibility(View.VISIBLE);
                holder.tvAddress.setText(formattedAddress);
                if(lifesquareLocation != null){
                    if(!lifesquareLocation.equals("")) {
                        holder.tvLifesquareLocation.setVisibility(View.VISIBLE);
                        holder.tvLifesquareLocation.setText("LifeCircle Location: " + lifesquareLocation);
                    } else {
                        holder.tvLifesquareLocation.setVisibility(View.GONE);
                        // holder.tvLifesquareLocation.setText("LifeCircle Location: Unkown");
                    }
                } else {
                    holder.tvLifesquareLocation.setVisibility(View.GONE);
                }
            } else {
                holder.tvAddress.setVisibility(View.GONE);
                holder.tvLifesquareLocation.setVisibility(View.GONE);
            }
            // TODO: distance, check iOS

		} catch(Exception e){

		}

		return view;
	}

	static class ViewHolder {
		TextView tvName;
		TextView tvAddress;
		TextView tvLifesquareLocation;
	}
}

package com.healthnotifier.healthnotifier.adapter;

import java.util.ArrayList;

import com.healthnotifier.healthnotifier.R;
import com.healthnotifier.healthnotifier.model.MainNavigationItem;

import android.content.Context;

import android.graphics.PorterDuff;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class MainNavigationAdapter extends BaseAdapter {

	private Context mContext;
	private ArrayList<MainNavigationItem> mNavigationList;
	private int mSelectedPosition = 0;

	private boolean mIsProvider = false;

	public MainNavigationAdapter(Context context, ArrayList<MainNavigationItem> navigationList) {
		mContext = context;
		mNavigationList = navigationList;
	}

	@Override
	public int getCount() {
		return mNavigationList.size();
	}

	@Override
	public Object getItem(int position) {
		if (mNavigationList != null)
			return mNavigationList.size();
		else
			return 0;
	}

	@Override
	public long getItemId(int position) {
		return 0;
	}

	public void refill(Context context, ArrayList<MainNavigationItem> navigationList) {
		mContext = context;
		mNavigationList = navigationList;
		notifyDataSetChanged();
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		// inflate view
		View view = convertView;
		if (view == null) {
			LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			view = inflater.inflate(R.layout.item_navigation, null);

			// view holder
			ViewHolder holder = new ViewHolder();
			holder.tvName = (TextView) view.findViewById(R.id.tvName);
			holder.tvCount = (TextView) view.findViewById(R.id.tvCount);
			holder.ivIcon = (ImageView) view.findViewById(R.id.ivIcon);
			holder.vDivider = view.findViewById(R.id.vDivider);
			view.setTag(holder);
		}

		// entity
		MainNavigationItem item = (MainNavigationItem) mNavigationList.get(position);

		if (item != null) {
			// view holder
			ViewHolder holder = (ViewHolder) view.getTag();

			holder.tvName.setText(item.label);
			/*
			// count badge LOL
			if(item.getCount()>0){
				holder.tvCount.setText(String.valueOf(item.getCount()));
				holder.tvCount.setVisibility(View.VISIBLE);
			}else{
				holder.tvCount.setVisibility(View.GONE);
			}
			*/
            holder.ivIcon.setImageResource(item.icon);

            // TODO: workaround for lower API tint mode SON
			if(mSelectedPosition == position) {
				holder.tvName.setTextColor(mContext.getResources().getColor(R.color.drawer_item_selected));
                holder.ivIcon.setColorFilter(mContext.getResources().getColor(R.color.drawer_item_selected), PorterDuff.Mode.SRC_ATOP);
			} else {
				holder.tvName.setTextColor(mContext.getResources().getColor(R.color.drawer_item_normal));
                holder.ivIcon.setColorFilter(mContext.getResources().getColor(R.color.drawer_item_normal), PorterDuff.Mode.SRC_ATOP);
			}
			
			if(position == (mNavigationList.size()-1)){
				holder.vDivider.setVisibility(View.GONE);
			}else{
				holder.vDivider.setVisibility(View.VISIBLE);
			}
			
		}

		return view;
	}
	
	public void setSelectedPosition(int position)
	{
		mSelectedPosition = position;
		notifyDataSetChanged();
	}


	public int getSelectedPosition()
	{
		return mSelectedPosition;
	}

	static class ViewHolder {
		TextView tvName;
		TextView tvCount;
		ImageView ivIcon;
		View vDivider;
	}
}

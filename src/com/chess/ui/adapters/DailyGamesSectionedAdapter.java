package com.chess.ui.adapters;

import android.content.Context;
import android.database.DataSetObserver;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import com.chess.R;

public class DailyGamesSectionedAdapter extends SectionedListAdapter {

	public DailyGamesSectionedAdapter(Context context) {
		super(context);
	}

	@Override
	protected View getHeaderView(String caption, int index, View convertView, ViewGroup parent) {
		if (convertView == null) {
			convertView = LayoutInflater.from(getContext()).inflate(R.layout.new_daily_games_section_header, parent, false);
			createViewHolder(convertView);
		}
		bindView(convertView, caption);
		return convertView;
	}

	private void createViewHolder(View convertView) {
		ViewHolder viewHolder = new ViewHolder();
		viewHolder.text = (TextView) convertView.findViewById(R.id.headerTitle);
		convertView.setTag(viewHolder);
	}

	private void bindView(View convertView, String text) {
		ViewHolder view = (ViewHolder) convertView.getTag();
		view.text.setText(text);
	}



	private class ViewHolder {
		TextView text;
	}

	@Override
	public void registerDataSetObserver(DataSetObserver observer) {
		super.registerDataSetObserver(observer);
		for (Section s : sections) {
			if (s.adapter instanceof BaseAdapter) {
				try {
					s.adapter.registerDataSetObserver(observer);
				} catch (Exception e) {
					// we have the same observable objects
					// so, do nothing
					Log.e("section adapter", "error" + e.getMessage());
				}
			}
		}
	}
}
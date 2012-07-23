package com.chess.ui.adapters;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.chess.R;
import com.chess.backend.statics.StaticData;
import com.chess.model.GameListCurrentItem;

import java.util.List;

public class OnlineCurrentGamesAdapter extends ItemsAdapter<GameListCurrentItem> {


	public OnlineCurrentGamesAdapter(Context context, List<GameListCurrentItem> itemList) {
		super(context, itemList);
	}

	@Override
	protected View createView(ViewGroup parent) {
		View view = inflater.inflate(R.layout.game_list_item, parent, false);
		ViewHolder holder = new ViewHolder();
		holder.playerTxt = (TextView) view.findViewById(R.id.playerTxt);
		holder.gameInfoTxt = (TextView) view.findViewById(R.id.gameInfoTxt);

		view.setTag(holder);
		return view;
	}

	@Override
	protected void bindView(GameListCurrentItem item, int pos, View convertView) {
		ViewHolder holder = (ViewHolder) convertView.getTag();

		String gameType = StaticData.SYMBOL_EMPTY;
		if (item.getGameType() != null && item.getGameType().equals("2")) {
			gameType = " (960)";
		}

		String draw = StaticData.SYMBOL_EMPTY;
		if (item.getIsDrawOfferPending().equals("p"))
			draw = "\n" + context.getString(R.string.drawoffered);

		holder.playerTxt.setText(item.getOpponentUsername() + gameType + draw);

		String infoText = StaticData.SYMBOL_EMPTY;
		if (item.getIsMyTurn().equals("1")) {

			String amount = item.getTimeRemainingAmount();
			if (amount.substring(0, 1).equals("0"))
				amount = amount.substring(1);
			if (item.getTimeRemainingUnits().equals("h"))
				infoText = amount + context.getString(R.string.hours);
			else
				infoText = amount + context.getString(R.string.days);
		}

		holder.gameInfoTxt.setText(infoText);
	}

	protected class ViewHolder {
		public TextView playerTxt;
		public TextView gameInfoTxt;
	}
}

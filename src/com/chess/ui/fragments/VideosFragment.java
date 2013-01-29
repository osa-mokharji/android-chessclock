package com.chess.ui.fragments;

import android.content.Context;
import android.os.Bundle;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;
import com.chess.R;
import com.chess.backend.RestHelper;
import com.chess.backend.entity.LoadItem;
import com.chess.backend.entity.new_api.VideoItem;
import com.chess.backend.interfaces.ActionBarUpdateListener;
import com.chess.backend.statics.AppData;
import com.chess.backend.statics.StaticData;
import com.chess.backend.tasks.RequestJsonTask;
import com.chess.ui.adapters.CustomSectionedAdapter;
import com.chess.ui.adapters.NewVideosAdapter;
import com.chess.ui.interfaces.ItemClickListenerFace;
import com.chess.utilities.AppUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

/**
 * Created with IntelliJ IDEA.
 * User: roger sent2roger@gmail.com
 * Date: 27.01.13
 * Time: 19:12
 */
public class VideosFragment extends CommonLogicFragment implements ItemClickListenerFace {
	public static final String GREY_COLOR_DIVIDER = "##";
	// 11/15/12 | 27 min
	private static final SimpleDateFormat dateFormatter = new SimpleDateFormat("dd/MM/yy");

	private VideosItemUpdateListener randomVideoUpdateListener;


	private String[] categories;
	private CustomSectionedAdapter sectionedAdapter;
	private NewVideosAdapter amazingGamesAdapter;
	private NewVideosAdapter endGamesGamesAdapter;
	private NewVideosAdapter openingsGamesAdapter;
	private NewVideosAdapter rulesBasicGamesAdapter;
	private NewVideosAdapter strategyGamesAdapter;
	private NewVideosAdapter tacticsGamesAdapter;

	private ViewHolder holder;
	private ForegroundColorSpan foregroundSpan;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		categories = getResources().getStringArray(R.array.category);
/*
<item>Amazing games</item>
<item>Endgames</item>
<item>Openings</item>
<item>Rules Basics</item>
<item>Strategy</item>
<item>Tactics</item>
*/

		amazingGamesAdapter = new NewVideosAdapter(getActivity(), new ArrayList<VideoItem.VideoDataItem>());
		endGamesGamesAdapter = new NewVideosAdapter(getActivity(), new ArrayList<VideoItem.VideoDataItem>());
		openingsGamesAdapter = new NewVideosAdapter(getActivity(), new ArrayList<VideoItem.VideoDataItem>());
		rulesBasicGamesAdapter = new NewVideosAdapter(getActivity(), new ArrayList<VideoItem.VideoDataItem>());
		strategyGamesAdapter = new NewVideosAdapter(getActivity(), new ArrayList<VideoItem.VideoDataItem>());
		tacticsGamesAdapter = new NewVideosAdapter(getActivity(), new ArrayList<VideoItem.VideoDataItem>());

		sectionedAdapter = new CustomSectionedAdapter(this, R.layout.new_arrow_section_header);

		sectionedAdapter.addSection(categories[0], amazingGamesAdapter);
		sectionedAdapter.addSection(categories[1], endGamesGamesAdapter);
		sectionedAdapter.addSection(categories[2], openingsGamesAdapter);
		sectionedAdapter.addSection(categories[3], rulesBasicGamesAdapter);
		sectionedAdapter.addSection(categories[4], strategyGamesAdapter);
		sectionedAdapter.addSection(categories[5], tacticsGamesAdapter);

		int lightGrey = getResources().getColor(R.color.new_subtitle_light_grey);
		foregroundSpan = new ForegroundColorSpan(lightGrey);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.new_videos_frame, container, false); // TODO restore
//		return inflater.inflate(R.layout.new_common_test, container, false);
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

		ListView listView = (ListView) view.findViewById(R.id.listView);
		listView.setAdapter(sectionedAdapter);

		holder = new ViewHolder();
		holder.titleTxt = (TextView) view.findViewById(R.id.titleTxt);
		holder.authorTxt = (TextView) view.findViewById(R.id.authorTxt);
		holder.dateTxt = (TextView) view.findViewById(R.id.dateTxt);
	}

	@Override
	public void onStart() {
		super.onStart();

		init();

		updateData();
	}

	@Override
	public void onStop() {
		super.onStop();
		randomVideoUpdateListener.releaseContext();
		randomVideoUpdateListener = null;
	}

	private void init() {
		randomVideoUpdateListener = new VideosItemUpdateListener(VideosItemUpdateListener.RANDOM);
	}

	private void updateData() {
		// get random video

		LoadItem loadItem = new LoadItem();
		loadItem.setLoadPath(RestHelper.CMD_VIDEOS);
		loadItem.addRequestParams(RestHelper.P_LOGIN_TOKEN, AppData.getUserToken(getContext()));
		loadItem.addRequestParams(RestHelper.P_PAGE_SIZE, RestHelper.V_VIDEO_ITEM_ONE);
		loadItem.addRequestParams(RestHelper.P_ITEMS_PER_PAGE, RestHelper.V_VIDEO_ITEM_ONE);

		new RequestJsonTask<VideoItem>(randomVideoUpdateListener).executeTask(loadItem);

		// get 2 items from every category
		for (int i = 0; i < categories.length; i++) {
			makeNextCategoryRequest(i);
		}

	}

	private void makeNextCategoryRequest(int code){  // TODO optimize
		String category = categories[code];
		VideosItemUpdateListener videoUpdateListener = new VideosItemUpdateListener(code);

		LoadItem loadItem = new LoadItem();

		loadItem.setLoadPath(RestHelper.CMD_VIDEOS);
		loadItem.addRequestParams(RestHelper.P_LOGIN_TOKEN, AppData.getUserToken(getContext()));
		loadItem.addRequestParams(RestHelper.P_PAGE_SIZE, RestHelper.V_VIDEO_ITEM_ONE);
		loadItem.addRequestParams(RestHelper.P_ITEMS_PER_PAGE, RestHelper.V_VIDEO_ITEM_ONE);
		loadItem.addRequestParams(RestHelper.P_CATEGORY, category);
		new RequestJsonTask<VideoItem>(videoUpdateListener).executeTask(loadItem);
	}

	@Override
	public Context getMeContext() {
		return getActivity();
	}

	private class VideosItemUpdateListener extends ActionBarUpdateListener<VideoItem> {

		final static int AMAZING_GAMES = 0;
		final static int END_GAMES = 1;
		final static int OPENINGS = 2;
		final static int RULES_BASICS = 3;
		final static int STRATEGY = 4;
		final static int TACTICS = 5;
		final static int RANDOM = 6;

		private int listenerCode;

		public VideosItemUpdateListener(int listenerCode) {
			super(getInstance(), VideoItem.class);
			this.listenerCode = listenerCode;
		}

		@Override
		public void updateData(VideoItem returnedObj) {

			switch (listenerCode){
				case RANDOM:

					VideoItem.VideoDataItem item = returnedObj.getData().getVideos().get(0);
					String firstName = item.getFirst_name();
					CharSequence chessTitle = item.getChess_title();
					String lastName =  item.getLast_name();
					CharSequence authorStr = GREY_COLOR_DIVIDER + chessTitle + GREY_COLOR_DIVIDER + StaticData.SYMBOL_SPACE
							+ firstName + StaticData.SYMBOL_SPACE + lastName;
					authorStr = AppUtils.setSpanBetweenTokens(authorStr, GREY_COLOR_DIVIDER, foregroundSpan);
					holder.authorTxt.setText(authorStr);

					holder.titleTxt.setText(item.getName());
					holder.dateTxt.setText(dateFormatter.format(new Date(item.getLive_date()))
							+ StaticData.SYMBOL_SPACE + item.getMinutes() + " min"); // TODO


					break;
				case AMAZING_GAMES:
					amazingGamesAdapter.setItemsList(returnedObj.getData().getVideos());
					amazingGamesAdapter.notifyDataSetInvalidated();
					break;
				case END_GAMES:
					endGamesGamesAdapter.setItemsList(returnedObj.getData().getVideos());
					endGamesGamesAdapter.notifyDataSetInvalidated();
					break;
				case OPENINGS:
					openingsGamesAdapter.setItemsList(returnedObj.getData().getVideos());
					openingsGamesAdapter.notifyDataSetInvalidated();
					break;
				case RULES_BASICS:
					rulesBasicGamesAdapter.setItemsList(returnedObj.getData().getVideos());
					rulesBasicGamesAdapter.notifyDataSetInvalidated();
					break;
				case STRATEGY:
					strategyGamesAdapter.setItemsList(returnedObj.getData().getVideos());
					strategyGamesAdapter.notifyDataSetInvalidated();
					break;
				case TACTICS:
					tacticsGamesAdapter.setItemsList(returnedObj.getData().getVideos());
					tacticsGamesAdapter.notifyDataSetInvalidated();
					break;

			}

			// add data to sectioned adapter

//			recent.setVisibility(View.VISIBLE);
//			int cnt = Integer.parseInt(returnedObj.getData().getTotal_videos_count());
//			if (cnt > 0){
//				item = returnedObj.getData().getVideos().get(0); // new VideoItemOld(returnedObj.split(RestHelper.SYMBOL_ITEM_SPLIT)[2].split("<->"));
//				title.setText(item.getName());
//				desc.setText(item.getDescription());
//
//				playBtn.setEnabled(true);
//			}
		}
	}

	protected class ViewHolder {
		public TextView titleTxt;
		public TextView authorTxt;
		public TextView dateTxt;
	}
}
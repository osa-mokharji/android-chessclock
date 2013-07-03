package com.chess.ui.fragments.videos;

import android.content.*;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import com.chess.R;
import com.chess.backend.RestHelper;
import com.chess.backend.entity.LoadItem;
import com.chess.backend.entity.new_api.CommonFeedCategoryItem;
import com.chess.backend.entity.new_api.VideoItem;
import com.chess.backend.statics.StaticData;
import com.chess.backend.tasks.RequestJsonTask;
import com.chess.db.DBConstants;
import com.chess.db.DBDataManager;
import com.chess.db.DbHelper;
import com.chess.db.tasks.LoadDataFromDbTask;
import com.chess.db.tasks.SaveVideoCategoriesTask;
import com.chess.db.tasks.SaveVideosListTask;
import com.chess.ui.adapters.NewVideosSectionedCursorAdapter;
import com.chess.ui.fragments.CommonLogicFragment;
import com.chess.ui.interfaces.ItemClickListenerFace;
import com.chess.utilities.AppUtils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: roger sent2roger@gmail.com
 * Date: 27.01.13
 * Time: 19:12
 */
public class VideosFragment extends CommonLogicFragment implements ItemClickListenerFace, AdapterView.OnItemClickListener, ExpandableListView.OnChildClickListener, ExpandableListView.OnGroupClickListener {

	public static final String GREY_COLOR_DIVIDER = "##";
	// 11/15/12 | 27 min
	private static final SimpleDateFormat dateFormatter = new SimpleDateFormat("dd/MM/yy");
	private static final int VIDEOS_PER_CATEGORY = 2;
	private static final int LIBRARY = 6;

	private ViewHolder holder;
	private ForegroundColorSpan foregroundSpan;

	private ListView listView;
	private ExpandableListView expListView;
	private View loadingView;
	private TextView emptyView;

	private NewVideosSectionedCursorAdapter videosCursorAdapter;

	private VideosItemUpdateListener videosItemUpdateListener;
	private VideosItemUpdateListener randomItemUpdateListener;
	private SaveVideosUpdateListener saveVideosUpdateListener;
	private VideosCursorUpdateListener videosCursorUpdateListener;

	private VideoCategoriesUpdateListener videoCategoriesUpdateListener;
	private SaveVideoCategoriesUpdateListener saveVideoCategoriesUpdateListener;

	private boolean need2Update = true;
	private boolean headerDataLoaded;
	private long headerDataId;
	private VideoItem.Data headerData;

	private String[] curriculumCategories;
	private String[][] curriculumItems;
	private String[][] curriculumItemsTitles;
	private boolean curriculumMode;
	private VideoGroupsListAdapter curriculumAdapter;
	private View tempCurriculumHeader;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);


		int lightGrey = getResources().getColor(R.color.new_subtitle_light_grey);
		foregroundSpan = new ForegroundColorSpan(lightGrey);

		if (getAppData().isUserChooseVideoLibrary()) { // TODO add api logic to check if user saw all videos
			curriculumMode = false;
		} else {
			// everyone is presented with CURRICULUM view by default unless:
			// a) they have seen every video or lesson, or
			// b) they have a rating above 1600, or
			// c) they chose "full library" from the bottom of the curriculum list ( http://i.imgur.com/aWcHqUh.png )
			curriculumMode = true;
		}

		curriculumCategories = getResources().getStringArray(R.array.videos_curriculum);
		{ // Titles
			String[] beginners = getResources().getStringArray(R.array.video_cur_beginners_titles);
			String[] openings = getResources().getStringArray(R.array.video_cur_openings_titles);
			String[] tactics = getResources().getStringArray(R.array.video_cur_tactics_titles);
			String[] strategy = getResources().getStringArray(R.array.video_cur_strategy_titles);
			String[] endgames = getResources().getStringArray(R.array.video_cur_endgames_titles);
			String[] amazingGames = getResources().getStringArray(R.array.video_cur_amazing_games_titles);

			curriculumItemsTitles = new String[][]{beginners, openings, tactics, strategy, endgames, amazingGames};
		}
		{ // Links
			String[] beginners = getResources().getStringArray(R.array.video_cur_beginners);
			String[] openings = getResources().getStringArray(R.array.video_cur_openings);
			String[] tactics = getResources().getStringArray(R.array.video_cur_tactics);
			String[] strategy = getResources().getStringArray(R.array.video_cur_strategy);
			String[] endgames = getResources().getStringArray(R.array.video_cur_endgames);
			String[] amazingGames = getResources().getStringArray(R.array.video_cur_amazing_games);

			curriculumItems = new String[][]{beginners, openings, tactics, strategy, endgames, amazingGames};
		}
		init();

	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.new_videos_frame, container, false);
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

		setTitle(R.string.videos);

		loadingView = view.findViewById(R.id.loadingView);
		emptyView = (TextView) view.findViewById(R.id.emptyView);

		{ // Library mode init
			tempCurriculumHeader =  view.findViewById(R.id.tempCurriculumHeader);
			tempCurriculumHeader.setOnClickListener(this);
			listView = (ListView) view.findViewById(R.id.listView);
			// add header
			View headerView = LayoutInflater.from(getActivity()).inflate(R.layout.new_videos_thumb_list_item, null, false);
			headerView.setOnClickListener(this);
			listView.addHeaderView(headerView);
			listView.setAdapter(videosCursorAdapter);
			listView.setOnItemClickListener(this);

			// TODO create loading view for header
			holder = new ViewHolder();
			holder.titleTxt = (TextView) headerView.findViewById(R.id.titleTxt);
			holder.authorTxt = (TextView) headerView.findViewById(R.id.authorTxt);
			holder.dateTxt = (TextView) headerView.findViewById(R.id.dateTxt);
		}

		{ // Curriculum mode
			expListView = (ExpandableListView) view.findViewById(R.id.expListView);

			expListView.setOnChildClickListener(this);
			expListView.setOnGroupClickListener(this);
			expListView.setGroupIndicator(null);
		}

		showLibrary();

		// adjust action bar icons
		getActivityFace().showActionMenu(R.id.menu_search, true);
		getActivityFace().showActionMenu(R.id.menu_notifications, false);
		getActivityFace().showActionMenu(R.id.menu_games, false);

		setTitlePadding(ONE_ICON);
	}

	private void showLibrary() {
		boolean show = !curriculumMode;
		listView.setVisibility(show? View.VISIBLE : View.GONE);
		tempCurriculumHeader.setVisibility(show? View.VISIBLE : View.GONE);
		expListView.setVisibility(show? View.GONE : View.VISIBLE);
		if (show) {

			if (need2Update) {
				boolean haveSavedData = DBDataManager.haveSavedVideos(getActivity());

				if (AppUtils.isNetworkAvailable(getActivity())) {
					updateData();
					getCategories();
				} else if (!haveSavedData) {
					emptyView.setText(R.string.no_network);
					showEmptyView(true);
				}

				if (haveSavedData) {
					loadFromDb();
				}
			} else { // load data to listHeader view
				fillListViewHeaderData();
			}

		} else {
			expListView.setAdapter(curriculumAdapter);
		}
	}

	private void init() {
		videosCursorAdapter = new NewVideosSectionedCursorAdapter(getContext(), null, VIDEOS_PER_CATEGORY);
		randomItemUpdateListener = new VideosItemUpdateListener(VideosItemUpdateListener.RANDOM);
		videosItemUpdateListener = new VideosItemUpdateListener(VideosItemUpdateListener.DATA_LIST);

		saveVideosUpdateListener = new SaveVideosUpdateListener();
		videosCursorUpdateListener = new VideosCursorUpdateListener();

		videoCategoriesUpdateListener = new VideoCategoriesUpdateListener();
		saveVideoCategoriesUpdateListener = new SaveVideoCategoriesUpdateListener();
		curriculumAdapter = new VideoGroupsListAdapter(curriculumCategories, curriculumItemsTitles);
	}

	private void updateData() {
		{// request random data for the header
			LoadItem loadItem = new LoadItem();
			loadItem.setLoadPath(RestHelper.CMD_VIDEOS);
			loadItem.addRequestParams(RestHelper.P_LOGIN_TOKEN, getUserToken());
			loadItem.addRequestParams(RestHelper.P_ITEMS_PER_PAGE, 1);

			new RequestJsonTask<VideoItem>(randomItemUpdateListener).executeTask(loadItem);
		}
		// get all video // TODO adjust to request only latest updates

		LoadItem loadItem = new LoadItem();
		loadItem.setLoadPath(RestHelper.CMD_VIDEOS);
		loadItem.addRequestParams(RestHelper.P_ITEMS_PER_PAGE, 8);
		loadItem.addRequestParams(RestHelper.P_ITEMS_PER_CATEGORY, VIDEOS_PER_CATEGORY);

		new RequestJsonTask<VideoItem>(videosItemUpdateListener).executeTask(loadItem);
	}

	private void getCategories() {
		LoadItem loadItem = new LoadItem();
		loadItem.setLoadPath(RestHelper.CMD_VIDEO_CATEGORIES);
		loadItem.addRequestParams(RestHelper.P_LOGIN_TOKEN, getUserToken());

		new RequestJsonTask<CommonFeedCategoryItem>(videoCategoriesUpdateListener).executeTask(loadItem);
	}

	@Override
	public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
		String url = curriculumItems[groupPosition][childPosition];
		startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
		return true;
	}

	@Override
	public boolean onGroupClick(ExpandableListView parent, View v, int groupPosition, long id) {
		if (groupPosition == LIBRARY) { // turn in to library mode
			getAppData().setUserChooseVideoLibrary(true);
			curriculumMode = false;
			showLibrary();
			return true;
		} else {
			return false;
		}
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		boolean headerAdded = listView.getHeaderViewsCount() > 0;
		int offset = headerAdded ? -1 : 0;

		if (position == 0) { // if listView header
			// see onClick(View) handle
		} else if (videosCursorAdapter.isSectionHeader(position + offset)) {
			String sectionName = videosCursorAdapter.getSectionName(position + offset);

			getActivityFace().openFragment(VideoCategoriesFragment.createInstance(sectionName));
		} else {
			int internalPosition = videosCursorAdapter.getRelativePosition(position + offset);
			Cursor cursor = (Cursor) parent.getItemAtPosition(internalPosition + 1);
			getActivityFace().openFragment(VideoDetailsFragment.createInstance(DBDataManager.getId(cursor)));
		}
	}

	@Override
	public void onClick(View v) {
		super.onClick(v);
		if (v.getId() == R.id.videoThumbItemView) {
			if (headerDataLoaded) {
				getActivityFace().openFragment(VideoDetailsFragment.createInstance(headerDataId));
			}
		} else if (v.getId() == R.id.tempCurriculumHeader) {
			getAppData().setUserChooseVideoLibrary(false);
			curriculumMode = true;
			showLibrary();
		}
	}

	private class VideoCategoriesUpdateListener extends ChessUpdateListener<CommonFeedCategoryItem> {
		public VideoCategoriesUpdateListener() {
			super(CommonFeedCategoryItem.class);
		}

		@Override
		public void showProgress(boolean show) {
			super.showProgress(show);
			showLoadingView(show);
		}

		@Override
		public void updateData(CommonFeedCategoryItem returnedObj) {
			super.updateData(returnedObj);

			List<CommonFeedCategoryItem.Data> dataList = returnedObj.getData();
			for (CommonFeedCategoryItem.Data category : dataList) {
				category.setName(category.getName().replace(StaticData.SYMBOL_AMP_CODE, StaticData.SYMBOL_AMP));
			}

			new SaveVideoCategoriesTask(saveVideoCategoriesUpdateListener, dataList, getContentResolver()).executeTask();
		}
	}

	private class VideosItemUpdateListener extends ChessUpdateListener<VideoItem> {

		private static final int RANDOM = 0;
		private static final int DATA_LIST = 1;
		private int listenerCode;

		public VideosItemUpdateListener(int listenerCode) {
			super(VideoItem.class);
			this.listenerCode = listenerCode;
		}

		@Override
		public void showProgress(boolean show) {
			super.showProgress(show);
			showLoadingView(show);
		}

		@Override
		public void updateData(VideoItem returnedObj) {
			switch (listenerCode) {
				case RANDOM:
					headerData = returnedObj.getData().get(0);

					fillListViewHeaderData();

					// save in Db to open in Details View
					ContentResolver contentResolver = getContentResolver();

					Uri uri = DBConstants.uriArray[DBConstants.VIDEOS];
					String[] arguments = new String[1];
					arguments[0] = String.valueOf(headerData.getName());
					Cursor cursor = contentResolver.query(uri, DBDataManager.PROJECTION_NAME,
							DBDataManager.SELECTION_NAME, arguments, null);

					ContentValues values = DBDataManager.putVideoItemToValues(headerData);

					if (cursor.moveToFirst()) {
						headerDataId = DBDataManager.getId(cursor);
						contentResolver.update(ContentUris.withAppendedId(uri, headerDataId), values, null, null);
					} else {
						Uri savedUri = contentResolver.insert(uri, values);
						headerDataId = Long.parseLong(savedUri.getPathSegments().get(1));
					}

					headerDataLoaded = true;

					break;
				case DATA_LIST:
					new SaveVideosListTask(saveVideosUpdateListener, returnedObj.getData(), getContentResolver()).executeTask();
					break;
			}
		}
	}

	private void fillListViewHeaderData() {
		if (!headerDataLoaded) {
			return;
		}

		String firstName = headerData.getFirstName();
		String chessTitle = headerData.getChessTitle();
		String lastName = headerData.getLastName();
		CharSequence authorStr = GREY_COLOR_DIVIDER + chessTitle + GREY_COLOR_DIVIDER + StaticData.SYMBOL_SPACE
				+ firstName + StaticData.SYMBOL_SPACE + lastName;
		authorStr = AppUtils.setSpanBetweenTokens(authorStr, GREY_COLOR_DIVIDER, foregroundSpan);
		holder.authorTxt.setText(authorStr);
		holder.titleTxt.setText(headerData.getName());
		holder.dateTxt.setText(dateFormatter.format(new Date(headerData.getCreateDate())));
	}

	protected class ViewHolder {
		public TextView titleTxt;
		public TextView authorTxt;
		public TextView dateTxt;
	}

	private class SaveVideosUpdateListener extends ChessUpdateListener<VideoItem.Data> {
		public SaveVideosUpdateListener() {
			super();
		}

		@Override
		public void updateData(VideoItem.Data returnedObj) {
			super.updateData(returnedObj);

			loadFromDb();
		}
	}

	private class SaveVideoCategoriesUpdateListener extends ChessUpdateListener<CommonFeedCategoryItem.Data> {
	}

	private void loadFromDb() {
		new LoadDataFromDbTask(videosCursorUpdateListener, DbHelper.getVideosListParams(),
				getContentResolver()).executeTask();
	}

	private class VideosCursorUpdateListener extends ChessUpdateListener<Cursor> {

		@Override
		public void showProgress(boolean show) {
			super.showProgress(show);
			showLoadingView(show);
		}

		@Override
		public void updateData(Cursor cursor) {
			super.updateData(cursor);

/*
			{ // Titles
				String[] beginners = getResources().getStringArray(R.array.video_cur_beginners_titles);
				String[] openings = getResources().getStringArray(R.array.video_cur_openings_titles);
				String[] tactics = getResources().getStringArray(R.array.video_cur_tactics_titles);
				String[] strategy = getResources().getStringArray(R.array.video_cur_strategy_titles);
				String[] endgames = getResources().getStringArray(R.array.video_cur_endgames_titles);
				String[] amazingGames = getResources().getStringArray(R.array.video_cur_amazing_games_titles);

				curriculumItemsTitles = new String[][]{beginners, openings, tactics, strategy, endgames, amazingGames};
			}

			*/

//			String[] groups = new String[]{};
//			String[][] titles = new String[][]{};
//			String prevCategory = "";
//			String[] tempCategory = new
//			do {
//				String category = DBDataManager.getString(cursor, DBConstants.V_CATEGORY);
//				if (!prevCategory.equals(category)) { // next group
//
//				}
//				String title = DBDataManager.getString(cursor, DBConstants.V_NAME);
//
//
//
//
//			} while (cursor.moveToNext());
//
//
//			expListView.setVisibility(View.VISIBLE);
//			expListView.setAdapter(new VideoGroupsListAdapter(groups, titles));

			videosCursorAdapter.changeCursor(cursor);
			listView.setAdapter(videosCursorAdapter);

			need2Update = false;
		}

		@Override
		public void errorHandle(Integer resultCode) {
			super.errorHandle(resultCode);
			if (resultCode == StaticData.EMPTY_DATA) {
				emptyView.setText("No Videos"); // TODO remove after debug, there should be videos
			} else if (resultCode == StaticData.UNKNOWN_ERROR) {
				emptyView.setText(R.string.no_network);
			}
			showEmptyView(true);
		}

	}

	private void showEmptyView(boolean show) {
		if (show) {
			// don't hide loadingView if it's loading
			if (loadingView.getVisibility() != View.VISIBLE) {
				loadingView.setVisibility(View.GONE);
			}

			emptyView.setVisibility(View.VISIBLE);
			listView.setVisibility(View.GONE);
		} else {
			emptyView.setVisibility(View.GONE);
			listView.setVisibility(View.VISIBLE);
		}
	}

	private void showLoadingView(boolean show) {
		if (show) {
			emptyView.setVisibility(View.GONE);
			if (videosCursorAdapter.getCount() == 0) {
				listView.setVisibility(View.GONE);
			}
			loadingView.setVisibility(View.VISIBLE);
		} else {
			listView.setVisibility(View.VISIBLE);
			loadingView.setVisibility(View.GONE);
		}
	}

	@Override
	public Context getMeContext() {
		return getActivity();
	}

	public class VideoGroupsListAdapter extends BaseExpandableListAdapter {
		private final LayoutInflater inflater;
		private String[] groups;
		private String[][] children;

		public VideoGroupsListAdapter(String[] groups, String[][] children) {
			this.groups = groups;
			this.children = children;
			inflater = LayoutInflater.from(getActivity());
		}

		@Override
		public int getGroupCount() {
			return groups.length;
		}

		@Override
		public int getChildrenCount(int groupPosition) {
			return children[groupPosition].length;
		}

		@Override
		public Object getGroup(int groupPosition) {
			return groups[groupPosition];
		}

		@Override
		public Object getChild(int groupPosition, int childPosition) {
			return children[groupPosition][childPosition];
		}

		@Override
		public long getGroupId(int groupPosition) {
			return groupPosition;
		}

		@Override
		public long getChildId(int groupPosition, int childPosition) {
			return childPosition;
		}

		@Override
		public boolean hasStableIds() {
			return true;
		}

		@Override
		public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
			ViewHolder holder;
			if (convertView == null) {
				convertView = inflater.inflate(R.layout.new_video_header, parent, false);
				holder = new ViewHolder();

				holder.text = (TextView) convertView.findViewById(R.id.headerTitleTxt);
				holder.icon = (TextView) convertView.findViewById(R.id.headerIconTxt);
				convertView.setTag(holder);
			} else {
				holder = (ViewHolder) convertView.getTag();
			}

			holder.text.setText(getGroup(groupPosition).toString());
			if (groupPosition == LIBRARY) {
				holder.icon.setText(R.string.ic_right);
			} else {
				if (isExpanded) {
					holder.icon.setText(R.string.ic_down);
				} else {
					holder.icon.setText(R.string.ic_up);
				}
			}

			return convertView;
		}

		@Override
		public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
			ViewHolder holder;
			if (convertView == null) {
				convertView = inflater.inflate(R.layout.new_video_list_item, parent, false);
				holder = new ViewHolder();

				holder.text = (TextView) convertView.findViewById(R.id.titleTxt);
				holder.icon = (TextView) convertView.findViewById(R.id.watchedIconTxt);
				convertView.setTag(holder);
			} else {
				holder = (ViewHolder) convertView.getTag();
			}

			holder.text.setText(getChild(groupPosition, childPosition).toString());
			if (isLastChild) {
				holder.icon.setText(R.string.ic_down);
			} else {
				holder.icon.setText(R.string.ic_up);
			}

			return convertView;
		}

		@Override
		public boolean isChildSelectable(int groupPosition, int childPosition) {
			return true;
		}

		private class ViewHolder {
			TextView text;
			TextView icon;
		}
	}

}

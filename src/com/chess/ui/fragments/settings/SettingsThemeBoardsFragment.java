package com.chess.ui.fragments.settings;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.IBinder;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import com.chess.R;
import com.chess.backend.GetAndSaveBoard;
import com.chess.backend.GetAndSaveTheme;
import com.chess.backend.LoadItem;
import com.chess.backend.RestHelper;
import com.chess.backend.entity.api.themes.BoardSingleItem;
import com.chess.backend.entity.api.themes.BoardsItem;
import com.chess.backend.image_load.EnhancedImageDownloader;
import com.chess.backend.image_load.ProgressImageView;
import com.chess.backend.interfaces.FileReadyListener;
import com.chess.backend.tasks.RequestJsonTask;
import com.chess.db.DbDataManager;
import com.chess.db.DbHelper;
import com.chess.db.DbScheme;
import com.chess.model.SelectionItem;
import com.chess.ui.adapters.CustomSectionedAdapter;
import com.chess.ui.adapters.ItemsAdapter;
import com.chess.ui.fragments.CommonLogicFragment;
import com.chess.ui.fragments.popup_fragments.PopupCustomViewFragment;
import com.chess.ui.interfaces.ItemClickListenerFace;

import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: roger sent2roger@gmail.com
 * Date: 26.10.13
 * Time: 15:01
 */
public class SettingsThemeBoardsFragment extends CommonLogicFragment implements ItemClickListenerFace, AdapterView.OnItemClickListener {

	private static final int THEME_SECTION = 0;
	private static final int DEFAULT_SECTION = 1;

	private static final String THEME_LOAD_TAG = "theme load popup";

	public static final String BOARD_ITEM = "board_item";
	public static final String SCREEN_WIDTH = "screen_width";

	private BoardsItemUpdateListener boardsItemUpdateListener;
	private CustomSectionedAdapter sectionedAdapter;
	private ThemeBoardsAdapter themeBoardsAdapter;
	private List<SelectionItem> defaultBoardSelectionList;
	private String themeBoardName;
	private List<SelectionItem> themeBoardSelectionList;
	private int screenWidth;
	private TextView loadProgressTxt;
	private TextView taskTitleTxt;
	private PopupCustomViewFragment loadProgressPopupFragment;
	private List<BoardSingleItem.Data> themeBoardItemsList;
	private SelectionItem selectedThemeBoardItem;
	private boolean isBoardLoading;
	private String boardUrl;
	private LoadServiceConnectionListener loadServiceConnectionListener;
	private boolean serviceBounded;
	private ProgressUpdateListener progressUpdateListener;
	private GetAndSaveBoard.ServiceBinder serviceBinder;
	private int selectedBoardId;
	private TextView progressTitleTxt;
	private ProgressBar themeLoadProgressBar;
	private View headerView;
	private boolean needToLoadThemeAfterConnected;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		screenWidth = getResources().getDisplayMetrics().widthPixels;

		themeBoardName = getAppData().getThemeBoardName();

		progressUpdateListener = new ProgressUpdateListener();
		loadServiceConnectionListener = new LoadServiceConnectionListener();
		boardsItemUpdateListener = new BoardsItemUpdateListener();

		Resources resources = getResources();
		themeBoardSelectionList = new ArrayList<SelectionItem>();
		themeBoardItemsList = new ArrayList<BoardSingleItem.Data>();

		// Boards bitmaps list init
		defaultBoardSelectionList = new ArrayList<SelectionItem>();
		defaultBoardSelectionList.add(new SelectionItem(resources.getDrawable(R.drawable.board_sample_wood_dark), getString(R.string.board_wood_dark)));
		defaultBoardSelectionList.add(new SelectionItem(resources.getDrawable(R.drawable.board_sample_wood_light), getString(R.string.board_wood_light)));
		defaultBoardSelectionList.add(new SelectionItem(resources.getDrawable(R.drawable.board_sample_blue), getString(R.string.board_blue)));
		defaultBoardSelectionList.add(new SelectionItem(resources.getDrawable(R.drawable.board_sample_brown), getString(R.string.board_brown)));
		defaultBoardSelectionList.add(new SelectionItem(resources.getDrawable(R.drawable.board_sample_green), getString(R.string.board_green)));
		defaultBoardSelectionList.add(new SelectionItem(resources.getDrawable(R.drawable.board_sample_grey), getString(R.string.board_grey)));
		defaultBoardSelectionList.add(new SelectionItem(resources.getDrawable(R.drawable.board_sample_marble), getString(R.string.board_marble)));
		defaultBoardSelectionList.add(new SelectionItem(resources.getDrawable(R.drawable.board_sample_red), getString(R.string.board_red)));
		defaultBoardSelectionList.add(new SelectionItem(resources.getDrawable(R.drawable.board_sample_tan), getString(R.string.board_tan)));

		for (SelectionItem selectionItem : defaultBoardSelectionList) {
			if (selectionItem.getCode().equals(themeBoardName)) {
				selectionItem.setChecked(true);
				break;
			}
		}

		sectionedAdapter = new CustomSectionedAdapter(this, R.layout.new_comp_archive_header,
				new int[]{THEME_SECTION, DEFAULT_SECTION});
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.new_common_header_list_frame, container, false);
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

		setTitle(R.string.select_style);

		headerView = view.findViewById(R.id.headerView);
		progressTitleTxt = (TextView) headerView.findViewById(R.id.progressTitleTxt);
		themeLoadProgressBar = (ProgressBar) headerView.findViewById(R.id.themeLoadProgressBar);
		headerView.setVisibility(View.GONE);

		ListView listView = (ListView) view.findViewById(R.id.listView);

		themeBoardsAdapter = new ThemeBoardsAdapter(getContext(), null);
		DefaultBoardsAdapter defaultBoardsAdapter = new DefaultBoardsAdapter(getActivity(), defaultBoardSelectionList);

		sectionedAdapter.addSection("Theme Boards", themeBoardsAdapter);
		sectionedAdapter.addSection("Default Boards", defaultBoardsAdapter);

		listView.setAdapter(sectionedAdapter);
		listView.setOnItemClickListener(this);
	}

	@Override
	public void onResume() {
		super.onResume();

		// Don't load custom board if we are not logged in
		if (!TextUtils.isEmpty(getUserToken())) {
			Cursor cursor = DbDataManager.query(getContentResolver(), DbHelper.getAll(DbScheme.Tables.THEME_BOARDS));

			if (cursor != null && cursor.moveToFirst()) {
				do {
					themeBoardItemsList.add(DbDataManager.getThemeBoardItemFromCursor(cursor));
				} while (cursor.moveToNext());

				updateUiData();
			} else {
				getBoards();
			}
		}
	}

	private void getBoards() {
		// load board line previews
		LoadItem loadItem = new LoadItem();
		loadItem.setLoadPath(RestHelper.getInstance().CMD_BOARDS);
		loadItem.addRequestParams(RestHelper.P_LOGIN_TOKEN, getUserToken());

		new RequestJsonTask<BoardsItem>(boardsItemUpdateListener).executeTask(loadItem);
	}

	@Override
	public Context getMeContext() {
		return getActivity();
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		int section = sectionedAdapter.getCurrentSection(position);

		if (section == THEME_SECTION) {
			if (isBoardLoading) {
				return;
			}

			// don't allow to select while it's loading
			isBoardLoading = true;

			selectedThemeBoardItem = (SelectionItem) parent.getItemAtPosition(position);
			for (SelectionItem selectionItem : themeBoardSelectionList) {
				if (selectedThemeBoardItem.getCode().equals(selectionItem.getCode())) {
					selectionItem.setChecked(true);
				} else {
					selectionItem.setChecked(false);
				}
			}
			for (SelectionItem selectionItem : defaultBoardSelectionList) {
				selectionItem.setChecked(false);
			}

			sectionedAdapter.notifyDataSetChanged();

			selectedBoardId = 1;
			for (BoardSingleItem.Data data : themeBoardItemsList) {
				if (data.getName().equals(selectedThemeBoardItem.getCode())) {
					selectedBoardId = data.getThemeBoardId();
					break;
				}
			}

			getAppData().setThemeBoardName(selectedThemeBoardItem.getCode());
			getAppData().setThemeBoardPreviewUrl(selectedThemeBoardItem.getText());

			if (serviceBounded) {
				showToast(R.string.loading_board);

				serviceBinder.getService().loadBoard(selectedBoardId, screenWidth);
			} else {
				needToLoadThemeAfterConnected = true;
				getActivity().bindService(new Intent(getActivity(), GetAndSaveBoard.class), loadServiceConnectionListener,
						Activity.BIND_AUTO_CREATE);
			}

		} else {
			SelectionItem defaultBoardItem = (SelectionItem) parent.getItemAtPosition(position);
			for (SelectionItem selectionItem : defaultBoardSelectionList) {
				if (defaultBoardItem.getText().equals(selectionItem.getText())) {
					selectionItem.setChecked(true);
					// save board theme name to appData
					getAppData().setUseThemeBoard(false);
					getAppData().setThemeBoardName(selectionItem.getText());
				} else {
					selectionItem.setChecked(false);
				}
			}
			if (themeBoardSelectionList != null) { // for guest mode we don't have theme board
				for (SelectionItem selectionItem : themeBoardSelectionList) {
					selectionItem.setChecked(false);
				}
			}

			// go back
			getActivityFace().showPreviousFragment();
		}
	}

	private class BoardsItemUpdateListener extends ChessLoadUpdateListener<BoardsItem> {

		private BoardsItemUpdateListener() {
			super(BoardsItem.class);
		}

		@Override
		public void updateData(BoardsItem returnedObj) {
			super.updateData(returnedObj);

			themeBoardItemsList = returnedObj.getData();

			updateUiData();

			for (BoardSingleItem.Data currentItem : themeBoardItemsList) {
				DbDataManager.saveThemeBoardItemToDb(getContentResolver(), currentItem);
			}
		}
	}

	private void updateUiData() {
		for (BoardSingleItem.Data boardItem : themeBoardItemsList) {
			SelectionItem selectionItem = new SelectionItem(null, boardItem.getLineBoardPreviewUrl());
			selectionItem.setCode(boardItem.getName());
			themeBoardSelectionList.add(selectionItem);
		}

		for (SelectionItem selectionItem : themeBoardSelectionList) {
			if (selectionItem.getCode().equals(themeBoardName)) {
				selectionItem.setChecked(true);
				break;
			}
		}

		themeBoardsAdapter.setItemsList(themeBoardSelectionList);
		sectionedAdapter.notifyDataSetChanged();
	}

	private class LoadServiceConnectionListener implements ServiceConnection {

		@Override
		public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
			serviceBounded = true;

			serviceBinder = (GetAndSaveBoard.ServiceBinder) iBinder;
			serviceBinder.getService().setProgressUpdateListener(progressUpdateListener);

			if (serviceBinder.getService().isInstallingBoard()) {
				isBoardLoading = true;
			}
			if (needToLoadThemeAfterConnected) {
				serviceBinder.getService().loadBoard(selectedBoardId, screenWidth);
			}
		}

		@Override
		public void onServiceDisconnected(ComponentName componentName) {
			serviceBounded = false;
			isBoardLoading = false;
		}
	}

	private class ProgressUpdateListener implements FileReadyListener {

		@Override
		public void changeTitle(final String title) {
			if (getActivity() == null) {
				return;
			}
			getActivity().runOnUiThread(new Runnable() {
				@Override
				public void run() {
					if (isBoardLoading) {
						headerView.setVisibility(View.VISIBLE);
						progressTitleTxt.setText(title);
						progressTitleTxt.setVisibility(View.VISIBLE);
					} else {
						headerView.setVisibility(View.GONE);
						progressTitleTxt.setVisibility(View.VISIBLE);
					}
				}
			});

		}

		@Override
		public void setProgress(final int progress) {
			if (getActivity() == null) {
				return;
			}

			getActivity().runOnUiThread(new Runnable() {
				@Override
				public void run() {
					if (progress == GetAndSaveTheme.DONE) {
						isBoardLoading = false;
						headerView.setVisibility(View.GONE);

						getActivity().unbindService(loadServiceConnectionListener);
						serviceBounded = false;
					} else {
						if (isBoardLoading) {
							headerView.setVisibility(View.VISIBLE);
							if (progress != GetAndSaveTheme.INDETERMINATE) {
								themeLoadProgressBar.setProgress(progress);
								themeLoadProgressBar.setIndeterminate(false);
							} else {
								themeLoadProgressBar.setIndeterminate(true);
							}

							themeLoadProgressBar.setVisibility(View.VISIBLE);
						} else {
							headerView.setVisibility(View.GONE);
						}
					}
				}
			});

		}
	}

	private class ThemeBoardsAdapter extends ItemsAdapter<SelectionItem> {

		private final int previewWidth;
		private final Bitmap placeHolderBitmap;
		private final RelativeLayout.LayoutParams imageParams;
		private final LinearLayout.LayoutParams linearLayoutParams;
		private final RelativeLayout.LayoutParams progressParams;
		private final EnhancedImageDownloader imageLoader;

		public ThemeBoardsAdapter(Context context, List<SelectionItem> menuItems) {
			super(context, menuItems);

			imageLoader = new EnhancedImageDownloader(context);

			int backIMgColor = getResources().getColor(R.color.upgrade_toggle_button_p);
			placeHolderBitmap = Bitmap.createBitmap(new int[]{backIMgColor}, 1, 1, Bitmap.Config.ARGB_8888);

			Drawable boardDrawableExample = resources.getDrawable(R.drawable.board_sample_wood_dark);
			previewWidth = boardDrawableExample.getIntrinsicWidth();
			int imageHeight = boardDrawableExample.getIntrinsicHeight();

			imageParams = new RelativeLayout.LayoutParams(previewWidth, imageHeight);
			linearLayoutParams = new LinearLayout.LayoutParams(previewWidth, imageHeight);

			progressParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
			progressParams.addRule(RelativeLayout.CENTER_IN_PARENT);
		}

		@Override
		protected View createView(ViewGroup parent) { // View to display in layout
			View view = inflater.inflate(R.layout.selection_progress_image_item, parent, false);
			ViewHolder holder = new ViewHolder();
			holder.image = (ProgressImageView) view.findViewById(R.id.image);

			holder.image.setLayoutParams(linearLayoutParams);

			// Change Placeholder
			holder.image.placeholder = placeHolderBitmap;

			// Change Image params
			holder.image.getImageView().setLayoutParams(imageParams);
			holder.image.getImageView().setScaleType(ImageView.ScaleType.FIT_XY);

			// Change ProgressBar params
			holder.image.getProgressBar().setLayoutParams(progressParams);

			holder.text = (CheckedTextView) view.findViewById(R.id.text);

			view.setTag(holder);

			return view;
		}

		@Override
		protected void bindView(SelectionItem item, int pos, View view) {
			ViewHolder holder = (ViewHolder) view.getTag();

			imageLoader.download(item.getText(), holder.image, previewWidth, previewWidth);

			holder.text.setText(item.getCode());
			holder.text.setChecked(item.isChecked());
		}

		private class ViewHolder {
			public CheckedTextView text;
			public ProgressImageView image;
		}

		public Context getContext() {
			return context;
		}
	}

	private class DefaultBoardsAdapter extends ItemsAdapter<SelectionItem> {

		private final int previewWidth;
		private final Bitmap placeHolderBitmap;
		//			private final float aspectRatio;
		private final RelativeLayout.LayoutParams imageParams;
		private final LinearLayout.LayoutParams linearLayoutParams;
		private final RelativeLayout.LayoutParams progressParams;

		public DefaultBoardsAdapter(Context context, List<SelectionItem> menuItems) {
			super(context, menuItems);

			int backIMgColor = getResources().getColor(R.color.upgrade_toggle_button_p);
			placeHolderBitmap = Bitmap.createBitmap(new int[]{backIMgColor}, 1, 1, Bitmap.Config.ARGB_8888);

			Drawable boardDrawableExample = resources.getDrawable(R.drawable.board_sample_wood_dark);
			previewWidth = boardDrawableExample.getIntrinsicWidth();
			int imageHeight = boardDrawableExample.getIntrinsicHeight();


			imageParams = new RelativeLayout.LayoutParams(previewWidth, imageHeight);
			linearLayoutParams = new LinearLayout.LayoutParams(previewWidth, imageHeight);

			progressParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
			progressParams.addRule(RelativeLayout.CENTER_IN_PARENT);
		}

		@Override
		protected View createView(ViewGroup parent) { // View to display in layout
			View view = inflater.inflate(R.layout.selection_progress_image_item, parent, false);
			ViewHolder holder = new ViewHolder();
			holder.image = (ProgressImageView) view.findViewById(R.id.image);

			holder.image.setLayoutParams(linearLayoutParams);

			// Change Placeholder
			holder.image.placeholder = placeHolderBitmap;

			// Change Image params
			holder.image.getImageView().setLayoutParams(imageParams);
			holder.image.getImageView().setScaleType(ImageView.ScaleType.FIT_XY);

			// Change ProgressBar params
			holder.image.getProgressBar().setLayoutParams(progressParams);

			holder.text = (CheckedTextView) view.findViewById(R.id.text);

			view.setTag(holder);

			return view;
		}

		@Override
		protected void bindView(SelectionItem item, int pos, View view) {
			ViewHolder holder = (ViewHolder) view.getTag();

			holder.image.setImageDrawable(item.getImage());

			holder.text.setText(item.getText());
			holder.text.setChecked(item.isChecked());
		}

		private class ViewHolder {
			public CheckedTextView text;
			public ProgressImageView image;
		}

		public Context getContext() {
			return context;
		}
	}



}
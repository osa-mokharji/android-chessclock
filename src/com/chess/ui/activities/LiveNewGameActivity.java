package com.chess.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import com.chess.R;
import com.chess.backend.statics.AppData;

public class LiveNewGameActivity extends LiveBaseActivity  {

	private Button currentGameBtn;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.live_new_game);

		initUpgradeAndAdWidgets();

		findViewById(R.id.friendchallenge).setOnClickListener(this);
		findViewById(R.id.challengecreate).setOnClickListener(this);

		currentGameBtn = (Button) findViewById(R.id.currentGameBtn);
		currentGameBtn.setOnClickListener(this);

//		AppData.setLiveChessMode(this, true); // should not duplicate logic
	}

	protected void onLiveServiceConnected() {
		if (liveService.currentGameExist()) {
			currentGameBtn.setVisibility(View.VISIBLE);
		} else {
			currentGameBtn.setVisibility(View.GONE);
		}
	}

	@Override
	public void onClick(View view) {
		if (view.getId() == R.id.upgradeBtn) {
			startActivity(AppData.getMembershipAndroidIntent(this));
		} else if (view.getId() == R.id.friendchallenge) {
			startActivity(new Intent(this, LiveFriendChallengeActivity.class));
		} else if (view.getId() == R.id.challengecreate) {
			startActivity(new Intent(this, LiveOpenChallengeActivity.class));
		} else if (view.getId() == R.id.currentGameBtn) {
			liveService.checkAndProcessFullGame();
		}
	}
}

package com.chess.activities;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.*;
import com.chess.R;
import com.chess.core.AppConstants;
import com.chess.core.CoreActivity;
import com.chess.core.Tabs;
import com.chess.lcc.android.LccHolder;
import com.chess.live.client.Challenge;
import com.chess.live.client.LiveChessClientFacade;
import com.chess.live.client.PieceColor;
import com.chess.live.util.GameTimeConfig;
import com.chess.utilities.ChessComApiParser;
import com.chess.utilities.MyProgressDialog;
import com.flurry.android.FlurryAgent;

public class FriendChallenge extends CoreActivity implements OnClickListener {
	private Spinner iplayas, dayspermove, friends;
	private AutoCompleteTextView initialTime;
	private AutoCompleteTextView bonusTime;
	private CheckBox isRated;
	private RadioButton chess960;

	private InitialTimeTextWatcher initialTimeTextWatcher;
	private InitialTimeValidator initialTimeValidator;
	private BonusTimeTextWatcher bonusTimeTextWatcher;
	private BonusTimeValidator bonusTimeValidator;

	private int[] daysArr = new int[]{
			1,
			2,
			3,
			5,
			7,
			14
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);


		if (mainApp.isLiveChess()) {
			setContentView(R.layout.challengeafriendlive);
		} else {
			setContentView(R.layout.challengeafriend);
			dayspermove = (Spinner) findViewById(R.id.dayspermove);
			chess960 = (RadioButton) findViewById(R.id.chess960);
			iplayas = (Spinner) findViewById(R.id.iplayas);
		}

		init();

		friends = (Spinner) findViewById(R.id.friend);
		isRated = (CheckBox) findViewById(R.id.ratedGame);
		initialTime = (AutoCompleteTextView) findViewById(R.id.initialTime);
		bonusTime = (AutoCompleteTextView) findViewById(R.id.bonusTime);
		if (mainApp.isLiveChess()) {
			initialTime.setText(mainApp.getSharedData().getString(AppConstants.CHALLENGE_INITIAL_TIME, "5"));
			initialTime.addTextChangedListener(initialTimeTextWatcher);
			initialTime.setValidator(initialTimeValidator);
			initialTime.setOnEditorActionListener(null);
			
			bonusTime.setText(mainApp.getSharedData().getString(AppConstants.CHALLENGE_BONUS_TIME, "0"));
			bonusTime.addTextChangedListener(bonusTimeTextWatcher );
			bonusTime.setValidator(bonusTimeValidator);
		}
		findViewById(R.id.createchallenge).setOnClickListener(this);
	}

	@Override
	public void LoadNext(int code) {
	}

	@Override
	public void LoadPrev(int code) {
		finish();
	}

	@Override
	public void Update(int code) {
		if (code == ERROR_SERVER_RESPONSE) {
			finish();
		} else if (code == INIT_ACTIVITY && !mainApp.isLiveChess()) {
			if (appService != null) {
				appService.RunSingleTask(0,
						"http://www." + LccHolder.HOST + "/api/get_friends?id=" + mainApp.getSharedData().getString(AppConstants.USER_TOKEN, ""),
						progressDialog = new MyProgressDialog(ProgressDialog.show(FriendChallenge.this, null, getString(R.string.gettingfriends), true))
				);
			}
		} else if (code == 0 || (code == INIT_ACTIVITY && mainApp.isLiveChess())) {
			String[] FRIENDS;
			if (mainApp.isLiveChess()) {
				FRIENDS = lccHolder.getOnlineFriends();
			} else {
				FRIENDS = ChessComApiParser.GetFriendsParse(response);
			}

			ArrayAdapter<String> adapterF = new ArrayAdapter<String>(FriendChallenge.this,
					android.R.layout.simple_spinner_item,
					FRIENDS);
			adapterF.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
			friends.setAdapter(adapterF);
			if (friends.getSelectedItem().equals("")) {
				new AlertDialog.Builder(FriendChallenge.this)
						.setIcon(android.R.drawable.ic_dialog_alert)
						.setTitle(getString(R.string.sorry))
						.setMessage(getString(R.string.nofriends))
						.setPositiveButton(getString(R.string.invitetitle), new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int whichButton) {
								startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.chess.com")));
							}
						})
						.setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int whichButton) {
								finish();
							}
						}).setCancelable(false)
						.create().show();
			}
		} else if (code == 1) {
			if (mainApp.isLiveChess()) {
				mainApp.getSharedDataEditor().putString(AppConstants.CHALLENGE_INITIAL_TIME, initialTime.getText().toString().trim());
				mainApp.getSharedDataEditor().putString(AppConstants.CHALLENGE_BONUS_TIME, bonusTime.getText().toString().trim());
				mainApp.getSharedDataEditor().commit();
				//mainApp.ShowDialog(this, getString(R.string.congratulations), getString(R.string.challengeSent));
			} else {
				mainApp.ShowDialog(this, getString(R.string.congratulations), getString(R.string.onlinegamecreated));
			}
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (mainApp.isLiveChess() && lccHolder.getUser() == null) {
			lccHolder.logout();
			startActivity(new Intent(this, Tabs.class));
		}
	}

	private void init(){
		initialTimeTextWatcher = new InitialTimeTextWatcher();
		initialTimeValidator = new InitialTimeValidator();
		bonusTimeTextWatcher = new BonusTimeTextWatcher();
		bonusTimeValidator = new BonusTimeValidator();
	}

	@Override
	public void onClick(View view) {
		if(view.getId() == R.id.createchallenge){
			if (friends.getCount() == 0) {
				return;
			}
			if (mainApp.isLiveChess()) {
				if (initialTime.getText().toString().length() < 1 || bonusTime.getText().toString().length() < 1) {
					initialTime.setText("10");
					bonusTime.setText("0");
				}
				/*PieceColor color;
											  switch(iplayas.getSelectedItemPosition())
											  {
												case 0:
												  color = PieceColor.UNDEFINED;
												  break;
												case 1:
												  color = PieceColor.WHITE;
												  break;
												case 2:
												  color = PieceColor.BLACK;
												  break;
												default:
												  color = PieceColor.UNDEFINED;
												  break;
											  }*/
				final Boolean rated = isRated.isChecked();
				final Integer initialTimeInteger = new Integer(initialTime.getText().toString());
				final Integer bonusTimeInteger = new Integer(bonusTime.getText().toString());
				final GameTimeConfig gameTimeConfig = new GameTimeConfig(initialTimeInteger * 60 * 10, bonusTimeInteger * 10);
				final Integer minRating = null;
				final Integer maxRating = null;
				final Challenge challenge = LiveChessClientFacade.createCustomSeekOrChallenge(
						lccHolder.getUser(), friends.getSelectedItem().toString().trim(), PieceColor.UNDEFINED, rated, gameTimeConfig,
						minRating, maxRating);
				if (appService != null) {
					FlurryAgent.onEvent("Challenge Created", null);
					lccHolder.getAndroid().runSendChallengeTask(
							//progressDialog = MyProgressDialog.show(FriendChallenge.this, null, getString(R.string.creating), true),
							null,
							challenge
					);
					Update(1);
				}
			} else {
				int color = iplayas.getSelectedItemPosition();
				int days = 1;
				days = daysArr[dayspermove.getSelectedItemPosition()];
				int israted = 0;
				int gametype = 0;

				if (isRated.isChecked()) {
					israted = 1;
				} else {
					israted = 0;
				}
				if (chess960.isChecked()) {
					gametype = 2;
				}
				String query = "http://www." + LccHolder.HOST + "/api/echess_new_game?id=" + mainApp.getSharedData().getString(AppConstants.USER_TOKEN, "") +
						"&timepermove=" + days +
						"&iplayas=" + color +
						"&israted=" + israted +
						"&game_type=" + gametype +
						"&opponent=" + friends.getSelectedItem().toString().trim();
				if (appService != null) {
					appService.RunSingleTask(1,
							query,
							progressDialog = new MyProgressDialog(ProgressDialog
									.show(FriendChallenge.this, null, getString(R.string.creating), true))
					);
				}
			}
		}
	}


	private class InitialTimeTextWatcher implements TextWatcher {
		@Override
		public void onTextChanged(CharSequence s, int start, int before, int count) {
			initialTime.performValidation();
		}

		@Override
		public void beforeTextChanged(CharSequence s, int start, int count, int after) {
		}

		@Override
		public void afterTextChanged(Editable s) {
			initialTime.performValidation();
		}
	}

	private class InitialTimeValidator implements AutoCompleteTextView.Validator {
		@Override
		public boolean isValid(CharSequence text) {
			final String textString = text.toString().trim();
			final Integer initialTime = new Integer(textString);
			return !textString.equals("") && initialTime >= 1 && initialTime <= 120;
		}

		@Override
		public CharSequence fixText(CharSequence invalidText) {
			return mainApp.getSharedData().getString(AppConstants.CHALLENGE_INITIAL_TIME, "5");
		}
	}

	private class BonusTimeTextWatcher implements TextWatcher {
		@Override
		public void onTextChanged(CharSequence s, int start, int before, int count) {
			bonusTime.performValidation();
		}

		@Override
		public void beforeTextChanged(CharSequence s, int start, int count, int after) {
		}

		@Override
		public void afterTextChanged(Editable s) {
			bonusTime.performValidation();
		}
	}

	private class BonusTimeValidator implements AutoCompleteTextView.Validator {
		@Override
		public boolean isValid(CharSequence text) {
			final String textString = new String(text.toString().toString());
			final Integer bonusTime = new Integer(textString);
			if (!textString.equals("") && bonusTime >= 0 && bonusTime <= 60) {
				return true;
			} else {
				return false;
			}
		}

		@Override
		public CharSequence fixText(CharSequence invalidText) {
			return mainApp.getSharedData().getString(AppConstants.CHALLENGE_BONUS_TIME, "0");
		}
	}

}

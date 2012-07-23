package com.chess.model;

import java.util.HashMap;

// TODO eliminate hashmaps and create Objects
public class GameListCurrentItem {

	public static final String GAME_TYPE = "game_type";
	public static final String GAME_ID = "game_id";
	public static final String TIMESTAMP = "time_stamp";
	public static final String IS_MY_TURN = "is_my_turn";
	public static final String OPPONENT_USERNAME = "opponent_username";
	public static final String OPPONENT_RATING = "opponent_rating";
	public static final String COLOR = "color";
	public static final String USERNAME_STRING_LENGTH = "username_string_length";
	public static final String TIME_REMAINING_AMOUNT = "time_remaining_amount";
	public static final String TIME_REMAINING_UNITS = "time_remaining_units";
	public static final String FEN_STRING_LENGTH = "fen_string_length";
	public static final String LAST_MOVE_FROM_SQUARE = "last_move_from_square";
	public static final String LAST_MOVE_TO_SQUARE = "last_move_to_square";
	public static final String IS_DRAW_OFFER_PENDING = "is_draw_offer_pending";
	public static final String IS_OPPONENT_ONLINE = "is_opponent_online";
	private static final String FEN = "fen";
	private static final String HAS_NEW_MESSAGE = "has_new_message";


	private HashMap<String, String> values;

	public GameListCurrentItem(String[] values) {
		this.values = new HashMap<String, String>();

		this.values.put(GAME_ID, values[0]);
		this.values.put(COLOR, values[1]);
		this.values.put(GAME_TYPE, values[2]);
		this.values.put(USERNAME_STRING_LENGTH, values[3]);
		this.values.put(OPPONENT_USERNAME, values[4]);
		this.values.put(OPPONENT_RATING, values[5]);
		this.values.put(TIME_REMAINING_AMOUNT, values[6]);
		this.values.put(TIME_REMAINING_UNITS, values[7]);
		this.values.put(FEN_STRING_LENGTH, values[8]);
		this.values.put(FEN, values[9]);
		this.values.put(TIMESTAMP, values[10]);
		this.values.put(LAST_MOVE_FROM_SQUARE, values[11]);
		this.values.put(LAST_MOVE_TO_SQUARE, values[12]);
		this.values.put(IS_DRAW_OFFER_PENDING, values[13]);
		this.values.put(IS_OPPONENT_ONLINE, values[14]);
		this.values.put(IS_MY_TURN, values[15]);
		this.values.put(HAS_NEW_MESSAGE, values[16]);

	}

	public long getGameId(){
		return Long.parseLong(values.get(GAME_ID));
	}

	public String getColor() {
		return values.get(COLOR);
	}

	public String getGameType() {
		return values.get(GAME_TYPE);
	}

	public String getUsernameStringLength() {
		return values.get(USERNAME_STRING_LENGTH);
	}

	public String getOpponentUsername() {
		return values.get(OPPONENT_USERNAME);
	}

	public String getOpponentRating() {
		return values.get(OPPONENT_RATING);
	}

	public String getTimeRemainingAmount() {
		return values.get(TIME_REMAINING_AMOUNT);
	}

	public String getTimeRemainingUnits() {
		return values.get(TIME_REMAINING_UNITS);
	}

	public String getFenStringLength() {
		return values.get(FEN_STRING_LENGTH);
	}

	public String getFen() {
		return values.get(FEN);
	}

	public String getTimestamp() {
		return values.get(TIMESTAMP);
	}

	public String getLastMoveFromSquare() {
		return values.get(LAST_MOVE_FROM_SQUARE);
	}

	public String getLastMoveToSquare() {
		return values.get(LAST_MOVE_TO_SQUARE);
	}

	public String getIsDrawOfferPending() { // TODO
		return values.get(IS_DRAW_OFFER_PENDING);
	}

	public String getIsOpponentOnline() {
		return values.get(IS_OPPONENT_ONLINE);
	}

	public String getHasNewMessage() { // TODO
		return values.get(HAS_NEW_MESSAGE);
	}

	public String getIsMyTurn() { // TODO
		return values.get(IS_MY_TURN);
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		for (String string : values.keySet()) {
			builder.append(" key = ").append(string).append(" value = ").append(values.get(string)).append("\n");
		}
		return builder.toString();
	}




//	The echess current games response looks like the following:
//	<
//	<game_id>: The game id
//	<player_color>: The users color he/she will play as, w = white, b = black
//	<game_type>: The chess game type.  1 = chess, 2 = chess960
//	<game_name>: The games name, can be null
//	<white_username>: The username for white
//	<black_username>: The username for black
//	<white_rating>: Whites rating
//	<black_rating>: Blacks rating
//	<time_stamp>: The timestamp for the game
//	<time_remaining_amount>: Remaining time for the game
//	<time_remaining_units>: the units, d for day and h for hours
//	<initial_fen>: the initial starting position, can be null
//	<last_move_from_square>: the last move, from square
//	<last_move_to_square>: the last move, to square
//	<is_draw_offer_pending>: Draw offer pending.  n = no, p = pending offer
//	<is_opponent_online>: Is the opponent online. 1 = yes, 0 = no
//	<is_my_turn>: is it the users turn to move. 1 = yes, 0 = no
//	<has_new_message>: new messages for the game. 1 = yes, 0 = no
//	<move_list>: the moves from the start of the game (all moves after <intial_fen>)
//	<days_per_move>: how many days per move.
//	>
//
}

package com.chess.ui.views;

import android.content.Context;
import android.graphics.Canvas;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.MotionEvent;
import com.chess.backend.statics.AppData;
import com.chess.backend.statics.StaticData;
import com.chess.ui.engine.ChessBoard;
import com.chess.ui.engine.Move;
import com.chess.ui.interfaces.BoardViewNetworkFace;
import com.chess.ui.interfaces.GameActivityFace;
import com.chess.ui.interfaces.GameNetworkActivityFace;

import java.util.Iterator;
import java.util.TreeSet;

public abstract class ChessBoardNetworkView extends ChessBoardBaseView implements BoardViewNetworkFace {

	private String whiteUserName;
	private String blackUserName;
	public GameNetworkActivityFace gameActivityFace;

	public ChessBoardNetworkView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	protected abstract boolean need2ShowSubmitButtons();

	@Override
	public void setGameActivityFace(GameActivityFace gameActivityFace) {
		super.setGameActivityFace(gameActivityFace);

		whiteUserName = gameActivityFace.getWhitePlayerName();
		blackUserName = gameActivityFace.getBlackPlayerName();
	}

	@Override
	public void afterMove() {
		getBoardFace().setMovesCount(getBoardFace().getHply());
		gameActivityFace.invalidateGameScreen();

		if (!getBoardFace().isAnalysis()) {
			if (need2ShowSubmitButtons()) {
				getBoardFace().setSubmit(true);
				gameActivityFace.showSubmitButtonsLay(true);
			} else {
				gameActivityFace.updateAfterMove();
			}
		}

		isGameOver();
	}

	@Override
	protected void onDraw(Canvas canvas) {
		canvas.setDrawFilter(drawFilter);
		super.onDraw(canvas);

		drawBoard(canvas);

		if (gameActivityFace != null && getBoardFace() != null) {
			drawPieces(canvas);

			drawHighlights(canvas);

			drawDragPosition(canvas);

			drawTrackballDrag(canvas);
		}

		drawCoordinates(canvas);
	}

	@Override
	public boolean onTrackballEvent(MotionEvent event) {
		if (useTouchTimer) { // start count before next touch
			handler.postDelayed(checkUserIsActive, StaticData.WAKE_SCREEN_TIMEOUT);
			userActive = true;
		}

		float sens = 0.3f;
		if (event.getAction() == MotionEvent.ACTION_MOVE) {
			track = true;
			if (event.getX() > sens)
				trackX += square;
			else if (event.getX() < -sens)
				trackX -= square;
			if (event.getY() > sens)
				trackY += square;
			else if (event.getY() < -sens)
				trackY -= square;
			if (trackX < 0)
				trackX = 0;
			if (trackY < 0)
				trackY = 0;
			if (trackX > 7 * square)
				trackX = 7 * square;
			if (trackY > 7 * square)
				trackY = 7 * square;
			invalidate();
		} else if (event.getAction() == MotionEvent.ACTION_DOWN) {
			int col = (trackX - trackX % square) / square;
			int row = (trackY - trackY % square) / square;

			if (firstClick) {
				from = ChessBoard.getPositionIndex(col, row, getBoardFace().isReside());
				if (getBoardFace().getPieces()[from] != 6 && getBoardFace().getSide() == getBoardFace().getColor()[from]) {
					pieceSelected = true;
					firstClick = false;
					invalidate();
				}
			} else {
				to = ChessBoard.getPositionIndex(col, row, getBoardFace().isReside());
				pieceSelected = false;
				firstClick = true;
				boolean found = false;

				TreeSet<Move> moves = getBoardFace().gen();
				Iterator<Move> moveIterator = moves.iterator();

				Move move = null;
				while (moveIterator.hasNext()) {
					move = moveIterator.next();
					if (move.from == from && move.to == to) {
						found = true;
						break;
					}
				}
				if ((((to < 8) && (getBoardFace().getSide() == ChessBoard.WHITE_SIDE)) ||
						((to > 55) && (getBoardFace().getSide() == ChessBoard.BLACK_SIDE))) &&
						(getBoardFace().getPieces()[from] == ChessBoard.PAWN) && found) {

					gameActivityFace.showChoosePieceDialog(col, row);
					return true;
				}
				if (found && getBoardFace().makeMove(move)) {
					invalidate();
					afterMove();
				} else if (getBoardFace().getPieces()[to] != 6 && getBoardFace().getSide() == getBoardFace().getColor()[to]) {
					pieceSelected = true;
					firstClick = false;
					from = ChessBoard.getPositionIndex(col, row, getBoardFace().isReside());
					invalidate();
				} else {
					invalidate();
				}
			}
		}
		return true;
	}

	@Override
	public void flipBoard() {
		getBoardFace().setReside(!getBoardFace().isReside());

		invalidate();
		gameActivityFace.invalidateGameScreen();
	}


	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if (useTouchTimer) { // start count before next touch
			handler.postDelayed(checkUserIsActive, StaticData.WAKE_SCREEN_TIMEOUT);
			userActive = true;
		}

		if (square == 0) {
			return super.onTouchEvent(event);
		}

        if (isLocked()) {
			return processTouchEvent(event);
		}

		track = false;
		if (!getBoardFace().isAnalysis()) {
			if (AppData.isFinishedEchessGameMode(getBoardFace()) || getBoardFace().isFinished() || getBoardFace().isSubmit() ||
					(getBoardFace().getHply() < getBoardFace().getMovesCount())) {
				return true;
			}

			if(TextUtils.isEmpty(whiteUserName) || TextUtils .isEmpty(blackUserName))
				return true;

			if (whiteUserName.equals(userName)  && !getBoardFace().isWhiteToMove()) {
				return true;
			}
			if (blackUserName.equals(userName) && getBoardFace().isWhiteToMove()) {
				return true;
			}

		}

		return super.onTouchEvent(event);
	}

    @Override
	public void showChat() {
		gameActivityFace.switch2Chat();
	}

//	public void updateMoves(String newMove) {
//		int[] moveFT = MoveParser.parse(getBoardFace()(), newMove);
//		if (moveFT.length == 4) {
//			Move move;
//			if (moveFT[3] == 2)
//				move = new Move(moveFT[0], moveFT[1], 0, 2);
//			else
//				move = new Move(moveFT[0], moveFT[1], moveFT[2], moveFT[3]);
//
//			getBoardFace().makeMove(move, false);
//		} else {
//			Move move = new Move(moveFT[0], moveFT[1], 0, 0);
//			getBoardFace().makeMove(move, false);
//		}
//	}

	public void updatePlayerNames(String whitePlayerName, String blackPlayerName) {
		whiteUserName = whitePlayerName.substring(0, whitePlayerName.indexOf(StaticData.SYMBOL_LEFT_PAR)).trim().toLowerCase();
		blackUserName = blackPlayerName.substring(0, blackPlayerName.indexOf(StaticData.SYMBOL_LEFT_PAR)).trim().toLowerCase();
	}

	public void setControlsView(ControlsNetworkView controlsView) {
		super.setControlsView(controlsView);
		controlsView.setBoardViewFace(this);
	}

	public void setGameActivityFace(GameNetworkActivityFace gameActivityFace) {
		super.setGameActivityFace(gameActivityFace);
		this.gameActivityFace = gameActivityFace;
	}

}

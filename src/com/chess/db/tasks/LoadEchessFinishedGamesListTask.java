package com.chess.db.tasks;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import com.chess.backend.interfaces.TaskUpdateInterface;
import com.chess.backend.statics.AppData;
import com.chess.backend.statics.StaticData;
import com.chess.backend.tasks.AbstractUpdateTask;
import com.chess.db.DBConstants;
import com.chess.db.DBDataManager;


public class LoadEchessFinishedGamesListTask extends AbstractUpdateTask<Cursor, Long> {

    private ContentResolver contentResolver;
	private static String[] arguments = new String[1];

	public LoadEchessFinishedGamesListTask(TaskUpdateInterface<Cursor> taskFace) {
        super(taskFace);

		contentResolver = taskFace.getMeContext().getContentResolver();
    }

    @Override
    protected Integer doTheTask(Long... ids) {
		String userName = AppData.getUserName(taskFace.getMeContext());

		Uri uri = DBConstants.ECHESS_FINISHED_LIST_GAMES_CONTENT_URI;
		arguments[0] = userName;
		item = contentResolver.query(uri, DBDataManager.PROJECTION_FINISHED_LIST_GAMES,
				DBDataManager.SELECTION_USER, arguments, null);

		if(item.moveToFirst()) {
			result = StaticData.RESULT_OK;
		}

        return result;
    }


}

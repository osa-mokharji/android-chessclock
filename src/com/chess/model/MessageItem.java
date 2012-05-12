package com.chess.model;

import com.chess.backend.statics.StaticData;
import com.chess.ui.core.AppConstants;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

public class MessageItem {
	public String owner = StaticData.SYMBOL_EMPTY;
	public String message = StaticData.SYMBOL_EMPTY;

	public MessageItem(String owner, String msg) {
		this.owner = owner;
		try {
			this.message = URLDecoder.decode(msg, AppConstants.UTF_8);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
	}
}

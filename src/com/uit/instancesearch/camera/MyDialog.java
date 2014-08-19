package com.uit.instancesearch.camera;

import android.app.Dialog;
import android.content.Context;
import android.widget.TextView;

public class MyDialog {
	
	final Dialog dialog;
	
	public MyDialog(Context context, String text) {
		dialog = new Dialog(context);
		dialog.setContentView(R.layout.dialog);
		TextView textView = (TextView)dialog.findViewById(R.id.dialog_text_view);
		textView.setText(text);
	}
	
	public void show() {
		dialog.show();
	}
}

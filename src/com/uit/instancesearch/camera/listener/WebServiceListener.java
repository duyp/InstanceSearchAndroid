package com.uit.instancesearch.camera.listener;

import android.graphics.Bitmap;

public interface WebServiceListener {
	public void onQuerying();
	public void onServerResponse();
	public void onResultRecieved(Bitmap result, String imageId);
	public void onFullImageRecieved(Bitmap result);
	public void onConnectionError();
}

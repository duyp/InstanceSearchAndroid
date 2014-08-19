package com.uit.instancesearch.camera.listener;

public interface ResultListener {
	public void onCompleted();
	public void onRequestFullImage(String imageId);
	public void onResultViewResize(int dx);
	public void onResultViewPrepareResize();
	public void onResultViewResized();
}
package com.uit.instancesearch.camera;

import java.util.ArrayList;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;

public class ImageResultAdapter extends BaseAdapter{

	Context mContext;
	ArrayList<Bitmap> contents;
	ArrayList<String> ids;
	
	ResultView mParent;
	
	public ImageResultAdapter(Context c, ResultView parent) {
		mContext = c;
		this.mParent = parent;
		contents = new ArrayList<Bitmap>();
		ids = new ArrayList<String>();
	}
	
	@Override
	public View getView(int position, View view, ViewGroup parent) {
		ImageView imageView;
		if(view == null) {
			imageView = new ImageView(mContext);
		} else {
			imageView = (ImageView)view;
		}
		imageView.setImageBitmap(contents.get(position));
		AbsListView.LayoutParams p = new AbsListView.LayoutParams(mParent.getColumnWidth(), mParent.getRowHeight());
		imageView.setLayoutParams(p);
		//l.addView(imageView);
		//LayoutParams p = new LayoutParams(200, 200);
		//l.setLayoutParams(p);
		return imageView;
	}

	@Override
	public int getCount() {
		return contents.size();
	}

	@Override
	public Object getItem(int position) {
		return contents.get(position);
	}

	public String getImageId(int position) {
		return ids.get(position);
	}
	
	@Override
	public long getItemId(int position) {
		return position;
	}
	
	
	public void addItem(Bitmap m, String imageId){
		contents.add(m);
		ids.add(imageId);
	}
	
	public void clear() {
		contents.clear();
		ids.clear();
	}

}

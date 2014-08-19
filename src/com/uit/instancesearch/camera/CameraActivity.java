package com.uit.instancesearch.camera;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;

import com.uit.instancesearch.camera.listener.MenuActionListener;
import com.uit.instancesearch.camera.listener.RegionSelectListener;
import com.uit.instancesearch.camera.listener.ResultListener;
import com.uit.instancesearch.camera.listener.ResultTouchListener;
import com.uit.instancesearch.camera.listener.WebServiceListener;
import com.uit.instancesearch.camera.tools.StringTools;

import android.support.v7.app.ActionBarActivity;
import android.annotation.TargetApi;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.hardware.Camera;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class CameraActivity extends ActionBarActivity implements RegionSelectListener, 
																 WebServiceListener,
																 ResultListener,
																 MenuActionListener {

	private static final int SELECT_PHOTO = 100;
	
	CameraManager 		cameraManager; 	// camera manager
	WSManager 			wsManager;		// web services manager
	Camera 				camera;			// camera
	
	CameraPreview 		cPreview;		// camera preview
	RegionSelectionView regionView;		// region selection view
	MenuView			menuView;
	ResultView			resultView;
	
	FrameLayout			queryView;
	ImageView			queryImageView;
	TextView			queryTextView;
	
	ImageView			selectImageView;
	LinearLayout		selectView;
	
	boolean 			imageSelecting;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		//Remove title bar
	    this.requestWindowFeature(Window.FEATURE_NO_TITLE);

	    //Remove notification bar
	    this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

	   //set content view AFTER ABOVE sequence (to avoid crash)
	    this.setContentView(R.layout.activity_camera);
	    
	    imageSelecting = false;
		camera = CameraManager.getCameraInstance();
		if (camera != null) {
			//camera preview
			cPreview = new CameraPreview(this, camera);
			FrameLayout preview = (FrameLayout)findViewById(R.id.camera_preview);
			preview.addView(cPreview);
			//region view
			regionView = (RegionSelectionView)this.findViewById(R.id.region_view);
			regionView.init(this);
			//web services
			wsManager = new WSManager(this, this);
			// initialize Camera manager
			cameraManager = new CameraManager(this, camera, cPreview, regionView, wsManager);
			cameraManager.initialize(this,this);
			//menu view
			menuView = (MenuView)this.findViewById(R.id.menu_view);
			// result view
			resultView= (ResultView)this.findViewById(R.id.result_view);
			resultView.initListener(this);
			// query view
			queryView = (FrameLayout)this.findViewById(R.id.query_view);
			queryImageView = (ImageView)this.findViewById(R.id.query_image_view);
			queryTextView = (TextView)this.findViewById(R.id.query_text_view);
			// selected view
			selectView = (LinearLayout)this.findViewById(R.id.selected_view);
			selectImageView = (ImageView)this.findViewById(R.id.selected_image_view);
		}
		
		MenuView menuView = (MenuView)this.findViewById(R.id.menu_view);
		menuView.initializeCameraListener(this);
		
		regionView.setWidthLimited(CameraManager.getScreenSize().x - menuView.getLayoutParams().width);
		
		regionView.setOnTouchListener(new ResultTouchListener(this));
		queryView.setOnTouchListener(new ResultTouchListener(this));
		queryImageView.setOnTouchListener(new ResultTouchListener(this));
		
		queryImageView.setOnClickListener(new View.OnClickListener() {
			static final int MAX_TIME = 500; // ms
			long firstClickedTime = -1;
			
			@Override
			public void onClick(View v) {
				if (firstClickedTime == -1) {
					firstClickedTime = System.currentTimeMillis();
				} else if (System.currentTimeMillis() - firstClickedTime <= MAX_TIME){ // double click
					Bitmap bitmap = ((BitmapDrawable)queryImageView.getDrawable()).getBitmap();
					saveImage(bitmap);
					firstClickedTime = -1;
				} else {
					firstClickedTime = System.currentTimeMillis();
				}
			}
		});
	}
	
	@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
	private void showResultView() {
		// result view
		ViewGroup.LayoutParams p = resultView.getLayoutParams();
		p.width = CameraManager.getScreenSize().x*2/5;
		resultView.setLayoutParams(p);
		resultView.restart();
		resultView.show();
		
		// query view
		ViewGroup.LayoutParams p1 = queryView.getLayoutParams();
		p1.width = CameraManager.getScreenSize().x - p.width;
		queryView.setLayoutParams(p1);
		
		//API 12
		queryView.animate().translationX(0).setDuration(1000).setInterpolator(new OvershootInterpolator()).start();
	}
	
	@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
	private void hideResultView() {
		resultView.clearResults();
		resultView.hide();
		
		queryView.animate().translationX(-queryView.getWidth()).setDuration(500).setInterpolator(new AccelerateDecelerateInterpolator()).start();
		//Toast.makeText(this, "S:" + queryView.getWidth(), Toast.LENGTH_LONG).show();
	}
	
	public void resizeResult(int dx) {
		
	}
	
	private void setQueryImage(Bitmap bm) {
		queryImageView.setImageBitmap(bm);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.camera, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
	void hideMenu() {
		menuView.hideMenu();
		menuView.setTouchEnabled(false);
	}
	
	void showMenu() {
		menuView.showMenu();
		menuView.setTouchEnabled(true);
	}
	
	/** A safe way to get an instance of the Camera object. */
	
    
    private void releaseCamera(){
        if (camera != null){
            camera.release();        // release the camera for other applications
            camera = null;
        }
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        if (!imageSelecting) {
        	releaseCamera();
        	android.os.Process.killProcess(android.os.Process.myPid());
        }
    }
    
    @Override
    protected void onResume() {
    	super.onResume();
    	if (imageSelecting) {
    		camera.stopPreview();
    	}
    	//camera = CameraManager.getCameraInstance();
    }

    @Override
    public void onBackPressed() {
    	if (regionView.isScanning()) {
    		regionView.stopScan();
    		wsManager.cancelExecute();
    	} else if (resultView.isShown()) {
    		this.onCompleted();
    	}
    }
    
	@Override
	public void onRegionSelected(Rect regionRect, MotionEvent e) {
		cameraManager.setRegionSelected(regionRect);
	}


	@Override
	public void onRegionConfirmed(Bitmap croppedImage) {
		setQueryImage(croppedImage);
	}
	
	@Override
	public void onRegionCancelScan() {
		if (imageSelecting) {
			selectView.setVisibility(View.INVISIBLE);
			imageSelecting = false;
		}
		showMenu();
		camera.startPreview();
		cameraManager.resumeFlash();
	}

	// Result listener
	@Override
	public void onCompleted() {
		hideResultView();
		camera.startPreview();
		cameraManager.resumeFlash();
		showMenu();
		regionView.setSelectEnabled(true);
		resultView.clearResults();
		//regionView.setVisibility(View.VISIBLE);
	}
	
	@Override
	public void onRequestFullImage(String imageId) {
		resultView.setItemClickEnabled(false);
		queryTextView.setText(R.string.loading);
		wsManager.executeFullImageRequest(imageId);
	}
	
	@Override
	public void onResultViewPrepareResize() {
		resultView.prepareResize();
	}
	
	@Override
	public void onResultViewResize(int dx) {
		queryTextView.setText(String.valueOf(dx));
		int x;
		if ((x=resultView.resize(dx)) != 0) {
			ViewGroup.LayoutParams p1 = queryView.getLayoutParams();
			p1.width = x;
			queryView.setLayoutParams(p1);
			queryView.invalidate();
		}
	}
	
	@Override
	public void onResultViewResized() {
		resultView.prepareResize();
	}
	// end Result listener
    

	// Web Service listener
	@Override
	public void onServerResponse() {
		//resultView.clearResults();
		
		regionView.doneScan();
		
		if (imageSelecting) {
			selectView.setVisibility(View.INVISIBLE);
			imageSelecting = false;
		}
		showResultView();
		hideMenu();
		camera.startPreview();
	}


	@Override
	public void onQuerying() {
		regionView.startScan();
		regionView.setSelectEnabled(false);
		hideMenu();
		Toast.makeText(this, "Analyzing... Press back to cancel.", Toast.LENGTH_SHORT).show();
	}

	@Override
	public void onResultRecieved(Bitmap result, String imageId) {
		resultView.addResultImage(result, imageId);
	}

	@Override
	public void onFullImageRecieved(Bitmap result) {
		setQueryImage(result);
		queryTextView.setText(R.string.none);
		resultView.setItemClickEnabled(true);
	}
	
	@Override
	public void onConnectionError() {
		if (regionView.isScanning()) {
			regionView.stopScan();
		}
		if (resultView.isShown()) {
			queryTextView.setText(R.string.none);
		}
	}
	// end Web Service listener
	
	
	@Override
	public void onCameraFlashChange(boolean on) {
		cameraManager.flashChange(on);
	}

	@Override
	public void onCameraCapture() {
		cameraManager.capture();
		cameraManager.pauseFlash();
	}

	@Override
	public void onSelectImage() {
		selectAnImage();
	}
	
	@Override
    protected void onActivityResult(int requestCode, int resultCode, Intent imageReturnedIntent){
    	super.onActivityResult(resultCode, resultCode, imageReturnedIntent);
    	switch(requestCode){
    	case SELECT_PHOTO:
    		if (resultCode == RESULT_OK){
    			Uri selectedImage = imageReturnedIntent.getData();
    			//String filename = selectedImage.getLastPathSegment();
    			try {
					InputStream imageStream = this.getContentResolver().openInputStream(selectedImage);
					Bitmap img = BitmapFactory.decodeStream(imageStream);
					img = CameraManager.scaleBitmap(img);
					
					selectView.setVisibility(View.VISIBLE);
					selectImageView.setImageBitmap(img);			
					
					regionView.setRegion(getImageRect(selectImageView));
					
					wsManager.executeQueryRequest(img);
					this.onQuerying();
					this.onRegionConfirmed(img);
					//selectedImageView.setVisibility(View.VISIBLE);
    			} catch (FileNotFoundException e) {
					e.printStackTrace();
				}
    		}
    		break;
    		
    	default: return;
    	}
    }
	
	Rect getImageRect(ImageView imageView) {
		if (imageView.getDrawable() != null) {
			int h = imageView.getMeasuredHeight(); //height of imageView
			int w = imageView.getMeasuredWidth(); //width of imageView
			int ih=h;
			int iw=w;
			int iH=imageView.getDrawable().getIntrinsicHeight();//original height of underlying image
			int iW=imageView.getDrawable().getIntrinsicWidth();//original width of underlying image
			
			if (ih/iH<=iw/iW) iw=iW*ih/iH;//rescaled width of image within ImageView
			else ih= iH*iw/iW;//rescaled height of image within ImageView
			
			float left, top, right, bottom;
			if (iw == w) {
				left = 0;
				right = iw;
				top = (h-ih)/2;
				bottom = top+ih;
			} else {
				top = 0;
				bottom = ih;
				left = (w-iw)/2;
				right = left+iw;
			}
			return new Rect((int)left,(int)top,(int)right,(int)bottom);
			//return new Rect(0,0,iw,ih);
		}
		return null;
	}
	
    public void selectAnImage(){
    	imageSelecting = true;
    	Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
    	photoPickerIntent.setType("image/*");
    	this.startActivityForResult(photoPickerIntent, SELECT_PHOTO);
    }
    
    public void saveImage(Bitmap bm) {
		FileOutputStream out;
		try {
			File f = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
			String outFile = f.toString() + "/issearch_" + StringTools.getRandomNumberString(10) + ".jpg";
		    out = new FileOutputStream(outFile);
		    bm.compress(Bitmap.CompressFormat.JPEG, 100, out);
		    Toast.makeText(this, "Saved to: "+outFile, Toast.LENGTH_LONG).show();
		} catch (Exception e) {
		    e.printStackTrace();
		    Toast.makeText(this,e.getMessage(), Toast.LENGTH_LONG).show();
		}
    }


}

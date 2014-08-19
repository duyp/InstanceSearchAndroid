package com.uit.instancesearch.camera;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.ksoap2.SoapEnvelope;
import org.ksoap2.serialization.SoapObject;
import org.ksoap2.serialization.SoapSerializationEnvelope;
import org.ksoap2.transport.HttpTransportSE;
import org.xmlpull.v1.XmlPullParserException;

import com.uit.instancesearch.camera.listener.WebServiceListener;
import com.uit.instancesearch.camera.tools.Encoder;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Build;
import android.util.Base64;
import android.widget.Toast;

public class WSManager {
	
	public static final String TAG_QUERY = "query";
	public static final String TAG_GET_IMAGE = "get";
	
	//private static final String URL = "http://phamduy.ddns.net:8080/InstanceSearch/services/ISService?wsdl";
	//private static final String URL = "http://192.168.0.101:8080/InstanceSearch/services/ISService?wsdl";
	private static final String URL = "http://192.168.24.59:8080/InstanceSearch/services/ISService?wsdl";
	public static final String NAMESPACE = "http://services.instancesearch.uit.com";
	public static final String SOAP_ACTION_PREFIX = "/";
	private static final String METHOD = "clientQueryRequest";
	
	WebServiceListener wsListener;
	Context context;
	
	boolean cancelled;
	
	public WSManager(Context c, WebServiceListener wsl){
		wsListener = wsl;
		context = c;
		cancelled = false;
	}
	
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	public void executeQueryRequest(Bitmap bm) {
		cancelled = false;
		String image = Encoder.encodeBitmap(bm);
		ServiceRunner runner = new ServiceRunner("Xperia Z", image, TAG_QUERY);
		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			runner.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
		} else {
			runner.execute();
		}
	}
	
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	public void executeFullImageRequest(String imageId) {
		ServiceRunner runner = new ServiceRunner("Xperia Z", imageId, TAG_GET_IMAGE);
		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			runner.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
		} else {
			runner.execute();
		}
	}
	
	public void cancelExecute() {
		cancelled = true;
	}
	
	private class ServiceRunner extends AsyncTask<String, String, String>{

		private String name;
		
		private String requestContent;
		private String requestTag;

		protected ServiceRunner(String name, String requestContent, String requestTag) {
			this.name = name;
			this.requestContent = requestContent;
			this.requestTag = requestTag;
		}
		
		@Override
		protected String doInBackground(String... params) {
			//publishProgress(new String[] {"info", "Uploading image..."});
			
				SoapSerializationEnvelope envelope = new SoapSerializationEnvelope(SoapEnvelope.VER11);
				SoapObject request = new SoapObject(NAMESPACE,METHOD);
				
				request.addProperty("name", name);
				request.addProperty("requestTag",requestTag);
				request.addProperty("queryImageContent",requestContent);
				
				envelope.bodyOut = request;
				
				HttpTransportSE transport = new HttpTransportSE(URL);
				try {
					transport.call(NAMESPACE+SOAP_ACTION_PREFIX+METHOD, envelope);
				} catch (XmlPullParserException e) {
					e.printStackTrace();
					publishProgress(new String[] {"err"});
				} catch (IOException e) {
					publishProgress(new String[] {"err"});
				}
				
				if (envelope.bodyIn != null && !cancelled) {
					SoapObject resultSoap = (SoapObject)envelope.bodyIn;
					// if error
					if (resultSoap.getProperty(0).toString().equals("err")) {
						publishProgress(new String[] {"err"});
					} else {
						if (requestTag.equals(TAG_QUERY)) {
							publishProgress("respond"); //wsListener.onServerResponse();
							int count = resultSoap.getPropertyCount();
							if (count > 1)
							for(int i = 0; i < count; i+=2) {
								publishProgress(new String[] {"result", resultSoap.getProperty(i).toString(), resultSoap.getProperty(i+1).toString()});
							}
						} else {
							publishProgress(new String[] {"result", resultSoap.getProperty(0).toString()});
						}
					}
				} else {
					//publishProgress(new String[]{"info", "Server not responding. Please try again!"});
				}
			return "OK";
		}
		
		@Override
		protected void onProgressUpdate(String... s) {
			if (cancelled) return;
			
			String action = s[0];
			if (action.equals("result")) {
				byte[] b = Base64.decode(s[1], Base64.DEFAULT);
				Bitmap result = BitmapFactory.decodeByteArray(b, 0, b.length);
				if (requestTag.equals(TAG_QUERY)) {
					wsListener.onResultRecieved(result, s[2]); // query result
				} else {
					wsListener.onFullImageRecieved(result);		// full image
				}
			} else if (action.equals("respond")) {
				wsListener.onServerResponse();
			} else if (action.equals("info")){
				Toast.makeText(context, s[1], Toast.LENGTH_LONG).show();
			} else if (action.equals("err")) {
				Toast.makeText(context, "Connection problem. Please try again!", Toast.LENGTH_LONG).show();
				wsListener.onConnectionError();
			}
		}
	}
}

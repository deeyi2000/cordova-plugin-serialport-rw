package android_serialport_api.sample;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.InvalidParameterException;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaArgs;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.apache.cordova.LOG;

import org.json.JSONArray;
import org.json.JSONException;

import android_serialport_api.SerialPort;
import android_serialport_api.SerialPortFinder;

public class SerialPortRW extends CordovaPlugin {
	private static final String TAG = "SerialPortRW";
	private SerialPortFinder mSerialPortFinder = new SerialPortFinder();
	//protected SerialPort mSerialPort;
	//protected OutputStream mOutputStream;
	//private InputStream mInputStream;
	//private ReadThread mReadThread;
	private SerialPortRW instance;
	//private int baudRate;
	//private String port;
	//private int Size=0;
	private boolean isTimer=true;
	private byte[] dateBuffer=new byte[128];
	private Long interval=(long) 0;
	Map<String,SerialModel> mSerialList = new HashMap<>(16);

	class SerialModel {
		InputStream mInputStream;
		OutputStream mOutputStream;
		SerialPortRW.ReadThread mReadThread;
		SerialPort mSerialPort;
		int Size=0;
		boolean isTimer=true;
		byte[] dateBuffer=new byte[128];
		Long interval=(long) 0;
	}

	 class ReadThread extends Thread {
		private CallbackContext mCallbackContext;
		private SerialModel  mModel;
		public ReadThread(SerialModel model,CallbackContext callbackContext) {
			super();
			mCallbackContext = callbackContext;
			this.mModel = model;
		}
		
		@Override
		public void run() {
			super.run();
			while (!isInterrupted()) {
				int size;
				try {
					byte[] buffer = new byte[64];
					size = mModel.mInputStream.read(buffer);
					mModel.interval=System.currentTimeMillis();
					for(int i=0;i<size;i++){
						if((mModel.Size+i) >= mModel.dateBuffer.length) break;
						mModel.dateBuffer[mModel.Size+i] = buffer[i];
					};
					mModel.Size+=size;
					if (size>0 && mModel.Size>0) {
						if(mModel.isTimer){
							mModel.isTimer=false;
							final Timer timer = new Timer(); 
							timer.schedule(new TimerTask(){
								public void run(){
									Long tsLong = System.currentTimeMillis();
									if(tsLong-75> mModel.interval){
										onDataReceived(mModel.dateBuffer, mModel.Size, mCallbackContext);
										mModel.interval=(long) 0;
										mModel.Size=0;
										mModel.dateBuffer=new byte[128];
										timer.cancel();
										mModel.isTimer=true;
									}
								}
							}, 75, 75);
						}
					}
				} catch (IOException e) {
					e.printStackTrace();
					return;
				}
			}
		}
	}

	@Override
	public boolean execute(String action, CordovaArgs args, CallbackContext callbackContext) throws JSONException {
		LOG.v(TAG, "Serial Action " + action);
		instance = this;
		if (action.equals("get")) {
			JSONArray resArr = new JSONArray();
			String[] entryValues = mSerialPortFinder.getAllDevicesPath();
			for (int i = 0; i < entryValues.length; i++) {
				resArr.put(i, entryValues[i]);
			}
			callbackContext.success(resArr);
			return true;
		} else if (action.equals("open")) {
			openSerialPort(args, callbackContext);
			return true;
		} else if (action.equals("close")) {
			closeSerialPort(args,callbackContext);
			return true;
		} else if (action.equals("emission")) {
			emission(args,callbackContext);
			return true;
		}else if(action.equals("listen")){
			registry(args,callbackContext);
			return true;
		}
		else if (action.equals("detect")) {
//			if (mSerialPort != null) {
//				callbackContext.success(1);
//			} else {
//				callbackContext.success(0);
//			}
			return true;
		}
		return true;
	}

	private void openSerialPort(final CordovaArgs args, final CallbackContext callbackContext) {
		cordova.getThreadPool().execute(new Runnable() {
			public void run() {
				try {
					int baudRate = args.getInt(1);
					String port = args.getString(0);
					SerialPort serialPort = new SerialPort(new File(port), baudRate, 0);
					SerialModel serialModel = new SerialModel();
					serialModel.mSerialPort = serialPort;
					mSerialList.put(port,serialModel);
					callbackContext.success(1);
				} catch (SecurityException e) {
					callbackContext.error(0);
				} catch (IOException e) {
					callbackContext.error(0);
				} catch (InvalidParameterException e) {
					callbackContext.error(0);
				} catch (JSONException e) {
					e.printStackTrace();
					callbackContext.error(0);
				}
			}
		});

	}
	
	
	protected void onDataReceived(final byte[] buffer, final int size, CallbackContext callbackContext) {
		final byte[] dataBuffer=new byte[size];
		for(int i=0;i<size;i++){
			dataBuffer[i]=buffer[i];
		}
		cordova.getActivity().runOnUiThread(new Runnable() {
			public void run() {
				if (callbackContext != null) {
		            PluginResult result = new PluginResult(PluginResult.Status.OK, dataBuffer);
		            result.setKeepCallback(true);
		            callbackContext.sendPluginResult(result);
		        }
			}
		});
	}

	public void registry(final CordovaArgs args,final CallbackContext callbackContext){
		try {
			String port = args.getString(0);
			SerialModel serialModel = mSerialList.get(port);
			if (serialModel.mSerialPort != null) {
				PluginResult result = new PluginResult(PluginResult.Status.NO_RESULT);
				result.setKeepCallback(true);
				callbackContext.sendPluginResult(result);
				startRead(serialModel,callbackContext);
			} else {
				callbackContext.error("Serial port not open");
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}

		
	}
	
	public void startRead(SerialModel serialModel,final CallbackContext callbackContext){
		serialModel.mOutputStream = serialModel.mSerialPort.getOutputStream();
		serialModel.mInputStream = serialModel.mSerialPort.getInputStream();
		ReadThread mReadThread = new ReadThread(serialModel,callbackContext);
		mReadThread.start();
		serialModel.mReadThread = mReadThread;
	}
	
	public JSONArray bytesToHexStrings(byte[] src) {
		if (src == null || src.length <= 0) {
			return null;
		}
		JSONArray res = new JSONArray();
		for (int i = 0; i < src.length; i++) {
			int v = src[i];
			try {
				res.put(i, v);
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
		return res;
	}
	
	public void emission(CordovaArgs args,final CallbackContext callbackContext) throws JSONException{
		final byte[] str = args.getArrayBuffer(1);
		String port = args.getString(0);
		SerialModel serialModel = mSerialList.get(port);
		serialModel.mOutputStream = serialModel.mSerialPort.getOutputStream();
		cordova.getThreadPool().execute(new Runnable() {
			public void run() {
				try {
					serialModel.mOutputStream.write(str);
					callbackContext.success();
				} catch (IOException e) {
					callbackContext.error(e.getMessage());
					e.printStackTrace();
				}
			}
		});
		
	}
	
	public void closeSerialPort(CordovaArgs args,CallbackContext callbackContext) {
		try {
			String port = args.getString(0);
			SerialModel serialModel = mSerialList.get(port);
			if (serialModel.mReadThread != null) {
				serialModel.mReadThread.interrupt();
			}
			serialModel.mSerialPort.close();
			serialModel.mSerialPort = null;
			callbackContext.success(0);
		} catch (JSONException e) {
			e.printStackTrace();
		}

	}
}

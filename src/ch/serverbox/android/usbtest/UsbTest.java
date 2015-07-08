package ch.serverbox.android.usbtest;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.android.future.usb.UsbAccessory;
import com.android.future.usb.UsbManager;

public class UsbTest extends Activity {
	private static String TAG = "UsbTest";
	
	private UsbAccessory mAccessory = null;
	private Button mBtSend = null;
	private FileOutputStream mFout = null;
	private FileInputStream mFin = null;
	private PendingIntent mPermissionIntent = null;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        //((NfcManager)getSystemService(NFC_SERVICE)).getDefaultAdapter().enableForegroundDispatch(this, intent, filters, techLists)
        IntentFilter i = new IntentFilter();
        i.addAction(UsbManager.ACTION_USB_ACCESSORY_ATTACHED);
        i.addAction(UsbManager.ACTION_USB_ACCESSORY_DETACHED);
        i.addAction("ch.serverbox.android.usbtest.USBPERMISSION");
        registerReceiver(mUsbReceiver,i);
        
        if(getIntent().getAction().equals("android.hardware.usb.action.USB_ACCESSORY_ATTACHED")){
        	Log.d("USB","Action is usb");
        	UsbAccessory accessory = UsbManager.getAccessory(getIntent());
        	mAccessory = accessory;
        	FileDescriptor fd = null;
        	try{
        		fd = UsbManager.getInstance(this).openAccessory(accessory).getFileDescriptor();
        	}catch(IllegalArgumentException e){
        		finish();
        	}catch(NullPointerException e){
        		finish();
        	}
        	mFout = new FileOutputStream(fd);
        	mFin = new FileInputStream(fd);
        }else{
        	UsbAccessory[] accessories = UsbManager.getInstance(this).getAccessoryList();
        	if(accessories != null){
	        	for(UsbAccessory a : accessories){
	        		l("accessory: "+a.getManufacturer());
	        		if(a.getManufacturer().equals("Nexus-Computing GmbH")){
	        			mPermissionIntent = PendingIntent.getBroadcast(this, 0, new Intent("ch.serverbox.android.usbtest.USBPERMISSION"),0);
	        			UsbManager.getInstance(this).requestPermission(a,mPermissionIntent);
	        			Log.d("USB", "permission requested");
	        			break;
	        		}
	        	}
        	} else {
        		Log.d("USB", "UsbAccessory is null!");
        	}
        }
        
        mBtSend = (Button)(findViewById(R.id.btSebd));
        mBtSend.setOnClickListener(new View.OnClickListener() {
        	
			@Override
			public void onClick(View v) {
				String s = ((EditText)findViewById(R.id.editText1)).getText().toString();
				queueWrite(s);
				queueRead();
//				try {
//
//					String recMsg = receive();
//					((TextView)findViewById(R.id.textView2)).setText(recMsg);
//					
//				} catch (IOException e) {
//					e.printStackTrace();
//				} 
				
			}
		});
    }
    
    @Override
    protected void onDestroy() {
    	unregisterReceiver(mUsbReceiver);
    	super.onDestroy();
    }
    
    public void queueWrite(final String data){
    	if(mAccessory == null){
    		return;
    	}
    	new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					//Log.d("USB", "Writing length "+data.length());
					//mFout.write(new byte[]{(byte)data.length()});
					Log.d("USB", "Writing data: "+data);
					mFout.write(data.getBytes());
					Log.d("USB","Done writing");
					
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}).start();
    }
    public void queueRead(){
    	if(mAccessory == null){
    		return;
    	}
    	new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					byte[] buff = new byte[1024];
					int len = mFin.read(buff);
					String msg = new String(buff,0,len,"UTF-8");
					Log.d(TAG, ">>> Read: " + msg);
					//((TextView)findViewById(R.id.textView2)).setText(msg);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}).start();
    }
    
	public String receive() throws IOException {
		byte[] buff = new byte[1024];
		int len = mFin.read(buff);
		return new String(buff,0,len,"UTF-8");
	}

	private final class ReadData {
		String data;
		public ReadData(){
		
		}
		public void setData(String data){
			this.data = data;
		}
		public String getData(){
			return this.data;
		}
	}
	private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (UsbManager.ACTION_USB_ACCESSORY_ATTACHED.equals(action)) {
					UsbAccessory accessory = UsbManager.getAccessory(intent);
					Log.d("USB","Attached!");
					if (intent.getBooleanExtra(
							UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
						//openAccessory(accessory);
						mAccessory = accessory;
			        	FileDescriptor fd = null;
			        	try{
			        		fd = UsbManager.getInstance(getApplicationContext()).openAccessory(accessory).getFileDescriptor();
			        	}catch(IllegalArgumentException e){
			        		finish();
			        	}catch(NullPointerException e){
			        		finish();
			        	}
			        	
			        	mFout = new FileOutputStream(fd);
			        	mFin = new FileInputStream(fd);
			        	mBtSend.setEnabled(true);
					} else {
						Log.d("USB", "permission denied for accessory "
								+ accessory);
					}
			} else if (UsbManager.ACTION_USB_ACCESSORY_DETACHED.equals(action)) {
				UsbAccessory accessory = UsbManager.getAccessory(intent);
				if (accessory != null && accessory.equals(mAccessory)) {
					if(mFout != null)
						try {
							mFout.close();
						} catch (IOException e) {
							e.printStackTrace();
						}
					if(mFin != null){
						try {
							mFin.close();
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
					mAccessory = null;
					mBtSend.setEnabled(false);
				}
			}else if("ch.serverbox.android.usbtest.USBPERMISSION".equals(action)){
				l("permission answered");
				if(intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)){
		        	UsbAccessory[] accessories = UsbManager.getInstance(getApplicationContext()).getAccessoryList();
		        	for(UsbAccessory a : accessories){
		        		l("accessory: "+a.getManufacturer());
		        		if(a.getManufacturer().equals("Nexus-Computing GmbH")){
		        			mAccessory = a;
		                	FileDescriptor fd = null;
		                	try{
		                		fd = UsbManager.getInstance(getApplicationContext()).openAccessory(a).getFileDescriptor();
		                	}catch(IllegalArgumentException e){
		                		finish();
		                	}catch(NullPointerException e){
		                		finish();
		                	}
		                	mFout = new FileOutputStream(fd);
		                	mFin = new FileInputStream(fd);
		        			l("added accessory");
		        			break;
		        		}
		        	}
				}
			}
		}
	};
	
	private void l(String l){
		Log.d("USB", l);
	}
}
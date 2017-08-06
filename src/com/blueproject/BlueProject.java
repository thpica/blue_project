package com.blueproject;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.widget.TextView;
import android.view.Window;
import android.view.WindowManager;
import android.view.Display;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.Button;
import android.widget.Toast;
import android.os.Vibrator;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import com.blueproject.WaveFormView;
import com.blueproject.DeviceListActivity;
import com.blueproject.BluetoothRfcommClient;



public class BlueProject extends Activity {
	// Message types sent from the BluetoothRfcommClient Handler
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;

    // Key names received from the BluetoothRfcommClient Handler
    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";

    // Intent request codes
    private static final int REQUEST_CONNECT_DEVICE = 1;
    private static final int REQUEST_ENABLE_BT = 2;
    
    
    static String[] timebase = {"5us", "10us", "20us", "50us", "100us", "200us", "500us", "1ms", "2ms", "5ms", "10ms", "20ms", "50ms" };
    static String[] ampscale = {"20mV", "50mV", "100mV", "200mV", "500mV", "1V","2V"};
    private SeekBar sbTimeDiv;
    private SeekBar sbVoltDiv;
    private TextView tvTimeDiv;
    private TextView tvVoltDiv;
    private Button btConnect;
    private Button btZero;
    public WaveFormView waveformview;
    public Vibrator vibrator;
    public Display display;
    public int screenHeight;
    public int screenWidth;
    
    // Local Bluetooth adapter
    private BluetoothAdapter mBluetoothAdapter = null;
    // Member object for the RFCOMM services
    private BluetoothRfcommClient mRfcommClient = null;
    
    private BluetoothDevice device = null;
    
    protected PowerManager.WakeLock mWakeLock;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	requestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, 
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.main);
        Display display = ((WindowManager) getSystemService(WINDOW_SERVICE)).getDefaultDisplay();
        waveformview =(WaveFormView) findViewById(R.id.waveformview);
        sbTimeDiv = (SeekBar)findViewById(R.id.sbtimediv);
        sbVoltDiv = (SeekBar)findViewById(R.id.sbvoltdiv);
        tvTimeDiv = (TextView)findViewById(R.id.tvtimediv);
        tvVoltDiv = (TextView)findViewById(R.id.tvvoltdiv);
        btConnect = (Button)findViewById(R.id.btconnect);
        btZero = (Button)findViewById(R.id.btzero);
        
     // Get local Bluetooth adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // If the adapter is null, then Bluetooth is not supported
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            finish();
            return;
        }        
        
        //interface init...
        tvTimeDiv.setText(timebase[sbTimeDiv.getProgress()]);
        tvVoltDiv.setText(ampscale[sbVoltDiv.getProgress()]);
        if(display.getOrientation()==1){
        	findViewById(R.id.rlparams).setVisibility(View.GONE);
        }
        screenHeight = display.getHeight();
        screenWidth = display.getWidth();
        
        mRfcommClient = new BluetoothRfcommClient(this, mHandler);
        
        //prevent sleeping
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        this.mWakeLock = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK, "My Tag"); 
        this.mWakeLock.acquire();
    }
    
    @Override
    public void onStart(){
    	super.onStart();
    	final Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
    	waveformview._athread.setRunning(true);
		waveformview._athread.start();
		if (screenHeight>screenWidth) waveformview._athread.buffer = new int[screenHeight];
		else waveformview._athread.buffer = new int[screenWidth];
		
		//enable bluetooth on start
		btActivate();
		
	    btConnect.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (mRfcommClient.getState()==3){
					btConnect.setText("Connect");
					btConnect.setCompoundDrawablesWithIntrinsicBounds(null, getResources().getDrawable(R.drawable.connect), null, null);
					btConnect.setTextAppearance(getApplicationContext(), R.style.Normal);
					waveformview._athread.setAcq(false);
					mRfcommClient.stop();
				}else{
					if (device == null){
						Intent serverIntent = new Intent(BlueProject.this,DeviceListActivity.class);
			            startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
					}else{
						mRfcommClient.connect(device);
						Toast.makeText(getApplicationContext(), "Reconnecting to "+ device.getName(), Toast.LENGTH_LONG).show();
						new WaitTillConnected().execute();
					}
		            btConnect.setText("Connecting...");
					btConnect.setCompoundDrawablesWithIntrinsicBounds(null, getResources().getDrawable(R.drawable.connecting), null, null);
					btConnect.setTextAppearance(getApplicationContext(), R.style.Italic);
					
				}
			}
			class WaitTillConnected extends AsyncTask<Void,Void,Void>{
				@Override
				protected Void doInBackground(Void... params) {
					while(mRfcommClient.getState()!=3 && mRfcommClient.getState()!=0);
					return null;
				}
				@Override
		    	protected void onPostExecute(Void result){
					if(mRfcommClient.getState()==3){
						btConnect.setText("Disconnect");
						btConnect.setCompoundDrawablesWithIntrinsicBounds(null, getResources().getDrawable(R.drawable.disconnect), null, null);
						btConnect.setTextAppearance(getApplicationContext(), R.style.Red);
						waveformview._athread.setAcq(true);
					}else if (mRfcommClient.getState()==0){
						btConnect.setText("Connect");
						btConnect.setCompoundDrawablesWithIntrinsicBounds(null, getResources().getDrawable(R.drawable.connect), null, null);
						btConnect.setTextAppearance(getApplicationContext(), R.style.Normal);
						waveformview._athread.setAcq(false);
					}
		    	}
		    }
		});
	    
	    btConnect.setOnLongClickListener(new View.OnLongClickListener() {
			
			@Override
			public boolean onLongClick(View v) {
				Intent serverIntent = new Intent(BlueProject.this,DeviceListActivity.class);
	            startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
	            vibrator.vibrate(200);
				return false;
			}
		});
	    
	    btZero.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				waveformview.position=0;
			}
		});
	    sbTimeDiv.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
			
	    	@Override
	 	   	public void onProgressChanged(SeekBar seekBar, int progress,
	 	   		boolean fromUser) {
		 	    tvTimeDiv.setText(timebase[progress]);
		 	    vibrator.vibrate(40);
	 	   }
	 
			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
			
			}
			
			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
			
			}
		});
	    sbVoltDiv.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
			
	    	@Override
	 	   public void onProgressChanged(SeekBar seekBar, int progress,
	 			boolean fromUser) {
	    		tvVoltDiv.setText(ampscale[progress]);
	    		vibrator.vibrate(40);
	    		switch (progress){
	    		case 6:
	    			waveformview.voltDivFactor=(float) 0.5;
	    			break;
	    		case 5:
	    			waveformview.voltDivFactor=(float) 1;
	    			break;
	    		case 4:
	    			waveformview.voltDivFactor=(float) 2;
	    			break;
	    		case 3:
	    			waveformview.voltDivFactor=(float) 5;
	    			break;
	    		case 2:
	    			waveformview.voltDivFactor=(float) 10;
	    			break;
	    		case 1:
	    			waveformview.voltDivFactor=(float) 20;
	    			break;
	    		case 0:
	    			waveformview.voltDivFactor=(float) 50;
	    			break;
	    		}
	 	   }
	 
			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
				
			}
			
			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
				
			}
		});
    }
    /*
    @Override
    public void onPause(){
    	super.onPause();
    	waveformview._athread.suspend();
    	waveformview._dthread.suspend();
    	mRfcommClient.disconnect();
    }
    */
    @Override
    public void onResume(){
    	super.onResume();
    	if (mRfcommClient != null) {
            // Only if the state is STATE_NONE, do we know that we haven't started already
            if (mRfcommClient.getState() == BluetoothRfcommClient.STATE_NONE) {
              // Start the Bluetooth  RFCOMM services
              mRfcommClient.start();
            }
        }
    	if (device!=null){
    		mRfcommClient.connect(device);
    	}
    }
    
    @Override
    public void onDestroy(){
    	super.onDestroy();
    	waveformview._athread.setRunning(false);
    	boolean retry=true;
		while (retry) {
            try {
                waveformview._athread.join();
                retry = false;
            } catch (InterruptedException e) {
                // we will try it again and again...
            }
		}
		if (mRfcommClient != null) mRfcommClient.stop();
		if (mWakeLock.isHeld()) mWakeLock.release();
	}
    
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        RelativeLayout rlParams = (RelativeLayout)findViewById(R.id.rlparams);
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
        	rlParams.setVisibility(View.GONE);
        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT){
        	rlParams.setVisibility(View.VISIBLE);
        }
    }

    public void btActivate(){
		if (!mBluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
		}
	}
 
    private final Handler mHandler = new Handler() {
    	
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case MESSAGE_STATE_CHANGE:
                switch (msg.arg1) {
                case BluetoothRfcommClient.STATE_CONNECTED:
                	
                    break;
                case BluetoothRfcommClient.STATE_CONNECTING:
                	
                    break;
                //case BluetoothRfcommClient.STATE_LISTEN:
                case BluetoothRfcommClient.STATE_NONE:

                    break;
                }
                break;
            case MESSAGE_WRITE:
                byte[] writeBuf = (byte[]) msg.obj;
                // construct a string from the buffer
                String writeMessage = new String(writeBuf);
                //mBTStatus.setText(writeMessage);
                break;
            case MESSAGE_READ:
            	int raw, data_length, x;
                waveformview.buffer = (int[]) msg.obj;
                data_length = msg.arg1;
                break;
            case MESSAGE_DEVICE_NAME:
                break;
            case MESSAGE_TOAST:
                Toast.makeText(getApplicationContext(), msg.getData().getString(TOAST),
                               Toast.LENGTH_SHORT).show();
                break;
            }
        }
    };

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
        case REQUEST_CONNECT_DEVICE:
            // When DeviceListActivity returns with a device to connect
            if (resultCode == Activity.RESULT_OK) {
                // Get the device MAC address
                String address = data.getExtras()
                                     .getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
                // Get the BLuetoothDevice object
               device = mBluetoothAdapter.getRemoteDevice(address);
                // Attempt to connect to the device
                mRfcommClient.connect(device);
                new WaitTillConnected().execute();
            }else{
            	btConnect.setText("Connect");
				btConnect.setCompoundDrawablesWithIntrinsicBounds(null, getResources().getDrawable(R.drawable.connect), null, null);
				btConnect.setTextAppearance(getApplicationContext(), R.style.Normal);
				waveformview._athread.setAcq(false);
            }
            break;
        case REQUEST_ENABLE_BT:
            // When the request to enable Bluetooth returns
            if (resultCode == Activity.RESULT_OK) {
                // Bluetooth is now enabled
            } else {
                // User did not enable Bluetooth or an error occured
                Toast.makeText(this, R.string.bt_not_enabled_leaving, Toast.LENGTH_SHORT).show();
                finish();
            }
        }
        
    }
    class WaitTillConnected extends AsyncTask<Void,Void,Void>{
		@Override
		protected Void doInBackground(Void... params) {
			while(mRfcommClient.getState()!=3 && mRfcommClient.getState()!=0);
			return null;
		}
		@Override
    	protected void onPostExecute(Void result){
			if(mRfcommClient.getState()==3){
				btConnect.setText("Disconnect");
				btConnect.setCompoundDrawablesWithIntrinsicBounds(null, getResources().getDrawable(R.drawable.disconnect), null, null);
				btConnect.setTextAppearance(getApplicationContext(), R.style.Red);
				waveformview._athread.setAcq(true);
			}else if (mRfcommClient.getState()==0){
				btConnect.setText("Connect");
				btConnect.setCompoundDrawablesWithIntrinsicBounds(null, getResources().getDrawable(R.drawable.connect), null, null);
				btConnect.setTextAppearance(getApplicationContext(), R.style.Normal);
				waveformview._athread.setAcq(false);
			}
    	}
    }
}
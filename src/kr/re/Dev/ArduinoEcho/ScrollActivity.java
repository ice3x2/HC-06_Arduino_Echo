package kr.re.Dev.ArduinoEcho;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.Set;

import kr.re.Dev.Bluetooth.BluetoothSerialClient;
import kr.re.Dev.Bluetooth.BluetoothSerialClient.BluetoothStreamingHandler;
import kr.re.Dev.Bluetooth.BluetoothSerialClient.OnBluetoothEnabledListener;
import kr.re.Dev.Bluetooth.BluetoothSerialClient.OnScanListener;
import kr.re.Dev.BluetoothEcho.R;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Html;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;


/**
 * Blutooth Arduino Echo.
 * 
 * 문자열의 끝은 '\0' 을 붙여서 구분한다.
 * 
 * www.dev.re.kr
 * @author ice3x2@gmail.com / Beom
 */
public class ScrollActivity extends Activity {
	
	private LinkedList<BluetoothDevice> mBluetoothDevices = new LinkedList<BluetoothDevice>();
	private ArrayAdapter<String> mDeviceArrayAdapter;
	private SeekBar mSeekBarController;
	private ProgressDialog mLoadingDialog;
	private AlertDialog mDeviceListDialog;
	private Menu mMenu;
	private BluetoothSerialClient mClient;
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		 super.onCreate(savedInstanceState);
		 setContentView(R.layout.activity_scroll);
		 mClient = BluetoothSerialClient.getInstance();
		 
		 if(mClient == null) {
			 Toast.makeText(getApplicationContext(), "Cannot use the Bluetooth device.", Toast.LENGTH_SHORT).show();
			 finish();
		 }
		 overflowMenuInActionBar();
		 initProgressDialog();
		 initDeviceListDialog();
		 initWidget();
		 
	}
	
	private void overflowMenuInActionBar(){
		 try {
		        ViewConfiguration config = ViewConfiguration.get(this);
		        Field menuKeyField = ViewConfiguration.class.getDeclaredField("sHasPermanentMenuKey");
		        if(menuKeyField != null) {
		            menuKeyField.setAccessible(true);
		            menuKeyField.setBoolean(config, false);
		        }
		    } catch (Exception ex) {
		        // 무시한다. 3.x 이 예외가 발생한다.
		    	// 또, 타블릿 전용으로 만들어진 3.x 버전의 디바이스는 보통 하드웨어 버튼이 존재하지 않는다. 
		    }
	}
	
	
	@Override
	protected void onPause() {
		mClient.cancelScan(getApplicationContext());
		super.onPause();
	}
	
	@Override
	protected void onResume() {
		super.onResume();
	enableBluetooth();

		
	}
	
	private void initProgressDialog() {
		 mLoadingDialog = new ProgressDialog(this);
		 mLoadingDialog.setCancelable(false);
	}
	
	private void initWidget() {
		mSeekBarController = (SeekBar) findViewById(R.id.seekBarControll);
		mSeekBarController.setMax(255);
		mSeekBarController.setProgress(0);
		mSeekBarController.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
				
			}
			
			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
				
				
			}
			
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
				((TextView)findViewById(R.id.textViewPower)).setText("Power : " + (int)(((float)progress / 255.0f) * 100.0f) + "%" );
				sendValue((byte)(progress));
			}
		});
		
	}
	
	private void initDeviceListDialog() {
		mDeviceArrayAdapter = new ArrayAdapter<String>(getApplicationContext(), R.layout.item_device);
		ListView listView = new ListView(getApplicationContext());
		listView.setAdapter(mDeviceArrayAdapter);
		listView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				String item =  (String) parent.getItemAtPosition(position); 
				for(BluetoothDevice device : mBluetoothDevices) {
					if(item.contains(device.getAddress())) {
						connect(device);
						mDeviceListDialog.cancel();
					}
				}
			}
		});
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("Select bluetooth device");
		builder.setView(listView);
		builder.setPositiveButton("Scan",
		 new DialogInterface.OnClickListener() {
		  public void onClick(DialogInterface dialog, int id) {
			  scanDevices();
		  }
		 });
		mDeviceListDialog = builder.create();
		mDeviceListDialog.setCanceledOnTouchOutside(false);
	}
	
	private void addDeviceToArrayAdapter(BluetoothDevice device) {
		if(mBluetoothDevices.contains(device)) { 
			mBluetoothDevices.remove(device);
			mDeviceArrayAdapter.remove(device.getName() + "\n" + device.getAddress());
		}
			mBluetoothDevices.add(device);
			mDeviceArrayAdapter.add(device.getName() + "\n" + device.getAddress() );
			mDeviceArrayAdapter.notifyDataSetChanged();
		
	}
	
	
	
	
	
	private void enableBluetooth() {
		BluetoothSerialClient btSet =  mClient;
		btSet.enableBluetooth(this, new OnBluetoothEnabledListener() {
			@Override
			public void onBluetoothEnabled(boolean success) {
				if(success) {
					getPairedDevices();
				} else {
					finish();
				}
			}
		});
	}
	

	
	
	private void getPairedDevices() {
		Set<BluetoothDevice> devices =  mClient.getPairedDevices();
		for(BluetoothDevice device: devices) {
			addDeviceToArrayAdapter(device);
		}
	}
	
	private void scanDevices() {
		BluetoothSerialClient btSet = mClient;
		btSet.scanDevices(getApplicationContext(), new OnScanListener() {
			String message ="";
			@Override
			public void onStart() {
				Log.d("Test", "Scan Start.");
				mLoadingDialog.show();
				message = "Scanning....";
				mLoadingDialog.setMessage("Scanning....");
				mLoadingDialog.setCancelable(true);
				mLoadingDialog.setCanceledOnTouchOutside(false);
				mLoadingDialog.setOnCancelListener(new OnCancelListener() {
					@Override
					public void onCancel(DialogInterface dialog) {
						BluetoothSerialClient btSet = mClient;
						btSet.cancelScan(getApplicationContext());
					}
				});
			}
			
			@Override
			public void onFoundDevice(BluetoothDevice bluetoothDevice) {
				addDeviceToArrayAdapter(bluetoothDevice);
				message += "\n" + bluetoothDevice.getName() + "\n" + bluetoothDevice.getAddress();
				mLoadingDialog.setMessage(message);
			}
			
			@Override
			public void onFinish() {
				Log.d("Test", "Scan finish.");
				message = "";
				mLoadingDialog.cancel();
				mLoadingDialog.setCancelable(false);
				mLoadingDialog.setOnCancelListener(null);
				mDeviceListDialog.show();
			}
		});
	}
	
	
	private void connect(BluetoothDevice device) {
		mLoadingDialog.setMessage("Connecting....");
		mLoadingDialog.setCancelable(false);
		mLoadingDialog.show();
		BluetoothSerialClient btSet =  mClient;
		btSet.connect(getApplicationContext(), device, mBTHandler);
	}
	
	private BluetoothStreamingHandler mBTHandler = new BluetoothStreamingHandler() {
		ByteBuffer mmByteBuffer = ByteBuffer.allocate(1024);
		
		@Override
		public void onError(Exception e) {
			mLoadingDialog.cancel();
			Toast.makeText(getApplicationContext(), "Messgae : Connection error -  +  e.toString()", Toast.LENGTH_LONG).show();;
			mMenu.getItem(0).setTitle(R.string.action_connect);
		}
		
		@Override
		public void onDisconnected() {
			mMenu.getItem(0).setTitle(R.string.action_connect);
			mLoadingDialog.cancel();
			Toast.makeText(getApplicationContext(), "Messgae : Disconnected.", Toast.LENGTH_LONG).show();;
		}
		@Override
		public void onData(byte[] buffer, int length) {

		}
		
		@Override
		public void onConnected() {
			Toast.makeText(getApplicationContext(),"Messgae : Connected. " + mClient.getConnectedDevice().getName() , Toast.LENGTH_LONG).show();;
			mLoadingDialog.cancel();
			mMenu.getItem(0).setTitle(R.string.action_disconnect);
		}
	};
	
	public void sendValue(byte value) {
		Log.i("sendValue", value + "");
		mBTHandler.write(new byte[]{value});
	}
	
	protected void onDestroy() {
		super.onDestroy();
		mClient.claer();
	};


	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		mMenu = menu;
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		boolean connect = mClient.isConnection();
		if(item.getItemId() == R.id.action_connect) {
			if (!connect) {
				mDeviceListDialog.show();
			} else {
				mBTHandler.close();
			}
			return true;
		} else {
			showCodeDlg();
			return true;
		}
	}

	private void showCodeDlg() {
		TextView codeView = new TextView(this);
		codeView.setText(Html.fromHtml(readCode()));
		codeView.setMovementMethod(new ScrollingMovementMethod());
		codeView.setBackgroundColor(Color.parseColor("#202020"));
		new AlertDialog.Builder(this, android.R.style.Theme_Holo_Light_DialogWhenLarge)
		.setView(codeView)
		.setPositiveButton("OK", new AlertDialog.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.cancel();
			}
		}).show();
	}
	
	private String readCode() {
		 try {
			InputStream is = getAssets().open("HC_06_Echo.txt");
			int length = is.available();
			byte[] buffer = new byte[length];
			is.read(buffer);
			is.close();
			String code = new String(buffer);
			buffer = null;
			return code;
		} catch (IOException e) {
			e.printStackTrace();
		}
		 return "";
	}
	

}

























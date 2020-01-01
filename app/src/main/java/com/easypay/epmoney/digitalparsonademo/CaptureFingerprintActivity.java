/* 
 * File: 		CaptureFingerprintActivity.java
 * Created:		2013/05/03
 * 
 * copyright (c) 2013 DigitalPersona Inc.
 */

package com.easypay.epmoney.digitalparsonademo;

import com.digitalpersona.uareu.Fid;
import com.digitalpersona.uareu.Quality;
import com.digitalpersona.uareu.Reader;
import com.digitalpersona.uareu.Reader.Priority;
import com.digitalpersona.uareu.UareUException;
import com.digitalpersona.uareu.jni.DpfjQuality;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.content.Context;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class CaptureFingerprintActivity extends Activity implements OnItemSelectedListener
{
	private Button m_back;
	private String m_deviceName = "";

	private Reader m_reader = null;
	private int m_DPI = 0;
	private Bitmap m_bitmap = null;
	private ImageView m_imgView;
	private TextView m_selectedDevice;
	private TextView m_title;
	private CheckBox m_spoof_enable;
	private boolean m_reset = false;
	private TextView m_text_conclusion;
	private String m_text_conclusionString;
	private Reader.CaptureResult cap_result = null;

	private Spinner m_spinner = null;
	private HashMap<String,Reader.ImageProcessing> m_imgProcMap = null;
	private boolean m_PADEnabled = false;
	private boolean bFirstTime = true;

	private void initializeActivity()
	{
		//getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		m_title = (TextView) findViewById(R.id.title);
		m_title.setText("Capture");
		m_selectedDevice = (TextView) findViewById(R.id.selected_device);
		m_deviceName = getIntent().getExtras().getString("device_name");

		m_selectedDevice.setText("Device: " + m_deviceName);

		m_bitmap = Globals.GetLastBitmap();
		if (m_bitmap == null) m_bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.black);
		m_imgView = (ImageView) findViewById(R.id.bitmap_image);
		m_imgView.setImageBitmap(m_bitmap);

		m_spoof_enable = (CheckBox) findViewById(R.id.checkBox);
		m_text_conclusion = (TextView) findViewById(R.id.text_conclusion);
		m_back = (Button) findViewById(R.id.back);

		m_back.setOnClickListener(new View.OnClickListener()
		{
			public void onClick(View v) 
			{
				onBackPressed ();
			}
		});

		m_spinner = (Spinner) findViewById(R.id.imgproc);
		m_spinner.setOnItemSelectedListener(this);

		m_imgProcMap = new HashMap<String,Reader.ImageProcessing>();
		m_imgProcMap.put("DEFAULT", Reader.ImageProcessing.IMG_PROC_DEFAULT);
		m_imgProcMap.put("PIV", Reader.ImageProcessing.IMG_PROC_PIV);
		m_imgProcMap.put("ENHANCED", Reader.ImageProcessing.IMG_PROC_ENHANCED);
		m_imgProcMap.put("ENHANCED_2", Reader.ImageProcessing.IMG_PROC_ENHANCED_2);
		
		Globals.DefaultImageProcessing = Reader.ImageProcessing.IMG_PROC_DEFAULT;
	}

	private void populateSpinner()
	{
		if (m_reader != null)
		{

            /*

                VID			PID			DEVICE			IMAGE PROCESSING
                ---			----		------			----------------
                0x05ba		0x000a		4500			DEFAULT
                0x05ba		0x000b		51XX,4500UID	DEFAULT, PIV, ENHANCED
                0x05ba		0x000c		5301			DEFAULT
                0x05ba		0x000d		5200			DEFAULT, PIV, ENHANCED, ENHANCED2
                0x05ba		0x000e		5300			DEFAULT, PIV, ENHANCED, ENHANCED2
                0x080b		0x010b		Drax30			DEFAULT, PIV, ENHANCED
                0x080b		0x0109		Pocket30		DEFAULT, PIV, ENHANCED
                0x05ba		0x7340		Pocket30-prot	DEFAULT, PIV, ENHANCED
             */

			List<String> options = new ArrayList<String>();

			int vid = m_reader.GetDescription().id.vendor_id;
            int pid = m_reader.GetDescription().id.product_id;

            if (vid == 0x05ba && pid == 0x000a)
			{
				// device: 4500
				options.add("DEFAULT");
			}
			else if (vid == 0x05ba && pid == 0x000b)
			{
				// device: 51XX,4500UID
				options.add("DEFAULT");
				options.add("PIV");
				options.add("ENHANCED");
			}
			else if (vid == 0x05ba && pid == 0x000c)
			{
				// device: 5301
				options.add("DEFAULT");
			}
			else if (vid == 0x05ba && pid == 0x000d)
			{
				// device: 5200
				options.add("DEFAULT");
				options.add("PIV");
				options.add("ENHANCED");
				options.add("ENHANCED_2");
			}
			else if (vid == 0x05ba && pid == 0x000e)
			{
				// device: 5300
				options.add("DEFAULT");
				options.add("PIV");
				options.add("ENHANCED");
				options.add("ENHANCED_2");
			}
			else if ((vid == 0x080b && (pid == 0x010b || pid == 0x0109)) || (vid == 0x05ba && pid == 0x7340))
			{
				// device: Drax30 or Pocket30
				options.add("DEFAULT");
				options.add("PIV");
				options.add("ENHANCED");
			}
			else
			{
				options.add("DEFAULT");
			}

			ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, options );
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_item);
			m_spinner.setAdapter(adapter);
		}
	}

	public void onItemSelected(AdapterView<?> parent, View view,
							   int pos, long id) {
		// An item was selected. You can retrieve the selected item using
		// parent.getItemAtPosition(pos)

		// get the current selection from the spinner
		String selection = (String) parent.getItemAtPosition(pos);

		// lookup the image processing mode and set
		Globals.DefaultImageProcessing = m_imgProcMap.get(selection);

		try
		{
			// abort the current capture (if any)
			if(!bFirstTime){
				m_reader.CancelCapture();
			}
			bFirstTime = false;
		}
		catch (UareUException ex)
		{
			// ignore all exceptions
		}

		// show a Toast to notify the user of the switch
		Toast.makeText(parent.getContext(),
				"OnItemSelectedListener : " + parent.getItemAtPosition(pos).toString(),
				Toast.LENGTH_SHORT).show();
	}

	public void onNothingSelected(AdapterView<?> parent) {
		// Another interface callback
	}

	private void displayDialog(String msg, UareUException e)
	{
		String str = String.format("%s \nReturned DP error %d \n%s", msg, (e.getCode() & 0xffff), e.toString());
		AlertDialog dialog = new AlertDialog.Builder(this)
							.setTitle("Error")
							.setMessage(str)
							.setPositiveButton("OK", null)
							.create();
		if(!isFinishing()) dialog.show();
	}

	private void checkPADEnable() {
		try {
			if (m_reader == null) {
				return;
			}
				m_spoof_enable.setVisibility(View.VISIBLE);
				m_spoof_enable.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						byte[] params = new byte[1];
						boolean checked = m_spoof_enable.isChecked();
						try {
							if (checked) {
								if (!m_PADEnabled) {
									params[0] = (byte) 1;
									m_reader.SetParameter(Reader.ParamId.DPFPDD_PARMID_PAD_ENABLE, params);
									m_PADEnabled = true;
								}
							} else {
								params[0] = (byte) 0;
								m_reader.SetParameter(Reader.ParamId.DPFPDD_PARMID_PAD_ENABLE, params);
								m_PADEnabled = false;
							}
						} catch (UareUException e) {
							Log.w("UareUSampleJava", "error during SetParameter: " + e.toString());
							displayDialog("Set PAD parameter fail!", e);
							m_spoof_enable.setChecked(m_PADEnabled = false); //Exception: uncheck PAD enable
						}
					}
				});
				m_spoof_enable.setChecked(m_PADEnabled); //General: check/uncheck PAD enable
			//}
		} catch (Exception e) {
			if (!m_reset) {
				Log.w("UareUSampleJava", "error during capture: " + e.toString());
				m_deviceName = "";
				onBackPressed();
			}
		}
	}

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_capture_stream);

		// ...
		initializeActivity();

		// initialize dp sdk
		try 
		{
			Context applContext = getApplicationContext();
			m_reader = Globals.getInstance().getReader(m_deviceName, applContext);
			m_reader.Open(Priority.EXCLUSIVE);
			m_DPI = Globals.GetFirstDPI(m_reader);

			byte[] result = m_reader.GetParameter(Reader.ParamId.DPFPDD_PARMID_PAD_ENABLE);
			//Log.i("-->app GetParameter", "PAD_buffer[0]=" + String.format("%s", result[0]));
			if (m_spoof_enable.isChecked() && !m_PADEnabled) {
				byte[] params = {1};
				m_reader.SetParameter(Reader.ParamId.DPFPDD_PARMID_PAD_ENABLE, params);
				m_PADEnabled = true;
			}

		} catch (Exception e) {
			Log.w("UareUSampleJava", "error during init of reader");
			m_deviceName = "";
			onBackPressed();
			return;
		}

		// Check PAD enable button
		checkPADEnable();
		// populate the spinner widget
		populateSpinner();

		// loop capture on a separate thread to avoid freezing the UI
		new Thread(new Runnable()
		{
			@Override
			public void run()
			{
				try 
				{
					m_reset = false;
					while (!m_reset)
					{
						// capture the image (synchronous)
						if (true)
						{
							cap_result = m_reader.Capture(Fid.Format.ANSI_381_2004, Globals.DefaultImageProcessing, m_DPI, -1);
						}

						// capture the image (asynchronous)
						if (false)
						{
							final Object captureComplete = new Object();

							m_reader.CaptureAsync(Fid.Format.ANSI_381_2004, Globals.DefaultImageProcessing, m_DPI, -1, new Reader.CaptureCallback() {
								public void CaptureResultEvent(Reader.CaptureResult result) {
									synchronized (captureComplete) {
										cap_result = result;
										captureComplete.notify();
									}
								}
							});

							// note: may need to place a time limit on the wait
							synchronized(captureComplete) {
								captureComplete.wait();
							}
						}


						// an error occurred
						if (cap_result == null) continue;
						
						if(cap_result.image != null){
							// save bitmap image locally
							m_bitmap = Globals.GetBitmapFromRaw(cap_result.image.getViews()[0].getImageData(), cap_result.image.getViews()[0].getWidth(), cap_result.image.getViews()[0].getHeight());

							// calculate nfiq score
							DpfjQuality quality = new DpfjQuality();
							int nfiqScore = quality.nfiq_raw(
									cap_result.image.getViews()[0].getImageData(),	// raw image data
									cap_result.image.getViews()[0].getWidth(),		// image width
									cap_result.image.getViews()[0].getHeight(),		// image height
									m_DPI,											// device DPI
									cap_result.image.getBpp(),						// image bpp
									Quality.QualityAlgorithm.QUALITY_NFIQ_NIST		// qual. algo.
							);

							// log NFIQ score
							Log.i("UareUSampleJava", "capture result nfiq score: " + nfiqScore);

							// update ui string
							m_text_conclusionString = Globals.QualityToString(cap_result);
							m_text_conclusionString += " (NFIQ score: " + nfiqScore + ")";
						}
						else{
							m_bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.black);
							// update ui string
							m_text_conclusionString = Globals.QualityToString(cap_result);
						}

						runOnUiThread(new Runnable()
						{
						    @Override public void run() 
						    {
						    	UpdateGUI();
						    }
						});
					}
				}
				catch (Exception e)
				{	
					if(!m_reset)
					{
						Log.w("UareUSampleJava", "error during capture: " + e.toString());
						m_deviceName = "";
						onBackPressed();
					}
				}
			}
		}).start();
	}

	public void UpdateGUI()
	{
		m_imgView.setImageBitmap(m_bitmap);
		m_imgView.invalidate();
		m_text_conclusion.setText(m_text_conclusionString);
	}
	
	@Override
	public void onBackPressed()
	{
		try 
		{
			m_reset = true;
			try {m_reader.CancelCapture(); } catch (Exception e) {}
			m_reader.Close();

		}
		catch (Exception e)
		{
			Log.w("UareUSampleJava", "error during reader shutdown");
		}

		Intent i = new Intent();
		i.putExtra("device_name", m_deviceName);
		setResult(Activity.RESULT_OK, i);
		finish();
	}

	// called when orientation has changed to manually destroy and recreate activity
	@Override 
	public void onConfigurationChanged(Configuration newConfig)
	{
		super.onConfigurationChanged(newConfig);
		setContentView(R.layout.activity_capture_stream);
		initializeActivity();
		// Check PAD enbale button
		checkPADEnable();
		// populate the spinner widget
		populateSpinner();
	}
}

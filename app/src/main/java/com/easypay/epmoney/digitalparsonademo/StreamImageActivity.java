/* 
 * File: 		StreamImageActivity.java
 * Created:		2013/05/03
 * 
 * copyright (c) 2013 DigitalPersona Inc.
 */

package com.easypay.epmoney.digitalparsonademo;

import com.digitalpersona.uareu.Fid;
import com.digitalpersona.uareu.Reader;
import com.digitalpersona.uareu.Reader.CaptureResult;
import com.digitalpersona.uareu.Reader.Priority;
import com.digitalpersona.uareu.UareUException;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import android.content.Context;
import android.widget.Spinner;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class StreamImageActivity extends Activity implements OnItemSelectedListener
{
	//private static final int PAD_SUPPORTED = 1;
	private Button m_back;
	private String m_deviceName = "";

	private Reader m_reader = null;
	private int m_DPI = 0;
	private Bitmap m_bitmap = null;
	private ImageView m_imgView;
	private TextView m_selectedDevice;
	private TextView m_title;
	private TextView m_text_conclusion;

	private CheckBox m_spoof_enable;
	private boolean m_reset = false;
	private CountDownTimer m_timer = null;

	private Spinner m_spinner = null;
	private HashMap<String,Reader.ImageProcessing> m_imgProcMap = null;
	//private boolean m_supportPAD = false;
	private boolean m_PADEnabled = false;

	private void initializeActivity()
	{
		m_title = (TextView) findViewById(R.id.title);
		m_title.setText("Stream Image");
		m_selectedDevice = (TextView) findViewById(R.id.selected_device);
		m_deviceName = getIntent().getExtras().getString("device_name");

		m_selectedDevice.setText("Device: " + m_deviceName);

		m_bitmap = Globals.GetLastBitmap();
		if (m_bitmap == null) BitmapFactory.decodeResource(getResources(), R.drawable.black);
		m_imgView = (ImageView) findViewById(R.id.bitmap_image);
		m_imgView.setImageBitmap(m_bitmap);

		m_spoof_enable= (CheckBox) findViewById(R.id.checkBox);
		m_spoof_enable.setVisibility(View.GONE);
		m_text_conclusion = (TextView) findViewById(R.id.text_conclusion);
		m_text_conclusion.setVisibility(View.GONE);

		m_back = (Button) findViewById(R.id.back);
		m_back.setOnClickListener(new View.OnClickListener()
		{
			public void onClick(View v)
			{
				onBackPressed(); 
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

		// show a Toast to notify the user of the switch
		Toast.makeText(parent.getContext(),
				"OnItemSelectedListener : " + parent.getItemAtPosition(pos).toString(),
				Toast.LENGTH_SHORT).show();
	}

	public void onNothingSelected(AdapterView<?> parent) {
		// Another interface callback
	}

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_capture_stream);

		initializeActivity();

		// initiliaze dp sdk
		try 
		{
			Context applContext = getApplicationContext();
			m_reader = Globals.getInstance().getReader(m_deviceName, applContext);
			m_reader.Open(Priority.EXCLUSIVE);
			m_DPI = Globals.GetFirstDPI(m_reader);
			m_reader.StartStreaming();
		} 
		catch (Exception e) 
		{
			Log.w("UareUSampleJava", "error during capture");
			m_deviceName = "";
			onBackPressed();
		}

		// populate the spinner widget
		populateSpinner();
		
		m_reset = false;

		// updates UI continuously
		m_timer = new CountDownTimer(25, 25)
		{
			public void onTick(long millisUntilFinished) {}
			public void onFinish()
			{
				try 
				{
					if (!m_reset)
					{
						CaptureResult res = m_reader.GetStreamImage(Fid.Format.ANSI_381_2004, Globals.DefaultImageProcessing, m_DPI);
						if (res != null && res.image != null)
						{
							m_bitmap = Globals.GetBitmapFromRaw(res.image.getViews()[0].getImageData(), res.image.getViews()[0].getWidth(), res.image.getViews()[0].getHeight());
							m_imgView.setImageBitmap(m_bitmap);
							m_imgView.invalidate();
							m_timer.start();
							return;
						}
					}
				}
				catch (Exception e)
				{
					if(!m_reset)
					{
						Log.w("UareUSampleJava", "error during streaming");
						m_deviceName = "";
						onBackPressed();
					}
					return;
				}
			}
		}.start();
	}

	@Override
	public void onBackPressed()
	{
		try
		{
			m_reset = true;
			if (m_reader != null)
			{
				try { m_reader.StopStreaming(); } catch (Exception e) {}
				m_reader.Close();
			}
		}
		catch (UareUException e)
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
		// populate the spinner widget
		populateSpinner();
	}
}

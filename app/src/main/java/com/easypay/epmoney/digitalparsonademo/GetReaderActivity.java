/* 
 * File: 		GetReaderActivity.java
 * Created:		2013/05/03
 * 
 * copyright (c) 2013 DigitalPersona Inc.
 */

package com.easypay.epmoney.digitalparsonademo;

import com.digitalpersona.uareu.Reader;
import com.digitalpersona.uareu.ReaderCollection;
import com.digitalpersona.uareu.UareUException;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

import android.content.Context;

public class GetReaderActivity extends Activity 
{
	private Button m_back;
	private String m_deviceName = "";

	private ListView m_readers;
	private ReaderCollection readers;
	private Bundle savedInstanceState = null;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_get_list);

		m_back = (Button) findViewById(R.id.back);
		m_deviceName = getIntent().getExtras().getString("device_name");
		m_back.setOnClickListener(new View.OnClickListener() 
		{
			public void onClick(View v) 
			{
				onBackPressed();
			}
		});

		// initialize dp sdk
		try
		{
			Context applContext = getApplicationContext();
			readers = Globals.getInstance().getReaders(applContext);
		} catch (UareUException e) 
		{
			onBackPressed();
		}

		int nSize = readers.size();
		if (nSize > 1)
		{
			String[] values = null;
			values = new String[nSize];
			for (int nCount = 0; nCount < nSize; nCount++)
			{
				values[nCount] = readers.get(nCount).GetDescription().name;
			}

			ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1, android.R.id.text1, values);
			m_readers = (ListView) findViewById(R.id.list);
			m_readers.setAdapter(adapter);
			m_readers.setOnItemClickListener(new OnItemClickListener() 
			{
				public void onItemClick(AdapterView<?> parent, View view,int position, long id)
				{
					Intent i = new Intent();
					i.putExtra("device_name", readers.get(position).GetDescription().name);
					setResult(Activity.RESULT_OK, i);

					InitDevice(position);

					finish();
				}
			});
		}
		else
		{
			Intent i = new Intent();
			i.putExtra("device_name", (nSize == 0 ? "" : readers.get(0).GetDescription().name));

			if (getIntent() != null &&
				getIntent().getAction() != null &&
				getIntent().getAction().equals(UsbManager.ACTION_USB_DEVICE_ATTACHED))
			{
				InitDevice(0);
			}
			setResult(Activity.RESULT_OK, i);
			finish();
		}
	}

	private void InitDevice(int position) {
		try
        {
            readers.get(position).Open(Reader.Priority.COOPERATIVE);
            readers.get(position).Close();
        }

        catch (Exception ex)
        {
            ex.printStackTrace();
        }
	}

	@Override
	public void onBackPressed()
	{
		Intent i = new Intent();
		i.putExtra("device_name", m_deviceName);
		setResult(Activity.RESULT_OK, i);
		finish();
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig)
	{
		onCreate(savedInstanceState);
		super.onConfigurationChanged(newConfig);
	}
}

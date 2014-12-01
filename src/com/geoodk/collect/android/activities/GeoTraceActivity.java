/*
 * Copyright (C) 2014 GeoODK
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

/**
 * Responsible for creating polygons/polyline with GPS Tracking
 *
 * @author Jon Nordling (jonnordling@gmail.com)
 */

package com.geoodk.collect.android.activities;

import org.osmdroid.DefaultResourceProxyImpl;
import org.osmdroid.ResourceProxy;
import org.osmdroid.tileprovider.tilesource.ITileSource;
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.tileprovider.tilesource.XYTileSource;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import com.geoodk.collect.android.R;
import com.geoodk.collect.android.preferences.MapSettings;
import com.geoodk.collect.android.spatial.MapHelper;


import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Toast;


public class GeoTraceActivity extends Activity {
	public int zoom_level = 3;
	public Boolean gpsStatus = true;
	private Boolean play_check = false;
	private MapView mapView;
	private SharedPreferences sharedPreferences;
	private DefaultResourceProxyImpl resource_proxy;
	private ITileSource baseTiles;
	public MyLocationNewOverlay mMyLocationOverlay;
	private ImageButton play_button;
	private ProgressDialog progress;
	private AlertDialog.Builder builder;
	private LayoutInflater inflater;
	private AlertDialog alert;
	private View traceSettingsView;
	
	
	@Override
	protected void onStart() {
		// TODO Auto-generated method stub
		super.onStart();
	}

	@Override
	protected void onRestart() {
		// TODO Auto-generated method stub
		super.onRestart();
	}

	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		setGPSStatus();
		super.onResume();
	}

	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
	}

	@Override
	protected void onStop() {
		// TODO Auto-generated method stub
		super.onStop();
	}

	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
	}


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.geotrace_layout);
		setTitle("GeoTrace"); // Setting title of the action
		
		sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
		Boolean online = sharedPreferences.getBoolean(MapSettings.KEY_online_offlinePrefernce, true);
		String basemap = sharedPreferences.getString(MapSettings.KEY_map_basemap, "MAPQUESTOSM");
		
		baseTiles = MapHelper.getTileSource(basemap);
		
		resource_proxy = new DefaultResourceProxyImpl(getApplicationContext());
		mapView = (MapView)findViewById(R.id.geotrace_mapview);
		mapView.setTileSource(baseTiles);
		mapView.setMultiTouchControls(true);
		mapView.setBuiltInZoomControls(true);
		mapView.setUseDataConnection(online);
		mapView.getController().setZoom(zoom_level);
		
		mMyLocationOverlay = new MyLocationNewOverlay(this, mapView);
        mMyLocationOverlay.runOnFirstFix(centerAroundFix);
        
        progress = new ProgressDialog(this);
        progress.setTitle("Loading Location");
        progress.setMessage("Wait while loading...");
        progress.show();
        // To dismiss the dialog
        
        
        play_button = (ImageButton)findViewById(R.id.geotrace_play_button);
        //This is the gps button and its functionality
        play_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
            	//setGPSStatus();
            	if (play_check==false){
            		play_button.setImageResource(R.drawable.stop_button);
            		alert.show();
            		play_check=true;
            	}else{
            		play_button.setImageResource(R.drawable.play_button);
            		play_check=false;
            	}
            }
        });
        
        inflater = this.getLayoutInflater();
        traceSettingsView = inflater.inflate(R.layout.geotrace_dialog, null);
        buildDialog();
        setGPSStatus();

		mapView.invalidate();
	}
	private void setGPSStatus(){
        if(gpsStatus ==false){
            //gps_button.setImageResource(R.drawable.ic_menu_mylocation_blue);
            upMyLocationOverlayLayers();
            //enableMyLocation();
            //zoomToMyLocation();
            gpsStatus = true;
        }else{
            //gps_button.setImageResource(R.drawable.ic_menu_mylocation);
            disableMyLocation();
            gpsStatus = false;
        }
    }
	
    private void disableMyLocation(){
    	LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
    	if (locationManager.isProviderEnabled(locationManager.GPS_PROVIDER)){
        	mMyLocationOverlay.setEnabled(false);
        	mMyLocationOverlay.disableFollowLocation();
        	mMyLocationOverlay.disableMyLocation();
        	gpsStatus =false;
    	}
    }
  private void upMyLocationOverlayLayers(){
    	LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
    	if (locationManager.isProviderEnabled(locationManager.GPS_PROVIDER)){
    		overlayMyLocationLayers();
    		//zoomToMyLocation();
    	}else{
    		showGPSDisabledAlertToUser();
    	}

    }
	
    private void overlayMyLocationLayers(){
        mapView.getOverlays().add(mMyLocationOverlay);
        mMyLocationOverlay.setEnabled(true);
        mMyLocationOverlay.enableMyLocation();
        mMyLocationOverlay.enableFollowLocation();
    }
    private Handler mHandler = new Handler(Looper.getMainLooper());

    private Runnable centerAroundFix = new Runnable() {
        public void run() {
            mHandler.post(new Runnable() {
                public void run() {
                    zoomToMyLocation();
                    progress.dismiss();
                }
            });
        }
    };
    
    private void zoomToMyLocation(){
    	if (mMyLocationOverlay.getMyLocation()!= null){
    		if (zoom_level ==3){
    			mapView.getController().setZoom(15);
    		}else{
    			mapView.getController().setZoom(zoom_level);
    		}
    		mapView.getController().setCenter(mMyLocationOverlay.getMyLocation());
    		//mapView.getController().animateTo(mMyLocationOverlay.getMyLocation());
    	}else{
    		mapView.getController().setZoom(zoom_level);
    	}
    	
    }
    
    private void showGPSDisabledAlertToUser(){
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setMessage("GPS is disabled in your device. Would you like to enable it?")
        .setCancelable(false)
        .setPositiveButton("Enable GPS",
                new DialogInterface.OnClickListener(){
            public void onClick(DialogInterface dialog, int id){
               // Intent callGPSSettingIntent = new Intent(
                startActivityForResult(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS), 0);
                //startActivity(callGPSSettingIntent);
            }
        });
        alertDialogBuilder.setNegativeButton("Cancel",
                new DialogInterface.OnClickListener(){
            public void onClick(DialogInterface dialog, int id){
                dialog.cancel();
            }
        });
        AlertDialog alert = alertDialogBuilder.create();
        alert.show();
    }
    
    public void setGeoTraceMode(View view){
    	//Toast.makeText(this, " ", Toast.LENGTH_LONG).show();
    	boolean checked = ((RadioButton) view).isChecked();
    	EditText time_number = (EditText) traceSettingsView.findViewById(R.id.trace_number);
    	Spinner time_units = (Spinner) traceSettingsView.findViewById(R.id.planets_spinner);
    	switch(view.getId()) {
	        case R.id.radio_pirates:
	            if (checked){
	            	time_number.setText("");
	            	time_number.setVisibility(View.GONE);
	            	time_units.setVisibility(View.GONE);
	            	
	            	time_number.invalidate();
	            	time_units.invalidate();
	            }
	                // Pirates are the best
	            	
	            break;
	        case R.id.radio_ninjas:
	            if (checked){	          
	            	time_number.setVisibility(View.VISIBLE);
	            	time_units.setVisibility(View.VISIBLE);
	            	
	            	time_number.invalidate();
	            	time_units.invalidate();
	            }
	            break;
    	}

    	//builder(RadioGroup)findViewById(R.id.radio_group);
    	//RadioGroup rg = (RadioGroup) findViewById(R.id.radio_group);
    	//int checked = rg.getCheckedRadioButtonId();
    }
    
    
    private void buildDialog(){
    	builder = new AlertDialog.Builder(this);
    	
    	builder.setTitle("Configure GeoTrace Settings");
    	//builder.setMessage("Configure GeoTrace Settings");
    	builder.setView(traceSettingsView)
        // Add action buttons
               .setPositiveButton("Start", new DialogInterface.OnClickListener() {
                   @Override
                   public void onClick(DialogInterface dialog, int id) {
                	   RadioGroup rg = (RadioGroup) findViewById(R.id.radio_group);
                	   	
                       // sign in the user ...
                   }
               })
               .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                   public void onClick(DialogInterface dialog, int id) {
                       dialog.cancel();
                   }
               });    
    	
    	alert = builder.create();
        //alert.show();
       
    }

    
    

	
}

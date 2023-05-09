package com.flutterbeacon;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Build;
import android.provider.Settings;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.altbeacon.beacon.BeaconTransmitter;

import java.lang.ref.WeakReference;

class FlutterPlatform {
    private final WeakReference<Activity> activityWeakReference;

    FlutterPlatform(Activity activity) {
        activityWeakReference = new WeakReference<>(activity);
    }

    private Activity getActivity() {
        return activityWeakReference.get();
    }

    void openLocationSettings() {
        Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        getActivity().startActivity(intent);
    }

    @SuppressLint("MissingPermission")
    void openBluetoothSettings() {
        Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        getActivity().startActivityForResult(intent, FlutterBeaconPlugin.REQUEST_CODE_BLUETOOTH);
    }

    void requestAuthorization() {
        if (Build.VERSION.SDK_INT >= 31) {
            ActivityCompat.requestPermissions(getActivity(), new String[]{
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT,
                    /*
                                        TODO we should request background location access somewhere
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION,*/
                    Manifest.permission.ACCESS_FINE_LOCATION
            }, FlutterBeaconPlugin.REQUEST_CODE_PERMISSIONS);
        } else {
            ActivityCompat.requestPermissions(getActivity(), new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION
            }, FlutterBeaconPlugin.REQUEST_CODE_PERMISSIONS);
        }
    }

    boolean checkLocationServicesPermission() {
        if (Build.VERSION.SDK_INT >= 31) {
            return ContextCompat.checkSelfPermission(getActivity(),
                    Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED
                    && ContextCompat.checkSelfPermission(getActivity(),
                    Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
                    && ContextCompat.checkSelfPermission(getActivity(),
                    Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                    /*
                    TODO we should request background location access somewhere
                    && ContextCompat.checkSelfPermission(getActivity(),
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED*/;
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return ContextCompat.checkSelfPermission(getActivity(),
                    Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        }

        return true;
    }

    boolean checkLocationServicesIfEnabled() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            LocationManager locationManager = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);
            return locationManager != null && locationManager.isLocationEnabled();
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            int mode = Settings.Secure.getInt(getActivity().getContentResolver(), Settings.Secure.LOCATION_MODE,
                    Settings.Secure.LOCATION_MODE_OFF);
            return (mode != Settings.Secure.LOCATION_MODE_OFF);
        }

        return true;
    }

    @SuppressLint("MissingPermission")
    boolean checkBluetoothIfEnabled() {
        BluetoothManager bluetoothManager = (BluetoothManager)
                getActivity().getSystemService(Context.BLUETOOTH_SERVICE);
        if (bluetoothManager == null) {
            throw new RuntimeException("No bluetooth service");
        }

        BluetoothAdapter adapter = bluetoothManager.getAdapter();

        return (adapter != null) && (adapter.isEnabled());
    }

    boolean isBroadcastSupported() {
        return BeaconTransmitter.checkTransmissionSupported(getActivity()) == 0;
    }

    boolean shouldShowRequestPermissionRationale(String permission) {
        return ActivityCompat.shouldShowRequestPermissionRationale(getActivity(), permission);
    }
}

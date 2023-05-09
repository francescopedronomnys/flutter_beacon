package com.flutterbeacon;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.MonitorNotifier;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.flutter.plugin.common.EventChannel;

class FlutterBeaconScanner {
    private static final String TAG = FlutterBeaconScanner.class.getSimpleName();
    private final FlutterBeaconPlugin plugin;

    private Handler handler;

    private EventChannel.EventSink eventSinkRanging;
    private EventChannel.EventSink eventSinkMonitoring;
    private List<Region> regionRanging;
    private List<Region> regionMonitoring;

    public FlutterBeaconScanner(FlutterBeaconPlugin plugin, Activity activity) {
        this.plugin = plugin;
        handler = new Handler(Looper.getMainLooper());
    }

    final EventChannel.StreamHandler rangingStreamHandler = new EventChannel.StreamHandler() {
        @Override
        public void onListen(Object o, EventChannel.EventSink eventSink) {
            Log.d("RANGING", "Start ranging = " + o);
            startRanging(o, eventSink);
        }

        @Override
        public void onCancel(Object o) {
            Log.d("RANGING", "Stop ranging = " + o);
            stopRanging();
        }
    };

    @SuppressWarnings("rawtypes")
    private void startRanging(Object o, EventChannel.EventSink eventSink) {
        if (o instanceof List) {
            List list = (List) o;
            if (regionRanging == null) {
                regionRanging = new ArrayList<>();
            } else {
                regionRanging.clear();
            }
            for (Object object : list) {
                if (object instanceof Map) {
                    Map map = (Map) object;
                    Region region = FlutterBeaconUtils.regionFromMap(map);
                    if (region != null) {
                        regionRanging.add(region);
                    }
                }
            }
        } else {
            eventSink.error("Beacon", "invalid region for ranging", null);
            return;
        }
        eventSinkRanging = eventSink;
        startRanging();
    }

    void startRanging() {
        if (regionRanging == null || regionRanging.isEmpty()) {
            Log.e("RANGING", "Region ranging is null or empty. Ranging not started.");
            return;
        }

        if (plugin.getBeaconManager() != null) {
            plugin.getBeaconManager().removeAllRangeNotifiers();
            plugin.getBeaconManager().addRangeNotifier(rangeNotifier);
            for (Region region : regionRanging) {
                plugin.getBeaconManager().startRangingBeacons(region);
            }
        }
    }

    void stopRanging() {
        if (regionRanging != null && !regionRanging.isEmpty()) {
            for (Region region : regionRanging) {
                plugin.getBeaconManager().stopRangingBeacons(region);
            }

            plugin.getBeaconManager().removeRangeNotifier(rangeNotifier);
        }
        eventSinkRanging = null;
    }

    private final RangeNotifier rangeNotifier = new RangeNotifier() {
        @Override
        public void didRangeBeaconsInRegion(Collection<Beacon> collection, Region region) {
            if (eventSinkRanging != null) {
                final Map<String, Object> map = new HashMap<>();
                map.put("region", FlutterBeaconUtils.regionToMap(region));
                map.put("beacons", FlutterBeaconUtils.beaconsToArray(new ArrayList<>(collection)));
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (eventSinkRanging != null) {
                            eventSinkRanging.success(map);
                        }
                    }
                });
            }
        }
    };

    final EventChannel.StreamHandler monitoringStreamHandler = new EventChannel.StreamHandler() {
        @Override
        public void onListen(Object o, EventChannel.EventSink eventSink) {
            startMonitoring(o, eventSink);
        }

        @Override
        public void onCancel(Object o) {
            stopMonitoring();
        }
    };

    @SuppressWarnings("rawtypes")
    private void startMonitoring(Object o, EventChannel.EventSink eventSink) {
        Log.d(TAG, "START MONITORING=" + o);
        if (o instanceof List) {
            List list = (List) o;
            if (regionMonitoring == null) {
                regionMonitoring = new ArrayList<>();
            } else {
                regionMonitoring.clear();
            }
            for (Object object : list) {
                if (object instanceof Map) {
                    Map map = (Map) object;
                    Region region = FlutterBeaconUtils.regionFromMap(map);
                    regionMonitoring.add(region);
                }
            }
        } else {
            eventSink.error("Beacon", "invalid region for monitoring", null);
            return;
        }
        eventSinkMonitoring = eventSink;
        startMonitoring();
    }

    void startMonitoring() {
        if (regionMonitoring == null || regionMonitoring.isEmpty()) {
            Log.e("MONITORING", "Region monitoring is null or empty. Monitoring not started.");
            return;
        }

        plugin.getBeaconManager().removeAllMonitorNotifiers();
        plugin.getBeaconManager().addMonitorNotifier(monitorNotifier);
        for (Region region : regionMonitoring) {
            plugin.getBeaconManager().startMonitoring(region);
        }
    }

    void stopMonitoring() {
        if (regionMonitoring != null && !regionMonitoring.isEmpty()) {
            for (Region region : regionMonitoring) {
                plugin.getBeaconManager().stopMonitoring(region);
            }
            plugin.getBeaconManager().removeMonitorNotifier(monitorNotifier);
        }
        eventSinkMonitoring = null;
    }

    private final MonitorNotifier monitorNotifier = new MonitorNotifier() {
        @Override
        public void didEnterRegion(Region region) {
            if (eventSinkMonitoring != null) {
                final Map<String, Object> map = new HashMap<>();
                map.put("event", "didEnterRegion");
                map.put("region", FlutterBeaconUtils.regionToMap(region));
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (eventSinkMonitoring != null) {
                            eventSinkMonitoring.success(map);
                        }
                    }
                });
            }
        }

        @Override
        public void didExitRegion(Region region) {
            if (eventSinkMonitoring != null) {
                final Map<String, Object> map = new HashMap<>();
                map.put("event", "didExitRegion");
                map.put("region", FlutterBeaconUtils.regionToMap(region));
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (eventSinkMonitoring != null) {
                            eventSinkMonitoring.success(map);
                        }
                    }
                });
            }
        }

        @Override
        public void didDetermineStateForRegion(int state, Region region) {
            if (eventSinkMonitoring != null) {
                final Map<String, Object> map = new HashMap<>();
                map.put("event", "didDetermineStateForRegion");
                map.put("state", FlutterBeaconUtils.parseState(state));
                map.put("region", FlutterBeaconUtils.regionToMap(region));
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (eventSinkMonitoring != null) {
                            eventSinkMonitoring.success(map);
                        }
                    }
                });
            }
        }
    };
}

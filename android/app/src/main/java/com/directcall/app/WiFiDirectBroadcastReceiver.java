package com.directcall.app;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class WiFiDirectBroadcastReceiver extends BroadcastReceiver {

    private WifiP2pManager manager;
    private WifiP2pManager.Channel channel;
    private MainActivity activity;

    public WiFiDirectBroadcastReceiver(WifiP2pManager manager, WifiP2pManager.Channel channel, MainActivity activity) {
        super();
        this.manager = manager;
        this.channel = channel;
        this.activity = activity;
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {
            // Check to see if Wi-Fi is enabled and notify appropriate activity
            int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
            if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
                Log.d("DirectCall", "Wi-Fi Direct is Enabled");
            } else {
                Log.d("DirectCall", "Wi-Fi Direct is Disabled");
                activity.updateConnectionStatus(false);
            }

        } else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {
            // Call WifiP2pManager.requestPeers() to get a list of current peers
            if (manager != null) {
                manager.requestPeers(channel, new WifiP2pManager.PeerListListener() {
                    @Override
                    public void onPeersAvailable(WifiP2pDeviceList peerList) {
                        List<WifiP2pDevice> refreshedPeers = new ArrayList<>(peerList.getDeviceList());
                        
                        try {
                            JSONArray jsonArray = new JSONArray();
                            for (WifiP2pDevice device : refreshedPeers) {
                                JSONObject obj = new JSONObject();
                                obj.put("name", device.deviceName);
                                obj.put("address", device.deviceAddress);
                                // A real app would read the DC-ID from a TXT record or service discovery, 
                                // but for now we'll pass the hardware name/address
                                jsonArray.put(obj);
                            }
                            // Send to WebView Javascript
                            activity.sendPeersToJavascript(jsonArray.toString());
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
            }

        } else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {
            // Respond to new connection or disconnections
            if (manager == null) return;
            NetworkInfo networkInfo = intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);
            if (networkInfo != null && networkInfo.isConnected()) {
                // We are connected with the other device
                manager.requestConnectionInfo(channel, new WifiP2pManager.ConnectionInfoListener() {
                    @Override
                    public void onConnectionInfoAvailable(WifiP2pInfo info) {
                        // This info contains the Group Owner IP, which is needed to create the Audio Sockets
                        activity.updateConnectionStatus(true);
                    }
                });
            } else {
                // It's a disconnect
                activity.updateConnectionStatus(false);
            }

        } else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {
            // Respond to this device's wifi state changing
        }
    }
}

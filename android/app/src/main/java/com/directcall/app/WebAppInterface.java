package com.directcall.app;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import android.webkit.JavascriptInterface;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

public class WebAppInterface {
    Context mContext;
    WifiP2pManager mManager;
    WifiP2pManager.Channel mChannel;

    /** Instantiate the interface and set the context */
    WebAppInterface(Context c, WifiP2pManager manager, WifiP2pManager.Channel channel) {
        mContext = c;
        mManager = manager;
        mChannel = channel;
    }

    @JavascriptInterface
    public void showToast(String toast) {
        Toast.makeText(mContext, toast, Toast.LENGTH_SHORT).show();
    }

    @SuppressLint("MissingPermission")
    @JavascriptInterface
    public void discoverPeers() {
        if (mManager != null && mChannel != null) {
            mManager.discoverPeers(mChannel, new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {
                    // Discovery started successfully. 
                    // BroadcastReceiver will listen for WIFI_P2P_PEERS_CHANGED_ACTION
                }

                @Override
                public void onFailure(int reasonCode) {
                    showToast("Failed to start discovery: " + reasonCode);
                }
            });
        }
    }

    @SuppressLint("MissingPermission")
    @JavascriptInterface
    public void connectToPeer(String deviceAddress) {
        WifiP2pConfig config = new WifiP2pConfig();
        config.deviceAddress = deviceAddress;
        
        mManager.connect(mChannel, config, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                showToast("Connecting to " + deviceAddress + "...");
            }

            @Override
            public void onFailure(int reason) {
                showToast("Connection failed. Retry.");
            }
        });
    }

    @JavascriptInterface
    public void disconnect() {
        if (mManager != null && mChannel != null) {
            mManager.removeGroup(mChannel, new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {
                    showToast("Disconnected.");
                }
                @Override
                public void onFailure(int reason) {
                    showToast("Disconnect failed.");
                }
            });
        }
    }
}

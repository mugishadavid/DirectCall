package com.directcall.app;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pManager;
import android.webkit.JavascriptInterface;
import android.widget.Toast;

public class WebAppInterface {
    Context mContext;
    WifiP2pManager mManager;
    WifiP2pManager.Channel mChannel;

    WebAppInterface(Context c, WifiP2pManager manager, WifiP2pManager.Channel channel) {
        mContext = c;
        mManager = manager;
        mChannel = channel;
    }

    @JavascriptInterface
    public void showToast(String toast) {
        Toast.makeText(mContext, toast, Toast.LENGTH_SHORT).show();
    }

    @JavascriptInterface
    public void registerOfflineId(String dcId, String name) {
        // Tells Java to start broadcasting this user's ID
        if (mContext instanceof MainActivity) {
            ((MainActivity) mContext).startAdvertisingService(dcId, name);
        }
    }

    @SuppressLint("MissingPermission")
    @JavascriptInterface
    public void discoverPeers() {
        // Now triggers DNS-SD Service Discovery instead of regular peer discovery
        if (mContext instanceof MainActivity) {
            ((MainActivity) mContext).startServiceDiscovery();
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
                showToast("Connecting...");
            }

            @Override
            public void onFailure(int reason) {
                showToast("Connection failed.");
            }
        });
    }

    @JavascriptInterface
    public void disconnect() {
        if (mManager != null && mChannel != null) {
            mManager.removeGroup(mChannel, new WifiP2pManager.ActionListener() {
                @Override public void onSuccess() {}
                @Override public void onFailure(int reason) {}
            });
        }
    }
}

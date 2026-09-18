package com.directcall.app;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceInfo;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceRequest;
import android.os.Build;
import android.os.Bundle;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private WebView webView;
    private WebAppInterface webAppInterface;

    // Wi-Fi P2P
    private WifiP2pManager manager;
    private WifiP2pManager.Channel channel;
    private BroadcastReceiver receiver;
    private IntentFilter intentFilter;

    private static final int PERMISSIONS_REQUEST_CODE = 1001;

    // Store discovered peers mapped by MAC address
    private final HashMap<String, JSONObject> discoveredPeers = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 1. Setup WebView
        webView = findViewById(R.id.webview);
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setMediaPlaybackRequiresUserGesture(false);
        webView.setWebViewClient(new WebViewClient());

        // 2. Setup Wi-Fi Direct Manager
        manager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        channel = manager.initialize(this, getMainLooper(), null);

        // 3. Setup Intent Filter for P2P events
        intentFilter = new IntentFilter();
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
        // Note: We removed PEERS_CHANGED_ACTION because we now rely purely on DNS-SD Service Discovery

        // 4. Connect the Javascript Bridge
        webAppInterface = new WebAppInterface(this, manager, channel);
        webView.addJavascriptInterface(webAppInterface, "AndroidBridge");

        // 5. Load the local offline HTML frontend
        webView.loadUrl("file:///android_asset/www/index.html");

        // 6. Request hardware permissions
        requestRequiredPermissions();
    }

    // --- OFFLINE DNS-SD ROUTING ---

    /**
     * Broadcasts this phone's DC-ID to anyone nearby listening.
     */
    public void startAdvertisingService(String dcId, String name) {
        Map<String, String> record = new HashMap<>();
        record.put("dc_id", dcId);
        record.put("name", name);
        
        WifiP2pDnsSdServiceInfo serviceInfo = WifiP2pDnsSdServiceInfo.newInstance(
                "_directcall", "_presence._tcp", record);
                
        manager.clearLocalServices(channel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                addActualService(serviceInfo);
            }
            @Override 
            public void onFailure(int error) {
                addActualService(serviceInfo); // Do it anyway!
            }
        });
    }

    private void addActualService(WifiP2pDnsSdServiceInfo serviceInfo) {
        manager.addLocalService(channel, serviceInfo, new WifiP2pManager.ActionListener() {
            @Override public void onSuccess() {}
            @Override public void onFailure(int error) {}
        });
    }

    /**
     * Scans for other phones broadcasting their DC-IDs.
     */
    public void startServiceDiscovery() {
        manager.setDnsSdResponseListeners(channel,
            (instanceName, registrationType, srcDevice) -> {
                // Not heavily used, we rely on the TXT record listener for the DC-ID
            },
            (fullDomain, record, device) -> {
                // This is where we catch the hidden radio signal!
                if (record.containsKey("dc_id")) {
                    try {
                        JSONObject obj = new JSONObject();
                        obj.put("name", record.get("name"));
                        obj.put("id", record.get("dc_id"));
                        obj.put("address", device.deviceAddress);
                        
                        discoveredPeers.put(device.deviceAddress, obj);
                        
                        // Send the updated phonebook to Javascript
                        JSONArray array = new JSONArray();
                        for (JSONObject peer : discoveredPeers.values()) {
                            array.put(peer);
                        }
                        sendPeersToJavascript(array.toString());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        );

        WifiP2pDnsSdServiceRequest serviceRequest = WifiP2pDnsSdServiceRequest.newInstance();
        manager.removeServiceRequest(channel, serviceRequest, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                addAndDiscoverServices(serviceRequest);
            }
            @Override 
            public void onFailure(int reason) {
                addAndDiscoverServices(serviceRequest); // Do it anyway!
            }
        });
    }

    private void addAndDiscoverServices(WifiP2pDnsSdServiceRequest serviceRequest) {
        manager.addServiceRequest(channel, serviceRequest, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                manager.discoverServices(channel, new WifiP2pManager.ActionListener() {
                    @Override public void onSuccess() {}
                    @Override public void onFailure(int error) {}
                });
            }
            @Override public void onFailure(int error) {}
        });
    }

    private void requestRequiredPermissions() {
        List<String> permissions = new ArrayList<>();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.NEARBY_WIFI_DEVICES);
        } else {
            permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }
        permissions.add(Manifest.permission.RECORD_AUDIO);

        List<String> needed = new ArrayList<>();
        for (String p : permissions) {
            if (ContextCompat.checkSelfPermission(this, p) != PackageManager.PERMISSION_GRANTED) {
                needed.add(p);
            }
        }

        if (!needed.isEmpty()) {
            ActivityCompat.requestPermissions(this, needed.toArray(new String[0]), PERMISSIONS_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    protected void onResume() {
        super.onResume();
        receiver = new WiFiDirectBroadcastReceiver(manager, channel, this);
        registerReceiver(receiver, intentFilter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (receiver != null) unregisterReceiver(receiver);
    }

    public void sendPeersToJavascript(String jsonPeers) {
        runOnUiThread(() -> {
            webView.evaluateJavascript("javascript:if(window.onPeersDiscovered) window.onPeersDiscovered('" + jsonPeers + "');", null);
        });
    }

    public void updateConnectionStatus(boolean isConnected) {
        runOnUiThread(() -> {
            webView.evaluateJavascript("javascript:if(window.onConnectionChanged) window.onConnectionChanged(" + isConnected + ");", null);
        });
    }
}

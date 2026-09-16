package com.directcall.app;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.wifi.p2p.WifiP2pManager;
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

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private WebView webView;
    private WebAppInterface webAppInterface;

    // Wi-Fi P2P
    private WifiP2pManager manager;
    private WifiP2pManager.Channel channel;
    private BroadcastReceiver receiver;
    private IntentFilter intentFilter;

    private static final int PERMISSIONS_REQUEST_CODE = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 1. Setup WebView
        webView = findViewById(R.id.webview);
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true); // Required for localStorage
        webSettings.setMediaPlaybackRequiresUserGesture(false); // For audio

        // Force links and redirects to open in the WebView instead of the system browser
        webView.setWebViewClient(new WebViewClient());

        // 2. Setup Wi-Fi Direct Manager
        manager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        channel = manager.initialize(this, getMainLooper(), null);

        // 3. Setup Intent Filter for P2P events
        intentFilter = new IntentFilter();
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

        // 4. Connect the Javascript Bridge
        webAppInterface = new WebAppInterface(this, manager, channel);
        webView.addJavascriptInterface(webAppInterface, "AndroidBridge");

        // 5. Load the local offline HTML frontend
        webView.loadUrl("file:///android_asset/www/index.html");

        // 6. Request hardware permissions
        requestRequiredPermissions();
    }

    private void requestRequiredPermissions() {
        List<String> permissions = new ArrayList<>();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // Android 13+
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
        if (requestCode == PERMISSIONS_REQUEST_CODE) {
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Permissions are required for DirectCall to work offline.", Toast.LENGTH_LONG).show();
                    return;
                }
            }
        }
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
        if (receiver != null) {
            unregisterReceiver(receiver);
        }
    }

    // Called from the BroadcastReceiver to send data to Javascript
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

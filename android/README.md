# DirectCall - Android Native Bridge
This directory will contain the native Android project (Java/Kotlin).
The native code will expose Wi-Fi Direct (P2P), Audio Capture, and SQLite functionality to the HTML5 frontend using a WebView Bridge interface.

Upcoming implementations:
1. `WifiDirectManager.java` - For Peer Discovery and Connection.
2. `AudioSocketStreamer.java` - For sending mic audio over local sockets.
3. `WebAppInterface.java` - Javascript bridge connecting the frontend to these native APIs.

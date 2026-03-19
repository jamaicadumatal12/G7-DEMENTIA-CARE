/*
 * ESP8266 WiFi Connection Test
 * 
 * This sketch tests WiFi connectivity on your ESP8266 device.
 * It will:
 * 1. Scan for available WiFi networks
 * 2. Attempt to connect to specified networks
 * 3. Display connection details (IP, signal strength, etc.)
 * 4. Test internet connectivity
 * 5. Monitor connection stability
 * 
 * Upload this to your ESP8266 and open Serial Monitor at 115200 baud
 */

#include <ESP8266WiFi.h>

// === WiFi Credentials ===
// Add your WiFi networks here (you can add multiple)
const char* ssids[] = {
  "ERICBUANG",
  "BackupNetwork"
};

const char* passwords[] = {
  "JAYSONBONGS",
  "backupPassword"
};

const int numNetworks = 2; // Update this to match number of networks above

// === Test Configuration ===
const int CONNECTION_TIMEOUT = 20; // seconds
const int TEST_INTERVAL = 5000; // milliseconds between tests
const char* TEST_HOST = "www.google.com"; // Host to ping for internet test

// === Status Variables ===
bool wifiConnected = false;
unsigned long lastTestTime = 0;
int connectionAttempts = 0;
int successfulConnections = 0;
int failedConnections = 0;

void setup() {
  Serial.begin(115200);
  delay(1000);
  
  Serial.println("\n\n");
  Serial.println("========================================");
  Serial.println("   ESP8266 WiFi Connection Test");
  Serial.println("========================================");
  Serial.println();
  
  // Print device info
  Serial.println("Device Information:");
  Serial.print("  Chip ID: ");
  Serial.println(ESP.getChipId());
  Serial.print("  Flash Size: ");
  Serial.print(ESP.getFlashChipSize() / 1024);
  Serial.println(" KB");
  Serial.print("  Free Heap: ");
  Serial.print(ESP.getFreeHeap());
  Serial.println(" bytes");
  Serial.println();
  
  // Start WiFi test
  testWiFiConnection();
}

void loop() {
  // Periodic connection test
  if (millis() - lastTestTime > TEST_INTERVAL) {
    testConnectionStatus();
    lastTestTime = millis();
  }
  
  delay(100);
}

void testWiFiConnection() {
  Serial.println("========================================");
  Serial.println("Starting WiFi Connection Test...");
  Serial.println("========================================");
  Serial.println();
  
  // Step 1: Scan for networks
  scanNetworks();
  
  // Step 2: Try to connect
  connectToWiFi();
  
  // Step 3: If connected, test internet
  if (wifiConnected) {
    testInternetConnection();
    printConnectionDetails();
  }
  
  Serial.println();
  Serial.println("========================================");
  Serial.println("Test Complete!");
  Serial.println("========================================");
}

void scanNetworks() {
  Serial.println("--- Step 1: Scanning for Networks ---");
  Serial.println();
  
  WiFi.mode(WIFI_STA);
  WiFi.disconnect();
  delay(100);
  
  int numNetworks = WiFi.scanNetworks();
  
  if (numNetworks == 0) {
    Serial.println("❌ No networks found!");
    Serial.println("   Make sure you're in range of a WiFi router.");
    Serial.println();
    return;
  }
  
  Serial.print("✅ Found ");
  Serial.print(numNetworks);
  Serial.println(" network(s):");
  Serial.println();
  
  // Print all networks
  for (int i = 0; i < numNetworks; i++) {
    Serial.print("  [");
    Serial.print(i + 1);
    Serial.print("] ");
    Serial.print(WiFi.SSID(i));
    Serial.print(" (");
    Serial.print(WiFi.RSSI(i));
    Serial.print(" dBm)");
    
    // Check if this is one of our target networks
    bool isTargetNetwork = false;
    for (int j = 0; j < numNetworks; j++) {
      if (WiFi.SSID(i) == String(ssids[j])) {
        isTargetNetwork = true;
        Serial.print(" ⭐ TARGET");
        break;
      }
    }
    
    Serial.print(" [");
    Serial.print(getEncryptionType(WiFi.encryptionType(i)));
    Serial.println("]");
  }
  
  Serial.println();
}

void connectToWiFi() {
  Serial.println("--- Step 2: Attempting Connection ---");
  Serial.println();
  
  WiFi.mode(WIFI_STA);
  wifiConnected = false;
  
  for (int i = 0; i < numNetworks; i++) {
    Serial.print("Attempting to connect to: ");
    Serial.print(ssids[i]);
    Serial.print(" ... ");
    
    WiFi.begin(ssids[i], passwords[i]);
    connectionAttempts++;
    
    int attempts = 0;
    while (WiFi.status() != WL_CONNECTED && attempts < CONNECTION_TIMEOUT) {
      delay(500);
      Serial.print(".");
      attempts++;
    }
    Serial.println();
    
    if (WiFi.status() == WL_CONNECTED) {
      wifiConnected = true;
      successfulConnections++;
      Serial.println("✅ CONNECTED!");
      Serial.println();
      break;
    } else {
      failedConnections++;
      Serial.print("❌ FAILED - Status Code: ");
      Serial.println(getStatusString(WiFi.status()));
      Serial.println();
    }
  }
  
  if (!wifiConnected) {
    Serial.println("❌ Could not connect to any network!");
    Serial.println();
    Serial.println("Troubleshooting Tips:");
    Serial.println("  1. Check if SSID and password are correct");
    Serial.println("  2. Verify router is powered on and working");
    Serial.println("  3. Check if device is within WiFi range");
    Serial.println("  4. Try moving closer to the router");
    Serial.println("  5. Check router settings (MAC filtering, etc.)");
    Serial.println();
  }
}

void testInternetConnection() {
  Serial.println("--- Step 3: Testing Internet Connection ---");
  Serial.println();
  
  Serial.print("Pinging ");
  Serial.print(TEST_HOST);
  Serial.print(" ... ");
  
  WiFiClient client;
  if (client.connect(TEST_HOST, 80)) {
    Serial.println("✅ SUCCESS!");
    Serial.println("   Internet connection is working.");
    client.stop();
  } else {
    Serial.println("❌ FAILED!");
    Serial.println("   WiFi is connected but no internet access.");
    Serial.println("   Check router internet connection.");
  }
  Serial.println();
}

void printConnectionDetails() {
  Serial.println("--- Connection Details ---");
  Serial.println();
  
  Serial.print("SSID: ");
  Serial.println(WiFi.SSID());
  
  Serial.print("IP Address: ");
  Serial.println(WiFi.localIP());
  
  Serial.print("Subnet Mask: ");
  Serial.println(WiFi.subnetMask());
  
  Serial.print("Gateway: ");
  Serial.println(WiFi.gatewayIP());
  
  Serial.print("DNS: ");
  Serial.println(WiFi.dnsIP());
  
  Serial.print("MAC Address: ");
  Serial.println(WiFi.macAddress());
  
  Serial.print("Signal Strength (RSSI): ");
  Serial.print(WiFi.RSSI());
  Serial.print(" dBm (");
  Serial.print(getSignalQuality(WiFi.RSSI()));
  Serial.println(")");
  
  Serial.print("Channel: ");
  Serial.println(WiFi.channel());
  
  Serial.print("Connection Attempts: ");
  Serial.println(connectionAttempts);
  
  Serial.print("Successful Connections: ");
  Serial.println(successfulConnections);
  
  Serial.print("Failed Connections: ");
  Serial.println(failedConnections);
  
  Serial.println();
}

void testConnectionStatus() {
  if (WiFi.status() == WL_CONNECTED) {
    if (!wifiConnected) {
      // Just reconnected
      Serial.println("✅ WiFi reconnected!");
      wifiConnected = true;
      printConnectionDetails();
    }
    
    // Print periodic status (every 30 seconds)
    static unsigned long lastStatusPrint = 0;
    if (millis() - lastStatusPrint > 30000) {
      Serial.print("[");
      Serial.print(millis() / 1000);
      Serial.print("s] WiFi OK - RSSI: ");
      Serial.print(WiFi.RSSI());
      Serial.print(" dBm - IP: ");
      Serial.println(WiFi.localIP());
      lastStatusPrint = millis();
    }
  } else {
    if (wifiConnected) {
      // Just disconnected
      Serial.println("❌ WiFi disconnected!");
      wifiConnected = false;
      Serial.println("Attempting to reconnect...");
      connectToWiFi();
    }
  }
}

String getStatusString(wl_status_t status) {
  switch (status) {
    case WL_NO_SHIELD: return "NO_SHIELD";
    case WL_IDLE_STATUS: return "IDLE";
    case WL_NO_SSID_AVAIL: return "NO_SSID_AVAIL";
    case WL_SCAN_COMPLETED: return "SCAN_COMPLETED";
    case WL_CONNECTED: return "CONNECTED";
    case WL_CONNECT_FAILED: return "CONNECT_FAILED";
    case WL_CONNECTION_LOST: return "CONNECTION_LOST";
    case WL_DISCONNECTED: return "DISCONNECTED";
    default: return "UNKNOWN";
  }
}

String getEncryptionType(uint8_t encryptionType) {
  switch (encryptionType) {
    case ENC_TYPE_NONE: return "Open";
    case ENC_TYPE_WEP: return "WEP";
    case ENC_TYPE_TKIP: return "WPA";
    case ENC_TYPE_CCMP: return "WPA2";
    case ENC_TYPE_AUTO: return "Auto";
    default: return "Unknown";
  }
}

String getSignalQuality(int rssi) {
  if (rssi >= -50) return "Excellent";
  else if (rssi >= -60) return "Very Good";
  else if (rssi >= -70) return "Good";
  else if (rssi >= -80) return "Fair";
  else if (rssi >= -90) return "Weak";
  else return "Very Weak";
}


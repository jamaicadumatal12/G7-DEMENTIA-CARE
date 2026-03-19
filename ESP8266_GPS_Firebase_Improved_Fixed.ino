/*
 * G7 Dementia Care - Improved ESP8266 GPS Tracker (FIXED VERSION)
 * Hardware: ESP8266 Round LCD Board, ATGM336H-5N GPS, Passive Buzzer
 * Features: Real-time GPS tracking, Safe zone monitoring, Firebase integration
 * Version: 2.1 - Fixed GPS timeout issues with better diagnostics
 */

#define DISABLE_FIREBASE_SD

#include <ESP8266WiFi.h>
#include <FirebaseESP8266.h>
#include <TinyGPSPlus.h>
#include <SoftwareSerial.h>
#include <TFT_eSPI.h>
#include <SPI.h>
#include <EEPROM.h>
#include <Ticker.h>

Ticker watchdog;

void resetWatchdog() {
    ESP.wdtFeed();
}

// === Wi-Fi and Firebase Credentials ===
const char* ssid     = "ESSA";
const char* password = "11111111";

#define FIREBASE_HOST "g7-dementia-care-default-rtdb.asia-southeast1.firebasedatabase.app"
#define FIREBASE_AUTH "CsmqaL84uFlUjh6i9NhWCoiw0xR99JpCQfmHs4VK"

// === Device Configuration ===
String DEVICE_ID     = "G7T55ZGF8";
const int EEPROM_SIZE = 32;
const String DEVICE_PREFIX = "G7T";

// === Pin Definitions ===
// IMPORTANT: SoftwareSerial(rxPin, txPin)
// GPS TX (sends data) -> ESP8266 RX (D1) - receives GPS data
// GPS RX (receives data) -> ESP8266 TX (D2) - sends commands to GPS
// NOTE: Changed from D5/D6 to D1/D2 to match your working hardware configuration
#define GPS_RX_PIN D1   // ESP8266 receives GPS data here (GPS TX connects to this)
#define GPS_TX_PIN D2   // ESP8266 sends commands here (GPS RX connects to this)
#define BUZZER_PIN D5   // Passive buzzer with transistor control (moved from D1 to avoid conflict)

// === Safe Zone Configuration ===
// Updated to match current GPS location (8.132393, 125.117562)
double safeLat     = 8.132393;  // Default safe zone center (updated from current location)
double safeLng     = 125.117562;
float dangerRadius = 100.0;   // Safe zone radius in meters

// === State Management ===
bool wasInSafeZone = true;
bool alertTriggered = false;
unsigned long lastAlertTime = 0;
const unsigned long ALERT_COOLDOWN = 30000; // 30 seconds between alerts

// === Timing Variables ===
unsigned long lastGPSUpdate = 0;
unsigned long lastFirebaseUpdate = 0;
unsigned long lastBuzzer = 0;
unsigned long lastWiFiCheck = 0;
unsigned long lastDisplayUpdate = 0;
unsigned long lastRawDataCheck = 0;
unsigned long gpsStartTime = 0;
unsigned long lastSafeZoneCheck = 0;
const unsigned long SAFE_ZONE_CHECK_INTERVAL = 60000; // Check for safe zone updates every 60 seconds

const unsigned long GPS_TIMEOUT = 30000;        // 30 seconds (increased for cold start)
const unsigned long FIREBASE_INTERVAL = 15000;  // 15 seconds
const unsigned long BUZZER_INTERVAL = 3000;     // 3 seconds
const unsigned long WIFI_CHECK_INTERVAL = 30000; // 30 seconds
const unsigned long DISPLAY_UPDATE_INTERVAL = 2000; // 2 seconds
const unsigned long RAW_DATA_CHECK_INTERVAL = 5000; // 5 seconds

// === Status Flags ===
bool wifiConnected = false;
bool firebaseConnected = false;
bool gpsValid = false;
bool safeZoneLoaded = false;
bool gpsDataReceived = false; // Track if we're receiving ANY data from GPS
int rawDataCount = 0; // Count raw bytes received

// === Firebase Objects ===
FirebaseData   firebaseData;
FirebaseAuth   auth;
FirebaseConfig config;

// === GPS and Display ===
TinyGPSPlus gps;
SoftwareSerial gpsSerial(GPS_RX_PIN, GPS_TX_PIN); // RX, TX pins
TFT_eSPI tft = TFT_eSPI();

// === Buzzer Functions ===
void playDangerAlert() {
    // Three clear beeps when entering danger zone
    for (int i = 0; i < 3; i++) {
        tone(BUZZER_PIN, 1500); // 1.5kHz
        delay(400);
        noTone(BUZZER_PIN);
        delay(200);
    }
}

void playSafeAlert() {
    // Short confirmation beep when returning to safe zone
    tone(BUZZER_PIN, 2000); // 2kHz
    delay(150);
    noTone(BUZZER_PIN);
}

void playStartupSound() {
    // Brief power-on chirp
    tone(BUZZER_PIN, 1200);
    delay(120);
    noTone(BUZZER_PIN);
}

void playContinuousAlert() {
    // Continuous beeping while outside safe zone
    if (millis() - lastBuzzer > BUZZER_INTERVAL) {
        tone(BUZZER_PIN, 1500, 1000);
        lastBuzzer = millis();
    }
}

// === Display Functions ===
void setupDisplay() {
    tft.init();
    tft.setRotation(2);  // Fix upside down text
    tft.fillScreen(TFT_BLACK);
    tft.setTextColor(TFT_WHITE, TFT_BLACK);
}

void displayText(const char* text, int x, int y, uint16_t color) {
    tft.setTextColor(color);
    tft.setCursor(x, y);
    tft.print(text);
}

void displayCenteredText(const char* text, int y, uint16_t color, int size = 2) {
    tft.setTextSize(size);
    int16_t x = (tft.width() - strlen(text) * 6 * size) / 2;
    displayText(text, x, y, color);
}

void displayStatus(const char* message, uint16_t color) {
    tft.fillScreen(TFT_BLACK);
    displayCenteredText(message, 100, color, 2);
}

void displayDeviceId() {
    tft.fillScreen(TFT_BLACK);
    displayCenteredText("Device ID:", 60, TFT_GREEN, 2);
    displayCenteredText(DEVICE_ID.c_str(), 100, TFT_WHITE, 3);
}

void displayGPSDiagnostics() {
    tft.fillScreen(TFT_BLACK);
    
    displayCenteredText("GPS Diagnostics", 20, TFT_WHITE, 2);
    
    // Raw data status
    displayText("Raw Data:", 10, 50, TFT_WHITE);
    displayText(gpsDataReceived ? "YES" : "NO", 100, 50, gpsDataReceived ? TFT_GREEN : TFT_RED);
    
    displayText("Bytes:", 10, 70, TFT_WHITE);
    displayText(String(rawDataCount).c_str(), 100, 70, TFT_WHITE);
    
    // GPS fix status
    displayText("GPS Fix:", 10, 90, TFT_WHITE);
    displayText(gpsValid ? "YES" : "NO", 100, 90, gpsValid ? TFT_GREEN : TFT_RED);
    
    if (gpsValid) {
        displayText("Satellites:", 10, 110, TFT_WHITE);
        displayText(String(gps.satellites.value()).c_str(), 100, 110, TFT_WHITE);
        
        displayText("HDOP:", 10, 130, TFT_WHITE);
        displayText(String(gps.hdop.hdop(), 1).c_str(), 100, 130, TFT_WHITE);
    } else {
        unsigned long elapsed = (millis() - gpsStartTime) / 1000;
        displayText("Wait Time:", 10, 110, TFT_WHITE);
        displayText(String(elapsed).c_str(), 100, 110, TFT_YELLOW);
        displayText("seconds", 130, 110, TFT_YELLOW);
    }
    
    // Connection status
    displayText("WiFi:", 10, 150, wifiConnected ? TFT_GREEN : TFT_RED);
    displayText("Firebase:", 10, 170, firebaseConnected ? TFT_GREEN : TFT_RED);
}

void displayGPSData(double lat, double lng, int sats, float hdop, float distance, bool isSafe) {
    tft.fillScreen(TFT_BLACK);
    
    // Header
    displayCenteredText("GPS Tracker", 20, TFT_WHITE, 2);
    
    // Coordinates
    displayText("LAT:", 10, 50, TFT_WHITE);
    displayText(String(lat, 6).c_str(), 10, 70, TFT_WHITE);
    
    displayText("LNG:", 10, 90, TFT_WHITE);
    displayText(String(lng, 6).c_str(), 10, 110, TFT_WHITE);
    
    // GPS Quality
    displayText("SAT:", 10, 130, TFT_WHITE);
    displayText(String(sats).c_str(), 50, 130, TFT_WHITE);
    
    displayText("HDOP:", 10, 150, TFT_WHITE);
    displayText(String(hdop, 1).c_str(), 60, 150, TFT_WHITE);
    
    // Distance and Status
    displayText("DIST:", 10, 170, TFT_WHITE);
    displayText(String(distance, 1).c_str(), 60, 170, TFT_WHITE);
    displayText("m", 100, 170, TFT_WHITE);
    
    // Status
    displayText("STATUS:", 10, 190, TFT_WHITE);
    displayText(isSafe ? "SAFE" : "DANGER", 80, 190, isSafe ? TFT_GREEN : TFT_RED);
    
    // Connection Status
    displayText("WiFi:", 10, 210, wifiConnected ? TFT_GREEN : TFT_RED);
    displayText("Firebase:", 10, 230, firebaseConnected ? TFT_GREEN : TFT_RED);
}

// === WiFi Functions ===
void connectToWiFi() {
    displayStatus("Connecting WiFi...", TFT_YELLOW);
    
    Serial.println("\n=== WiFi Connection ===");
    Serial.print("SSID: "); Serial.println(ssid);
    Serial.println("Attempting to connect...");
    
    WiFi.mode(WIFI_STA);
    WiFi.disconnect(); // Disconnect any existing connection
    delay(100);
    WiFi.begin(ssid, password);
    
    int attempts = 0;
    while (WiFi.status() != WL_CONNECTED && attempts < 30) { // Increased to 30 seconds
        delay(1000);
        Serial.print(".");
        attempts++;
        
        // Show progress every 5 seconds
        if (attempts % 5 == 0) {
            Serial.print(" ("); Serial.print(attempts); Serial.print("s)");
        }
    }
    
    if (WiFi.status() == WL_CONNECTED) {
        wifiConnected = true;
        Serial.println("\n✅ WiFi Connected!");
        Serial.print("IP Address: "); Serial.println(WiFi.localIP());
        Serial.print("Signal Strength (RSSI): "); Serial.print(WiFi.RSSI()); Serial.println(" dBm");
        Serial.println("========================");
        displayStatus("WiFi OK", TFT_GREEN);
        delay(1000);
    } else {
        wifiConnected = false;
        Serial.println("\n❌ WiFi Connection Failed!");
        Serial.println("Possible issues:");
        Serial.println("  1. Wrong WiFi SSID or password");
        Serial.println("  2. WiFi network not in range");
        Serial.println("  3. Router is down or blocking connection");
        Serial.print("  Current SSID in code: "); Serial.println(ssid);
        Serial.println("========================");
        displayStatus("WiFi FAIL", TFT_RED);
    }
}

void checkWiFiConnection() {
    if (millis() - lastWiFiCheck > WIFI_CHECK_INTERVAL) {
        if (WiFi.status() != WL_CONNECTED) {
            if (wifiConnected) {
                Serial.println("⚠️ WiFi disconnected, attempting to reconnect...");
            }
            wifiConnected = false;
            firebaseConnected = false; // Firebase also disconnected if WiFi is down
            connectToWiFi();
            
            // Re-initialize Firebase if WiFi reconnected
            if (wifiConnected && !firebaseConnected) {
                Serial.println("Re-initializing Firebase after WiFi reconnection...");
                initFirebase();
                if (firebaseConnected) {
                    loadSafeZoneFromFirebase(); // Reload safe zone
                }
            }
        }
        lastWiFiCheck = millis();
    }
}

// === Firebase Functions ===
void initFirebase() {
    if (!wifiConnected) {
        Serial.println("Cannot initialize Firebase - WiFi not connected!");
        return;
    }
    
    displayStatus("Firebase...", TFT_YELLOW);
    
    config.host = FIREBASE_HOST;
    config.signer.tokens.legacy_token = FIREBASE_AUTH;
    
    Firebase.begin(&config, &auth);
    Firebase.reconnectWiFi(true);
    
    // Wait for Firebase to be ready
    int attempts = 0;
    while (!Firebase.ready() && attempts < 10) {
        delay(1000);
        attempts++;
    }
    
    if (Firebase.ready()) {
        firebaseConnected = true;
        Serial.println("Firebase Connected!");
        displayStatus("Firebase OK", TFT_GREEN);
        delay(1000);
    } else {
        firebaseConnected = false;
        Serial.println("Firebase Connection Failed!");
        displayStatus("Firebase FAIL", TFT_RED);
    }
}

void sendLocationToFirebase(double lat, double lng, int sats, float hdop, bool isDanger) {
    if (!firebaseConnected || !Firebase.ready()) {
        Serial.println("Firebase not ready!");
        return;
    }
    
    // Validate GPS data
    if (lat == 0.0 && lng == 0.0) {
        Serial.println("Invalid GPS coordinates, skipping Firebase update");
        return;
    }
    
    String path = "/devices/" + DEVICE_ID;
    FirebaseJson json;
    json.set("latitude", lat);
    json.set("longitude", lng);
    json.set("satellites", sats);
    json.set("hdop", hdop);
    json.set("inDanger", isDanger);
    json.set("timestamp", millis());
    json.set("deviceId", DEVICE_ID);
    json.set("status", "active");  // Device is active
    json.set("gpsStatus", "valid"); // GPS has valid fix
    
    if (Firebase.setJSON(firebaseData, path, json)) {
        Serial.println("✅ Location sent to Firebase successfully");
    } else {
        Serial.println("❌ Firebase Error: " + firebaseData.errorReason());
        firebaseConnected = false;
    }
}

// Send device heartbeat/status to Firebase (even without GPS fix)
// "Heartbeat" = periodic status update that keeps device showing as "active" in Firebase
// Like a heartbeat in your body - it shows the device is alive and working!
void sendDeviceStatusToFirebase() {
    if (!firebaseConnected || !Firebase.ready()) {
        Serial.println("⚠️ Cannot send heartbeat - Firebase not ready");
        return;
    }
    
    String path = "/devices/" + DEVICE_ID;
    FirebaseJson json;
    json.set("deviceId", DEVICE_ID);
    json.set("timestamp", millis());
    json.set("status", "active");
    json.set("gpsStatus", gpsValid ? "valid" : (gpsDataReceived ? "searching" : "no_signal"));
    json.set("wifiConnected", wifiConnected);
    json.set("firebaseConnected", firebaseConnected);
    
    String gpsStatusStr = gpsValid ? "valid" : (gpsDataReceived ? "searching" : "no_signal");
    
    if (gpsValid && gps.location.isValid()) {
        json.set("latitude", gps.location.lat());
        json.set("longitude", gps.location.lng());
        json.set("satellites", gps.satellites.value());
        json.set("hdop", gps.hdop.hdop());
    } else {
        json.set("latitude", 0.0);
        json.set("longitude", 0.0);
        json.set("satellites", 0);
        json.set("hdop", 99.9);
    }
    
    if (Firebase.setJSON(firebaseData, path, json)) {
        Serial.println("\n📡📡📡 DEVICE HEARTBEAT SENT 📡📡📡");
        Serial.println("  Device ID: " + DEVICE_ID);
        Serial.println("  Status: active");
        Serial.println("  GPS Status: " + gpsStatusStr);
        Serial.println("  WiFi: " + String(wifiConnected ? "connected" : "disconnected"));
        Serial.println("  Firebase: " + String(firebaseConnected ? "connected" : "disconnected"));
        if (gpsValid && gps.location.isValid()) {
            Serial.print("  GPS: "); Serial.print(gps.location.lat(), 6); 
            Serial.print(", "); Serial.println(gps.location.lng(), 6);
            Serial.print("  Satellites: "); Serial.println(gps.satellites.value());
        } else {
            Serial.println("  GPS: Waiting for fix...");
        }
        Serial.println("  Timestamp: " + String(millis()));
        Serial.println("========================================\n");
    } else {
        Serial.println("❌ Failed to send heartbeat: " + firebaseData.errorReason());
    }
}

void sendAlertToFirebase(String alertMessage) {
    if (!firebaseConnected || !Firebase.ready()) {
        return;
    }
    
    String path = "/alerts/latest";
    FirebaseJson json;
    json.set("message", alertMessage);
    json.set("timestamp", millis());
    json.set("deviceId", DEVICE_ID);
    
    if (Firebase.setJSON(firebaseData, path, json)) {
        Serial.println("Alert sent to Firebase: " + alertMessage);
    } else {
        Serial.println("Failed to send alert: " + firebaseData.errorReason());
    }
}

// === Safe Zone Loading from Firebase ===
// This version ALWAYS uses the newest safe zone (highest timestamp)
void loadSafeZoneFromFirebase() {
    if (!firebaseConnected || !Firebase.ready()) {
        Serial.println("Firebase not ready for safe zone loading!");
        return;
    }

    String path = "/zones/" + DEVICE_ID;
    Serial.println("Loading safe zone from Firebase path: " + path);
    
    if (Firebase.getJSON(firebaseData, path)) {
        String jsonString = firebaseData.jsonString();
        Serial.println("Raw JSON: " + jsonString);
        
        if (jsonString.length() > 0 && jsonString != "null") {
            FirebaseJson json;
            json.setJsonData(jsonString);
            FirebaseJsonData jsonData;

            bool foundSafeZone = false;
            String latestKey = "";
            long long latestTimestamp = -1;  // Use long long for large timestamp values (64-bit)

            // First pass: find the SAFE zone with the highest timestamp
            // Only check keys that are objects (zones), not primitive values
            int keyCount = json.iteratorBegin();
            for (int i = 0; i < keyCount; i++) {
                int type;
                String key, value;
                json.iteratorGet(i, type, key, value);
                
                // Firebase push keys (zone objects) always start with '-' 
                // Primitives like "deviceId", "latitude", "radius", "timestamp", "type" do NOT start with '-'
                // This is the most reliable way to identify zone objects vs nested properties
                if (key.length() > 0 && key.charAt(0) == '-') {
                    Serial.println("Checking zone key: " + key);
                    
                    // This is a Firebase push key (zone object), try to get its type
                    if (json.get(jsonData, key + "/type")) {
                        Serial.print("  Type found: '"); Serial.print(jsonData.stringValue); Serial.println("'");
                        
                        // Verify we got a valid string value and it's a safe zone
                        if (jsonData.stringValue.length() > 0 && jsonData.stringValue == "safe") {
                            Serial.println("  ✅ This is a safe zone!");
                            
                            // Get timestamp (read as double and convert to long long for large numbers)
                            if (json.get(jsonData, key + "/timestamp")) {
                                // Timestamps are large numbers (milliseconds since epoch)
                                // Read as double (handles large numbers) then cast to long long
                                double tsDouble = jsonData.doubleValue;
                                long long ts = (long long)tsDouble;
                                
                                Serial.print("  Timestamp: "); Serial.println(ts);

                                if (ts > latestTimestamp) {
                                    latestTimestamp = ts;
                                    latestKey = key;
                                    foundSafeZone = true;
                                    Serial.print("  ✅ New latest zone found! Key: "); Serial.println(key);
                                } else {
                                    Serial.print("  ⚠️ Older timestamp (current latest: "); Serial.print(latestTimestamp); Serial.println(")");
                                }
                            } else {
                                Serial.println("  ⚠️ Failed to get timestamp");
                            }
                        } else {
                            Serial.print("  ⚠️ Not a safe zone. Type: '"); Serial.print(jsonData.stringValue); Serial.println("'");
                        }
                    } else {
                        Serial.println("  ⚠️ Failed to get type field");
                    }
                }
                // Skip all other keys (nested properties like deviceId, latitude, etc.)
            }
            json.iteratorEnd();

            if (foundSafeZone) {
                // Load data only from the latest safe zone
                Serial.println("✅ Using latest safe zone key: " + latestKey);

                json.get(jsonData, latestKey + "/latitude");
                if (jsonData.doubleValue != 0) {
                    safeLat = jsonData.doubleValue;
                }

                json.get(jsonData, latestKey + "/longitude");
                if (jsonData.doubleValue != 0) {
                    safeLng = jsonData.doubleValue;
                }

                json.get(jsonData, latestKey + "/radius");
                if (jsonData.intValue > 0) {
                    dangerRadius = jsonData.intValue;
                }

                safeZoneLoaded = true;
                Serial.println("\n✅✅✅ SAFE ZONE LOADED SUCCESSFULLY ✅✅✅");
                Serial.println("✅ Safe zone loaded from Firebase (latest only):");
                Serial.print("  Center: "); Serial.print(safeLat, 6); Serial.print(", "); Serial.println(safeLng, 6);
                Serial.print("  Radius: "); Serial.print(dangerRadius); Serial.println("m");
                Serial.println("========================================\n");
                displayStatus("Zone Loaded", TFT_GREEN);
                delay(1000);
            } else {
                Serial.println("⚠️ No safe zone found in Firebase - using default");
                Serial.println("  Default center: " + String(safeLat, 6) + ", " + String(safeLng, 6));
                Serial.println("  Default radius: " + String(dangerRadius) + "m\n");
            }
        } else {
            Serial.println("No zones data found for this device - using default");
        }
    } else {
        Serial.println("❌ Failed to load safe zone: " + firebaseData.errorReason());
    }
}

// === GPS Functions ===
float calculateDistance(double lat1, double lon1, double lat2, double lon2) {
    return TinyGPSPlus::distanceBetween(lat1, lon1, lat2, lon2);
}

void checkGPSStatus() {
    // Check if we're receiving ANY data from GPS module
    if (millis() - lastRawDataCheck > RAW_DATA_CHECK_INTERVAL) {
        if (!gpsDataReceived) {
            Serial.println("⚠ WARNING: No raw data received from GPS module!");
            Serial.println("Check wiring:");
            Serial.println("  - GPS VCC -> 3.3V");
            Serial.println("  - GPS GND -> GND");
            Serial.println("  - GPS TX -> ESP8266 D1 (GPS_RX_PIN)");
            Serial.println("  - GPS RX -> ESP8266 D2 (GPS_TX_PIN)");
            Serial.print("  - GPS baud rate: 9600");
            Serial.println();
        }
        lastRawDataCheck = millis();
    }
    
    // Check GPS fix timeout (only if we're receiving data)
    if (gpsDataReceived && millis() - lastGPSUpdate > GPS_TIMEOUT) {
        gpsValid = false;
        Serial.println("GPS timeout - no valid fix received (but data is coming)");
        Serial.println("This might be normal for cold start - wait up to 30-60 seconds");
    } else if (!gpsDataReceived && millis() - gpsStartTime > 10000) {
        // No data at all after 10 seconds - likely wiring issue
        Serial.println("❌ CRITICAL: No data received from GPS module!");
        Serial.println("Possible issues:");
        Serial.println("  1. GPS module not powered (check VCC connection)");
        Serial.println("  2. Wrong TX/RX pins (GPS TX should go to ESP8266 D5)");
        Serial.println("  3. GPS module not working");
        Serial.println("  4. Wrong baud rate");
    }
}

// === Main Setup ===
void setup() {
    Serial.begin(115200);
    delay(1000); // Give serial time to initialize
    
    EEPROM.begin(EEPROM_SIZE);
    
    // Initialize buzzer
    pinMode(BUZZER_PIN, OUTPUT);
    digitalWrite(BUZZER_PIN, LOW);
    
    // Setup display
    setupDisplay();
    displayDeviceId();
    playStartupSound();
    
    Serial.println("\n\n========================================");
    Serial.println("   G7 Dementia Care Device");
    Serial.println("========================================");
    Serial.println("Device ID: " + DEVICE_ID);
    Serial.println("GPS Configuration:");
    Serial.println("  RX Pin (GPS TX ->): D1");
    Serial.println("  TX Pin (GPS RX ->): D2");
    Serial.println("  Baud Rate: 9600");
    Serial.println("========================================\n");
    
    // Initialize GPS serial with proper baud rate
    Serial.println("Initializing GPS serial...");
    gpsSerial.begin(9600);
    gpsStartTime = millis();
    delay(500); // Give GPS time to initialize
    
    Serial.println("GPS serial initialized. Waiting for data...");
    Serial.println("(This may take 30-60 seconds for cold start)\n");
    
    // Show diagnostics initially
    displayGPSDiagnostics();
    
    delay(3000);
    
    // Connect to WiFi and Firebase
    connectToWiFi();
    if (wifiConnected) {
        initFirebase();
        // Load safe zone from Firebase on startup
        if (firebaseConnected) {
            loadSafeZoneFromFirebase();
        }
    }
    
    // Setup watchdog timer
    watchdog.attach(1, resetWatchdog);
    
    Serial.println("System initialized successfully!");
    Serial.println("Monitoring GPS data...\n");
}

// === Main Loop ===
void loop() {
    // Check WiFi connection
    checkWiFiConnection();
    
    // Periodically check for safe zone updates from Firebase
    if (firebaseConnected && millis() - lastSafeZoneCheck > SAFE_ZONE_CHECK_INTERVAL) {
        loadSafeZoneFromFirebase();
        lastSafeZoneCheck = millis();
    }
    
    // Check GPS status
    checkGPSStatus();
    
    // Process GPS data - check if ANY data is available
    if (gpsSerial.available() > 0) {
        gpsDataReceived = true; // We're getting data!
        rawDataCount += gpsSerial.available();
        
        // Read and process all available data
        while (gpsSerial.available() > 0) {
            char c = gpsSerial.read();
            
            // Show raw NMEA sentences for debugging (first few sentences)
            static int echoCount = 0;
            static String currentSentence = "";
            currentSentence += c;
            
            if (c == '\n') {
                // End of NMEA sentence
                if (echoCount < 5) { // Show first 5 sentences
                    Serial.print("📡 NMEA: " + currentSentence);
                    echoCount++;
                }
                currentSentence = "";
            }
            
            // Limit total characters shown
            if (echoCount >= 5 && currentSentence.length() > 80) {
                currentSentence = ""; // Clear buffer
            }
            
            // Feed to GPS parser
            if (gps.encode(c)) {
                lastGPSUpdate = millis();
                
                if (gps.location.isValid() && gps.location.isUpdated()) {
                    gpsValid = true;
                    double lat = gps.location.lat();
                    double lng = gps.location.lng();
                    int sats = gps.satellites.value();
                    float hdop = gps.hdop.hdop();
                    
                    // Check GPS quality
                    if (sats >= 3 && hdop < 8.0) {
                        // Calculate distance to safe zone
                        float distance = calculateDistance(lat, lng, safeLat, safeLng);
                        bool isInSafeZone = (distance <= dangerRadius);
                        
                        // Handle zone transitions
                        if (!isInSafeZone && wasInSafeZone) {
                            // Just left safe zone
                            if (millis() - lastAlertTime > ALERT_COOLDOWN) {
                                playDangerAlert();
                                sendAlertToFirebase("⚠ Patient left safe zone!");
                                lastAlertTime = millis();
                                Serial.println("🚨 ALERT: Left safe zone!");
                            }
                        } else if (isInSafeZone && !wasInSafeZone) {
                            // Just entered safe zone
                            playSafeAlert();
                            sendAlertToFirebase("✅ Patient returned to safe zone");
                            Serial.println("✅ SAFE: Entered safe zone!");
                        }
                        
                        wasInSafeZone = isInSafeZone;
                        
                        // Continuous alert while outside safe zone
                        if (!isInSafeZone) {
                            playContinuousAlert();
                        }
                        
                        // Send to Firebase periodically
                        if (millis() - lastFirebaseUpdate > FIREBASE_INTERVAL) {
                            sendLocationToFirebase(lat, lng, sats, hdop, !isInSafeZone);
                            lastFirebaseUpdate = millis();
                        }
                        
                        // Update display periodically
                        if (millis() - lastDisplayUpdate > DISPLAY_UPDATE_INTERVAL) {
                            displayGPSData(lat, lng, sats, hdop, distance, isInSafeZone);
                            lastDisplayUpdate = millis();
                        }
                        
                        // Serial output for debugging - show GPS data every time
                        Serial.println("\n🌍🌍🌍 GPS DATA RECEIVED 🌍🌍🌍");
                        Serial.println("═══════════════════════════════════");
                        Serial.print("📍 Latitude:  "); Serial.println(lat, 6);
                        Serial.print("📍 Longitude: "); Serial.println(lng, 6);
                        Serial.print("🛰️  Satellites: "); Serial.println(sats);
                        Serial.print("📊 HDOP:      "); Serial.println(hdop, 2);
                        Serial.print("📏 Distance to safe zone: "); Serial.print(distance, 1); Serial.println("m");
                        Serial.print("🛡️  Status:    "); Serial.println(isInSafeZone ? "✅ SAFE ZONE" : "⚠️ DANGER ZONE");
                        Serial.print("⏰ Time:      "); Serial.print(millis() / 1000); Serial.println(" seconds");
                        Serial.println("═══════════════════════════════════\n");
                    } else {
                        // Show GPS data even if quality is insufficient
                        Serial.println("\n⚠️⚠️⚠️ GPS FIX RECEIVED BUT QUALITY INSUFFICIENT ⚠️⚠️⚠️");
                        Serial.println("═══════════════════════════════════════════════════");
                        Serial.print("📍 Latitude:  "); Serial.println(lat, 6);
                        Serial.print("📍 Longitude: "); Serial.println(lng, 6);
                        Serial.print("🛰️  Satellites: "); Serial.print(sats); Serial.println(" (need ≥3)");
                        Serial.print("📊 HDOP:      "); Serial.print(hdop, 2); Serial.println(" (need <8.0)");
                        Serial.println("\n  ⚠️ This location may not be accurate!");
                        Serial.println("  Waiting for better GPS signal...");
                        Serial.println("  💡 Try moving device outdoors or near window");
                        Serial.println("═══════════════════════════════════════════════════\n");
                    }
                }
            }
        }
    } else {
        // No GPS data available - show detailed status periodically
        static unsigned long lastStatusPrint = 0;
        if (millis() - lastStatusPrint > 10000) { // Print status every 10 seconds
            Serial.println("\n📡📡📡 DETAILED GPS DIAGNOSTICS 📡📡📡");
            Serial.println("═══════════════════════════════════════");
            Serial.println("  Raw data received: " + String(gpsDataReceived ? "YES ✅" : "NO ❌"));
            Serial.println("  GPS fix valid: " + String(gpsValid ? "YES ✅" : "NO ⏳"));
            Serial.println("  Bytes received: " + String(rawDataCount));
            
            if (gpsDataReceived) {
                // Show GPS module status even without fix
                Serial.println("\n  📡 GPS Module Status:");
                Serial.print("    Satellites tracked: ");
                if (gps.satellites.isValid()) {
                    Serial.println(String(gps.satellites.value()) + " 🛰️");
                } else {
                    Serial.println("Unknown (no satellite data yet)");
                }
                
                Serial.print("    HDOP: ");
                if (gps.hdop.isValid()) {
                    Serial.println(String(gps.hdop.hdop(), 2));
                } else {
                    Serial.println("Unknown");
                }
                
                Serial.print("    Date: ");
                if (gps.date.isValid()) {
                    Serial.print(String(gps.date.month()) + "/" + String(gps.date.day()) + "/" + String(gps.date.year()));
                } else {
                    Serial.print("Invalid");
                }
                
                Serial.print("    Time: ");
                if (gps.time.isValid()) {
                    Serial.print(String(gps.time.hour()) + ":" + String(gps.time.minute()) + ":" + String(gps.time.second()));
                } else {
                    Serial.print("Invalid");
                }
                Serial.println();
                
                unsigned long waitTime = (millis() - gpsStartTime) / 1000;
                Serial.println("\n  ⏱️  Waiting time: " + String(waitTime) + " seconds");
                
                if (waitTime > 120) {
                    Serial.println("\n  ⚠️⚠️⚠️ TROUBLESHOOTING (2+ minutes waiting):");
                    Serial.println("    1. Move device OUTDOORS or near a WINDOW");
                    Serial.println("    2. Ensure clear view of SKY (no roof/metal blocking)");
                    Serial.println("    3. Check GPS module POWER (should have LED blinking)");
                    Serial.println("    4. Verify GPS wiring:");
                    Serial.println("       - GPS TX → ESP8266 D1");
                    Serial.println("       - GPS RX → ESP8266 D2");
                    Serial.println("       - GPS VCC → 3.3V");
                    Serial.println("       - GPS GND → GND");
                    Serial.println("    5. If indoors, GPS may NEVER get fix!");
                } else if (waitTime > 60) {
                    Serial.println("\n  ⏳ Still waiting... (normal for cold start)");
                    Serial.println("    Try moving device outdoors for faster fix");
                }
            } else {
                Serial.println("\n  ❌ NO GPS DATA RECEIVED!");
                Serial.println("    Check wiring:");
                Serial.println("    - GPS TX → ESP8266 D1");
                Serial.println("    - GPS VCC → 3.3V");
                Serial.println("    - GPS GND → GND");
            }
            Serial.println("═══════════════════════════════════════\n");
            lastStatusPrint = millis();
        }
        
        // Update diagnostics display
        if (millis() - lastDisplayUpdate > DISPLAY_UPDATE_INTERVAL) {
            displayGPSDiagnostics();
            lastDisplayUpdate = millis();
        }
    }
    
    // Send device status/heartbeat to Firebase periodically (even without GPS)
    // HEARTBEAT = Periodic status update that shows device is "alive" and active
    // Like your heartbeat - it proves you're alive! Same for the device.
    // This ensures device shows as "active" in Firebase even while waiting for GPS
    if (firebaseConnected && millis() - lastFirebaseUpdate > FIREBASE_INTERVAL) {
        if (!gpsValid || !gps.location.isValid()) {
            // GPS not ready yet, send heartbeat status update anyway
            sendDeviceStatusToFirebase();
            lastFirebaseUpdate = millis();
        }
    }
    
    delay(50); // Small delay for stability
}


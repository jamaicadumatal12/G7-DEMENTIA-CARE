/*
 * G7 Dementia Care - Improved ESP8266 GPS Tracker
 * Hardware: ESP8266 Round LCD Board, ATGM336H-5N GPS, Passive Buzzer
 * Features: Real-time GPS tracking, Safe zone monitoring, Firebase integration
 * Version: 2.0 - Improved with better error handling and robustness
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
const char* ssid     = "ERICBUANG";
const char* password = "JAYSONBONGS";

#define FIREBASE_HOST "g7-dementia-care-default-rtdb.asia-southeast1.firebasedatabase.app"
#define FIREBASE_AUTH "CsmqaL84uFlUjh6i9NhWCoiw0xR99JpCQfmHs4VK"

// === Device Configuration ===
String DEVICE_ID     = "G7T55ZGF5";
const int EEPROM_SIZE = 32;
const String DEVICE_PREFIX = "G7T";

// === Pin Definitions ===
#define BUZZER_PIN D1   // Passive buzzer with transistor control
#define GPS_RX D5       // GPS TX -> ESP8266 D5
#define GPS_TX D6       // GPS RX -> ESP8266 D6

// === Safe Zone Configuration ===
double safeLat     = 8.1545;  // Default safe zone center
double safeLng     = 125.1275;
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

const unsigned long GPS_TIMEOUT = 10000;        // 10 seconds
const unsigned long FIREBASE_INTERVAL = 15000;  // 15 seconds
const unsigned long BUZZER_INTERVAL = 3000;     // 3 seconds
const unsigned long WIFI_CHECK_INTERVAL = 30000; // 30 seconds
const unsigned long DISPLAY_UPDATE_INTERVAL = 2000; // 2 seconds

// === Status Flags ===
bool wifiConnected = false;
bool firebaseConnected = false;
bool gpsValid = false;
bool safeZoneLoaded = false;

// === Firebase Objects ===
FirebaseData   firebaseData;
FirebaseAuth   auth;
FirebaseConfig config;

// === GPS and Display ===
TinyGPSPlus gps;
SoftwareSerial gpsSerial(GPS_RX, GPS_TX);
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
    
    WiFi.mode(WIFI_STA);
    WiFi.begin(ssid, password);
    
    int attempts = 0;
    while (WiFi.status() != WL_CONNECTED && attempts < 20) {
        delay(1000);
        Serial.print(".");
        attempts++;
    }
    
    if (WiFi.status() == WL_CONNECTED) {
        wifiConnected = true;
        Serial.println("\nWiFi Connected!");
        Serial.print("IP: "); Serial.println(WiFi.localIP());
        displayStatus("WiFi OK", TFT_GREEN);
        delay(1000);
    } else {
        wifiConnected = false;
        Serial.println("\nWiFi Connection Failed!");
        displayStatus("WiFi FAIL", TFT_RED);
    }
}

void checkWiFiConnection() {
    if (millis() - lastWiFiCheck > WIFI_CHECK_INTERVAL) {
        if (WiFi.status() != WL_CONNECTED) {
            Serial.println("WiFi disconnected, reconnecting...");
            wifiConnected = false;
            connectToWiFi();
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
    
    if (Firebase.setJSON(firebaseData, path, json)) {
        Serial.println("Location sent to Firebase successfully");
    } else {
        Serial.println("Firebase Error: " + firebaseData.errorReason());
        firebaseConnected = false;
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

// === GPS Functions ===
float calculateDistance(double lat1, double lon1, double lat2, double lon2) {
    return TinyGPSPlus::distanceBetween(lat1, lon1, lat2, lon2);
}

void checkGPSStatus() {
    if (millis() - lastGPSUpdate > GPS_TIMEOUT) {
        gpsValid = false;
        Serial.println("GPS timeout - no signal received");
        displayStatus("GPS Timeout", TFT_RED);
    }
}

// === Main Setup ===
void setup() {
    Serial.begin(115200);
    EEPROM.begin(EEPROM_SIZE);
    
    // Initialize buzzer
    pinMode(BUZZER_PIN, OUTPUT);
    digitalWrite(BUZZER_PIN, LOW);
    
    // Setup display
    setupDisplay();
    displayDeviceId();
    playStartupSound();
    
    Serial.println("=== G7 Dementia Care Device ===");
    Serial.println("Device ID: " + DEVICE_ID);
    Serial.println("================================");
    delay(3000);
    
    // Initialize GPS
    gpsSerial.begin(9600);
    
    // Connect to WiFi and Firebase
    connectToWiFi();
    if (wifiConnected) {
        initFirebase();
    }
    
    // Setup watchdog timer
    watchdog.attach(1, resetWatchdog);
    
    Serial.println("System initialized successfully!");
}

// === Main Loop ===
void loop() {
    // Check WiFi connection
    checkWiFiConnection();
    
    // Check GPS status
    checkGPSStatus();
    
    // Process GPS data
    while (gpsSerial.available() > 0) {
        if (gps.encode(gpsSerial.read())) {
            lastGPSUpdate = millis();
            gpsValid = true;
            
            if (gps.location.isValid() && gps.location.isUpdated()) {
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
                    
                    // Serial output for debugging
                    Serial.println("=== GPS DATA ===");
                    Serial.print("LAT: "); Serial.println(lat, 6);
                    Serial.print("LNG: "); Serial.println(lng, 6);
                    Serial.print("SAT: "); Serial.println(sats);
                    Serial.print("HDOP: "); Serial.println(hdop);
                    Serial.print("DISTANCE: "); Serial.print(distance); Serial.println("m");
                    Serial.print("STATUS: "); Serial.println(isInSafeZone ? "SAFE" : "DANGER");
                    Serial.println("================");
                } else {
                    Serial.println("GPS quality insufficient");
                    Serial.print("Satellites: "); Serial.print(sats); 
                    Serial.print(", HDOP: "); Serial.println(hdop);
                }
            }
        }
    }
    
    delay(50); // Small delay for stability
}

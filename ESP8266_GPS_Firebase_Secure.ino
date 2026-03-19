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
String DEVICE_ID     = "G7T55ZGF4";
const int EEPROM_SIZE = 32;
const String DEVICE_PREFIX = "G7T";

// === Buzzer Pin ===
// Use a dedicated GPIO for the buzzer (D5 recommended). Avoid TX (GPIO1)
#define BUZZER_PIN D5

// === Buzzer Functions ===
void playDangerAlert() {
  // Three clear beeps when entering danger
  for (int i = 0; i < 3; i++) {
    tone(BUZZER_PIN, 1500); // 1.5kHz
    delay(400);
    noTone(BUZZER_PIN);
    delay(200);
  }
}

void playSafeAlert() {
  // Short confirmation beep when returning safe
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

FirebaseData   firebaseData;
FirebaseAuth   auth;
FirebaseConfig config;

// === Safe Zone Coordinates ===
double safeLat     = 7.123456;
double safeLng     = 125.123456;
float dangerRadius = 500.0; // Increased to match Android app default

// === Safe Zone Variables ===
bool safeZoneLoaded = false;
unsigned long lastSafeZoneCheck = 0;
const unsigned long SAFE_ZONE_CHECK_INTERVAL = 30000; // Check every 30 seconds

// === Alert State Management ===
bool wasInSafeZone = true; // Track previous state
bool alertTriggered = false; // Prevent repeated alerts
unsigned long lastAlertTime = 0;
const unsigned long ALERT_COOLDOWN = 30000; // 30 seconds between alerts

// === GPS Variables ===
unsigned long lastGPSUpdate = 0;
const unsigned long GPS_TIMEOUT = 5000;
bool gpsTimedOut = false;

// === Wi-Fi Status Flag ===
bool wifiConnected = false;

// === Battery Monitoring - Removed for simplicity ===

// (Heartbeat removed: only alert on danger/safe events)

// === Display Setup ===
TFT_eSPI tft = TFT_eSPI();

// === GPS and Serial Setup ===
TinyGPSPlus gps;
SoftwareSerial ss(D1, D2);

void setupDisplay() {
  tft.init();
  tft.setRotation(2);  // Changed from 0 to 2 to fix upside down text
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

// === Wi-Fi / Firebase Setup ===
void connectToWiFi() {
  displayStatus("Connecting WiFi...", TFT_YELLOW);
  const char* ssids[] = {"ERICBUANG", "BackupNetwork"};
  const char* passwords[] = {"JAYSONBONGS", "backupPassword"};
  const int maxNetworks = 2;

  WiFi.mode(WIFI_STA);
  wifiConnected = false;

  Serial.println("=== WiFi Connection Debug ===");
  Serial.print("Available networks: ");
  int numNetworks = WiFi.scanNetworks();
  Serial.println(numNetworks);
  
  for (int i = 0; i < numNetworks; i++) {
    Serial.print("Network "); Serial.print(i); Serial.print(": ");
    Serial.print(WiFi.SSID(i)); Serial.print(" (");
    Serial.print(WiFi.RSSI(i)); Serial.println(" dBm)");
  }

  for (int i = 0; i < maxNetworks; i++) {
    Serial.printf("🔍 Connecting to Wi-Fi: %s\n", ssids[i]);
    WiFi.begin(ssids[i], passwords[i]);

    int attempts = 0;
    while (WiFi.status() != WL_CONNECTED && attempts < 20) { // Increased timeout
      delay(1000);
      Serial.print(".");
      attempts++;
    }
    Serial.println();

    if (WiFi.status() == WL_CONNECTED) {
      wifiConnected = true;
      Serial.printf("✅ Connected to %s\n", ssids[i]);
      Serial.print("IP: "); Serial.println(WiFi.localIP());
      Serial.print("Signal Strength: "); Serial.print(WiFi.RSSI()); Serial.println(" dBm");
      displayStatus("WiFi OK", TFT_GREEN);
      delay(800);
      break;
    } else {
      Serial.printf("❌ Failed to connect to %s\n", ssids[i]);
      Serial.print("WiFi Status: "); Serial.println(WiFi.status());
    }
  }

  if (!wifiConnected) {
    Serial.println("❌ All Wi-Fi attempts failed.");
    Serial.println("Please check:");
    Serial.println("1. WiFi credentials are correct");
    Serial.println("2. Router is working");
    Serial.println("3. Device is within range");
    displayStatus("WiFi FAIL", TFT_RED);
  }
}

void initFirebase() {
  if (!wifiConnected) {
    Serial.println("❌ Cannot initialize Firebase - WiFi not connected!");
    return;
  }

  Serial.println("=== Firebase Connection Debug ===");
  displayStatus("Firebase...", TFT_YELLOW);
  
  config.host = FIREBASE_HOST;
  config.signer.tokens.legacy_token = FIREBASE_AUTH;
  
  Serial.print("Firebase Host: "); Serial.println(FIREBASE_HOST);
  Serial.print("Auth Token: "); Serial.print("CsmqaL84uFlUjh6i9NhWCoiw0xR99JpCQfmHs4VK"); Serial.println("...");
  
  Firebase.begin(&config, &auth);
  Firebase.reconnectWiFi(true);

  // Wait for Firebase to be ready
  int firebaseAttempts = 0;
  while (!Firebase.ready() && firebaseAttempts < 10) {
    Serial.print("Waiting for Firebase... ");
    Serial.println(firebaseAttempts + 1);
    delay(1000);
    firebaseAttempts++;
  }

  if (Firebase.ready()) {
    Serial.println("✅ Firebase connection successful");
    displayStatus("Firebase OK", TFT_GREEN);
    delay(800);

    // Test write to Firebase with device authentication
    String testPath = "/connection_test/" + DEVICE_ID;
    FirebaseJson json;
    json.set("timestamp", String(millis()));
    json.set("device_id", DEVICE_ID);
    json.set("test", "connection_test");
    json.set("auth_status", "authenticated");

    Serial.print("Testing write to: "); Serial.println(testPath);
    
    if (Firebase.setJSON(firebaseData, testPath, json)) {
      Serial.println("✅ Test write successful");
      Serial.print("Response: "); Serial.println(firebaseData.payload());
    } else {
      Serial.println("❌ Test write failed");
      Serial.print("Error: "); Serial.println(firebaseData.errorReason());
      Serial.print("Error Code: "); Serial.println(firebaseData.httpCode());
    }
  } else {
    Serial.println("❌ Firebase connection failed");
    Serial.println("Please check:");
    Serial.println("1. Firebase credentials are correct");
    Serial.println("2. Internet connection is stable");
    Serial.println("3. Firebase project is active");
    displayStatus("Firebase FAIL", TFT_RED);
  }
}

void debugFirebaseConnection() {
  Serial.println("=== FIREBASE DEBUG ===");
  Serial.println("Firebase ready: " + String(Firebase.ready()));
  Serial.println("WiFi connected: " + String(WiFi.status() == WL_CONNECTED));
  Serial.println("Device ID: " + DEVICE_ID);
  
  // Test read zones path
  String testPath = "/zones/" + DEVICE_ID;
  Serial.println("Testing path: " + testPath);
  
  if (Firebase.getString(firebaseData, testPath)) {
    Serial.println("✅ Path accessible");
    Serial.println("Data: " + firebaseData.stringData());
  } else {
    Serial.println("❌ Path not accessible");
    Serial.println("Error: " + firebaseData.errorReason());
    Serial.println("HTTP Code: " + String(firebaseData.httpCode()));
  }
  
  Serial.println("=====================");
}

void sendLocationToFirebase(double lat, double lng, int sats, float hdop, bool isDanger) {
  if (!Firebase.ready()) {
    Serial.println("Firebase not ready!");
    displayStatus("FB Not Ready", TFT_YELLOW);
    return;
  }

  // Validate GPS data before sending
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
  json.set("batteryLevel", 100); // Fixed battery level for simplicity
  json.set("timestamp", millis());
  json.set("deviceId", DEVICE_ID); // Add deviceId field for validation

  if (Firebase.setJSON(firebaseData, path, json)) {
    Serial.println("✅ Location sent successfully");
  } else {
    Serial.println("❌ Firebase error: " + firebaseData.errorReason());
    Serial.println("HTTP Code: " + String(firebaseData.httpCode()));
    // Retry logic could be added here
  }
}

void loadSafeZoneFromFirebase() {
  if (!Firebase.ready()) {
    Serial.println("Firebase not ready for safe zone loading!");
    return;
  }

  String path = "/zones/" + DEVICE_ID;
  Serial.println("Loading safe zone from path: " + path);
  
  if (Firebase.getJSON(firebaseData, path)) {
    FirebaseJson &json = firebaseData.jsonObject();
    FirebaseJsonData jsonData;
    
    Serial.println("Raw JSON response: " + firebaseData.jsonString());
    
    // Check if we got valid data
    if (firebaseData.jsonString() == "null" || firebaseData.jsonString().length() == 0) {
      Serial.println("No safe zone data found for device " + DEVICE_ID);
      return;
    }
    
    // Read zone data directly (not nested)
    bool foundSafeZone = false;
    
    if (json.get(jsonData, "type")) {
      String zoneType = jsonData.stringValue;
      Serial.println("Zone type: " + zoneType);
      
      if (zoneType == "safe") {
        // Get latitude
        if (json.get(jsonData, "latitude")) {
          safeLat = jsonData.doubleValue;
          Serial.println("Latitude: " + String(safeLat, 6));
        }
        
        // Get longitude
        if (json.get(jsonData, "longitude")) {
          safeLng = jsonData.doubleValue;
          Serial.println("Longitude: " + String(safeLng, 6));
        }
        
        // Get radius
        if (json.get(jsonData, "radius")) {
          dangerRadius = jsonData.doubleValue;
          Serial.println("Radius: " + String(dangerRadius));
        }
        
        safeZoneLoaded = true;
        foundSafeZone = true;
        
        Serial.println("✅ Safe zone loaded successfully:");
        Serial.println("  Center: " + String(safeLat, 6) + ", " + String(safeLng, 6));
        Serial.println("  Radius: " + String(dangerRadius) + "m");
        
        displayStatus("Zone Loaded", TFT_GREEN);
        delay(1000);
      }
    }
    
    if (!foundSafeZone) {
      Serial.println("❌ No valid safe zone found in Firebase data");
      safeZoneLoaded = false;
    }
    
  } else {
    Serial.println("❌ Failed to load safe zone from Firebase");
    Serial.println("Error: " + firebaseData.errorReason());
    Serial.println("HTTP Code: " + String(firebaseData.httpCode()));
    safeZoneLoaded = false;
  }
}

void setup() {
  Serial.begin(115200);
  EEPROM.begin(EEPROM_SIZE);
  
  // Initialize buzzer
  pinMode(BUZZER_PIN, OUTPUT);
  digitalWrite(BUZZER_PIN, LOW);
  
  setupDisplay();
  displayDeviceId();
  playStartupSound();
  // Test chirp already played in playStartupSound(); no extra test tones

  Serial.println("=== G7 Dementia Care Device ===");
  Serial.println("Device ID: " + DEVICE_ID);
  Serial.println("================================");
  delay(3000);

  ss.begin(9600);
  connectToWiFi();
  initFirebase();
  
  // Add debug after Firebase init
  delay(2000);
  debugFirebaseConnection();
  
  loadSafeZoneFromFirebase();
  
  // Setup watchdog timer
  watchdog.attach(1, resetWatchdog); // Reset every second
}

void loop() {
  if (WiFi.status() != WL_CONNECTED) {
    Serial.println("WiFi disconnected!");
    connectToWiFi();
  }

  // No periodic tones; buzzer only sounds on danger/safe transitions

  // Periodically check for safe zone updates
  if (millis() - lastSafeZoneCheck > SAFE_ZONE_CHECK_INTERVAL) {
    loadSafeZoneFromFirebase();
    lastSafeZoneCheck = millis();
  }

  // Battery monitoring removed for simplicity

  // Process GPS data immediately - no delays
  while (ss.available() > 0) {
    if (gps.encode(ss.read())) {
      lastGPSUpdate = millis();

      if (gps.location.isValid() && gps.location.isUpdated()) {
        double lat = gps.location.lat();
        double lng = gps.location.lng();
        int sats = gps.satellites.value();
        float hdop = gps.hdop.hdop();
        
        // Check GPS quality before processing
        if (sats >= 3 && hdop < 8.0) {
          Serial.println("=== GPS DATA VALID ===");
          Serial.print("Latitude: "); Serial.println(lat, 6);
          Serial.print("Longitude: "); Serial.println(lng, 6);
          Serial.print("Satellites: "); Serial.println(sats);
          Serial.print("HDOP: "); Serial.println(hdop);
          
          // Check if we have a valid safe zone loaded
          bool isInSafeZone = true; // Default to safe if no zone loaded
          float distance = 0;
          
          if (safeZoneLoaded) {
            // Use accurate distance calculation
            distance = TinyGPSPlus::distanceBetween(lat, lng, safeLat, safeLng);
            isInSafeZone = (distance <= dangerRadius);
            
            Serial.print("Distance to safe zone: "); Serial.print(distance); 
            Serial.print("m (radius: "); Serial.print(dangerRadius); Serial.println("m)");
            
            // Handle state changes and alerts
            if (!isInSafeZone && wasInSafeZone) {
              // Just left safe zone
              if (millis() - lastAlertTime > ALERT_COOLDOWN) {
                playDangerAlert();
                alertTriggered = true;
                lastAlertTime = millis();
                Serial.println("🚨 ALERT: Left safe zone!");
              }
            } else if (isInSafeZone && !wasInSafeZone) {
              // Just entered safe zone
              playSafeAlert();
              alertTriggered = false;
              Serial.println("✅ SAFE: Entered safe zone!");
            }
            
            wasInSafeZone = isInSafeZone;
          } else {
            Serial.println("No safe zone loaded - defaulting to SAFE");
          }
          
          // Send location to Firebase
          Serial.println("=== SENDING TO FIREBASE ===");
          sendLocationToFirebase(lat, lng, sats, hdop, !isInSafeZone);

          // Print status to Serial Monitor
          Serial.println("=== GPS DATA ===");
          Serial.print("LAT: "); Serial.println(lat, 6);
          Serial.print("LNG: "); Serial.println(lng, 6);
          Serial.print("SAT: "); Serial.println(sats);
          Serial.print("HDOP: "); Serial.println(hdop);
          Serial.print("DISTANCE: "); Serial.print(distance); Serial.println("m");
          Serial.print("STATUS: "); Serial.println(isInSafeZone ? "SAFE" : "DANGER");
          Serial.println("================");

          // Update display
          tft.fillScreen(TFT_BLACK);
          displayCenteredText("GPS Data", 30, TFT_WHITE, 3);
          displayCenteredText("LAT:", 80, TFT_WHITE, 2);
          displayCenteredText(String(lat, 6).c_str(), 110, TFT_WHITE, 2);
          displayCenteredText("LNG:", 140, TFT_WHITE, 2);
          displayCenteredText(String(lng, 6).c_str(), 170, TFT_WHITE, 2);
          displayCenteredText("SAT:", 200, TFT_WHITE, 2);
          displayCenteredText(String(sats).c_str(), 230, TFT_WHITE, 2);
          displayCenteredText("HDOP:", 260, TFT_WHITE, 2);
          displayCenteredText(String(hdop).c_str(), 290, TFT_WHITE, 2);
          displayCenteredText("DISTANCE:", 320, TFT_WHITE, 2);
          displayCenteredText(String(distance, 1).c_str(), 350, TFT_WHITE, 2);
          displayCenteredText("BAT: 100%", 380, TFT_GREEN, 2);
          displayCenteredText(isInSafeZone ? "SAFE" : "DANGER", 440, isInSafeZone ? TFT_GREEN : TFT_RED, 3);
        } else {
          Serial.println("GPS quality insufficient - waiting for better signal");
          Serial.print("Satellites: "); Serial.print(sats); Serial.print(", HDOP: "); Serial.println(hdop);
          displayStatus("Waiting GPS fix", TFT_YELLOW);
        }
      }
    }
  }

  if (millis() - lastGPSUpdate > GPS_TIMEOUT) {
    Serial.println("GPS Error!");
    displayStatus("GPS Error", TFT_RED);
    delay(1000);
  } else {
    // Show GPS status while waiting
    static unsigned long lastStatusUpdate = 0;
    if (millis() - lastStatusUpdate > 2000) {
      displayStatus("Waiting GPS fix", TFT_YELLOW);
      lastStatusUpdate = millis();
    }
  }

  delay(50); // Reduced delay for better GPS processing
}

#define DISABLE_FIREBASE_SD

#include <ESP8266WiFi.h>
#include <FirebaseESP8266.h>
#include <TinyGPSPlus.h>
#include <SoftwareSerial.h>
#include <TFT_eSPI.h>
#include <SPI.h>
#include <EEPROM.h>

// === Wi-Fi and Firebase Credentials ===
const char* ssid     = "ERICBUANG";
const char* password = "JAYSONBONGS";

#define FIREBASE_HOST "g7-dementia-care-default-rtdb.asia-southeast1.firebasedatabase.app"
#define FIREBASE_AUTH "CsmqaL84uFlUjh6i9NhWCoiw0xR99JpCQfmHs4VK"

// === Device Configuration ===
String DEVICE_ID     = "G7T55ZGF5";
const int EEPROM_SIZE = 32;
const String DEVICE_PREFIX = "G7T";

// === Buzzer Pin ===
#define BUZZER_PIN D5

// === Buzzer Functions ===
void playDangerAlert() {
  for (int i = 0; i < 3; i++) {
    digitalWrite(BUZZER_PIN, HIGH);
    delay(500);
    digitalWrite(BUZZER_PIN, LOW);
    delay(500);
  }
}

void playSafeAlert() {
  digitalWrite(BUZZER_PIN, HIGH);
  delay(200);
  digitalWrite(BUZZER_PIN, LOW);
}

void playStartupSound() {
  digitalWrite(BUZZER_PIN, HIGH);
  delay(100);
  digitalWrite(BUZZER_PIN, LOW);
  delay(100);
  digitalWrite(BUZZER_PIN, HIGH);
  delay(100);
  digitalWrite(BUZZER_PIN, LOW);
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

// === Battery Monitoring ===
#define BATTERY_PIN A0
unsigned long lastBatteryCheck = 0;
const unsigned long BATTERY_CHECK_INTERVAL = 60000; // Check every minute
int batteryLevel = 100;

int getBatteryLevel() {
  int rawValue = analogRead(BATTERY_PIN);
  // Convert to percentage (adjust these values based on your battery)
  int percentage = map(rawValue, 0, 1023, 0, 100);
  return constrain(percentage, 0, 100);
}

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
  }
}

void initFirebase() {
  if (!wifiConnected) {
    Serial.println("❌ Cannot initialize Firebase - WiFi not connected!");
    return;
  }

  Serial.println("=== Firebase Connection Debug ===");
  
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

    // Test write to Firebase
    String testPath = "/connection_test/" + DEVICE_ID;
    FirebaseJson json;
    json.set("timestamp", String(millis()));
    json.set("device_id", DEVICE_ID);
    json.set("test", "connection_test");

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
  }
}

void sendLocationToFirebase(double lat, double lng, int sats, float hdop, bool isDanger) {
  if (!Firebase.ready()) {
    Serial.println("Firebase not ready!");
    return;
  }

  String path = "/devices/" + DEVICE_ID;
  FirebaseJson json;
  json.set("latitude", lat);
  json.set("longitude", lng);
  json.set("satellites", sats);
  json.set("hdop", hdop);
  json.set("inDanger", isDanger);
  json.set("batteryLevel", batteryLevel);
  json.set("timestamp", millis());  // Changed from String(millis()) to millis()

  if (Firebase.setJSON(firebaseData, path, json)) {
    Serial.println("✅ Location sent to path: " + path);
    Serial.print("LAT: "); Serial.println(lat, 6);
    Serial.print("LNG: "); Serial.println(lng, 6);
    Serial.print("BAT: "); Serial.print(batteryLevel); Serial.println("%");
  } else {
    Serial.println("❌ Firebase error: " + firebaseData.errorReason());
  }
}

void loadSafeZoneFromFirebase() {
  if (!Firebase.ready()) {
    Serial.println("Firebase not ready for safe zone loading!");
    return;
  }

  String path = "/zones/" + DEVICE_ID;
  
  if (Firebase.getJSON(firebaseData, path)) {
    String jsonString = firebaseData.jsonString();
    Serial.println("Raw JSON: " + jsonString);
    
    if (jsonString.length() > 0 && jsonString != "null") {
      // Parse the JSON manually for simplicity
      FirebaseJson json;
      json.setJsonData(jsonString);
      
      // Try to get the first zone
      FirebaseJsonData jsonData;
      bool foundSafeZone = false;
      
      // Parse keys to find safe zone - simplified approach
      int keyCount = json.iteratorBegin();
      for (int i = 0; i < keyCount; i++) {
        int type;
        String key, value;
        json.iteratorGet(i, type, key, value);
        Serial.println("Checking key: " + key);
        
        // Get zone type
        json.get(jsonData, key + "/type");
        if (jsonData.stringValue == "safe") {
          // Get zone data
          json.get(jsonData, key + "/latitude");
          if (jsonData.doubleValue != 0) {
            safeLat = jsonData.doubleValue;
          }
          
          json.get(jsonData, key + "/longitude");
          if (jsonData.doubleValue != 0) {
            safeLng = jsonData.doubleValue;
          }
          
          json.get(jsonData, key + "/radius");
          if (jsonData.intValue > 0) {
            dangerRadius = jsonData.intValue;
          }
          
          safeZoneLoaded = true;
          foundSafeZone = true;
          Serial.println("✅ Safe zone loaded from Firebase:");
          Serial.print("  Center: "); Serial.print(safeLat, 6); Serial.print(", "); Serial.println(safeLng, 6);
          Serial.print("  Radius: "); Serial.print(dangerRadius); Serial.println("m");
          break;
        }
      }
      json.iteratorEnd();
      
      if (!foundSafeZone) {
        Serial.println("No safe zone found in Firebase data");
      }
    } else {
      Serial.println("No zones data found for this device");
    }
  } else {
    Serial.println("❌ Failed to load safe zone: " + firebaseData.errorReason());
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

  Serial.println("=== G7 Dementia Care Device ===");
  Serial.println("Device ID: " + DEVICE_ID);
  Serial.println("================================");
  delay(3000);

  ss.begin(9600);
  connectToWiFi();
  initFirebase();
  loadSafeZoneFromFirebase(); // Load safe zone on startup
}

void loop() {
  if (WiFi.status() != WL_CONNECTED) {
    Serial.println("WiFi disconnected!");
    connectToWiFi();
  }

  // Periodically check for safe zone updates
  if (millis() - lastSafeZoneCheck > SAFE_ZONE_CHECK_INTERVAL) {
    loadSafeZoneFromFirebase();
    lastSafeZoneCheck = millis();
  }

  // Check battery level periodically
  if (millis() - lastBatteryCheck > BATTERY_CHECK_INTERVAL) {
    batteryLevel = getBatteryLevel();
    lastBatteryCheck = millis();
    Serial.print("Battery Level: "); Serial.print(batteryLevel); Serial.println("%");
  }

  while (ss.available() > 0) {
    if (gps.encode(ss.read())) {
      lastGPSUpdate = millis();

      if (gps.location.isValid() && gps.location.isUpdated()) {
        double lat = gps.location.lat();
        double lng = gps.location.lng();
        int sats = gps.satellites.value();
        float hdop = gps.hdop.hdop();
        
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
        displayCenteredText("BAT:", 380, TFT_WHITE, 2);
        String batteryText = String(batteryLevel) + "%";
        displayCenteredText(batteryText.c_str(), 410, batteryLevel > 20 ? TFT_GREEN : TFT_RED, 2);
        displayCenteredText(isInSafeZone ? "SAFE" : "DANGER", 440, isInSafeZone ? TFT_GREEN : TFT_RED, 3);
      }
    }
  }

  if (millis() - lastGPSUpdate > GPS_TIMEOUT) {
    Serial.println("GPS Error!");
    delay(2000);
  }

  delay(100);
}
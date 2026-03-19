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
String DEVICE_ID     = "G7T55ZGF1";
const int EEPROM_SIZE = 32;
const String DEVICE_PREFIX = "G7T";

// === Buzzer Pin ===
#define BUZZER_PIN D5

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

// === GPS Variables ===
unsigned long lastGPSUpdate = 0;
const unsigned long GPS_TIMEOUT = 5000;
bool gpsTimedOut = false;

// === Wi-Fi Status Flag ===
bool wifiConnected = false;

// === Display Setup ===
TFT_eSPI tft = TFT_eSPI();

// === GPS and Serial Setup ===
TinyGPSPlus gps;
SoftwareSerial ss(D1, D2);

void setupDisplay() {
  tft.init();
  tft.setRotation(0);
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

  for (int i = 0; i < maxNetworks; i++) {
    Serial.printf("🔍 Connecting to Wi-Fi: %s\n", ssids[i]);
    WiFi.begin(ssids[i], passwords[i]);

    String statusMsg = "WiFi: " + String(ssids[i]);
    displayStatus(statusMsg.c_str(), TFT_YELLOW);

    int attempts = 0;
    while (WiFi.status() != WL_CONNECTED && attempts < 10) {
      delay(1000);
      Serial.print(".");
      attempts++;
    }
    Serial.println();

    if (WiFi.status() == WL_CONNECTED) {
      wifiConnected = true;
      Serial.printf("✅ Connected to %s\n", ssids[i]);
      Serial.print("IP: "); Serial.println(WiFi.localIP());
      displayStatus("WiFi OK", TFT_GREEN);
      delay(1000);
      break;
    } else {
      Serial.printf("❌ Failed to connect to %s\n", ssids[i]);
    }
  }

  if (!wifiConnected) {
    Serial.println("❌ All Wi-Fi attempts failed.");
    displayStatus("WiFi Failed!", TFT_RED);
    delay(2000);
  }
}

void initFirebase() {
  if (!wifiConnected) return;

  config.host = FIREBASE_HOST;
  config.signer.tokens.legacy_token = FIREBASE_AUTH;
  Firebase.begin(&config, &auth);
  Firebase.reconnectWiFi(true);

  if (Firebase.ready()) {
    Serial.println("✅ Firebase connection successful");
    displayStatus("Firebase OK", TFT_GREEN);
    delay(500);

    String testPath = "/connection_test";
    FirebaseJson json;
    json.set("timestamp", String(millis()));

    if (Firebase.setJSON(firebaseData, testPath, json)) {
      Serial.println("✅ Test write successful");
    } else {
      Serial.println("❌ Test write failed: " + firebaseData.errorReason());
    }
  } else {
    Serial.println("❌ Firebase connection failed");
    displayStatus("Firebase Err!", TFT_RED);
    delay(2000);
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
  json.set("timestamp", String(millis()));

  if (Firebase.setJSON(firebaseData, path, json)) {
    Serial.println("✅ Location sent to path: " + path);
    Serial.print("LAT: "); Serial.println(lat, 6);
    Serial.print("LNG: "); Serial.println(lng, 6);
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
      
      // Get all keys in the JSON
      String keys = json.keys();
      Serial.println("Keys: " + keys);
      
      // Parse keys to find safe zone
      int keyCount = json.iteratorBegin();
      for (int i = 0; i < keyCount; i++) {
        String key = json.iteratorKey(i);
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
  
  setupDisplay();

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

  while (ss.available() > 0) {
    if (gps.encode(ss.read())) {
      lastGPSUpdate = millis();

      if (gps.location.isValid() && gps.location.isUpdated()) {
        double lat = gps.location.lat();
        double lng = gps.location.lng();
        int sats = gps.satellites.value();
        float hdop = gps.hdop.hdop();
        
        // Check if we have a valid safe zone loaded
        bool isInSafeZone = true; // Default to safe if no zone loaded
        float distance = 0;
        
        if (safeZoneLoaded) {
          // Use accurate distance calculation
          distance = TinyGPSPlus::distanceBetween(lat, lng, safeLat, safeLng);
          isInSafeZone = (distance <= dangerRadius);
          
          Serial.print("Distance to safe zone: "); Serial.print(distance); 
          Serial.print("m (radius: "); Serial.print(dangerRadius); Serial.println("m)");
        } else {
          Serial.println("No safe zone loaded - defaulting to SAFE");
        }
        
        // Send location to Firebase
        sendLocationToFirebase(lat, lng, sats, hdop, !isInSafeZone);

        // Update display with location info and safe/danger status
        tft.fillScreen(!isInSafeZone ? TFT_RED : TFT_GREEN);
        
        // Display Device ID
        tft.setTextSize(2);
        int16_t idX = (tft.width() - (DEVICE_ID.length() * 12)) / 2;
        displayText(DEVICE_ID.c_str(), idX, 20, TFT_BLACK);
        
        // Display LAT/LNG
        char buffer[20];
        tft.setTextSize(2);
        displayText("LAT:", 10, 60, TFT_BLACK);
        sprintf(buffer, "%.6f", lat);
        displayText(buffer, 10, 80, TFT_WHITE);
        
        displayText("LNG:", 10, 110, TFT_BLACK);
        sprintf(buffer, "%.6f", lng);
        displayText(buffer, 10, 130, TFT_WHITE);
        
        // Display distance if safe zone is loaded
        if (safeZoneLoaded) {
          displayText("DIST:", 10, 160, TFT_BLACK);
          sprintf(buffer, "%.0fm", distance);
          displayText(buffer, 10, 180, TFT_WHITE);
        }
        
        // Display status
        displayText(!isInSafeZone ? "DANGER!" : "SAFE", 10, 200, TFT_BLACK);

        // Print status to Serial Monitor
        Serial.println("=== GPS DATA ===");
        Serial.print("LAT: "); Serial.println(lat, 6);
        Serial.print("LNG: "); Serial.println(lng, 6);
        Serial.print("SAT: "); Serial.println(sats);
        Serial.print("HDOP: "); Serial.println(hdop);
        Serial.print("DISTANCE: "); Serial.print(distance); Serial.println("m");
        Serial.print("STATUS: "); Serial.println(isInSafeZone ? "SAFE" : "DANGER");
        Serial.println("================");
      }
    }
  }

  if (millis() - lastGPSUpdate > GPS_TIMEOUT) {
    displayStatus("GPS Error!", TFT_RED);
    delay(2000);
    displayDeviceId();
  }

  delay(100);
} 
#define DISABLE_FIREBASE_SD

#include <ESP8266WiFi.h>
#include <FirebaseESP8266.h>
#include <TinyGPSPlus.h>
#include <SoftwareSerial.h>
#include <LovyanGFX.hpp>
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
float dangerRadius = 20.0;

// === GPS Variables ===
unsigned long lastGPSUpdate = 0;
const unsigned long GPS_TIMEOUT = 5000;
bool gpsTimedOut = false;

// === Wi-Fi Status Flag ===
bool wifiConnected = false;

// === Display Setup ===
class LGFX : public lgfx::LGFX_Device {
  lgfx::Panel_GC9A01 _panel;
  lgfx::Bus_SPI      _bus;
public:
  LGFX(void) {
    {
      auto cfg = _bus.config();
      cfg.spi_mode   = 0;
      cfg.freq_write = 40000000;
      cfg.freq_read  = 16000000;
      cfg.spi_3wire  = true;
      cfg.pin_sclk   = 14;
      cfg.pin_mosi   = 13;
      cfg.pin_miso   = -1;
      cfg.pin_dc     = 2;
      _bus.config(cfg);
      _panel.setBus(&_bus);
    }
    {
      auto cfg = _panel.config();
      cfg.pin_cs          = 15;
      cfg.pin_rst         = 0;
      cfg.pin_busy        = -1;
      cfg.panel_width     = 240;
      cfg.panel_height    = 240;
      cfg.offset_x        = 0;
      cfg.offset_y        = 0;
      cfg.offset_rotation = 0;
      cfg.dummy_read_pixel = 8;
      cfg.dummy_read_bits  = 1;
      cfg.invert          = true;
      _panel.config(cfg);
    }
    setPanel(&_panel);
  }
};

LGFX tft;
TinyGPSPlus gps;
SoftwareSerial ss(D1, D2);

// === Display Helpers ===
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

void fetchDangerRadiusFromFirebase() {
  if (!wifiConnected) return;
  String path = "/patients/" + DEVICE_ID + "/radius";
  if (Firebase.getFloat(firebaseData, path)) {
    dangerRadius = firebaseData.floatData();
    Serial.print("📏 Radius from Firebase: ");
    Serial.println(dangerRadius);
  } else {
    Serial.print("⚠️ Failed to fetch radius: ");
    Serial.pri
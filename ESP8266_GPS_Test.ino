#include <SoftwareSerial.h>
#include <TinyGPSPlus.h>
#include <TFT_eSPI.h>

// GPS and Serial Setup
TinyGPSPlus gps;
SoftwareSerial ss(D1, D2); // TX, RX pins

// Display Setup
TFT_eSPI tft = TFT_eSPI();

void setupDisplay() {
  tft.init();
  tft.setRotation(2);
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

void setup() {
  Serial.begin(115200);
  ss.begin(9600);
  
  setupDisplay();
  displayCenteredText("GPS Test", 50, TFT_WHITE, 3);
  displayCenteredText("Waiting for GPS...", 100, TFT_YELLOW, 2);
  
  Serial.println("GPS Test Starting...");
  Serial.println("Make sure GPS module is connected:");
  Serial.println("VCC -> 3.3V");
  Serial.println("GND -> GND");
  Serial.println("TX -> D1 (GPIO5)");
  Serial.println("RX -> D2 (GPIO4)");
}

void loop() {
  while (ss.available() > 0) {
    if (gps.encode(ss.read())) {
      if (gps.location.isValid()) {
        double lat = gps.location.lat();
        double lng = gps.location.lng();
        int sats = gps.satellites.value();
        float hdop = gps.hdop.hdop();
        
        Serial.println("=== GPS DATA ===");
        Serial.print("Latitude: "); Serial.println(lat, 6);
        Serial.print("Longitude: "); Serial.println(lng, 6);
        Serial.print("Satellites: "); Serial.println(sats);
        Serial.print("HDOP: "); Serial.println(hdop);
        Serial.println("================");
        
        // Update display
        tft.fillScreen(TFT_BLACK);
        displayCenteredText("GPS FOUND!", 50, TFT_GREEN, 3);
        displayCenteredText("LAT:", 100, TFT_WHITE, 2);
        displayCenteredText(String(lat, 6).c_str(), 130, TFT_WHITE, 2);
        displayCenteredText("LNG:", 160, TFT_WHITE, 2);
        displayCenteredText(String(lng, 6).c_str(), 190, TFT_WHITE, 2);
        displayCenteredText("SAT:", 220, TFT_WHITE, 2);
        displayCenteredText(String(sats).c_str(), 250, TFT_WHITE, 2);
        displayCenteredText("HDOP:", 280, TFT_WHITE, 2);
        displayCenteredText(String(hdop).c_str(), 310, TFT_WHITE, 2);
        
        delay(5000); // Show for 5 seconds
      } else {
        // Show GPS status
        tft.fillScreen(TFT_BLACK);
        displayCenteredText("GPS Test", 50, TFT_WHITE, 3);
        displayCenteredText("Waiting for GPS...", 100, TFT_YELLOW, 2);
        displayCenteredText("Move outdoors!", 150, TFT_RED, 2);
        displayCenteredText("Clear sky view needed", 180, TFT_RED, 2);
      }
    }
  }
  
  // Show timeout after 30 seconds
  static unsigned long startTime = millis();
  if (millis() - startTime > 30000) {
    tft.fillScreen(TFT_BLACK);
    displayCenteredText("GPS Test", 50, TFT_WHITE, 3);
    displayCenteredText("No GPS signal", 100, TFT_RED, 2);
    displayCenteredText("Check connections:", 150, TFT_YELLOW, 2);
    displayCenteredText("VCC->3.3V", 180, TFT_WHITE, 2);
    displayCenteredText("GND->GND", 210, TFT_WHITE, 2);
    displayCenteredText("TX->D1", 240, TFT_WHITE, 2);
    displayCenteredText("RX->D2", 270, TFT_WHITE, 2);
  }
  
  delay(100);
}

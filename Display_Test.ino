#include <SPI.h>
#include <TFT_eSPI.h>

TFT_eSPI tft = TFT_eSPI();

void setup() {
  Serial.begin(115200);
  Serial.println("Display Test Starting...");
  
  tft.init();
  tft.setRotation(0);
  tft.fillScreen(TFT_BLACK);
  
  Serial.println("Display initialized");
  
  // Test basic display
  tft.setTextColor(TFT_WHITE, TFT_BLACK);
  tft.setTextSize(2);
  tft.setCursor(10, 10);
  tft.println("Display Test");
  
  tft.setTextSize(1);
  tft.setCursor(10, 50);
  tft.println("If you can see this,");
  tft.setCursor(10, 70);
  tft.println("display is working!");
  
  // Test colors
  tft.fillRect(10, 100, 50, 30, TFT_RED);
  tft.fillRect(70, 100, 50, 30, TFT_GREEN);
  tft.fillRect(130, 100, 50, 30, TFT_BLUE);
  
  Serial.println("Display test complete");
}

void loop() {
  // Blink the text
  tft.setTextColor(TFT_YELLOW, TFT_BLACK);
  tft.setCursor(10, 150);
  tft.println("Working!");
  delay(1000);
  
  tft.setTextColor(TFT_WHITE, TFT_BLACK);
  tft.setCursor(10, 150);
  tft.println("Working!");
  delay(1000);
} 
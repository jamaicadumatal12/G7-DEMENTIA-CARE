/*
 * Clear Display Test - This will clear the display if upload works
 */

#include <TFT_eSPI.h>

TFT_eSPI tft = TFT_eSPI();

void setup() {
    Serial.begin(115200);
    delay(1000);
    
    Serial.println("=== CLEAR DISPLAY TEST ===");
    Serial.println("If you see this message, upload worked!");
    
    // Initialize display
    tft.init();
    tft.setRotation(2);
    tft.fillScreen(TFT_BLACK);  // Clear the display completely
    tft.setTextColor(TFT_WHITE, TFT_BLACK);
    tft.setTextSize(2);
    tft.setCursor(10, 50);
    tft.print("UPLOAD WORKED!");
    
    Serial.println("Display cleared and message written");
}

void loop() {
    // Blink a dot to show it's running
    static bool dot = false;
    static unsigned long lastBlink = 0;
    
    if (millis() - lastBlink > 1000) {
        dot = !dot;
        tft.fillCircle(200, 100, 5, dot ? TFT_GREEN : TFT_BLACK);
        lastBlink = millis();
        
        if (dot) {
            Serial.println("Running...");
        }
    }
}

/*
 * SIMPLE GPS TEST - Based on your OLD WORKING CODE
 * This uses D1/D2 pins like your old working code
 * Upload this and check Serial Monitor for GPS data
 */

#include <SoftwareSerial.h>
#include <TinyGPSPlus.h>

// Use D1/D2 like your OLD WORKING CODE
TinyGPSPlus gps;
SoftwareSerial gpsSerial(D1, D2); // RX, TX - Same as your old working code!

void setup() {
    Serial.begin(115200);
    delay(1000);
    
    Serial.println("\n\n========================================");
    Serial.println("   SIMPLE GPS TEST");
    Serial.println("========================================");
    Serial.println("Using D1/D2 pins (like your old code)");
    Serial.println("GPS TX -> ESP8266 D1");
    Serial.println("GPS RX -> ESP8266 D2");
    Serial.println("========================================\n");
    
    // Initialize GPS at 9600 baud
    gpsSerial.begin(9600);
    delay(500);
    
    Serial.println("Waiting for GPS data...");
    Serial.println("(You should see data within 1-2 seconds)\n");
    Serial.println("Raw GPS data:");
    Serial.println("----------------------------------------");
}

void loop() {
    // Read and display ALL raw GPS data
    if (gpsSerial.available() > 0) {
        while (gpsSerial.available() > 0) {
            char c = gpsSerial.read();
            Serial.print(c); // Print raw data
            
            // Also feed to GPS parser
            if (gps.encode(c)) {
                // GPS data parsed successfully
                if (gps.location.isValid()) {
                    Serial.println("\n----------------------------------------");
                    Serial.println("✅ GPS FIX OBTAINED!");
                    Serial.print("Latitude: ");
                    Serial.println(gps.location.lat(), 6);
                    Serial.print("Longitude: ");
                    Serial.println(gps.location.lng(), 6);
                    Serial.print("Satellites: ");
                    Serial.println(gps.satellites.value());
                    Serial.print("HDOP: ");
                    Serial.println(gps.hdop.hdop(), 1);
                    Serial.println("----------------------------------------\n");
                }
            }
        }
    } else {
        // No data - show status every 5 seconds
        static unsigned long lastStatus = 0;
        if (millis() - lastStatus > 5000) {
            Serial.println("\n⚠ Still waiting for GPS data...");
            Serial.println("Check:");
            Serial.println("  1. GPS TX -> ESP8266 D1");
            Serial.println("  2. GPS RX -> ESP8266 D2");
            Serial.println("  3. GPS VCC -> 3.3V");
            Serial.println("  4. GPS GND -> GND");
            Serial.println();
            lastStatus = millis();
        }
    }
    
    delay(10);
}


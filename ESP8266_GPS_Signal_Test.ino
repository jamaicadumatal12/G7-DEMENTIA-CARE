/*
 * GPS SIGNAL RECEPTION TEST
 * 
 * Simple test to check if your GPS module is receiving satellite signals
 * 
 * CONNECTIONS:
 * GPS VCC  -> ESP8266 3.3V
 * GPS GND  -> ESP8266 GND
 * GPS TX   -> ESP8266 D1
 * GPS RX   -> ESP8266 D2
 * 
 * Upload and open Serial Monitor at 115200 baud
 */

#include <SoftwareSerial.h>
#include <TinyGPSPlus.h>

SoftwareSerial gpsSerial(D1, D2); // RX, TX
TinyGPSPlus gps;

unsigned long startTime = 0;
int dataCount = 0;
bool gotFix = false;

void setup() {
    Serial.begin(115200);
    delay(1000);
    
    Serial.println("\n\n╔══════════════════════════════════════════════════════╗");
    Serial.println("║         GPS SIGNAL RECEPTION TEST                    ║");
    Serial.println("╚══════════════════════════════════════════════════════╝\n");
    
    Serial.println("Initializing GPS...");
    gpsSerial.begin(9600);
    delay(500);
    
    startTime = millis();
    Serial.println("Testing GPS signal reception...\n");
    Serial.println("═══════════════════════════════════════════════════════\n");
}

void loop() {
    // Read GPS data
    if (gpsSerial.available() > 0) {
        char c = gpsSerial.read();
        dataCount++;
        
        if (gps.encode(c)) {
            // Check for GPS fix
            if (gps.location.isValid()) {
                if (!gotFix) {
                    gotFix = true;
                    unsigned long timeToFix = (millis() - startTime) / 1000;
                    
                    Serial.println("\n╔══════════════════════════════════════════════════════╗");
                    Serial.println("║              ✅ GPS SIGNAL RECEIVED!                 ║");
                    Serial.println("╚══════════════════════════════════════════════════════╝\n");
                    
                    Serial.println("📍 LOCATION:");
                    Serial.print("   Latitude:  ");
                    Serial.println(gps.location.lat(), 6);
                    Serial.print("   Longitude: ");
                    Serial.println(gps.location.lng(), 6);
                    
                    Serial.println("\n📡 SIGNAL QUALITY:");
                    Serial.print("   Satellites: ");
                    int sats = gps.satellites.value();
                    Serial.print(sats);
                    if (sats >= 4) {
                        Serial.println(" ✅ EXCELLENT");
                    } else if (sats >= 3) {
                        Serial.println(" ⚠️  FAIR (need more)");
                    } else {
                        Serial.println(" ❌ POOR (need at least 3-4)");
                    }
                    
                    Serial.print("   HDOP: ");
                    float hdop = gps.hdop.hdop();
                    Serial.print(hdop, 1);
                    if (hdop < 2.0) {
                        Serial.println(" ✅ EXCELLENT");
                    } else if (hdop < 4.0) {
                        Serial.println(" ✅ GOOD");
                    } else if (hdop < 8.0) {
                        Serial.println(" ⚠️  FAIR");
                    } else {
                        Serial.println(" ❌ POOR");
                    }
                    
                    Serial.print("\n⏱️  Time to fix: ");
                    Serial.print(timeToFix);
                    Serial.println(" seconds");
                    
                    Serial.println("\n═══════════════════════════════════════════════════════\n");
                } else {
                    // Update every 10 seconds
                    static unsigned long lastUpdate = 0;
                    if (millis() - lastUpdate > 10000) {
                        Serial.print("📍 GPS Active | Sats: ");
                        Serial.print(gps.satellites.value());
                        Serial.print(" | HDOP: ");
                        Serial.print(gps.hdop.hdop(), 1);
                        Serial.print(" | Lat: ");
                        Serial.print(gps.location.lat(), 6);
                        Serial.print(" | Lng: ");
                        Serial.println(gps.location.lng(), 6);
                        lastUpdate = millis();
                    }
                }
            }
        }
    } else {
        // No data received
        static unsigned long lastWarning = 0;
        if (millis() - lastWarning > 5000) {
            unsigned long elapsed = (millis() - startTime) / 1000;
            
            if (dataCount == 0) {
                Serial.println("\n❌ NO DATA RECEIVED FROM GPS MODULE");
                Serial.println("\nCHECK:");
                Serial.println("1. Power: GPS VCC → 3.3V, GPS GND → GND");
                Serial.println("2. Data: GPS TX → D1, GPS RX → D2");
                Serial.println("3. GPS LED should blink if module has one");
                Serial.println();
            } else if (!gotFix) {
                Serial.print("\n⚠️  Receiving data (");
                Serial.print(dataCount);
                Serial.print(" bytes) but no GPS fix yet... (");
                Serial.print(elapsed);
                Serial.println("s elapsed)");
                Serial.println("→ Move device outdoors with clear sky view");
                Serial.println("→ Wait 30-60 seconds for cold start\n");
            }
            lastWarning = millis();
        }
    }
    
    delay(10);
}


/*
 * COMPREHENSIVE GPS TEST FOR ESP8266
 * 
 * This test will help diagnose GPS signal reception issues:
 * 1. Checks if GPS hardware is connected properly
 * 2. Displays raw GPS data stream
 * 3. Shows GPS signal quality (satellites, HDOP)
 * 4. Displays location when GPS fix is obtained
 * 5. Provides diagnostic information
 * 
 * CONNECTIONS:
 * GPS VCC  -> ESP8266 3.3V
 * GPS GND  -> ESP8266 GND
 * GPS TX   -> ESP8266 D1 (GPIO5)
 * GPS RX   -> ESP8266 D2 (GPIO4)
 * 
 * Upload this code and open Serial Monitor at 115200 baud
 */

#include <SoftwareSerial.h>
#include <TinyGPSPlus.h>

// GPS Serial Configuration
// SoftwareSerial(rxPin, txPin)
// GPS TX (sends data) -> ESP8266 RX (D1) - receives GPS data
// GPS RX (receives data) -> ESP8266 TX (D2) - sends commands to GPS
SoftwareSerial gpsSerial(D1, D2); // RX, TX pins
TinyGPSPlus gps;

// Test variables
unsigned long testStartTime = 0;
unsigned long lastDataTime = 0;
unsigned long lastStatusTime = 0;
unsigned long lastFixTime = 0;
int rawDataCount = 0;
int nmeaSentenceCount = 0;
bool hasReceivedData = false;
bool hasValidFix = false;
int testBaudRates[] = {9600, 4800, 115200, 38400};
int currentBaudIndex = 0;
unsigned long baudTestStartTime = 0;
const unsigned long BAUD_TEST_DURATION = 10000; // 10 seconds per baud rate
const unsigned long STATUS_INTERVAL = 5000; // Show status every 5 seconds

void printHeader() {
    Serial.println("\n\n");
    Serial.println("╔══════════════════════════════════════════════════════════╗");
    Serial.println("║         COMPREHENSIVE GPS SIGNAL TEST                   ║");
    Serial.println("╚══════════════════════════════════════════════════════════╝");
    Serial.println();
    Serial.println("CONNECTIONS:");
    Serial.println("  GPS VCC  -> ESP8266 3.3V");
    Serial.println("  GPS GND  -> ESP8266 GND");
    Serial.println("  GPS TX   -> ESP8266 D1 (GPIO5)");
    Serial.println("  GPS RX   -> ESP8266 D2 (GPIO4)");
    Serial.println();
    Serial.println("═══════════════════════════════════════════════════════════");
    Serial.println();
}

void printStatus() {
    unsigned long elapsed = (millis() - testStartTime) / 1000;
    Serial.println("\n─────────────────────────────────────────────────────────");
    Serial.print("TEST STATUS (Running for ");
    Serial.print(elapsed);
    Serial.println(" seconds)");
    Serial.println("─────────────────────────────────────────────────────────");
    
    // Hardware connection status
    if (hasReceivedData) {
        Serial.println("✅ HARDWARE: GPS module is connected and sending data");
        Serial.print("   Raw bytes received: ");
        Serial.println(rawDataCount);
        Serial.print("   NMEA sentences parsed: ");
        Serial.println(nmeaSentenceCount);
    } else {
        Serial.println("❌ HARDWARE: No data received from GPS module");
        Serial.println("   → Check power connections (VCC/GND)");
        Serial.println("   → Check data connections (TX/RX)");
        Serial.println("   → Verify GPS module is powered (LED should blink)");
    }
    
    // GPS signal status
    if (hasValidFix) {
        Serial.println("✅ GPS SIGNAL: Valid fix obtained!");
        Serial.print("   Satellites: ");
        Serial.println(gps.satellites.value());
        Serial.print("   HDOP: ");
        Serial.println(gps.hdop.hdop(), 1);
        Serial.print("   Latitude: ");
        Serial.println(gps.location.lat(), 6);
        Serial.print("   Longitude: ");
        Serial.println(gps.location.lng(), 6);
        Serial.print("   Altitude: ");
        Serial.print(gps.altitude.meters());
        Serial.println(" meters");
        Serial.print("   Time since fix: ");
        Serial.print((millis() - lastFixTime) / 1000);
        Serial.println(" seconds");
    } else if (hasReceivedData) {
        Serial.println("⚠️  GPS SIGNAL: Receiving data but no valid fix yet");
        Serial.println("   → GPS needs clear view of sky");
        Serial.println("   → Move device outdoors");
        Serial.println("   → Wait 30-60 seconds for cold start");
        Serial.println("   → GPS may need time to download almanac");
    } else {
        Serial.println("❌ GPS SIGNAL: No data received");
    }
    
    // Time since last data
    if (hasReceivedData) {
        unsigned long timeSinceData = (millis() - lastDataTime) / 1000;
        if (timeSinceData > 5) {
            Serial.print("⚠️  WARNING: No data received for ");
            Serial.print(timeSinceData);
            Serial.println(" seconds");
        }
    }
    
    Serial.println("─────────────────────────────────────────────────────────\n");
}

void printGPSInfo() {
    Serial.println("\n╔══════════════════════════════════════════════════════════╗");
    Serial.println("║                    GPS FIX OBTAINED!                    ║");
    Serial.println("╚══════════════════════════════════════════════════════════╝");
    
    Serial.println("\n📍 LOCATION:");
    Serial.print("   Latitude:  ");
    Serial.println(gps.location.lat(), 6);
    Serial.print("   Longitude: ");
    Serial.println(gps.location.lng(), 6);
    Serial.print("   Altitude:  ");
    Serial.print(gps.altitude.meters());
    Serial.println(" meters");
    
    Serial.println("\n📡 SIGNAL QUALITY:");
    Serial.print("   Satellites: ");
    Serial.print(gps.satellites.value());
    if (gps.satellites.value() >= 4) {
        Serial.println(" ✅ (Good)");
    } else if (gps.satellites.value() >= 3) {
        Serial.println(" ⚠️  (Fair)");
    } else {
        Serial.println(" ❌ (Poor - need more satellites)");
    }
    
    Serial.print("   HDOP: ");
    Serial.print(gps.hdop.hdop(), 1);
    if (gps.hdop.hdop() < 2.0) {
        Serial.println(" ✅ (Excellent)");
    } else if (gps.hdop.hdop() < 4.0) {
        Serial.println(" ✅ (Good)");
    } else if (gps.hdop.hdop() < 8.0) {
        Serial.println(" ⚠️  (Fair)");
    } else {
        Serial.println(" ❌ (Poor - high error)");
    }
    
    Serial.println("\n🕐 TIME:");
    if (gps.date.isValid() && gps.time.isValid()) {
        Serial.print("   Date: ");
        Serial.print(gps.date.month());
        Serial.print("/");
        Serial.print(gps.date.day());
        Serial.print("/");
        Serial.println(gps.date.year());
        Serial.print("   Time: ");
        if (gps.time.hour() < 10) Serial.print("0");
        Serial.print(gps.time.hour());
        Serial.print(":");
        if (gps.time.minute() < 10) Serial.print("0");
        Serial.print(gps.time.minute());
        Serial.print(":");
        if (gps.time.second() < 10) Serial.print("0");
        Serial.println(gps.time.second());
    } else {
        Serial.println("   Time not available yet");
    }
    
    Serial.println("\n📊 STATISTICS:");
    Serial.print("   Raw bytes received: ");
    Serial.println(rawDataCount);
    Serial.print("   NMEA sentences: ");
    Serial.println(nmeaSentenceCount);
    Serial.print("   Time to first fix: ");
    Serial.print((lastFixTime - testStartTime) / 1000);
    Serial.println(" seconds");
    
    Serial.println("\n═══════════════════════════════════════════════════════════\n");
}

void printDiagnostics() {
    Serial.println("\n╔══════════════════════════════════════════════════════════╗");
    Serial.println("║                  DIAGNOSTIC INFORMATION                 ║");
    Serial.println("╚══════════════════════════════════════════════════════════╝");
    
    if (!hasReceivedData) {
        Serial.println("\n❌ PROBLEM: No data received from GPS module");
        Serial.println("\nPOSSIBLE CAUSES:");
        Serial.println("1. Power Issue:");
        Serial.println("   → Check GPS VCC is connected to 3.3V (NOT 5V!)");
        Serial.println("   → Check GPS GND is connected to ESP8266 GND");
        Serial.println("   → Use multimeter to verify 3.3V at GPS module");
        Serial.println("   → Check if GPS LED is blinking (if module has LED)");
        Serial.println();
        Serial.println("2. Wiring Issue:");
        Serial.println("   → GPS TX must connect to ESP8266 D1 (RX pin)");
        Serial.println("   → GPS RX must connect to ESP8266 D2 (TX pin)");
        Serial.println("   → Connections are CROSSED (TX to RX, RX to TX)");
        Serial.println("   → Check for loose connections or broken wires");
        Serial.println();
        Serial.println("3. GPS Module Issue:");
        Serial.println("   → GPS module may be damaged");
        Serial.println("   → Try a different GPS module");
        Serial.println("   → Check GPS module model (ATGM336H-5N, NEO-6M, etc.)");
    } else if (!hasValidFix) {
        Serial.println("\n⚠️  PROBLEM: Receiving data but no GPS fix");
        Serial.println("\nPOSSIBLE CAUSES:");
        Serial.println("1. No Clear Sky View:");
        Serial.println("   → GPS needs clear view of sky (not indoors!)");
        Serial.println("   → Move device near window or outdoors");
        Serial.println("   → Avoid buildings, trees, or metal objects");
        Serial.println();
        Serial.println("2. Cold Start:");
        Serial.println("   → First time or after long power-off");
        Serial.println("   → GPS needs 30-60 seconds to download almanac");
        Serial.println("   → Be patient and wait");
        Serial.println();
        Serial.println("3. Weak Signal:");
        Serial.println("   → Check satellite count (need at least 3-4)");
        Serial.println("   → Check HDOP value (lower is better)");
        Serial.println("   → Move to location with better sky view");
    } else {
        Serial.println("\n✅ GPS IS WORKING CORRECTLY!");
        Serial.println("   Your GPS module is receiving signals and providing location data.");
    }
    
    Serial.println("\n═══════════════════════════════════════════════════════════\n");
}

void setup() {
    Serial.begin(115200);
    delay(2000);
    
    printHeader();
    
    Serial.println("Initializing GPS at 9600 baud...");
    gpsSerial.begin(9600);
    delay(500);
    
    testStartTime = millis();
    baudTestStartTime = millis();
    
    Serial.println("Starting GPS test...");
    Serial.println("(This test will run continuously)");
    Serial.println();
    Serial.println("Raw GPS data stream:");
    Serial.println("─────────────────────────────────────────────────────────");
}

void loop() {
    // Check for GPS data
    if (gpsSerial.available() > 0) {
        hasReceivedData = true;
        lastDataTime = millis();
        
        while (gpsSerial.available() > 0) {
            char c = gpsSerial.read();
            rawDataCount++;
            
            // Print raw data (first 100 characters to show it's working)
            if (rawDataCount <= 100) {
                Serial.print(c);
            }
            
            // Feed to GPS parser
            if (gps.encode(c)) {
                nmeaSentenceCount++;
                
                // Check if we have a valid fix
                if (gps.location.isValid() && !hasValidFix) {
                    hasValidFix = true;
                    lastFixTime = millis();
                    printGPSInfo();
                }
            }
        }
    }
    
    // Print status every STATUS_INTERVAL
    if (millis() - lastStatusTime > STATUS_INTERVAL) {
        printStatus();
        lastStatusTime = millis();
    }
    
    // Print diagnostics after 30 seconds if no fix
    static bool diagnosticsPrinted = false;
    if (!hasValidFix && !diagnosticsPrinted && (millis() - testStartTime > 30000)) {
        printDiagnostics();
        diagnosticsPrinted = true;
    }
    
    // Update GPS info if we have a fix (every 10 seconds)
    if (hasValidFix && (millis() - lastFixTime > 10000)) {
        static unsigned long lastInfoUpdate = 0;
        if (millis() - lastInfoUpdate > 10000) {
            Serial.println("\n📡 GPS UPDATE:");
            Serial.print("   Satellites: ");
            Serial.print(gps.satellites.value());
            Serial.print(" | HDOP: ");
            Serial.print(gps.hdop.hdop(), 1);
            Serial.print(" | Lat: ");
            Serial.print(gps.location.lat(), 6);
            Serial.print(" | Lng: ");
            Serial.println(gps.location.lng(), 6);
            lastInfoUpdate = millis();
        }
    }
    
    delay(10);
}


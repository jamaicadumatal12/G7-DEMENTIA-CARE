/*
 * GPS Diagnostic Test for ESP8266
 * This simple test will help identify GPS connection issues
 * 
 * Upload this code and open Serial Monitor at 115200 baud
 * It will test different pin configurations and baud rates
 */

#include <SoftwareSerial.h>

// Try different pin configurations
// Configuration 1: D5 (RX), D6 (TX) - Most common
SoftwareSerial gpsSerial1(D5, D6);

// Configuration 2: D1 (RX), D2 (TX) - Alternative
SoftwareSerial gpsSerial2(D1, D2);

// Configuration 3: D7 (RX), D8 (TX) - Another option
SoftwareSerial gpsSerial3(D7, D8);

SoftwareSerial* currentSerial = &gpsSerial1;
int currentConfig = 1;
int currentBaud = 9600;

void setup() {
    Serial.begin(115200);
    delay(2000);
    
    Serial.println("\n\n========================================");
    Serial.println("   GPS DIAGNOSTIC TEST");
    Serial.println("========================================");
    Serial.println();
    Serial.println("This test will check:");
    Serial.println("1. If GPS module is powered");
    Serial.println("2. If data is being received");
    Serial.println("3. Which pins are working");
    Serial.println("4. Which baud rate is correct");
    Serial.println();
    Serial.println("========================================");
    Serial.println();
    
    // Test Configuration 1: D5, D6 at 9600 baud
    testConfiguration(1, D5, D6, 9600);
    delay(2000);
    
    // Test Configuration 1: D5, D6 at 4800 baud
    testConfiguration(1, D5, D6, 4800);
    delay(2000);
    
    // Test Configuration 2: D1, D2 at 9600 baud
    testConfiguration(2, D1, D2, 9600);
    delay(2000);
    
    // Test Configuration 2: D1, D2 at 4800 baud
    testConfiguration(2, D1, D2, 4800);
    delay(2000);
    
    Serial.println("\n========================================");
    Serial.println("   TEST COMPLETE");
    Serial.println("========================================");
    Serial.println();
    Serial.println("If NO data was received in any test:");
    Serial.println("  ❌ GPS module is NOT powered or NOT connected");
    Serial.println("  → Check VCC connection (should be 3.3V)");
    Serial.println("  → Check GND connection");
    Serial.println("  → Check if GPS module LED is blinking");
    Serial.println();
    Serial.println("If data WAS received:");
    Serial.println("  ✅ GPS is working! Use that configuration");
    Serial.println();
}

void testConfiguration(int configNum, int rxPin, int txPin, int baudRate) {
    Serial.println("----------------------------------------");
    Serial.print("Testing Configuration ");
    Serial.print(configNum);
    Serial.print(": RX=D");
    Serial.print(rxPin);
    Serial.print(", TX=D");
    Serial.print(txPin);
    Serial.print(" at ");
    Serial.print(baudRate);
    Serial.println(" baud");
    Serial.println("----------------------------------------");
    
    SoftwareSerial testSerial(rxPin, txPin);
    testSerial.begin(baudRate);
    delay(500); // Give GPS time to send data
    
    int bytesReceived = 0;
    int testDuration = 5000; // Test for 5 seconds
    unsigned long startTime = millis();
    String sampleData = "";
    
    Serial.println("Listening for GPS data (5 seconds)...");
    Serial.print("Raw data: ");
    
    while (millis() - startTime < testDuration) {
        if (testSerial.available() > 0) {
            while (testSerial.available() > 0) {
                char c = testSerial.read();
                bytesReceived++;
                
                // Print first 200 characters
                if (sampleData.length() < 200) {
                    Serial.print(c);
                    sampleData += c;
                }
            }
        }
        delay(10);
    }
    
    Serial.println();
    Serial.println();
    
    if (bytesReceived > 0) {
        Serial.print("✅ SUCCESS! Received ");
        Serial.print(bytesReceived);
        Serial.println(" bytes");
        Serial.print("Sample data: ");
        Serial.println(sampleData.substring(0, 100));
        Serial.println();
        Serial.println("🎉 THIS CONFIGURATION WORKS!");
        Serial.print("   Use: RX=D");
        Serial.print(rxPin);
        Serial.print(", TX=D");
        Serial.print(txPin);
        Serial.print(", Baud=");
        Serial.println(baudRate);
    } else {
        Serial.println("❌ FAILED - No data received");
        Serial.println("   Possible issues:");
        Serial.println("   - Wrong pins (GPS TX should connect to ESP8266 RX)");
        Serial.println("   - Wrong baud rate");
        Serial.println("   - GPS module not powered");
        Serial.println("   - GPS module not working");
    }
    Serial.println();
}

void loop() {
    // Continuous monitoring of the best configuration
    if (currentConfig == 1) {
        monitorGPS(&gpsSerial1, D5, D6);
    } else if (currentConfig == 2) {
        monitorGPS(&gpsSerial2, D1, D2);
    }
}

void monitorGPS(SoftwareSerial* serial, int rxPin, int txPin) {
    if (serial->available() > 0) {
        Serial.print("[GPS] ");
        while (serial->available() > 0) {
            char c = serial->read();
            Serial.print(c);
        }
    }
    delay(100);
}


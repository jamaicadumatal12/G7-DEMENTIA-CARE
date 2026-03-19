/*
 * ESP8266 Debug Version - Find where it's getting stuck
 */

#include <ESP8266WiFi.h>

// WiFi credentials
const char* ssid = "ERICBUANG";
const char* password = "JAYSONBONGS";

void setup() {
    Serial.begin(115200);
    delay(1000);
    
    Serial.println("=== DEBUG VERSION ===");
    Serial.println("Step 1: Serial started");
    
    // Test WiFi connection
    Serial.println("Step 2: Starting WiFi...");
    WiFi.begin(ssid, password);
    
    int attempts = 0;
    while (WiFi.status() != WL_CONNECTED && attempts < 20) {
        delay(500);
        Serial.print(".");
        attempts++;
    }
    
    if (WiFi.status() == WL_CONNECTED) {
        Serial.println("\nStep 3: WiFi Connected!");
        Serial.print("IP: ");
        Serial.println(WiFi.localIP());
    } else {
        Serial.println("\nStep 4: WiFi FAILED!");
        Serial.println("WiFi Status: " + String(WiFi.status()));
        return;
    }
    
    Serial.println("Step 5: Setup complete - entering loop");
}

void loop() {
    Serial.println("Loop running - GPS would be here");
    delay(5000);
}

/*
 * ESP8266 Firebase Test - Debug Version
 * This is a simplified version to test Firebase connection
 */

#include <ESP8266WiFi.h>
#include <FirebaseESP8266.h>

// WiFi credentials
const char* ssid = "ERICBUANG";
const char* password = "JAYSONBONGS";

// Firebase configuration
#define FIREBASE_HOST "g7-dementia-care-default-rtdb.asia-southeast1.firebasedatabase.app"
#define FIREBASE_AUTH "CsmqaL84uFlUjh6i9NhWCoiw0xR99JpCQfmHs4VK"

FirebaseData firebaseData;
FirebaseAuth auth;
FirebaseConfig config;

// Device ID
String DEVICE_ID = "G7T55ZGF5";

void setup() {
    Serial.begin(115200);
    delay(1000);
    
    Serial.println("=== ESP8266 Firebase Test ===");
    Serial.println("Device ID: " + DEVICE_ID);
    
    // Connect to WiFi
    Serial.print("Connecting to WiFi: ");
    Serial.println(ssid);
    WiFi.begin(ssid, password);
    
    while (WiFi.status() != WL_CONNECTED) {
        delay(500);
        Serial.print(".");
    }
    Serial.println("\nWiFi Connected!");
    Serial.print("IP Address: ");
    Serial.println(WiFi.localIP());
    
    // Initialize Firebase
    config.host = FIREBASE_HOST;
    config.signer.tokens.legacy_token = FIREBASE_AUTH;
    
    Firebase.begin(&config, &auth);
    Firebase.reconnectWiFi(true);
    
    Serial.println("Firebase initialized.");
    
    // Wait for Firebase to be ready
    int attempts = 0;
    while (!Firebase.ready() && attempts < 10) {
        delay(1000);
        Serial.print("Waiting for Firebase... ");
        Serial.println(attempts + 1);
        attempts++;
    }
    
    if (Firebase.ready()) {
        Serial.println("✅ Firebase is ready!");
        
        // Test 1: Simple string write
        Serial.println("Test 1: Writing simple string...");
        if (Firebase.setString(firebaseData, "/test/simple", "Hello from ESP8266")) {
            Serial.println("✅ Simple string write successful!");
        } else {
            Serial.println("❌ Simple string write failed: " + firebaseData.errorReason());
        }
        
        // Test 2: Device data write
        Serial.println("Test 2: Writing device data...");
        String devicePath = "/devices/" + DEVICE_ID;
        FirebaseJson json;
        json.set("latitude", 8.1545);
        json.set("longitude", 125.1275);
        json.set("satellites", 8);
        json.set("hdop", 1.2);
        json.set("inDanger", false);
        json.set("timestamp", millis());
        json.set("deviceId", DEVICE_ID);
        
        if (Firebase.setJSON(firebaseData, devicePath, json)) {
            Serial.println("✅ Device data write successful!");
            Serial.println("Path: " + devicePath);
        } else {
            Serial.println("❌ Device data write failed: " + firebaseData.errorReason());
            Serial.println("HTTP Code: " + String(firebaseData.httpCode()));
        }
        
        // Test 3: Alert write
        Serial.println("Test 3: Writing alert...");
        FirebaseJson alertJson;
        alertJson.set("message", "Test alert from ESP8266");
        alertJson.set("timestamp", millis());
        alertJson.set("deviceId", DEVICE_ID);
        
        if (Firebase.setJSON(firebaseData, "/alerts/latest", alertJson)) {
            Serial.println("✅ Alert write successful!");
        } else {
            Serial.println("❌ Alert write failed: " + firebaseData.errorReason());
        }
        
    } else {
        Serial.println("❌ Firebase not ready after 10 attempts!");
    }
}

void loop() {
    // Keep the connection alive
    if (WiFi.status() != WL_CONNECTED) {
        Serial.println("WiFi disconnected!");
        WiFi.begin(ssid, password);
        while (WiFi.status() != WL_CONNECTED) {
            delay(500);
            Serial.print(".");
        }
        Serial.println("WiFi reconnected!");
    }
    
    // Send test data every 30 seconds
    static unsigned long lastTest = 0;
    if (millis() - lastTest > 30000) {
        if (Firebase.ready()) {
            String devicePath = "/devices/" + DEVICE_ID;
            FirebaseJson json;
            json.set("latitude", 8.1545 + random(-10, 10) * 0.001); // Add some variation
            json.set("longitude", 125.1275 + random(-10, 10) * 0.001);
            json.set("satellites", 8);
            json.set("hdop", 1.2);
            json.set("inDanger", false);
            json.set("timestamp", millis());
            json.set("deviceId", DEVICE_ID);
            
            if (Firebase.setJSON(firebaseData, devicePath, json)) {
                Serial.println("✅ Periodic update successful!");
            } else {
                Serial.println("❌ Periodic update failed: " + firebaseData.errorReason());
            }
        }
        lastTest = millis();
    }
    
    delay(1000);
}

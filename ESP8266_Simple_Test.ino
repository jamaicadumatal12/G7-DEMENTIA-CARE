/*
 * Simple Test - Find where ESP8266 gets stuck
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

String DEVICE_ID = "G7T55ZGF6";

void setup() {
    Serial.begin(115200);
    delay(1000);
    
    Serial.println("=== SIMPLE TEST ===");
    Serial.println("Device ID: " + DEVICE_ID);
    
    // Test 1: WiFi Connection
    Serial.println("Test 1: Connecting to WiFi...");
    WiFi.begin(ssid, password);
    
    int attempts = 0;
    while (WiFi.status() != WL_CONNECTED && attempts < 20) {
        delay(1000);
        Serial.print(".");
        attempts++;
    }
    
    if (WiFi.status() == WL_CONNECTED) {
        Serial.println("\n✅ WiFi Connected!");
        Serial.print("IP: ");
        Serial.println(WiFi.localIP());
    } else {
        Serial.println("\n❌ WiFi Failed!");
        Serial.println("WiFi Status: " + String(WiFi.status()));
        return;
    }
    
    // Test 2: Firebase Connection
    Serial.println("Test 2: Connecting to Firebase...");
    config.host = FIREBASE_HOST;
    config.signer.tokens.legacy_token = FIREBASE_AUTH;
    
    Firebase.begin(&config, &auth);
    Firebase.reconnectWiFi(true);
    
    // Wait for Firebase
    int firebaseAttempts = 0;
    while (!Firebase.ready() && firebaseAttempts < 10) {
        delay(1000);
        Serial.print("Firebase... ");
        Serial.println(firebaseAttempts + 1);
        firebaseAttempts++;
    }
    
    if (Firebase.ready()) {
        Serial.println("✅ Firebase Connected!");
        
        // Test 3: Write to Firebase
        Serial.println("Test 3: Writing to Firebase...");
        String path = "/devices/" + DEVICE_ID;
        FirebaseJson json;
        json.set("latitude", 8.1545);
        json.set("longitude", 125.1275);
        json.set("satellites", 8);
        json.set("hdop", 1.2);
        json.set("inDanger", false);
        json.set("timestamp", millis());
        json.set("deviceId", DEVICE_ID);
        
        if (Firebase.setJSON(firebaseData, path, json)) {
            Serial.println("✅ Firebase Write Successful!");
            Serial.println("Path: " + path);
        } else {
            Serial.println("❌ Firebase Write Failed!");
            Serial.println("Error: " + firebaseData.errorReason());
            Serial.println("HTTP Code: " + String(firebaseData.httpCode()));
        }
    } else {
        Serial.println("❌ Firebase Failed!");
    }
    
    Serial.println("Test Complete!");
}

void loop() {
    // Keep connection alive
    if (WiFi.status() != WL_CONNECTED) {
        Serial.println("WiFi disconnected!");
        WiFi.begin(ssid, password);
        while (WiFi.status() != WL_CONNECTED) {
            delay(500);
            Serial.print(".");
        }
        Serial.println("WiFi reconnected!");
    }
    
    delay(5000);
}
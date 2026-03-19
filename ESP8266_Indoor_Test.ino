/*
 * Indoor Test Version - Uses mock GPS data for testing
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

// Mock GPS data (for indoor testing)
double mockLat = 8.1545;
double mockLng = 125.1275;
int mockSats = 8;
float mockHdop = 1.2;

// Safe zone
double safeLat = 8.1545;
double safeLng = 125.1275;
float dangerRadius = 100.0;

// Timing
unsigned long lastFirebaseUpdate = 0;
const unsigned long FIREBASE_INTERVAL = 15000; // 15 seconds

void setup() {
    Serial.begin(115200);
    delay(1000);
    
    Serial.println("=== INDOOR TEST MODE ===");
    Serial.println("Device ID: " + DEVICE_ID);
    Serial.println("Using MOCK GPS data for indoor testing");
    
    // Connect WiFi
    Serial.println("Connecting to WiFi...");
    WiFi.begin(ssid, password);
    
    while (WiFi.status() != WL_CONNECTED) {
        delay(1000);
        Serial.print(".");
    }
    Serial.println("\nWiFi Connected!");
    Serial.print("IP: ");
    Serial.println(WiFi.localIP());
    
    // Connect Firebase
    Serial.println("Connecting to Firebase...");
    config.host = FIREBASE_HOST;
    config.signer.tokens.legacy_token = FIREBASE_AUTH;
    
    Firebase.begin(&config, &auth);
    Firebase.reconnectWiFi(true);
    
    while (!Firebase.ready()) {
        delay(1000);
        Serial.print("Firebase... ");
    }
    Serial.println("\nFirebase Connected!");
    
    Serial.println("System ready - using mock GPS data");
}

void loop() {
    // Add some variation to mock GPS data
    mockLat += random(-10, 10) * 0.0001; // Small random movement
    mockLng += random(-10, 10) * 0.0001;
    
    // Calculate distance to safe zone
    float distance = sqrt(pow(mockLat - safeLat, 2) + pow(mockLng - safeLng, 2)) * 111000; // Rough conversion to meters
    bool isInSafeZone = (distance <= dangerRadius);
    
    // Print mock GPS data
    Serial.println("=== MOCK GPS DATA ===");
    Serial.print("LAT: "); Serial.println(mockLat, 6);
    Serial.print("LNG: "); Serial.println(mockLng, 6);
    Serial.print("SAT: "); Serial.println(mockSats);
    Serial.print("HDOP: "); Serial.println(mockHdop);
    Serial.print("DISTANCE: "); Serial.print(distance); Serial.println("m");
    Serial.print("STATUS: "); Serial.println(isInSafeZone ? "SAFE" : "DANGER");
    Serial.println("====================");
    
    // Send to Firebase every 15 seconds
    if (millis() - lastFirebaseUpdate > FIREBASE_INTERVAL) {
        String path = "/devices/" + DEVICE_ID;
        FirebaseJson json;
        json.set("latitude", mockLat);
        json.set("longitude", mockLng);
        json.set("satellites", mockSats);
        json.set("hdop", mockHdop);
        json.set("inDanger", !isInSafeZone);
        json.set("timestamp", millis());
        json.set("deviceId", DEVICE_ID);
        json.set("mode", "MOCK_GPS"); // Indicate this is mock data
        
        if (Firebase.setJSON(firebaseData, path, json)) {
            Serial.println("✅ Mock location sent to Firebase!");
        } else {
            Serial.println("❌ Firebase Error: " + firebaseData.errorReason());
        }
        lastFirebaseUpdate = millis();
    }
    
    delay(5000); // Update every 5 seconds
}

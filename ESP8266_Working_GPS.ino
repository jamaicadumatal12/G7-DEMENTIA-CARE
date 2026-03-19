/*
 * Working GPS Code - Based on Simple Test + GPS
 */

#include <ESP8266WiFi.h>
#include <FirebaseESP8266.h>
#include <TinyGPSPlus.h>
#include <SoftwareSerial.h>

// WiFi credentials
const char* ssid = "ERICBUANG";
const char* password = "JAYSONBONGS";

// Firebase configuration
#define FIREBASE_HOST "g7-dementia-care-default-rtdb.asia-southeast1.firebasedatabase.app"
#define FIREBASE_AUTH "CsmqaL84uFlUjh6i9NhWCoiw0xR99JpCQfmHs4VK"

// GPS pins
#define GPS_RX D5
#define GPS_TX D6

FirebaseData firebaseData;
FirebaseAuth auth;
FirebaseConfig config;

String DEVICE_ID = "G7T55ZGF6";

// GPS objects
TinyGPSPlus gps;
SoftwareSerial gpsSerial(GPS_RX, GPS_TX);

// Safe zone
double safeLat = 8.1545;
double safeLng = 125.1275;
float dangerRadius = 100.0;

// Timing
unsigned long lastFirebaseUpdate = 0;
const unsigned long FIREBASE_INTERVAL = 30000; // 30 seconds

void setup() {
    Serial.begin(115200);
    delay(1000);
    
    Serial.println("=== WORKING GPS CODE ===");
    Serial.println("Device ID: " + DEVICE_ID);
    
    // Initialize GPS
    gpsSerial.begin(9600);
    Serial.println("GPS initialized");
    
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
    
    Serial.println("System ready - waiting for GPS...");
}

void loop() {
    // Process GPS data
    while (gpsSerial.available() > 0) {
        if (gps.encode(gpsSerial.read())) {
            if (gps.location.isValid()) {
                double lat = gps.location.lat();
                double lng = gps.location.lng();
                int sats = gps.satellites.value();
                float hdop = gps.hdop.hdop();
                
                // Check GPS quality
                if (sats >= 3 && hdop < 8.0) {
                    // Calculate distance to safe zone
                    float distance = TinyGPSPlus::distanceBetween(lat, lng, safeLat, safeLng);
                    bool isInSafeZone = (distance <= dangerRadius);
                    
                    // Print GPS data
                    Serial.println("=== GPS DATA ===");
                    Serial.print("LAT: "); Serial.println(lat, 6);
                    Serial.print("LNG: "); Serial.println(lng, 6);
                    Serial.print("SAT: "); Serial.println(sats);
                    Serial.print("HDOP: "); Serial.println(hdop);
                    Serial.print("DISTANCE: "); Serial.print(distance); Serial.println("m");
                    Serial.print("STATUS: "); Serial.println(isInSafeZone ? "SAFE" : "DANGER");
                    Serial.println("================");
                    
                    // Send to Firebase every 30 seconds
                    if (millis() - lastFirebaseUpdate > FIREBASE_INTERVAL) {
                        String path = "/devices/" + DEVICE_ID;
                        FirebaseJson json;
                        json.set("latitude", lat);
                        json.set("longitude", lng);
                        json.set("satellites", sats);
                        json.set("hdop", hdop);
                        json.set("inDanger", !isInSafeZone);
                        json.set("timestamp", millis());
                        json.set("deviceId", DEVICE_ID);
                        
                        if (Firebase.setJSON(firebaseData, path, json)) {
                            Serial.println("✅ Location sent to Firebase!");
                        } else {
                            Serial.println("❌ Firebase Error: " + firebaseData.errorReason());
                        }
                        lastFirebaseUpdate = millis();
                    }
                } else {
                    Serial.println("GPS quality insufficient - waiting...");
                }
            }
        }
    }
    
    // If no GPS for 10 seconds, show status
    static unsigned long lastGPSMessage = 0;
    if (millis() - lastGPSMessage > 10000) {
        Serial.println("Waiting for GPS signal...");
        lastGPSMessage = millis();
    }
    
    delay(100);
}

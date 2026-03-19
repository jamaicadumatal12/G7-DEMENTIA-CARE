# G7 Dementia Care - Patient Monitoring System

A comprehensive Android application for monitoring dementia patients with real-time GPS tracking and safe zone alerts.

## 🚀 Features

- **Real-time GPS Tracking**: Monitor patient locations in real-time
- **Safe Zone Management**: Set up circular safe zones with accurate radius calculations
- **Alert System**: Get notifications when patients leave safe zones
- **Patient Management**: Add, edit, and manage patient information
- **Offline Detection**: Detect when GPS devices go offline
- **Multi-device Support**: Support for multiple GPS tracking devices

## 🔧 Recent Fixes Applied

### 1. Safe Zone Radius Accuracy Issues ✅

**Problem**: The safe zone circles were not accurately representing the actual safe zone boundaries due to incorrect geographic calculations.

**Solution**: 
- Implemented accurate geographic circle drawing using proper spherical trigonometry
- Updated both `MapActivity` and `RealTimePatientMapActivity` with correct circle calculation formulas
- Added consistent distance calculation using Haversine formula in `NetworkUtils`

**Files Updated**:
- `app/src/main/java/com/example/dashboard/RealTimePatientMapActivity.java`
- `app/src/main/java/com/example/dashboard/MapActivity.java`
- `app/src/main/java/com/example/dashboard/utils/NetworkUtils.java`

### 2. Arduino Device Synchronization ✅

**Problem**: Arduino devices were using hardcoded safe zone coordinates instead of reading from Firebase.

**Solution**:
- Added Firebase safe zone loading functionality to Arduino devices
- Implemented periodic safe zone updates (every 30 seconds)
- Increased default radius from 20m to 500m to match Android app
- Added distance display on device screen

**Files Updated**:
- `ESP8266_GPS_Firebase.ino`

### 3. Distance Calculation Consistency ✅

**Problem**: Different parts of the app used different distance calculation methods.

**Solution**:
- Created centralized distance calculation utility in `NetworkUtils`
- Implemented consistent Haversine formula across all components
- Added detailed logging for debugging distance calculations

## 📱 App Structure

### Core Activities

1. **MainActivity**: Dashboard with patient list and navigation
2. **LoginActivity**: User authentication
3. **RegisterActivity**: User registration
4. **AddPatientActivity**: Add new patients
5. **PatientDetailsActivity**: View and edit patient information
6. **MapActivity**: Set up safe zones
7. **RealTimePatientMapActivity**: Real-time patient monitoring
8. **PatientMapActivity**: View individual patient locations

### Key Components

- **PatientModel**: Data model for patient information
- **FirebaseHelper**: Firebase database operations
- **NetworkUtils**: Network and distance calculation utilities
- **NotificationHelper**: Push notification management
- **SessionManager**: User session management

## 🗺️ Safe Zone System

### How It Works

1. **Setting Safe Zones**: 
   - Use `MapActivity` to select a location and set radius
   - Safe zone data is stored in Firebase under `/zones/{deviceId}`

2. **Real-time Monitoring**:
   - `RealTimePatientMapActivity` continuously monitors patient locations
   - Compares current position with safe zone boundaries
   - Triggers alerts when patients leave safe zones

3. **Device Synchronization**:
   - Arduino devices periodically load safe zone data from Firebase
   - Calculate distance using TinyGPSPlus library
   - Display safe/danger status on device screen

### Technical Details

- **Distance Calculation**: Haversine formula for accurate geographic distances
- **Circle Drawing**: Spherical trigonometry for accurate circle representation
- **Radius Range**: 100m to 5km (configurable)
- **Update Frequency**: Real-time for Android, 30 seconds for Arduino devices

## 🔌 Arduino Device Setup

### Hardware Requirements
- ESP8266 WiFi module
- NEO-6M GPS module
- TFT display (optional)
- Buzzer for alerts

### Configuration
1. Update WiFi credentials in Arduino code
2. Set Firebase credentials
3. Configure device ID (must match Android app)
4. Upload code to ESP8266

### Features
- Automatic WiFi connection with fallback networks
- Real-time GPS tracking
- Safe zone boundary checking
- Visual status display
- Firebase data synchronization

## 🚨 Alert System

### Types of Alerts
1. **Safe Zone Violation**: Patient leaves designated safe zone
2. **Device Offline**: GPS device stops sending data
3. **Low GPS Signal**: Poor satellite reception

### Alert Delivery
- In-app notifications
- Toast messages
- Status updates in real-time map
- Device screen indicators

## 📊 Firebase Database Structure

```
/database
├── /users
│   └── {userId}
│       ├── email
│       ├── name
│       └── patients
├── /patients
│   └── {patientId}
│       ├── deviceId
│       ├── name
│       ├── age
│       ├── gender
│       └── timestamp
├── /devices
│   └── {deviceId}
│       ├── latitude
│       ├── longitude
│       ├── satellites
│       ├── hdop
│       ├── inDanger
│       └── timestamp
└── /zones
    └── {deviceId}
        └── {zoneId}
            ├── type: "safe"
            ├── latitude
            ├── longitude
            ├── radius
            ├── timestamp
            └── patientId
```

## 🛠️ Maintenance Guide

### Regular Tasks
1. **Monitor Firebase Usage**: Check database read/write limits
2. **Update Device Firmware**: Keep Arduino devices updated
3. **Test Safe Zones**: Verify accuracy with known distances
4. **Check GPS Signal**: Ensure devices have good satellite reception

### Troubleshooting

#### Safe Zone Not Accurate
- Verify GPS coordinates are correct
- Check if radius is appropriate for location
- Test with known distances
- Review distance calculation logs

#### Device Not Connecting
- Check WiFi credentials
- Verify Firebase configuration
- Monitor serial output for errors
- Ensure stable power supply

#### App Crashes
- Check Android logs for errors
- Verify Firebase permissions
- Update to latest app version
- Clear app cache if needed

## 🔒 Security Considerations

- Firebase Authentication required for all operations
- Device IDs must be unique and secure
- GPS data is encrypted in transit
- User sessions are managed securely

## 📈 Performance Optimization

- Efficient Firebase queries with proper indexing
- Optimized map rendering with viewport culling
- Background location updates with battery optimization
- Minimal network usage for Arduino devices

## 🤝 Contributing

When making changes to the app:

1. Test safe zone accuracy thoroughly
2. Verify distance calculations with known coordinates
3. Update both Android and Arduino code if needed
4. Document any changes in this README
5. Test on multiple devices and locations

## 📞 Support

For technical support or questions:
- Check the logs for error messages
- Verify Firebase configuration
- Test with simple coordinates first
- Ensure all dependencies are up to date

---

**Version**: 2.0  
**Last Updated**: December 2024  
**Status**: Production Ready ✅
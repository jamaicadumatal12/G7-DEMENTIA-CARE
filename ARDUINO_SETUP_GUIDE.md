# Arduino IDE Setup Guide for ESP8266 GPS Firebase Project

## Prerequisites
- Arduino IDE installed
- ESP8266 development board
- GPS module (NEO-6M or similar)
- Circular display (GC9A01)
- USB cable for programming

## Step 1: Add ESP8266 Board Support

1. Open Arduino IDE
2. Go to **File → Preferences**
3. In "Additional Board Manager URLs" add:
   ```
   https://arduino.esp8266.com/stable/package_esp8266com_index.json
   ```
4. Click **OK**

## Step 2: Install ESP8266 Board Package

1. Go to **Tools → Board → Boards Manager**
2. Search for "ESP8266"
3. Find "ESP8266 by ESP8266 Community"
4. Click **Install** (wait for completion)

## Step 3: Install Required Libraries

Go to **Tools → Manage Libraries** and install:

### 1. FirebaseESP8266
- Search: "FirebaseESP8266"
- Author: Mobizt
- Click **Install**

### 2. TinyGPSPlus
- Search: "TinyGPSPlus"
- Author: Mikal Hart
- Click **Install**

### 3. LovyanGFX
- Search: "LovyanGFX"
- Author: lovyan03
- Click **Install**

## Step 4: Configure Board Settings

1. Go to **Tools → Board → ESP8266 Boards → NodeMCU 1.0**
2. Set these parameters:
   - **Upload Speed**: 115200
   - **CPU Frequency**: 80 MHz
   - **Flash Frequency**: 40 MHz
   - **Flash Mode**: DIO
   - **Flash Size**: 4MB (FS:2MB OTA:~1019KB)
   - **Debug port**: Disabled
   - **Debug Level**: None
   - **Reset Method**: nodemcu
   - **Crystal Frequency**: 26 MHz
   - **Port**: Select your COM port (e.g., COM3, COM4, etc.)

## Step 5: Open Your Code

1. Go to **File → Open**
2. Navigate to your project folder
3. Select **ESP8266_GPS_Firebase.ino**
4. Click **Open**

## Step 6: Verify Your Code

1. Click the **Verify** button (✓) or press **Ctrl+R**
2. Wait for compilation to complete
3. If successful, you'll see "Compilation completed successfully"

## Step 7: Upload to ESP8266

1. **Connect your ESP8266** to your computer via USB
2. **Select the correct COM port** in Tools → Port
3. Click the **Upload** button (→) or press **Ctrl+U**
4. **Wait for upload to complete**
5. You should see "Upload completed successfully"

## Step 8: Monitor Serial Output

1. Click the **Serial Monitor** button (🔍) or press **Ctrl+Shift+M**
2. Set baud rate to **115200**
3. You should see output like:
   ```
   Device ID: G7T55ZGF1
   🔍 Connecting to Wi-Fi: ERICBUANG
   ✅ Connected to ERICBUANG
   IP: 192.168.1.xxx
   ✅ Firebase connection successful
   ✅ Test write successful
   ```

## Troubleshooting

### If upload fails:
- Check USB cable connection
- Try different USB port
- Press and hold FLASH button on ESP8266 during upload
- Check if correct COM port is selected

### If WiFi fails:
- Verify WiFi credentials in code
- Check WiFi signal strength
- Ensure WiFi network is 2.4GHz (ESP8266 doesn't support 5GHz)

### If GPS doesn't work:
- Check GPS module wiring
- Move device outdoors for better signal
- Verify GPS module is powered

### If display doesn't work:
- Check display wiring connections
- Verify SPI pins are correctly configured
- Check if display is receiving power

## Hardware Connections

### ESP8266 Pin Connections:
- **GPS Module**:
  - TX → D1 (GPIO5)
  - RX → D2 (GPIO4)
  - VCC → 3.3V
  - GND → GND

- **Display (GC9A01)**:
  - SCLK → D5 (GPIO14)
  - MOSI → D7 (GPIO13)
  - CS → D8 (GPIO15)
  - RST → D3 (GPIO0)
  - DC → D4 (GPIO2)
  - VCC → 3.3V
  - GND → GND

## Success Indicators

When everything is working correctly, you should see:
1. **Serial Monitor**: Connection success messages
2. **Display**: Device ID, then GPS coordinates
3. **Firebase**: Data appearing in your Firebase console
4. **Android App**: Device found when registering patient

## Next Steps

After successful upload:
1. Test your Android app registration
2. Set up safe zones in the app
3. Monitor patient location tracking
4. Test alerts when patient leaves safe zone 
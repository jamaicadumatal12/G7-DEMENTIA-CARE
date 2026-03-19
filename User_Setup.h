// User_Setup.h for GC9A01 Circular Display

#define GC9A01_DRIVER

#define TFT_WIDTH  240
#define TFT_HEIGHT 240

#define TFT_MISO -1
#define TFT_MOSI 13  // D7
#define TFT_SCLK 14  // D5
#define TFT_CS   15  // D8
#define TFT_DC    2  // D4
#define TFT_RST   0  // D3

#define LOAD_GLCD
#define LOAD_FONT2
#define LOAD_FONT4
#define LOAD_FONT6
#define LOAD_FONT7
#define LOAD_FONT8
#define LOAD_GFXFF

#define SMOOTH_FONT

#define SPI_FREQUENCY  40000000
#define SPI_READ_FREQUENCY  20000000
#define SPI_TOUCH_FREQUENCY  2500000 
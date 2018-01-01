#include <Adafruit_GPS.h>
#include <SoftwareSerial.h>

SoftwareSerial mySerial(3, 2);//GPS connection RX2, TX3
SoftwareSerial XBee(4,5);//XBee connection RX5, TX4
Adafruit_GPS GPS(&mySerial);

#define GPSECHO  false // Set GPSECHO to 'false' to turn off echoing the GPS data to the Serial console

boolean usingInterrupt = false;
void useInterrupt(boolean);

void setup() { 
  
  Serial.begin(115200);   //Serial start
  
  XBee.begin(9600);   //XBee serial start

  GPS.begin(9600);   //GPS serial start
  
  useInterrupt(true);

  delay(1000);
  
  mySerial.println(PMTK_Q_RELEASE);
}

SIGNAL(TIMER0_COMPA_vect) {
  char c = GPS.read();
  
#ifdef UDR0
  if (GPSECHO)
    if (c) UDR0 = c;  

#endif
}

void useInterrupt(boolean v) {
  if (v) {
    
    OCR0A = 0xAF;
    TIMSK0 |= _BV(OCIE0A);
    usingInterrupt = true;
  }
  else {
    
    TIMSK0 &= ~_BV(OCIE0A);
    usingInterrupt = false;
  }
}

uint32_t timer = millis();
void loop() {                   
  
  if (! usingInterrupt) {
    
    char c = GPS.read(); // read data from the GPS in the 'main loop'
    
    if (GPSECHO)
      if (c) Serial.print(c);
  }
  
  // if a sentence is received, we can check the checksum, parse it...
  if (GPS.newNMEAreceived()) {
    
  if (!GPS.parse(GPS.lastNMEA()))   // this also sets the newNMEAreceived() flag to false
      return;  // we can fail to parse a sentence in which case we should just wait for another
  }

  // if millis() or timer wraps around, we'll just reset it
  if (timer > millis())  timer = millis();

  // approximately every 2 seconds or so, print out the current stats
  if (millis() - timer > 2000) { 
    
    timer = millis(); // reset the timer
    
    Serial.print( GPS.latitudeDegrees,4);Serial.print(":");Serial.print(GPS.longitudeDegrees,4);Serial.print("~");
      
    XBee.print(GPS.latitudeDegrees, 4);XBee.print(":");XBee.print(GPS.longitudeDegrees, 4);XBee.print("~");
      
    }
    
    }
  


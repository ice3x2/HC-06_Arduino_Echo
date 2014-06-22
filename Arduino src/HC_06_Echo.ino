#include <SoftwareSerial.h>

#define rxPin 2
#define txPin 3

#define ledPin 13

#define BUFF_SIZE 256

SoftwareSerial swSerial(txPin, rxPin);
uint8_t buffer[BUFF_SIZE];
uint8_t index = 0;
uint8_t data;


void setup() {
  swSerial.begin(9600);
  pinMode(ledPin, OUTPUT);
}

void loop() {
  digitalWrite(ledPin, LOW);
  while(!swSerial.available());
  digitalWrite(ledPin, HIGH);
  while(swSerial.available()) {
    data = swSerial.read();
    buffer[index++] = data;
    if(index == BUFF_SIZE || data == '\0') break;
    delay(1);
  }   

  for(uint8_t i = 0; i < index; ++i) {
    swSerial.write(buffer[i]);
  }
  index = 0;
}


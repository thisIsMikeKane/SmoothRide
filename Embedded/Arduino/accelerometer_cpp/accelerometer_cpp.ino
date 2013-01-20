#include <SoftwareSerial.h>
#include <AcceleroMMA7361.h>

#include <math.h>

int stPin = 7;
int gselPin = 6;
int zeroGPin = 5;
int sleepPin = 4;
int xPin = A4;
int yPin = A3;
int zPin = A2;

int ledPin = 13;
int potPin = A1;

int rxPin = 2;
int txPin = 3;
SoftwareSerial BTSerial = SoftwareSerial(rxPin,txPin);

void print(float x, float y, float z);
void updateAccels();
void onBump(boolean);
float map2(float,float,float,float);

AcceleroMMA7361 accelero; //accelerometer
float x;
float y;
float z;

float threshold = .5; //default, but not needed

void setup()
{
  Serial.begin(9600);
  BTSerial.begin(9600);
  pinMode(rxPin, INPUT);
  pinMode(txPin, OUTPUT);
  pinMode(ledPin,OUTPUT);
  accelero.begin(sleepPin, stPin, zeroGPin, gselPin, xPin, yPin, zPin);
  accelero.setARefVoltage(3.3);
  accelero.setSensitivity(LOW);
  accelero.calibrate(); //make sure accel is flat before running
  Serial.print("\n");
}

long int timeoutCount = 0;
short int sum1 = 0x0000,
          sum2 = 0x0000;

void loop()
{  
  updateAccels();
  if(BTSerial.available()){
    byte input = 0;
    input = BTSerial.read();
    if(input == 0x64){ //char ASCII 'd'
      Serial.println("Data push request");
      BTSerial.write((char)(sum1 & 0x00FF));
      BTSerial.write((char)(sum1 >> 8));
      BTSerial.write((char)(sum2 & 0x00FF));
      BTSerial.write((char)(sum2 >> 8));
      Serial.println("Pushed data: flushing vars");
      timeoutCount = 0; //flush because we pushed data
      sum1 = 0;
      sum2 = 0;
    }
  }
  if(timeoutCount >= 1500){ //every 30 seconds data isn't requested
     Serial.println("Timeout: flushing vars");
     timeoutCount = 0; //flush to avoid overflows
     sum1 = 0;
     sum2 = 0;
  }
  //print(x,y,z); //print the data to the hardware console
  
  double sum = 0;
  for(int i=0;i<10;i++){
    updateAccels();
    sum += sqrt((x*x)+(y*y)+(z*z));
  }
  (sum /= 10)--; //remove gravity
  
  //Serial.println(sum);
  threshold = map2(analogRead(potPin),0,1023,.2,1);
  //Serial.println(threshold); //print the current thresh to console
  if(sum >= threshold){ //greater than 1g in any direction
    onBump(true); //true because it's large
    sum1++;
    Serial.print("sum1 updated to: ");
    Serial.print(sum1);
    Serial.print("\r\n");
  }else if(sum >= threshold/2){
    onBump(false); //false because small
    Serial.print("sum2 updated to: ");
    Serial.print(sum2);
    Serial.print("\r\n");
    sum2++;
  }
  timeoutCount++;
}

void print(float x, float y, float z){
  Serial.print("x: ");
  Serial.print(x);
  Serial.print("\ty: ");
  Serial.print(y);
  Serial.print("\tz: ");
  Serial.print(z);
  Serial.print("\n");
}

void updateAccels(){
  x = accelero.getXAccel();
  y = accelero.getYAccel();
  z = accelero.getZAccel();
  x /= 100;
  y /= 100;
  z /= 100;
}

void onBump(boolean large){
  if(large){
    Serial.println("LARGE BUMP DETECTED"); //print to hardware
  }else
    Serial.println("small bump detected"); //print to hardware
    
  digitalWrite(ledPin,HIGH);
  delay(100);
  digitalWrite(ledPin,LOW);
}

float map2(float x, float in_min, float in_max, float out_min, float out_max)
{
  return (x - in_min) * (out_max - out_min) / (in_max - in_min) + out_min;
}

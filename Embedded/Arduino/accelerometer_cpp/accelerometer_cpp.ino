#include <AcceleroMMA7361.h>

#include <math.h>

//#define DEBUG

void print(float x, float y, float z);
void updateAccels();
void onBump();
float map2(float,float,float,float);

AcceleroMMA7361 accelero;
float x;
float y;
float z;

float threshold = .5;

void setup()
{
  Serial.begin(9600);
  pinMode(7,OUTPUT);
  accelero.begin(2, 5, 3, 4, A0, A1, A2);
  accelero.setARefVoltage(3.3);
  accelero.setSensitivity(LOW);
  accelero.calibrate(); //make sure accel is flat before running
  Serial.print("\n");
}

void loop()
{
  updateAccels();
  digitalWrite(7,LOW);
  
  #ifdef DEBUG
  print(x,y,z);
  #endif
  
  double sum = 0;
  for(int i=0;i<10;i++){
    updateAccels();
    sum += sqrt((x*x)+(y*y)+(z*z));
  }
  (sum /= 10)--; //remove gravity
  
  //Serial.println(sum);
  threshold = map2(analogRead(A3),0,1023,.2,1);
  //Serial.println(threshold);
  if(sum >= threshold) //greater than 1g in any direction
    onBump();
  
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

void onBump(){
  Serial.println("BUMP DETECTED");
  digitalWrite(7,HIGH);
  tone(9,500,100);
  //send packet via bluetooth
  //CODE goes here
}

float map2(float x, float in_min, float in_max, float out_min, float out_max)
{
  return (x - in_min) * (out_max - out_min) / (in_max - in_min) + out_min;
}

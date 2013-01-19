#include <AcceleroMMA7361.h>
#include <math.h>

#define DEBUG

void print(float x, float y, float z);
void updateAccels();

AcceleroMMA7361 accelero;
float x;
float y;
float z;

void setup()
{
  Serial.begin(9600);
  accelero.begin(2, 5, 3, 4, A0, A1, A2);
  accelero.setARefVoltage(3);
  accelero.setSensitivity(LOW);
  accelero.calibrate(); //make sure accel is flat before running
  Serial.print("\n");
}

void loop()
{
  //updateAccels();
  
  #ifdef DEBUG
  print(x,y,z);
  #endif
  
  double sum = 0;
  for(int i=0;i<50;i++){
    updateAccels();
    sum += sqrt((x*x)+(y*y)+(z*z));
  }
  (sum /= 50)--;
  
  Serial.print(sum);
  Serial.print("\n");
  delay(1);
}

void print(float x, float y, float z){
  Serial.print("x: ");
  Serial.print(x);
  Serial.print("g\ty: ");
  Serial.print(y);
  Serial.print("g\tz: ");
  Serial.print(z);
  Serial.print("g\n");
}

void updateAccels(){
  x = accelero.getXAccel();
  y = accelero.getYAccel();
  z = accelero.getZAccel();
  x /= 100;
  y /= 100;
  z /= 100;
}

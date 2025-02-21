#include <SoftwareSerial.h>
long pmcf10=0;
long pmcf25=0;
long pmcf100=0;
long pmat10=0;
long pmat25=0;
long pmat100=0;
char buf[50];
int count = 0;
unsigned char c, last_c;

SoftwareSerial Sensor (2, 4); // 接收腳RX, 傳送腳TX
SoftwareSerial Blueteeth(11,10);

void setup() {
  // put your setup code here, to run once:
  Serial.begin(9600);
  Blueteeth.begin(9600);
 Sensor.begin(9600);
} 
void loop() { 
  // put your main code here, to run repeatedly:
  bool start = false;
  unsigned char high;
  while(Sensor.available())
  {
last_c = c;
    c = Sensor.read();
    if(last_c == 0x42 && c == 0x4d)
    {
     count = 1;
    }
    if(count == 4 || count == 6 || count == 8 || count == 10 || count == 12 || count == 14)
    {
      high = c;
    }
    else if(count==5)
    {
}
else if(count==7)
    {
      pmcf25 = 256*high + c;
      Serial.print("CF=1, PM2.5= : ");
      Serial.print(pmcf25);
      Serial.print(" ug/m3");
      Serial.print("\t\t");
      Blueteeth.println(pmcf25);

}
    else if(count==9)
    {
    }
    else if(count==11)
    {
    }
    else if(count==13)
    {
      pmat25 = 256*high + c;
      Serial.print("atmosphere, PM2.5= : ");
      Serial.print(pmat25);
      Serial.println(" ug/m3");
}
else if(count==15)
    {
}

    if (count < 100) count++;
    }
}

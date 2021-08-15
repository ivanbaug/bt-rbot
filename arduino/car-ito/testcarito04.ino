HardwareSerial & Blue = Serial;

#include <Servo.h>

/**
 * Commands
*/

#define START_CMD_CHAR '*'
#define END_CMD_CHAR '#'
#define DIV_CMD_CHAR '|'
#define CMD_DIGITALWRITE 10
#define CMD_ANALOGWRITE 11
#define CMD_TEXT 12
#define CMD_READ_ARDUDROID 13
#define CMD_MOVE_CAR 14
#define CMD_READ_SENSORS 15
#define CMD_MOVE_ARMS 16
#define MAX_COMMAND 20  // max command number code. used for error checking.
#define MIN_COMMAND 10  // minimum command number code. used for error checking. 
#define IN_STRING_LENGHT 40
#define MAX_ANALOGWRITE 255
#define PIN_HIGH 3
#define PIN_LOW 2

//Pic's definitions
#define RIGHT_FW 11
#define RIGHT_BW 6
#define LEFT_FW 3
#define LEFT_BW 5

#define LEFT_ARM 9
#define RIGHT_ARM 10

#define C_FW 10
#define C_BW 11
#define C_L 12
#define C_R 13
#define C_STOP 14 

#define C_ARM_R 10
#define C_ARM_L 11

Servo SERVO_R;
Servo SERVO_L;

//Other definitions 
String inText;
char str[256];
unsigned int zeroState = 0;
unsigned int forwardState = 0;

long int data;
 

long int chup = 80;
long int chdw = 81; 
long int chri = 82;
long int chle = 83;

long int offup = 70;
long int offdw = 71; 
long int offri = 72;
long int offle = 73;
 
char state = 0;
 
 
void setup()
{
  Blue.begin(115200);
  Blue.setTimeout(5);

  pinMode(RIGHT_FW, OUTPUT);
  pinMode(RIGHT_BW, OUTPUT);
  pinMode(LEFT_FW, OUTPUT);
  pinMode(LEFT_BW, OUTPUT);

  SERVO_R.attach(9);
  SERVO_L.attach(10);
}
 
void loop(){

  int ard_command = 0;
  int pin_num = 0;
  int pin_value = 0;

  char get_char = ' ';  //read serial

  // wait for incoming data
  if (Serial.available() < 1) return; // if serial empty, return to loop().

  // parse incoming command start flag 
  get_char = Serial.read();
  if (get_char != START_CMD_CHAR) return; // if no command start flag, return to loop().

  // parse incoming command type
  ard_command = Serial.parseInt(); // read the command
  
  // parse incoming pin# and value  
  pin_num = Serial.parseInt(); // read the pin
  pin_value = Serial.parseInt();  // read the value

  // 1) GET TEXT COMMAND FROM ARDUDROID
  if (ard_command == CMD_TEXT){   
    inText =""; //clears variable for new input   
    while (Serial.available())  {
      char c = Serial.read();  //gets one byte from serial buffer
      delay(5);
      if (c == END_CMD_CHAR) { // if we the complete string has been read
        // add your code here
        break;
      }              
      else {
        if (c !=  DIV_CMD_CHAR) {
          inText += c; 
          delay(5);
        }
      }
    }
  }

  // 2) GET digitalWrite DATA FROM ARDUDROID
  if (ard_command == CMD_DIGITALWRITE){  
    if (pin_value == PIN_LOW) pin_value = LOW;
    else if (pin_value == PIN_HIGH) pin_value = HIGH;
    else return; // error in pin value. return. 
    set_digitalwrite( pin_num,  pin_value);  // Uncomment this function if you wish to use 
    return;  // return from start of loop()
  }

  // 3) GET analogWrite DATA FROM ARDUDROID
  if (ard_command == CMD_ANALOGWRITE) {  
    analogWrite(  pin_num, pin_value ); 
    // add your code here
    return;  // Done. return to loop();
  }

  // 4) SEND DATA TO ARDUDROID
  if (ard_command == CMD_READ_ARDUDROID) { 
    // char send_to_android[] = "Place your text here." ;
    // Serial.println(send_to_android);   // Example: Sending text
    //Serial.print("Analog 14 = "); 
    //Serial.println(analogRead(A14));  // Example: Read and send Analog pin value to Arduino
    return;  // Done. return to loop();
  }
  // 5) MOVE CAR With PWM
  if (ard_command == CMD_MOVE_CAR){ 

    zeroState=0;
    forwardState=100;

    analogWrite(RIGHT_BW, 0);  
    analogWrite(LEFT_BW, 0);
    analogWrite(RIGHT_FW, 0);  
    analogWrite(LEFT_FW, 0);  
  
  switch(pin_num){
  
    case C_BW:
      analogWrite(RIGHT_BW, 0);  
      analogWrite(LEFT_BW, 0);
      analogWrite(RIGHT_FW, pin_value);  
      analogWrite(LEFT_FW, pin_value);  
      break;
    case C_FW:
      analogWrite(RIGHT_BW, pin_value);  
      analogWrite(LEFT_BW, pin_value);
      analogWrite(RIGHT_FW, 0);  
      analogWrite(LEFT_FW, 0);
      break;
    case C_L:
      analogWrite(RIGHT_BW, 0);  
      analogWrite(LEFT_BW, pin_value);
      analogWrite(RIGHT_FW, 0);  
      analogWrite(LEFT_FW, 0);
      break;
    case C_R:
      analogWrite(RIGHT_BW, pin_value);  
      analogWrite(LEFT_BW, 0);
      analogWrite(RIGHT_FW, 0);  
      analogWrite(LEFT_FW, 0);
      break;
    case C_STOP:
      analogWrite(RIGHT_BW, zeroState);  
      analogWrite(LEFT_BW, zeroState);
      analogWrite(RIGHT_FW, 0);  
      analogWrite(LEFT_FW, 0);
      break;
    default:
      analogWrite(RIGHT_BW, 0);  
      analogWrite(LEFT_BW, 0);
      analogWrite(RIGHT_FW, 0);  
      analogWrite(LEFT_FW, 0);
      break;
  }
  return;  // return from start of loop()
  }
  // 6) MOVE SERVO ARMS
  if (ard_command == CMD_MOVE_ARMS) { 
  
    switch(pin_num){
      case C_ARM_R:
        SERVO_R.write(pin_value);
        break;
      case C_ARM_L:
        SERVO_L.write(pin_value);
        break;
      default:
        break;
    }
  return;
  }
    
}
// 2a) select the requested pin# for DigitalWrite action
void set_digitalwrite(int pin_num, int pin_value){
  /*
  switch (pin_num) {
  case 13:
    pinMode(13, OUTPUT);
    digitalWrite(13, pin_value);  
    // add your code here      
    break;
  case 12:
    pinMode(12, OUTPUT);
    digitalWrite(12, pin_value);   
    // add your code here       
    break;
  case 11:
    pinMode(11, OUTPUT);
    digitalWrite(11, pin_value);         
    // add your code here 
    break;
  case 10:
    pinMode(10, OUTPUT);
    digitalWrite(10, pin_value);         
    // add your code here 
    break;
  case 9:
    pinMode(9, OUTPUT);
    digitalWrite(9, pin_value);         
    // add your code here 
    break;
  case 8:
    pinMode(8, OUTPUT);
    digitalWrite(8, pin_value);         
    // add your code here 
    break;
  case 7:
    pinMode(7, OUTPUT);
    digitalWrite(7, pin_value);         
    // add your code here 
    break;
  case 6:
    pinMode(6, OUTPUT);
    digitalWrite(6, pin_value);         
    // add your code here 
    break;
  case 5:
    pinMode(5, OUTPUT);
    digitalWrite(5, pin_value); 
    // add your code here       
    break;
  case 4:
    pinMode(4, OUTPUT);
    digitalWrite(4, pin_value);         
    // add your code here 
    break;
  case 3:
    pinMode(3, OUTPUT);
    digitalWrite(3, pin_value);         
    // add your code here 
    break;
  case 2:
    pinMode(2, OUTPUT);
    digitalWrite(2, pin_value); 
    // add your code here       
    break;      
    // default: 
    // if nothing else matches, do the default
    // default is optional
  } 
  */
}

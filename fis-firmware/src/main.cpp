#include <Arduino.h>
#include <strings.h>
#include <BLEDevice.h>
#include <BLEUtils.h>
#include <BLEServer.h>
#define _TASK_SLEEP_ON_IDLE_RUN
#define _TASK_STD_FUNCTION   // Compile with support for std::function 
#include <TaskScheduler.h>


#define SERVICE_UUID   "4fafc201-1fb5-459e-8fcc-c5c9c331914b"
#define IGNITION0_UUID "beb5483e-36e1-4688-b7f5-ea07361b26a0"
#define IGNITION1_UUID "beb5483e-36e1-4688-b7f5-ea07361b26a1"
#define IGNITION2_UUID "beb5483e-36e1-4688-b7f5-ea07361b26a2"
#define IGNITION3_UUID "beb5483e-36e1-4688-b7f5-ea07361b26a3"

#define IGNITION0_PIN 16
#define IGNITION1_PIN 17
#define IGNITION2_PIN 18
#define IGNITION3_PIN 19

#define IGNITION_TIME 500
#define TASK_TIME     IGNITION_TIME/10

Scheduler runner;
BLECharacteristic *ignition0 = NULL;
BLECharacteristic *ignition1 = NULL;
BLECharacteristic *ignition2 = NULL;
BLECharacteristic *ignition3 = NULL;

class IgnitionCallback : public BLECharacteristicCallbacks {
  public:
    IgnitionCallback(uint8_t pin) : _pin(pin) {}

    void onRead(BLECharacteristic *pCharacteristic) { }

    void onWrite(BLECharacteristic *pCharacteristic) {
      pCharacteristic->getValue() == "s" ? digitalWrite(_pin, HIGH) : digitalWrite(_pin, LOW);
      // Start task to reset the pin
      _task.set(IGNITION_TIME * TASK_MILLISECOND, TASK_ONCE, [this, pCharacteristic]() {
        digitalWrite(this->_pin, LOW);
        pCharacteristic->setValue("r");
      });
      runner.addTask(_task);
      _task.enableDelayed();
    }

  private:
    uint8_t _pin;
    Task _task;
};

IgnitionCallback *ignition0_callback = new IgnitionCallback(IGNITION0_PIN);
IgnitionCallback *ignition1_callback = new IgnitionCallback(IGNITION1_PIN);
IgnitionCallback *ignition2_callback = new IgnitionCallback(IGNITION2_PIN);
IgnitionCallback *ignition3_callback = new IgnitionCallback(IGNITION3_PIN);

void setup() {
  Serial.begin(9600);
  Serial.println("Starting BLE work!");

  runner.init();

  pinMode(IGNITION0_PIN, OUTPUT);
  pinMode(IGNITION1_PIN, OUTPUT);
  pinMode(IGNITION2_PIN, OUTPUT);
  pinMode(IGNITION3_PIN, OUTPUT);

  digitalWrite(IGNITION0_PIN, LOW);
  digitalWrite(IGNITION1_PIN, LOW);
  digitalWrite(IGNITION2_PIN, LOW);
  digitalWrite(IGNITION3_PIN, LOW);

  BLEDevice::init("Fireworks ignition system");
  BLEServer *pServer = BLEDevice::createServer();
  BLEService *pService = pServer->createService(SERVICE_UUID);
  ignition0 = pService->createCharacteristic(IGNITION0_UUID, BLECharacteristic::PROPERTY_READ | BLECharacteristic::PROPERTY_WRITE);
  ignition1 = pService->createCharacteristic(IGNITION1_UUID, BLECharacteristic::PROPERTY_READ | BLECharacteristic::PROPERTY_WRITE);
  ignition2 = pService->createCharacteristic(IGNITION2_UUID, BLECharacteristic::PROPERTY_READ | BLECharacteristic::PROPERTY_WRITE);
  ignition3 = pService->createCharacteristic(IGNITION3_UUID, BLECharacteristic::PROPERTY_READ | BLECharacteristic::PROPERTY_WRITE);

  ignition0->setValue("r");
  ignition1->setValue("r");
  ignition2->setValue("r");
  ignition3->setValue("r");

  ignition0->setCallbacks(ignition0_callback);
  ignition1->setCallbacks(ignition1_callback);
  ignition2->setCallbacks(ignition2_callback);
  ignition3->setCallbacks(ignition3_callback);

  pService->start();
  // BLEAdvertising *pAdvertising = pServer->getAdvertising();  // this still is working for backward compatibility
  BLEAdvertising *pAdvertising = BLEDevice::getAdvertising();
  pAdvertising->addServiceUUID(SERVICE_UUID);
  pAdvertising->setScanResponse(true);
  pAdvertising->setMinPreferred(0x06);  // functions that help with iPhone connections issue
  pAdvertising->setMinPreferred(0x12);
  BLEDevice::startAdvertising();
  Serial.println("Characteristic defined! Now you can read it in your phone!");
}

void loop() {
  // put your main code here, to run repeatedly:
  runner.execute();
}

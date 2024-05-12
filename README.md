# Parking Manager
## Functional Requirements
- Lot has parking spaces of small, medium and large
- Lot spaces has an unique lot id assigned
- Cars has a license plate number (unique id)
- The lot can have 1-10 number of gates, cars could arrive simultaneously and leave simultaneously
- The system assigns the parking lots upon entering the lot, assignment is valid until the care leaves the gate
- Drivers behave correctly and take the assigned spot 100% of the time
- The most convenient parking spot is the one with the smallest number
- The system should
  - assign the most convenient spot (smallest id available) 
  - Turn the driver away as no spots available
## Non functional requirements
- Lot can be very big
- Simultaneous arrivals and leaves are limited to max 10 at the same time
- A gate can pass 20 cars/minute max
- QPS = 3 TPS peak
- Has to be able to operate 24/7, no downtime for upgrades
- High availability, redundant on all levels
## Settings:
- Parking lot structure is given at startup and does not change runtime

## Service endpoints
- lotId enter(LicPlate, size, color)
- void exit(LicPlate)

## Architecture:
- 2 load balancer monitoring each other
- 3+ java app server (spring)
- Data layer: Redis cluster or pair with sentinel
-- Reservation transactions as conditional updates - reserve only if it is not taken, retry with the next otherwise
-- configured with durability

## build:

build: `mvn clean compile`

configure redis password: `$Env:redis_password='changeme'`

run: `mvn spring-boot:run`


tests (powershell):

`curl -Method Post -Body '{}' http://localhost:8080/enter/111`

`curl -Method Post -Body '{}' http://localhost:8080/exit/111`

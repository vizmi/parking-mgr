# Parking Manager

A REST service that assigns parking spots to arriving cars. The interesting part isn't the CRUD shape of it — it's that up to 10 gates can process arrivals and departures at the same time, and no two cars must ever be handed the same spot.

## Correctness under concurrency

The naive approach — read the list of free spots, pick one, write it back as taken — races: two gates can read the same free spot before either writes its claim, and both cars get sent to it.

This service avoids that by making "pick the smallest free spot and remove it" a single atomic operation instead of a read-then-write:

- Free spots live in a Redis sorted set (`empty.lots`), scored by lot id.
- `enter()` reserves a spot with `ZPOPMIN`, which atomically returns *and removes* the lowest-scored member in one round trip (see `PMService.enter`). Two concurrent callers can never be handed the same element — Redis serializes the command, so the second caller simply gets the next-lowest spot.
- `exit()` returns the spot with `ZADD`, making it available for the next `ZPOPMIN`.

This means the "smallest available id, no double-booking" guarantee from the requirements below holds regardless of how many gates fire simultaneously — it's enforced by Redis's command atomicity, not by locking or by trusting caller ordering.

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

# Asia Pacific Airport Simulation

This project models a small airport where six aircraft compete for shared physical resources. The system is built entirely using hand-written monitors (`synchronized`, `wait()`, and `notifyAll()`), adhering to a restricted-library rule that forbids automatic concurrency facilities. 

### Key Features
* **Resource Management:** Coordinates access to one runway, three gates, and one refueling truck.
* **ATC Admission Control:** A dedicated Air Traffic Control thread enforces a maximum ground capacity of three aircraft.
* **Emergency Priority:** Handles congested sky queues by allowing a designated emergency aircraft to bypass standard FIFO ordering.
* **Passenger Multithreading:** Spawns 12 individual passenger group threads (disembarking and embarking) with randomized sizes between 10 and 50 passengers.
* **Concurrent Servicing:** Utilizes two-way handshakes with daemon threads for cleaning and restocking crews alongside the refueling process.
* **Statistics Tracking:** A synchronized monitor records and prints the maximum, average, and minimum waiting times, alongside total boarded passengers.

### Concurrency Safety Mechanisms
* **Race Conditions:** Prevented by wrapping every shared resource (runway, ground count, gates, truck, statistics) in a monitor with check-and-update atomicity.
* **Deadlock Prevention:** Enforces a consistent acquisition order and early resource release, ensuring aircraft never hold the runway while waiting for a gate.
* **Livelock Prevention:** The ATC prioritizes take-off requests before landing requests to prevent ground gridlock.
* **Starvation Bounding:** Normal aircraft cannot be indefinitely bypassed, as the emergency overtake is limited to a single designated aircraft.

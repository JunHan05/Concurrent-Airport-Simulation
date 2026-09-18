/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package ccp.assignment;

import java.util.Random;

public class Plane implements Runnable {
    private final String id;
    private final boolean isEmergency;
    private final int passengerCount;

    private final ATC atc;
    private final GroundCapacity ground;
    private final Runway runway;
    private final GatePool gatePool;
    private final FuelTruck fuelTruck;
    private final Statistics stats;

    public Plane(String id, boolean isEmergency, ATC atc, GroundCapacity ground, Runway runway, GatePool gatePool, FuelTruck fuelTruck, Statistics stats) {
        this.id = id;
        this.isEmergency = isEmergency;
        this.atc = atc;
        this.ground = ground;
        this.runway = runway;
        this.gatePool = gatePool;
        this.fuelTruck = fuelTruck;
        this.stats = stats;
        this.passengerCount = new Random().nextInt(41) + 10; // 10 to 50 passengers
    }

    public String getId() { return id; }

    @Override
    public void run() {
        String me = Thread.currentThread().getName();
        Gate assignedGate = null;
        // Waiting time is measured from the moment THIS plane starts requesting to land.
        long arrivalTime = System.currentTimeMillis();
        try {
            // ---- REQUEST LANDING (announced by the plane's OWN thread) ----
            System.out.println(me + " : " + id + ": Requesting Landing."
                    + (isEmergency ? " [EMERGENCY - FUEL SHORTAGE]" : ""));
            atc.requestLanding(id, isEmergency);

            // Wait until the ATC thread grants landing AND assigns a gate.
            Object lock = AirportSimulation.getLock(id);
            synchronized (lock) {
                while (AirportSimulation.getAssignedGate(id) == null) {
                    lock.wait();
                }
            }
            assignedGate = AirportSimulation.getAssignedGate(id);
            long waitDuration = System.currentTimeMillis() - arrivalTime;

            // ---- LAND ----
            ground.enter(id);           // never blocks (ATC caps admissions at 3) - safety net
            runway.acquire(id);
            System.out.println(me + " : " + id + ": Landing.");
            Thread.sleep(500);
            System.out.println(me + " : " + id + ": Landed.");

            // ---- COAST TO GATE, releasing the runway EARLY to avoid gridlock ----
            System.out.println(me + " : " + id + ": Coasting to Gate-" + assignedGate.getId() + ".");
            runway.release(id);
            Thread.sleep(300);
            System.out.println(me + " : " + id + ": Docked at Gate-" + assignedGate.getId() + ".");

            // ---- DISEMBARK (Type-1 passengers: spawned BY THE PLANE) ----
            spawnDisembarkingPassengers();

            // ---- SERVICE: cleaning + restocking crews concurrently; ONE shared fuel truck ----
            fuelTruck.acquire(id);
            assignedGate.servicePlane(id);
            System.out.println(me + " : " + id + ": Refuelling in progress.");
            Thread.sleep(2800);
            fuelTruck.release(id);

            // ---- EMBARK (Type-2 passengers: spawned BY THE GATE) ----
            assignedGate.spawnEmbarkingPassengers(id, passengerCount);

            // ---- REQUEST TAKE-OFF (clearance granted by the ATC thread) ----
            System.out.println(me + " : " + id + ": Requesting Taking off.");
            atc.requestTakeoff(id);
            synchronized (lock) {
                while (!AirportSimulation.isTakeoffGranted(id)) {
                    lock.wait();
                }
            }

            // ---- UNDOCK, TAKE OFF ----
            System.out.println(me + " : " + id + ": Undocked from Gate-" + assignedGate.getId() + ".");
            gatePool.release(assignedGate);
            assignedGate = null;
            runway.acquire(id);
            System.out.println(me + " : " + id + ": Taking-off.");
            Thread.sleep(500);
            runway.release(id);
            ground.leave(id);

            // Tell ATC a ground slot has freed so it can admit the next circling plane.
            atc.notifyDeparture(id);

            stats.recordPlaneServed(waitDuration, passengerCount);

        } catch (InterruptedException e) {
            System.out.println(me + " : " + id + ": Lifecycle interrupted.");
            Thread.currentThread().interrupt();
        } finally {
            if (assignedGate != null) {
                gatePool.release(assignedGate); // fallback so a failure never strands a gate
            }
        }
    }

    // Type-1 passenger GROUP: ONE thread spawned BY THE PLANE (they landed with it).
    private void spawnDisembarkingPassengers() throws InterruptedException {
        Passenger group = new Passenger(Passenger.Type.DISEMBARK, id, id + "'s Passengers", passengerCount);
        Thread t = new Thread(group, "Thread-" + id + "-Passengers");
        t.start();
        t.join();   // servicing may not begin until everyone is off the plane
    }
}

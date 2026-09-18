/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package ccp.assignment;

public class Gate {
    private final int id;
    private boolean occupied = false;
    private String dockedPlaneId = null;

    private final CleaningCrew cleaningCrew;
    private final RestockingCrew restockingCrew;

    public Gate(int id) {
        this.id = id;
        this.cleaningCrew = new CleaningCrew("Gate-" + id + "-CleaningCrew");
        this.restockingCrew = new RestockingCrew("Gate-" + id + "-RestockingCrew");
        // Support crews are persistent background workers that only exist to serve planes.
        // Marked as DAEMON so they don't keep the JVM alive after all planes are done.
        Thread cleaningThread = new Thread(cleaningCrew, cleaningCrew.getName());
        Thread restockingThread = new Thread(restockingCrew, restockingCrew.getName());
        cleaningThread.setDaemon(true);
        restockingThread.setDaemon(true);
        cleaningThread.start();
        restockingThread.start();
    }

    public int getId() { return id; }
    public synchronized boolean isOccupied() { return occupied; }

    // Reservation happens at ATC grant time (no announcement here — ATC announces assignment, the plane announces its own docking when it physically arrives).
    public synchronized void reserve(String planeId) {
        this.occupied = true;
        this.dockedPlaneId = planeId;
    }

    public synchronized void free() {
        this.occupied = false;
        this.dockedPlaneId = null;
    }

    public void servicePlane(String planeId) throws InterruptedException {
        // Trigger both persistent crews, then wait for both via the two-way handshake.
        cleaningCrew.triggerWork(planeId);
        restockingCrew.triggerWork(planeId);
        cleaningCrew.waitForCompletion();
        restockingCrew.waitForCompletion();
    }

    // Type-2 passenger GROUP: ONE thread spawned BY THE GATE (they wait at the gate to board).
    public void spawnEmbarkingPassengers(String planeId, int count) throws InterruptedException {
        Passenger group = new Passenger(Passenger.Type.EMBARK, planeId, "Gate-" + id + "'s Passengers", count);
        Thread t = new Thread(group, "Thread-Gate-" + id + "-Passengers-" + planeId);
        t.start();
        t.join();   // plane may not undock until boarding is complete
    }
}
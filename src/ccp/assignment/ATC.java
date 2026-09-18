/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package ccp.assignment;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/*
 * Air Traffic Control runs on ITS OWN thread and is the single decision-maker:
 *  - grants/denies landing (admission control: max 3 planes on the ground),
 *  - assigns the gate at grant time,
 *  - grants take-off clearance,
 *  - applies hand-rolled emergency priority (PriorityBlockingQueue is banned).
 * Every ATC announcement is printed BY the ATC thread (Figure 2 requirement).
 */
public class ATC implements Runnable {
    private final List<String> circlingQueue = new ArrayList<>(); // planes waiting in the sky
    private final List<String> takeoffQueue = new ArrayList<>();  // planes asking to depart
    private final Set<String> denialAnnounced = new HashSet<>();  // avoid repeating denials
    private String emergencyPlaneId = null;

    private int admittedCount = 0;              // planes granted landing but not yet departed
    private static final int MAX_GROUND = 3;

    private volatile boolean running = true;    // volatile: written by main, read by ATC loop
    private final GatePool gatePool;

    public ATC(GatePool gatePool) { this.gatePool = gatePool; }

    // Called by a plane thread. No printing here — the PLANE announces its own request.
    public synchronized void requestLanding(String planeId, boolean isEmergency) {
        if (isEmergency) {
            emergencyPlaneId = planeId;
            circlingQueue.add(0, planeId);      // emergency goes to the FRONT of the queue
        } else {
            circlingQueue.add(planeId);
        }
        notifyAll();                            // wake the ATC thread
    }

    // Called by a plane thread when it is ready to depart.
    public synchronized void requestTakeoff(String planeId) {
        takeoffQueue.add(planeId);
        notifyAll();
    }

    // Called by a plane thread AFTER it has left the airport: frees one ground slot.
    public synchronized void notifyDeparture(String planeId) {
        if (admittedCount > 0) admittedCount--;
        notifyAll();
    }
    
    // CONGESTION BARRIER: blocks the caller until at least n planes are circling.
    // Used by main to launch the emergency plane only once the congested scenario truly exists (requestLanding() already notifyAll()s, which wakes this wait).
    public synchronized void awaitCircling(int n) throws InterruptedException {
        while (circlingQueue.size() < n) {
            wait();
        }
    }

    public synchronized void shutdown() {
        running = false;
        notifyAll();
    }

    @Override
    public void run() {
        String me = Thread.currentThread().getName();
        System.out.println(me + " : ATC: Air Traffic Control Tower operational.");
        try {
            while (true) {
                String landPlane = null, takeoffPlane = null;
                boolean emergency = false, stop = false;

                synchronized (this) {
                    while (true) {
                        // 1) Take-offs first: departures free ground slots for waiting planes.
                        if (!takeoffQueue.isEmpty()) {
                            takeoffPlane = takeoffQueue.remove(0);
                            break;
                        }
                        // 2) Admit a landing only when the ground has room (max 3).
                        if (!circlingQueue.isEmpty() && admittedCount < MAX_GROUND) {
                            if (emergencyPlaneId != null && circlingQueue.contains(emergencyPlaneId)) {
                                landPlane = emergencyPlaneId;         // emergency jumps the queue
                                circlingQueue.remove(emergencyPlaneId);
                                emergencyPlaneId = null;
                                emergency = true;
                            } else {
                                landPlane = circlingQueue.remove(0);  // otherwise FIFO
                            }
                            admittedCount++;                          // reserve the ground slot
                            break;
                        }
                        // 3) Ground full: announce the denial once per waiting plane (sample format).
                        if (!circlingQueue.isEmpty() && admittedCount >= MAX_GROUND) {
                            for (String p : circlingQueue) {
                                if (denialAnnounced.add(p)) {
                                    if (p.equals(emergencyPlaneId)) {
                                        System.out.println(me + " : ATC: EMERGENCY acknowledged for " + p + ". Priority position granted in circle queue.");
                                    } else {
                                        System.out.println(me + " : ATC: Landing Permission Denied for " + p + ", Airport Full. Please join the circle queue.");
                                    }
                                }
                            }
                        }
                        // 4) Nothing left to do and shutdown requested -> exit.
                        if (!running && circlingQueue.isEmpty() && takeoffQueue.isEmpty()) {
                            stop = true;
                            break;
                        }
                        wait();   // sleep until a request, a departure, or shutdown wakes us
                    }
                }

                if (stop) break;

                if (takeoffPlane != null) {
                    // Grant departure clearance (printed by the ATC thread).
                    System.out.println(me + " : ATC: Taking-off is granted for " + takeoffPlane + ". Runway is free.");
                    Object lock = AirportSimulation.getLock(takeoffPlane);
                    synchronized (lock) {
                        AirportSimulation.setTakeoffGranted(takeoffPlane);
                        lock.notifyAll();
                    }
                }

                if (landPlane != null) {
                    // Grant landing + assign a gate. Guaranteed non-blocking: at most 2 other
                    // planes are admitted (we hold slot #3), each holding at most 1 of 3 gates,
                    // so at least one gate is always free at admission time.
                    if (emergency) {
                        System.out.println(me + " : ATC: EMERGENCY Landing Permission granted for "
                                + landPlane + " (jumping the queue).");
                    } else {
                        System.out.println(me + " : ATC: Landing Permission granted for " + landPlane + ".");
                    }
                    Gate gate = gatePool.assign(landPlane);
                    System.out.println(me + " : ATC: Gate-" + gate.getId() + " assigned for " + landPlane + ".");

                    Object lock = AirportSimulation.getLock(landPlane);
                    synchronized (lock) {
                        AirportSimulation.setAssignedGate(landPlane, gate);
                        lock.notifyAll();
                    }
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        System.out.println(me + " : ATC: Air Traffic Control Tower shutting down cleanly.");
    }
}

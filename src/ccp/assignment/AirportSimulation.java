/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Main.java to edit this template
 */
package ccp.assignment;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Random;

public class AirportSimulation {
    // Per-plane handshake objects: the ATC thread hands decisions to plane threads here.
    private static final Map<String, Object> locks = new HashMap<>();
    private static final Map<String, Gate> assignedGates = new HashMap<>();
    private static final Set<String> takeoffGranted = new HashSet<>();

    public static synchronized Object getLock(String planeId) {
        locks.putIfAbsent(planeId, new Object());
        return locks.get(planeId);
    }
    public static synchronized Gate getAssignedGate(String planeId) { return assignedGates.get(planeId); }
    public static synchronized void setAssignedGate(String planeId, Gate g) { assignedGates.put(planeId, g); }
    public static synchronized boolean isTakeoffGranted(String planeId) { return takeoffGranted.contains(planeId); }
    public static synchronized void setTakeoffGranted(String planeId) { takeoffGranted.add(planeId); }

    public static void main(String[] args) {
        System.out.println("--- STARTING ASIA PACIFIC AIRPORT SIMULATION ---\n");

        GroundCapacity ground = new GroundCapacity();
        Runway runway = new Runway();
        GatePool gatePool = new GatePool();
        FuelTruck fuelTruck = new FuelTruck();
        Statistics stats = new Statistics();

        ATC atcInstance = new ATC(gatePool);
        Thread atcThread = new Thread(atcInstance, "Thread-ATC");
        atcThread.start();

        Plane[] planes = new Plane[6];
        Thread[] planeThreads = new Thread[6];
        for (int i = 0; i < 6; i++) {
            String planeId = "Plane-" + (i + 1);
            boolean isEmergency = (i == 5); // the 6th plane is the emergency aircraft
            planes[i] = new Plane(planeId, isEmergency, atcInstance, ground, runway, gatePool, fuelTruck, stats);
            planeThreads[i] = new Thread(planes[i], "Thread-" + planeId);
        }

        try {
            Random rand = new Random();
        
            // Normal planes arrive every 0-2 seconds (per the brief's random-arrival rule).
            for (int i = 0; i < 5; i++) {
                planeThreads[i].start(); 
                Thread.sleep(rand.nextInt(2000));
            }
            
            // CCP tool used to guarantee the congested scenario: do not launch the emergency until at least 2 planes are circling, so the emergency is never first in the queue and always has planes to jump ahead of.
            atcInstance.awaitCircling(2);
            System.out.println("Congestion confirmed: launching EMERGENCY Plane-6...");
            planeThreads[5].start();

            for (Thread pt : planeThreads) {
                pt.join();
            }

            atcInstance.shutdown();
            atcThread.join();

        } catch (InterruptedException e) {
            System.out.println("Main simulation thread interrupted.");
            Thread.currentThread().interrupt();
        }

        stats.printReport(gatePool);
        System.out.println("--- SIMULATION SYSTEM TERMINATED SUCCESSFULLY ---");
    }
}
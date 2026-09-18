/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package ccp.assignment;

public class GatePool {
    private final Gate[] gates = new Gate[3];

    public GatePool() {
        for (int i = 0; i < 3; i++) {
            gates[i] = new Gate(i + 1);
        }
    }

    // Called ONLY by the ATC thread at grant time. Reserves a free gate for the plane.
    // (Printing "Gate assigned" is done by the ATC; docking is announced by the plane.)
    public synchronized Gate assign(String planeId) throws InterruptedException {
        while (true) {
            for (Gate gate : gates) {
                if (!gate.isOccupied()) {
                    gate.reserve(planeId);
                    return gate;
                }
            }
            wait(); // defensive: with 3 gates + ground cap 3 this never actually blocks
        }
    }

    // Called by the plane thread when it undocks.
    public synchronized void release(Gate gate) {
        gate.free();
        notifyAll();
    }

    public synchronized boolean areAllGatesEmpty() {
        for (Gate gate : gates) {
            if (gate.isOccupied()) return false;
        }
        return true;
    }
}

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package ccp.assignment;

/*
 * One Passenger thread represents a WHOLE GROUP of passengers (per lecturer's clarification):
 *  - Type 1 (DISEMBARK): the group that landed with the plane. Spawned BY THE PLANE.
 *  - Type 2 (EMBARK):    the group waiting at the gate to board. Spawned BY THE GATE.
 * 6 planes x 2 groups = 12 passenger threads in total.
 */
public class Passenger implements Runnable {
    public enum Type { DISEMBARK, EMBARK }

    private final Type type;
    private final String planeId;
    private final String groupLabel;   // e.g. "Plane-1's Passengers" or "Gate-2's Passengers"
    private final int count;           // how many passengers this group represents (<= 50)

    public Passenger(Type type, String planeId, String groupLabel, int count) {
        this.type = type;
        this.planeId = planeId;
        this.groupLabel = groupLabel;
        this.count = count;
    }

    @Override
    public void run() {
        String me = Thread.currentThread().getName();
        try {
            if (type == Type.DISEMBARK) {
                System.out.println(me + " : " + groupLabel + ": Disembarking out of " + planeId + ".");
                Thread.sleep(15L * count);   // time proportional to group size
                System.out.println(me + " : " + groupLabel + ": All " + count
                        + " passengers have disembarked from " + planeId + ".");
            } else {
                System.out.println(me + " : " + groupLabel + ": Boarding " + planeId + " now.");
                Thread.sleep(15L * count);
                System.out.println(me + " : " + groupLabel + ": All " + count
                        + " passengers have boarded " + planeId + ".");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package ccp.assignment;

public class Statistics {
    private int planesServed = 0;
    private int totalPassengersBoarded = 0;
    private long minWaitingTime = Long.MAX_VALUE;
    private long maxWaitingTime = 0;
    private long totalWaitingTime = 0;

    public synchronized void recordPlaneServed(long waitingTime, int passengersBoarded) {
        planesServed++;
        totalPassengersBoarded += passengersBoarded;
        totalWaitingTime += waitingTime;
        if (waitingTime < minWaitingTime) minWaitingTime = waitingTime;
        if (waitingTime > maxWaitingTime) maxWaitingTime = waitingTime;
    }

    public synchronized void printReport(GatePool gatePool) {
        System.out.println("\n=========================================");
        System.out.println("       ATC MANAGER SANITY CHECK         ");
        System.out.println("=========================================");
        boolean gatesEmpty = gatePool.areAllGatesEmpty();
        System.out.println("Sanity Check: Are all gates empty? -> " + (gatesEmpty ? "PASSED" : "FAILED"));
        System.out.println("\n=========================================");
        System.out.println("          AIRPORT RUN STATISTICS         ");
        System.out.println("=========================================");
        System.out.println("Total Planes Fully Served : " + planesServed);
        System.out.println("Total Passengers Boarded  : " + totalPassengersBoarded);
        if (planesServed > 0) {
            System.out.println("Minimum Waiting Time      : " + minWaitingTime + " ms");
            System.out.println("Maximum Waiting Time      : " + maxWaitingTime + " ms");
            System.out.println("Average Waiting Time      : " + ((double) totalWaitingTime / planesServed) + " ms");
        } else {
            System.out.println("No planes were served.");
        }
        System.out.println("=========================================\n");
    }
}

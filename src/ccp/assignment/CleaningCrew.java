/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package ccp.assignment;

public class CleaningCrew implements Runnable {
    private final String name;
    private String activePlaneId = null;
    private boolean workRequested = false;
    private boolean workDone = false;

    public CleaningCrew(String name) { this.name = name; }
    public String getName() { return name; }

    public synchronized void triggerWork(String planeId) {
        this.activePlaneId = planeId;
        this.workRequested = true;
        this.workDone = false;
        notifyAll();
    }

    public synchronized void waitForCompletion() throws InterruptedException {
        while (!workDone) {
            wait();
        }
    }

    @Override
    public void run() {
        try {
            while (true) {
                synchronized (this) {
                    while (!workRequested) {
                        wait();
                    }
                }
                // Work happens OUTSIDE the monitor so the plane isn't blocked while the crew works.
                // Reading activePlaneId here without the lock is safe: only one plane can be docked
                // at this gate at a time, so the field cannot change mid-service, and the
                // synchronized handshake before/after provides the memory-visibility barrier.
                System.out.println(Thread.currentThread().getName() + " : Starting cleanup for " + activePlaneId);
                Thread.sleep(3500);
                System.out.println(Thread.currentThread().getName() + " : Cleaning completed for " + activePlaneId);
                synchronized (this) {
                    workRequested = false;
                    workDone = true;
                    notifyAll();
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}

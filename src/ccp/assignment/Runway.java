/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package ccp.assignment;

public class Runway {
    private boolean free = true;

    public synchronized void acquire(String planeId) throws InterruptedException {
        while (!free) {
            wait();
        }
        free = false;
        System.out.println(Thread.currentThread().getName() + " : " + planeId + " entered the Runway.");
    }

    public synchronized void release(String planeId) {
        free = true;
        System.out.println(Thread.currentThread().getName() + " : " + planeId + " cleared the Runway.");
        notifyAll();
    }
}

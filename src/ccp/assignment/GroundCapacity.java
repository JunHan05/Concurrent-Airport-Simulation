/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package ccp.assignment;

public class GroundCapacity {
    private int onGround = 0;
    private final int MAX = 3;

    public synchronized void enter(String planeId) throws InterruptedException {
        while (onGround >= MAX) {
            wait();
        }
        onGround++;
        System.out.println(Thread.currentThread().getName() + " : " + planeId + " entered airport ground bounds.");
    }

    public synchronized void leave(String planeId) {
        onGround--;
        System.out.println(Thread.currentThread().getName() + " : " + planeId + " departed airport ground bounds.");
        notifyAll();
    }
}


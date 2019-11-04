package edu.uci.ics.marior6.manager.core;

import javafx.util.Pair;
import java.util.LinkedList;

public class ResourceControlBlock {
    private int counter;                //number of resources available
    private int inventory;              //initial number of resources
    private LinkedList<Pair<Integer, Integer>> waitlist;   //processes blocked on this resource

    public ResourceControlBlock() { }

    public ResourceControlBlock(int state, int inventory, LinkedList<Pair<Integer, Integer>> waitlist) {
        this.counter = state;
        this.inventory = inventory;
        this.waitlist = waitlist;
    }

    public int getCounter() {
        return counter;
    }

    public void setCounter(int counter) {
        this.counter = counter;
    }

    public int getInventory() {
        return inventory;
    }

    public void setInventory(int inventory) {
        this.inventory = inventory;
    }

    public LinkedList<Pair<Integer, Integer>> getWaitlist() {
        return waitlist;
    }

    public void setWaitlist(LinkedList<Pair<Integer, Integer>> waitlist) {
        this.waitlist = waitlist;
    }
}

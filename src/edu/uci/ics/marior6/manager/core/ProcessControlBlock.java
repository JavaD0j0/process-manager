package edu.uci.ics.marior6.manager.core;

import javafx.util.Pair;
import java.util.LinkedList;

public class ProcessControlBlock {
    private int state;                      //current state of the process (-1: Free, 1: ready, 0: blocked)
    private Integer parent;                 //index of the process that created this process
    private int priority;                   //priority given to this process (1 or 2)
    private LinkedList<Integer> children;   //list of processes created by this process
    private LinkedList<Pair<Integer, Integer>> resources;  //list of resources held by this process

    public ProcessControlBlock() { }

    public ProcessControlBlock(int state, Integer parent, int priority, LinkedList<Integer> children,
                               LinkedList<Pair<Integer, Integer>> resources) {
        this.state = state;
        this.parent = parent;
        this.priority = priority;
        this.children = children;
        this.resources = resources;
    }

    public int getState() {
        return state;
    }

    public void setState(int state) {
        this.state = state;
    }

    public Integer getParent() {
        return parent;
    }

    public void setParent(Integer parent) {
        this.parent = parent;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public LinkedList<Integer> getChildren() {
        return children;
    }

    public void setChildren(LinkedList<Integer> children) {
        this.children = children;
    }

    public LinkedList<Pair<Integer, Integer>> getResources() {
        return resources;
    }

    public void setResources(LinkedList<Pair<Integer, Integer>> resources) {
        this.resources = resources;
    }
}

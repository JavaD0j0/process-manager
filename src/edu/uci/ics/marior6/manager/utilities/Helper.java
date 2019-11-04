package edu.uci.ics.marior6.manager.utilities;

import edu.uci.ics.marior6.manager.core.ProcessControlBlock;
import edu.uci.ics.marior6.manager.core.ResourceControlBlock;
import javafx.util.Pair;

import java.util.HashMap;
import java.util.LinkedList;

import static edu.uci.ics.marior6.manager.Manager.*;

public class Helper {
    public static int insertNewProcess(ProcessControlBlock process) {
        int index = -1;
        for (int i = 0; i < processControlBlock.size(); ++i){
            if(processControlBlock.get(i).getState() == FREE){
                processControlBlock.set(i, process);
                index = i;
                break;
            }
        }
        return index;
    }

    public static void insertToRL(Integer processIndex, Integer priority) {
        LinkedList<Integer> listOfProcesses = readyList.get(priority);
        LinkedList<Integer> newList = new LinkedList<>();
        if(listOfProcesses == null){
            System.out.println("There are no processes at this priority... adding first one!");
            newList.add(processIndex);
            readyList.put(priority, newList);
        } else {
            System.out.println("Number of processes at priority " + priority + ": " + listOfProcesses);
            newList.addAll(listOfProcesses);
            newList.add(processIndex);
            readyList.replace(priority, newList);
        }
        //clear variables
        //newList.remove(processIndex);
        if (listOfProcesses != null) listOfProcesses.clear();
    }

    public static void insertNewChild(Integer childIndex) {
        LinkedList<Integer> children = processControlBlock.get(runningProcess).getChildren();
        LinkedList<Integer> newChildren = new LinkedList<>();
        if(children == null) {
            System.out.println("There are no children for this process... adding first one!");
        } else {
            System.out.println("Process " + runningProcess + " children: " + children);
            newChildren.addAll(children);
        }
        newChildren.add(childIndex);
        processControlBlock.get(runningProcess).setChildren(newChildren);

        if(children != null) children.clear();
        System.out.println("Done adding process" + childIndex + " to process" + runningProcess + " children list!");
    }

    public static int eraseChildren(LinkedList<Integer> children, int counter) {
        if (children != null) {
            int childrenSize = children.size();
            for (int i = 0; i < childrenSize; i++) {
                Integer child = children.getFirst();
                //check this children for other children and recursively call
                LinkedList<Integer> otherChildren = processControlBlock.get(child).getChildren();
                if (otherChildren != null) {
                    eraseChildren(otherChildren, counter);
                }
                releaseResource(child); //should i switch this?
                eraseFromReadyListOrWaitlist(child);
                processControlBlock.get(child).setState(FREE);
                processControlBlock.get(child).setPriority(0);
                processControlBlock.get(child).setParent(null);
                processControlBlock.get(child).setChildren(null);
                processControlBlock.get(child).setResources(null);
                children.remove();
                counter++;
            }
        }
        return counter;
    }

    public static void eraseFromReadyListOrWaitlist(Integer childIndex) {
        Integer jPriority = processControlBlock.get(childIndex).getPriority();
        if (readyList.get(jPriority).contains(childIndex)) {
            readyList.get(jPriority).remove(childIndex);
            System.out.println("Process " + childIndex + " has been removed form RL");
            System.out.println("New RL at level " + jPriority + ": " + readyList.get(jPriority));
        } else if (processControlBlock.get(childIndex).getState() == BLOCKED) {
            for (int i = 0; i < resourceControlBlock.size(); i++) {
                ResourceControlBlock controlBlock = resourceControlBlock.get(i);
                LinkedList<Pair<Integer, Integer>> waitlist = controlBlock.getWaitlist();
                System.out.println("Resource " + i + " waitlist: " + waitlist);
                if (waitlist != null) {
                    for (int j = 0; j < waitlist.size(); ++j) {
                        if (waitlist.get(j).getKey().equals(childIndex)) {
                            Pair<Integer, Integer> pair = new Pair<>(waitlist.get(j).getKey(), waitlist.get(j).getValue());
                            waitlist.remove(pair);
                            System.out.println("Removed " + childIndex + " from resource " + i + " waitlist!");
                            if (waitlist.size() == 0)
                                controlBlock.setWaitlist(null);
                            else
                                controlBlock.setWaitlist(waitlist);
                        }
                    }
                }
            }
        }
    }

    public static void releaseResource(Integer childIndex) {
        LinkedList<Pair<Integer, Integer>> resources = processControlBlock.get(childIndex).getResources();
        if (resources != null) {
            //FIXME: add int resourceSize = resources.size(); and fix the for loop
            for (int k = 0; k < resources.size(); ++k) {
                int resource = resources.get(k).getKey();   //resource being released
                int amount = resources.get(k).getValue();   //amount of resource being released
                int counter = resourceControlBlock.get(resource).getCounter();
                resourceControlBlock.get(resource).setCounter(counter + amount);
                //FIXME: should it be counter instead of amount??
                checkResourceWaitlist(resource, amount);
                resources.remove();
            }
            System.out.println("Process " + childIndex + " resources: " + resources);
            if (resources.size() == 0)
                processControlBlock.get(childIndex).setResources(null);
            else
                processControlBlock.get(childIndex).setResources(resources);
        }
    }

    public static void checkResourceWaitlist(int resource, int amountReleased) {
        LinkedList<Pair<Integer, Integer>> waitlist = resourceControlBlock.get(resource).getWaitlist();
        System.out.println("Waitlist for resource " + resource + " is: " + waitlist);
        int counter = resourceControlBlock.get(resource).getCounter();
        //loop thru the waitlist or until counter reached 0 and no more outstanding request can be made
        while (waitlist != null && counter > 0) {
            Pair<Integer, Integer> nextPair = waitlist.getFirst();
            Integer amount = nextPair.getValue();
            System.out.println("Next process in waitlist is: " + nextPair.getKey());
            if (counter >= amount/*amountReleased*/) {
                counter = counter - amount;
                resourceControlBlock.get(resource).setCounter(counter);
                boolean alreadyExisted = false;
                LinkedList<Pair<Integer, Integer>> processResources = processControlBlock.get(nextPair.getKey()).getResources();
                LinkedList<Pair<Integer, Integer>> newResources = new LinkedList<>();
                if (processResources == null) {
                    System.out.println("There are no resources for this process... adding first one!");
                } else {
                    System.out.println("Adding resource " + resource + " to process " + nextPair.getKey());
                    newResources.addAll(processResources);
                }
                if (processResources != null){
                    for (Pair<Integer, Integer> processResource : processResources) {
                        if (processResource.getKey() == resource) {
                            int amountHeld = processResource.getValue();
                            newResources.remove(new Pair<>(resource, amountHeld));
                            amountHeld += amountReleased;
                            newResources.add(new Pair<>(resource, amountHeld));
                            alreadyExisted = true;
                        }
                    }
                }
                if (!alreadyExisted)
                    newResources.add(new Pair<>(resource, amount));

                processControlBlock.get(nextPair.getKey()).setResources(newResources);
                System.out.println("Process " + nextPair.getKey() + " has resources: " + processControlBlock.get(nextPair.getKey()).getResources());
                processControlBlock.get(nextPair.getKey()).setState(READY);
                System.out.println("Process " + nextPair.getKey() + " is READY!");
                waitlist.remove(nextPair);
                if (waitlist.size() != 0) {
                    resourceControlBlock.get(resource).setWaitlist(waitlist);
                } else {
                    resourceControlBlock.get(resource).setWaitlist(null);
                    waitlist = null;
                }
                System.out.println("New waitlist for resource " + resource + " is: " + resourceControlBlock.get(resource).getWaitlist());
                LinkedList<Integer> pList = readyList.get(processControlBlock.get(nextPair.getKey()).getPriority());
                System.out.println("RL before removing: " + pList);
                //adding new pair into ready list
                pList.add(nextPair.getKey());
                readyList.put(processControlBlock.get(nextPair.getKey()).getPriority(), pList);
                System.out.println("New RL: " + readyList.get(processControlBlock.get(nextPair.getKey()).getPriority()));
            } else {
                break;
            }
        }
    }

    public static boolean badRequest(int resource, int amount) {
        LinkedList<Pair<Integer, Integer>> list = processControlBlock.get(runningProcess).getResources();
        if(list != null) {
            int index = -1;
            for (int i = 0; i < list.size(); ++i) {
                if (list.get(i).getKey() == resource) {
                    index = i;
                }
            }
            if(index != -1){
                Pair<Integer, Integer> alreadyHeld = list.get(index);
                System.out.println("Process " + runningProcess + " holds resources: " + alreadyHeld);
                return Integer.parseInt(String.valueOf(Integer.sum(amount, alreadyHeld.getValue()))) > resourceControlBlock.get(resource).getInventory();
            }
        }
        return Integer.sum(amount, 0) > resourceControlBlock.get(resource).getInventory();
    }

    public static boolean badRelease(int resource, int amount) {
        LinkedList<Pair<Integer, Integer>> list = processControlBlock.get(runningProcess).getResources();
        if(list != null) {
            int index = -1;
            for (int i = 0; i < list.size(); ++i) {
                if (list.get(i).getKey() == resource) {
                    index = i;
                }
            }
            if(index != -1) {
                Pair<Integer, Integer> alreadyHeld = list.get(index);
                System.out.println("Process " + runningProcess + " holds: " + alreadyHeld.getValue() + " of resource " + alreadyHeld.getKey());
                return amount > Integer.parseInt(String.valueOf(alreadyHeld.getValue()));
            }
            else {
                return true;
            }
        }
        return true;
    }

    public static void outputPCB(){
        for (int i = 0; i < PCB_SIZE; ++i) {
            int state = processControlBlock.get(i).getState();
            System.out.print("PCB[" + i + "] state: " + (state == 1 ? "READY" : state == -1 ? "FREE" : "BLOCKED"));
            System.out.print(" - Parent: " + processControlBlock.get(i).getParent());
            System.out.print(" - Priority: " + processControlBlock.get(i).getPriority());
            System.out.print(" - Children: " + processControlBlock.get(i).getChildren());
            System.out.print(" - Resources: ");
            if(processControlBlock.get(i).getResources() == null)
                System.out.print("null");
            else
                for (int j = 0; j < processControlBlock.get(i).getResources().size(); j++){
                    Pair<Integer, Integer> map = processControlBlock.get(i).getResources().get(j);
                    String key = map.getKey().toString();
                    String val = map.getValue().toString();
                    System.out.print("{" + key + ", " + val + "},");
                }
            System.out.println();
        }
    }

    public static void outputRCB() {
        for (int i = 0; i < RCB_SIZE; ++i){
            System.out.println("RCB[" + i + "] counter: " + resourceControlBlock.get(i).getCounter() +
                    " and waitlist: " + resourceControlBlock.get(i).getWaitlist());
        }
    }

    public static void outputRL(){
        for(Integer key : readyList.keySet()){
            LinkedList<Integer> pList = readyList.get(key);
            if(pList == null){
                System.out.println(key + " : " + null);
            }
            else{
                System.out.println(key + " : " + pList);
            }
        }
    }
}

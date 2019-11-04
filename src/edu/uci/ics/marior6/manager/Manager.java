package edu.uci.ics.marior6.manager;

import edu.uci.ics.marior6.manager.core.ProcessControlBlock;
import edu.uci.ics.marior6.manager.core.ResourceControlBlock;
import edu.uci.ics.marior6.manager.utilities.Helper;
import javafx.util.Pair;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class Manager {
    public static final int READY     = 1;
    public static final int BLOCKED   = 0;
    public static final int FREE      = -1;
    private static final int ALLOCATED = 0;
    public static final int PCB_SIZE  = 16;
    public static final int RCB_SIZE  = 4;
    public static Integer runningProcess = ALLOCATED;
    private static boolean init = false;
    public static ArrayList<ProcessControlBlock> processControlBlock = new ArrayList<>(PCB_SIZE);
    public static ArrayList<ResourceControlBlock> resourceControlBlock = new ArrayList<>(RCB_SIZE);
    public static Map<Integer, LinkedList<Integer>> readyList;// = new TreeMap<>(RL_SIZE);
    private static BufferedReader reader = null;
    private static BufferedWriter writer = null;

    public static void main(String[] args) {
        System.out.println("Starting process and resource manager...");
        Manager processManager = new Manager();
        File file;
        Path currentRelativePath = Paths.get("");
        String path = currentRelativePath.toAbsolutePath().toString();
        //System.out.println("Current relative path is: " + path);
        //initialize all data structures
        processManager.initPCB();
        processManager.initRCB();
        processManager.initRL();
        try {
            //Create output file in current working directory
            file = new File(path + "\\output.txt\\");
            if (file.exists()) {
                if (file.createNewFile()) {
                    System.out.println("Creating new output file...");
                }
            }
            writer = new BufferedWriter(new FileWriter(file));
            System.out.println("Output handler ready...");
            if (args.length == 0) {
                Scanner scanner = new Scanner(System.in);
                while (true) {
                    System.out.print("> ");
                    String cmd = scanner.nextLine();
                    if (!cmd.isEmpty()) {
                        if(cmd.equals("q")) {
                            scanner.close();
                            writer.close();
                            System.exit(0);
                        }
                        //execute command
                        processManager.executeArguments(cmd);
                    } else {
                        System.out.println("You did not enter a command. Try again...");
                    }
                }
            }else {
                System.out.println("Input file passed as parameter to program..." + args[0]);
                reader = new BufferedReader(new FileReader(args[0]));
                System.out.println("Input handler ready...");
                String cmd = null;
                while ((cmd = reader.readLine()) != null) {
                    if (cmd.isEmpty()) {
                        writer.flush();
                        //writer.newLine();
                    } else {
                        processManager.executeArguments(cmd);
                        writer.flush();
                    }
                }
                reader.close();
                writer.close();
            }
        } catch (IOException e) {
            System.out.println("Error opening file!");
            e.printStackTrace();
        }
    }

    private void executeArguments(String cmd) {
        //System.out.println("The cmd is: " + cmd);
        String[] operation = cmd.split(" ");
        try {
            switch (operation[0]) {
                case "in":
                    if (init){ writer.newLine(); }
                    init(); //initialize PCB, RCB and RL
                    break;
                case "cr":
                    int op1 = Integer.parseInt(operation[1]); //priority given to process
                    if ((op1 == 1 || op1 == 2)) {
                        create(op1);
                    } else {
                        writer.write(-1 + "");
                    }
                    break;
                case "de":
                    op1 = Integer.parseInt(operation[1]); //process to destroy
                    if (op1 > 0 && op1 < PCB_SIZE) {
                        destroy(op1);
                    } else {
                        writer.write(-1 + "");
                    }
                    break;
                case "rq":
                    op1 = Integer.parseInt(operation[1]); //resource requesting
                    int op2 = Integer.parseInt(operation[2]); //amount of that resource requesting
                    if ((runningProcess == 0) || (op1 < 0 || op1 > 3) || op2 == 0 || Helper.badRequest(op1, op2)) {
                        //write to file here -1
                        writer.write(-1 + "");
                        break;
                    }
                    request(op1, op2);
                    break;
                case "rl":
                    op1 = Integer.parseInt(operation[1]); //resource to be released
                    op2 = Integer.parseInt(operation[2]); //amount of that resource to release
                    if ((runningProcess == 0) || (op1 < 0 || op1 > 3) || op2 == 0 || Helper.badRelease(op1, op2)) {
                        //write to file here -1 and maybe call init in a newline
                        writer.write(-1 + "");
                        break;
                    }
                    release(op1, op2);
                    break;
                case "to":
                    timeout();
                    break;
                case "p":
                    Helper.outputPCB();
                    Helper.outputRCB();
                    Helper.outputRL();
                    break;
                default:
                    System.out.println("Operation is not valid! Try again...");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void create(int priority) {
        try {
            ProcessControlBlock newProcess = new ProcessControlBlock();
            newProcess.setState(READY);
            newProcess.setPriority(priority);
            newProcess.setChildren(null);
            newProcess.setResources(null);
            newProcess.setParent(runningProcess);
            int processIndex = Helper.insertNewProcess(newProcess);
            if (processIndex == -1) {
                writer.write(-1 + "");
                return;
            }
            Helper.insertToRL(processIndex, newProcess.getPriority());
            //insert newProcess as child of runningProcess
            Helper.insertNewChild(processIndex);
            System.out.println("Process " + processIndex + " created");
            scheduler();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void destroy(int childrenToDestroy) {
        int counter = 0;
        LinkedList<Integer> parentChildren = processControlBlock.get(runningProcess).getChildren();
        if (parentChildren.contains(childrenToDestroy)) {
            //remove all children of child to destroy and resources associated with them
            LinkedList<Integer> children = processControlBlock.get(childrenToDestroy).getChildren();
            //System.out.println("Process " + childrenToDestroy + " has children: " + children);
            counter = Helper.eraseChildren(children, counter);
            //System.out.println("Number of child processes deleted: " + counter);
            //System.out.println("Process " + childrenToDestroy + " new children list: " + children);
            //remove process j from process i list of children
            parentChildren.remove(new Integer(childrenToDestroy));
            //System.out.println("Process " + childrenToDestroy + " has been removed!");
            //System.out.println("Process " + runningProcess + " new children list: " + parentChildren);
            if (parentChildren.size() == 0)
                processControlBlock.get(runningProcess).setChildren(null);
            else
                processControlBlock.get(runningProcess).setChildren(parentChildren);
            counter++;
            //remove process j from ready list or waiting list
            Helper.eraseFromReadyListOrWaitlist(childrenToDestroy);
            //release all resources of process j
            Helper.releaseResource(childrenToDestroy);
            //set PCB of j to free
            processControlBlock.get(childrenToDestroy).setState(FREE);
            processControlBlock.get(childrenToDestroy).setPriority(0);
            processControlBlock.get(childrenToDestroy).setParent(null);
            processControlBlock.get(childrenToDestroy).setChildren(null);
            processControlBlock.get(childrenToDestroy).setResources(null);
            System.out.println(counter + " processes destroyed");
            try {
                writer.write(runningProcess + " ");
            } catch (IOException e) {
                System.out.println("Error writing to file...");
                e.printStackTrace();
            }
        } else {
            //System.out.println("Process " + childrenToDestroy + " is not a children of " + runningProcess);
            //output -1 in file here
            try {
                writer.write(-1 + "");
            } catch (IOException e) {
                System.out.println("Error writing to file...");
                e.printStackTrace();
            }
        }
    }

    private void request(int resource, int amountRequested) {
        int counter = resourceControlBlock.get(resource).getCounter();
        boolean alreadyExisted = false;
        //System.out.println("The number of resources available for resource " + resource + " are: " + counter);
        LinkedList<Pair<Integer, Integer>> processResources = processControlBlock.get(runningProcess).getResources();
        LinkedList<Pair<Integer, Integer>> newResources = new LinkedList<>();
        if (counter != 0 && counter >= amountRequested) {
            //change counter by amount requested and add resource to process i
            resourceControlBlock.get(resource).setCounter(counter - amountRequested);
            //System.out.println("New number of resources available for resource " + resource + " are: " + resourceControlBlock.get(resource).getCounter());
            if (processResources == null) {
                System.out.println("There are no resources for this process... adding first one!");
            } else {
                //System.out.println("Adding resource " + resource + " to process " + runningProcess);
                newResources.addAll(processResources);
            }
            if(processResources != null){
                for (Pair<Integer, Integer> processResource : processResources) {
                    if (processResource.getKey() == resource) {
                        int amountHeld = processResource.getValue();
                        newResources.remove(new Pair<>(resource, amountHeld));
                        amountHeld += amountRequested;
                        newResources.add(new Pair<>(resource, amountHeld));
                        alreadyExisted = true;
                    }
                }
            }
            if (!alreadyExisted)
                newResources.add(new Pair<>(resource, amountRequested));

            processControlBlock.get(runningProcess).setResources(newResources);
            //System.out.println("Process " + runningProcess + " has resources: " + processControlBlock.get(runningProcess).getResources());
            System.out.println("Process " + runningProcess + " running");
            try{
                writer.write(runningProcess + " ");
            } catch (IOException e) {
                System.out.println("Error writing to file...");
                e.printStackTrace();
            }
        } else {
            int priority = processControlBlock.get(runningProcess).getPriority();
            processControlBlock.get(runningProcess).setState(BLOCKED);
            //System.out.println("Process " + runningProcess + " is BLOCKED!");
            LinkedList<Integer> pList = readyList.get(priority);
            //remove process from RL and update RL
            //System.out.println("RL before removing: " + pList);
            pList.remove(runningProcess);
            readyList.replace(priority, pList);
            //System.out.println("New RL: " + readyList.get(priority));
            //adding process to resource waitlist
            LinkedList<Pair<Integer, Integer>> waitlist = resourceControlBlock.get(resource).getWaitlist();
            LinkedList<Pair<Integer, Integer>> newList = new LinkedList<>();
            //System.out.println("Waitlist for resource " + resource + " is: " + waitlist);
            if(waitlist == null) {
                System.out.println("There are no processes in this waitlist... adding first one!");
            } else {
                //System.out.println("The waitlist for resource " + resource + " has: " + waitlist);
                newList.addAll(waitlist);
            }
            newList.add(new Pair<>(runningProcess, amountRequested)); //change to Integer??
            resourceControlBlock.get(resource).setWaitlist(newList);
            //System.out.println("New waitlist for resource " + resource + " is: " + resourceControlBlock.get(resource).getWaitlist());
            runningProcess = BLOCKED; //setting current process variable to nothing since current one got blocked
            scheduler();
        }
    }

    private void release(int resource, int amountReleased) {
        int counter = resourceControlBlock.get(resource).getCounter();
        //System.out.println("The number of resources available for resource " + resource + " are: " + counter);
        LinkedList<Pair<Integer, Integer>> list = processControlBlock.get(runningProcess).getResources();
        //System.out.println("Resource list for process " + runningProcess + " is: " + list);
        for (int i = 0; i < list.size(); ++i) {
            Integer r = list.get(i).getKey();
            Integer a = list.get(i).getValue();
            if (r == resource) {
                //if amount to be released is equal to amount held, remove resource completely
                if (amountReleased == a) {
                    Pair<Integer, Integer> pair = new Pair<>(r, a);
                    list.remove(pair);
                } else {
                    //else, do partial release and set new amount of resources held
                    Integer remainder = a - amountReleased;
                    Pair<Integer, Integer> pair = new Pair<>(r, remainder);
                    list.set(i, pair);

                }
                if (list.size() == 0)
                    processControlBlock.get(runningProcess).setResources(null);
                else
                    processControlBlock.get(runningProcess).setResources(list);
            }
        }
        //System.out.println("New resource list for process " + runningProcess + " is: " + processControlBlock.get(runningProcess).getResources());
        resourceControlBlock.get(resource).setCounter(counter + amountReleased);
        //System.out.println("New number of resources available for resource " + resource + " are: " + resourceControlBlock.get(resource).getCounter());
        //check resource waitlist and fulfill any other outstanding requests
        Helper.checkResourceWaitlist(resource, amountReleased);
        scheduler();
    }

    private void timeout() {
        int priority = processControlBlock.get(runningProcess).getPriority();
        LinkedList<Integer> list = readyList.get(priority);
        //System.out.println("The list of processes at priority " + priority + " are: " + list);
        Integer head = list.poll();
        list.addLast(head);
        readyList.replace(priority, list);
        //System.out.println("New list of processes at priority " + priority + " are: " + readyList.get(priority));
        scheduler();
    }

    private void scheduler() {
        //loop thru different priorities and find highest priority ready
        try {
            LinkedList<Integer> pList;
            for (int i = 2; i >= 0; --i) {
                pList = readyList.get(i);
                //System.out.println("Ready List " + i + ": " + readyList.get(i));
                if (pList != null && pList.size() != 0) {
                    Integer highestPriorityProcess = pList.element();
                    //its not switching back when i have it '>'
                    if (processControlBlock.get(highestPriorityProcess).getPriority() >= processControlBlock.get(runningProcess).getPriority()) {
                        runningProcess = highestPriorityProcess;
                        //display to file here just process number
                        writer.write(runningProcess + " ");
                        System.out.println("Process " + runningProcess + " running");
                        break;
                    }
                    else {
                        //display to file here just process number
                        writer.write(runningProcess + " ");
                        System.out.println("Process " + runningProcess + " running");
                        break;
                    }
                }
            }
        } catch (IOException e) {
            System.out.println("Error writing to file...");
            e.printStackTrace();
        }
    }

    private void init() {
        init = true;
        initPCB(); initRL(); initRCB();
        //create process 0
        ProcessControlBlock newProcess = new ProcessControlBlock(READY, null, 0, null, null);
        int processIndex = Helper.insertNewProcess(newProcess);
        Helper.insertToRL(processIndex, newProcess.getPriority());
        //write current running process to file here
        runningProcess = processIndex;
        try{
            writer.write(runningProcess + " ");
        } catch (IOException e) {
            System.out.println("Error writing to file...");
            e.printStackTrace();
        }
        System.out.println("Process " + runningProcess + " is running...");
    }

    private void initPCB() {
        //System.out.println("Initializing the PCB...");
        processControlBlock.clear();
        for (int i = 0; i < PCB_SIZE; ++i) {
            ProcessControlBlock p = new ProcessControlBlock();
            p.setState(FREE);
            processControlBlock.add(p);
        }
        //System.out.println("Done initializing the PCB!");
    }

    private void initRL() {
        if (readyList != null){ readyList.clear(); }
        Map<Integer, LinkedList<Integer>> map = new HashMap<>();
        //System.out.println("Initializing the RL...");
        map.put(0, null);
        map.put(1, null);
        map.put(2, null);
        readyList = new TreeMap<>(map).descendingMap();
        //System.out.println("Done initializing the RL!");
    }

    private void initRCB() {
        //System.out.println("Initializing the RCB...");
        resourceControlBlock.clear();
        for (int i = 0; i < RCB_SIZE; ++i){
            ResourceControlBlock r = new ResourceControlBlock();
            if(i == 0 || i == 1) {
                r.setInventory(1);
                r.setCounter(1);
            } else if(i == 2){
                r.setInventory(2);
                r.setCounter(2);
            } else {
                r.setInventory(3);
                r.setCounter(3);
            }
            resourceControlBlock.add(r);
        }
        //System.out.println("Done initializing the RCB!");
    }
}

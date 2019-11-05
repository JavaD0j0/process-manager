Process and Resource Manager
-----------------------------

Description:
This program simulates an Operating System managing multiple processes running at the same time.
It can handle up to 16 process simultaneously with each process owning different resources at
any point in time. The manager takes care of choosing the process with the highest priority to
be the running process, and keeps track of all other process in a Ready List (RL). Each resource
has multi-units and a process can request and release as many units of a resource as it wants, as
long as it doesn't go over the maximum amount. The timeout function simulates a context switch
where the manager switches to a different process to run if any available and ready to run.

How to run:
1) Download the jar file
2) Open the command line and run the jar file like (java -jar programName inputFile):
        "java -jar Project1-ProcessAndResourceManager.jar input.txt"
        (*** if no input file provided, it will run the shell mode ***)
3) Output file should be created in the current working directory

Notes:
Commands on the input text file are as follow:
    - 'in' = initialize the process manager
    - 'cr priority' = this will create a new process with the priority given (Priority: 1 or 2)
    - 'de childProcess' = this will destroy the child process and all its sub-children
    - 'rq resource units' = this will request # of units of resource
    - 'rl resource units' = this will release # of units of resource
    - 'to' = this will simulate a context switch and choose a different process to run
    - 'p' = this will print information regarding each PCB, RCB and RLs
    - 'q' = this will quit the program and an output file will be created in current directory
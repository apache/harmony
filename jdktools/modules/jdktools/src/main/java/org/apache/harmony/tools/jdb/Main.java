/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.apache.harmony.tools.jdb;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.Map.Entry;

import org.eclipse.jdi.Bootstrap;
import org.eclipse.jdi.internal.VirtualMachineManagerImpl;
import org.eclipse.jdi.internal.connect.SocketAttachingConnectorImpl;

import com.sun.jdi.LocalVariable;
import com.sun.jdi.Location;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.StackFrame;
import com.sun.jdi.StringReference;
import com.sun.jdi.ThreadReference;
import com.sun.jdi.Value;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.VirtualMachineManager;
import com.sun.jdi.connect.Connector;
import com.sun.jdi.connect.IllegalConnectorArgumentsException;
import com.sun.jdi.event.BreakpointEvent;
import com.sun.jdi.event.ClassPrepareEvent;
import com.sun.jdi.event.Event;
import com.sun.jdi.event.EventIterator;
import com.sun.jdi.event.EventQueue;
import com.sun.jdi.event.EventSet;
import com.sun.jdi.event.ExceptionEvent;
import com.sun.jdi.event.LocatableEvent;
import com.sun.jdi.event.VMDisconnectEvent;
import com.sun.jdi.event.VMStartEvent;
import com.sun.jdi.request.BreakpointRequest;
import com.sun.jdi.request.ClassPrepareRequest;
import com.sun.jdi.request.EventRequest;
import com.sun.jdi.request.EventRequestManager;
import com.sun.jdi.request.ExceptionRequest;

public class Main {

    static enum Command {
        
        CONNECTORS("connectors", "connectors", "") {
            @SuppressWarnings("unchecked")
            final String run(String[] args) {
                List<Connector> connectors = 
                    Main.jdb.virturalMachineManager.allConnectors();
                StringBuilder sb = new StringBuilder(
                        "Available connectors are:\n");
                for (Connector c : connectors) {
                    sb.append(String.format(
                            "\n  Connector: %s  Transport: %s\n", 
                            c.name(), c.transport().name()));
                    sb.append(String.format(
                            "    description: %s\n", 
                            c.description()));
                    Map<String, Connector.Argument> argumentMap = 
                        c.defaultArguments();
                    Set<Entry<String, Connector.Argument>> argumentSet = 
                        argumentMap.entrySet();
                    for (Entry<String, Connector.Argument> e : argumentSet) {
                        Connector.Argument arg = e.getValue();
                        if (arg.mustSpecify()) {
                            sb.append(String.format(
                                    "\n    Required Argument: %s", arg.name()));
                        } else {
                            sb.append(String.format(
                                    "\n    Argument: %s", arg.name()));
                        }
                        String defVal = arg.value();
                        if (null == defVal) {
                            sb.append(" <no default>\n");
                        } else {
                            sb.append(String.format(
                                    " Default value: %s\n", defVal));
                        }
                        sb.append(String.format(
                                "    description: %s\n", arg.description()));
                    }
                }
                return sb.toString();
            }
        },
        
        RUN("run", "run [class [args]]", "") {
            final String run(String[] args) {
                if (jdb.status == Status.CONNECTED) {
                    jdb.targetVM.resume();
                }
                return "";
            }
        },
        
        THREADS("threads", "threads [threadgroup]", "") {
            final String run(String[] args) {
                StringBuilder sb = new StringBuilder("** command list **\n");
                return sb.toString();
            }
        },
        
        THREAD("thread", "thread <thread id>", "") {
            final String run(String[] args) {
                return "";
            }
        },
        
        SUSPEND("suspend", "suspend [thread id(s)]", "") {
            final String run(String[] args) {
                return "";
            }
        },
        
        RESUME("resume", "resume [thread id(s)]", "") {
            final String run(String[] args) {
                return "";
            }
        },
        
        WHERE("where", "where [<thread id> | all]", "") {
            final String run(String[] args) {
                return "";
            }
        },
        
        WHEREI("wherei", "wherei [<thread id> | all]", "") {
            final String run(String[] args) {
                return "";
            }
        },
        
        UP("up", "up [n frames]", "") {
            final String run(String[] args) {
                return "";
            }
        },
        
        DOWN("down", "down [n frames]", "") {
            final String run(String[] args) {
                return "";
            }
        },
        
        KILL("kill", "kill <thread id> <expr>", "") {
            final String run(String[] args) {
                return "";
            }
        },
        
        INTERRUPT("interrupt", "interrupt <thread id>", "") {
            final String run(String[] args) {
                return "";
            }
        },
        
        PRINT("print", "print <expr>", "") {
            final String run(String[] args) {
                return "";
            }
        },
        
        DUMP("dump", "dump <expr>", "") {
            final String run(String[] args) {
                return "";
            }
        },
        
        EVAL("eval", "eval <expr>", "") {
            final String run(String[] args) {
                return "";
            }
        },
        
        SET("set", "set <lvalue> = <expr>", "") {
            final String run(String[] args) {
                return "";
            }
        },
        
        LOCALS("locals", "locals", "") {
            final String run(String[] args) {
                return "";
            }
        },
        
        CLASSES("classes", "classes", "") {
            final String run(String[] args) {
                return "";
            }
        },
        
        CLASS("class", "class <class id>", "") {
            final String run(String[] args) {
                return "";
            }
        },
        
        METHODS("methods", "methods <class id>", "") {
            final String run(String[] args) {
                return "";
            }
        },
        
        FIELDS("fields", "fields <class id>", "") {
            final String run(String[] args) {
                return "";
            }
        },
        
        THREADGROUPS("threadgroups", "threadgroups", "") {
            final String run(String[] args) {
                return "";
            }
        },
        
        THREADGROUP("threadgroup", "threadgroup <name>", "") {
            final String run(String[] args) {
                return "";
            }
        },
        
        STOP_IN("stop", "stop in <class id>.<method>[(argument_type,...)]", "") {
            final boolean match(String[] args) {
                return super.match(args) && args[1].equals("in");
            }
            final String run(String[] args) {
                return "";
            }
        },
        
        STOP_AT("stop", "stop at <class id>:<line>", "") {
            final boolean match(String[] args) {
                return super.match(args) && args[1].equals("at");
            }
            final String run(String[] args) throws Exception {
                String[] params = args[2].split(":");
                List<ReferenceType> classes = 
                    jdb.targetVM.classesByName(params[0]);
                if (classes.isEmpty()) {
                    if (!jdb.deferringLineBreakpoint.containsKey(params[0])) {
                        jdb.deferringLineBreakpoint.put(
                                params[0], new ArrayList<Integer>());
                    }
                    jdb.deferringLineBreakpoint.get(
                            params[0]).add(Integer.parseInt(params[1]));
                    return String.format(
                            "Deferring breakpoint %s\n" +
                            "it will be set after the class is loaded.", 
                            args[2]);
                } else {
                    List<Location> locations = classes.get(0).
                            locationsOfLine(Integer.parseInt(params[1]));
                    if (locations.isEmpty()) {
                        Location location = locations.get(0);
                        BreakpointRequest breakpointRequest = jdb.eventRequestManager
                            .createBreakpointRequest(location);
                        breakpointRequest.setSuspendPolicy(EventRequest.SUSPEND_ALL);
                        breakpointRequest.enable();
                        return String.format("Breakpoint set: " + location);
                    } else {
                        return "";
                    }
                }
            }
        },
        
        CLEAR_METHOD("clear", "clear <class id>.<method>[(argument_type,...)]", "") {
            final boolean match(String[] args) {
                return false;
            }
            final String run(String[] args) {
                return "";
            }
        },
        
        CLEAR_LINE("clear", "clear <class id>:<line>", "") {
            final boolean match(String[] args) {
                return false;
            }
            final String run(String[] args) {
                return "";
            }
        },
        
        CLEAR("clear", "clear", "") {
            final boolean match(String[] args) {
                return false;
            }
            final String run(String[] args) {
                return "";
            }
        },
        
        CATCH("catch", "catch [uncaught|caught|all]", "") {
            final String run(String[] args) {
                return "";
            }
        },
        
        IGNORE("ignore", "ignore [uncaught|caught|all]", "") {
            final String run(String[] args) {
                return "";
            }
        },
        
        WATCH("watch", "watch [access|all] <class id>.<field name>", "") {
            final String run(String[] args) {
                return "";
            }
        },
        
        UNWATCH("unwatch", "unwatch [access|all] <class id>.<field name>", "") {
            final String run(String[] args) {
                return "";
            }
        },
        
        TRACE_METHODS("trace", "trace [go] methods [thread]", "") {
            final boolean match(String[] args) {
                if (args[0].equals(prefix) && 
                        (args.length == 1 || !args[1].equals("up"))) {
                    return true;
                } else {
                    return false;
                }
            }
            final String run(String[] args) {
                return "";
            }
        },
        
        TRACE_METHOD("trace", "trace [go] method exit | exits [thread]", "") {
            final boolean match(String[] args) {
                return false;
            }
            final String run(String[] args) {
                return "";
            }
        },
        
        UNTRACE("untrace", "untrace [methods]", "") {
            final String run(String[] args) {
                return "";
            }
        },
        
        STEP("step", "step", "") {
            final boolean match(String[] args) {
                if (args[0].equals(prefix) && 
                        (args.length == 1 || !args[1].equals("up"))) {
                    return true;
                } else {
                    return false;
                }
            }
            final String run(String[] args) {
                return "";
            }
        },
        
        STEP_UP("step", "step up", "") {
            final boolean match(String[] args) {
                if (args[0].equals(prefix) && 
                        args.length >=2 && args[1].equals("up")) {
                    return true;
                } else {
                    return false;
                }
            }
            final String run(String[] args) {
                return "";
            }
        },
        
        STEPI("stepi", "stepi", "") {
            final String run(String[] args) {
                return "";
            }
        },
        
        NEXT("next", "next", "") {
            final String run(String[] args) {
                return "";
            }
        },
        
        CONT("cont", "cont", "") {
        },
        
        LIST("list", "list [line number|method]", "") {
            final String run(String[] args) {
                return "";
            }
        },
        
        USE("use|sourcepath", "use (or sourcepath) [source file path]", "") {
            final boolean match(String[] args) {
                if (args[0].equals("use") || args[0].equals("sourcepath")) {
                    return true;
                } else {
                    return false;
                }
            }
            final String run(String[] args) {
                return "";
            }
        },
        
        EXCLUDE("exclude", "exclude [<class pattern>, ... | \"none\"]", "") {
            final String run(String[] args) {
                return "";
            }
        },
        
        CLASSPATH("classpath", "classpath", "") {
            final String run(String[] args) {
                return "";
            }
        },
        
        MONITOR_CMD("monitoer", "monitor <command>", "") {
            final boolean match(String[] args) {
                return false;
            }
            final String run(String[] args) {
                return "";
            }
        },
        
        MONITOR("monitoer", "monitor", "") {
            final boolean match(String[] args) {
                return false;
            }
            final String run(String[] args) {
                return "";
            }
        },
        
        UNMONITOR("unmonitor", "unmonitor <monitor#>", "") {
            final String run(String[] args) {
                return "";
            }
        },
        
        READ("read", "read <filename>", "") {
            final String run(String[] args) {
                return "";
            }
        },
        
        LOCK("lock", "lock <expr>", "") {
            final String run(String[] args) {
                return "";
            }
        },
        
        THREADLOCKS("threadlocks", "threadlocks [thread id]", "") {
            final String run(String[] args) {
                return "";
            }
        },
        
        POP("pop", "pop", "") {
            final String run(String[] args) {
                return "";
            }
        },
        
        REENTER("reenter", "reenter", "") {
            final String run(String[] args) {
                return "";
            }
        },
        
        REDEFINE("redefine", "redefine <class id> <class file name>", "") {
            final String run(String[] args) {
                return "";
            }
        },
        
        DISABLEGC("disablegc", "disablegc <expr>", "") {
            final String run(String[] args) {
                return "";
            }
        },
        
        ENABLEGC("enablegc", "enablegc <expr>", "") {
            final String run(String[] args) {
                return "";
            }
        },
        
        REPEAT("!!", "!!", "") {
            Command lastCmd = null;
            String[] lastArgs = null;
            final String run(String[] args) {
                return "";
            }
        },
        
        HELP("help|?", "help (or ?)", "") {
            final boolean match(String[] args) {
                if (args[0].equals("help") || args[0].equals("?")) {
                    return true;
                } else {
                    return false;
                }
            }
            final String run(String[] args) {
                StringBuilder sb = new StringBuilder("** command list **\n");
                return sb.toString();
            }
        },
        
        VERSION("version", "version", "") {
            final String run(String[] args) {
                return "";
            }
        },
        
        EXIT("exit|quit", "exit (or quit)", "") {
            final boolean match(String[] args) {
                if (args[0].equals("exit") || args[0].equals("quit")) {
                    return true;
                } else {
                    return false;
                }
            }
        },
        
        NONCMD("", "" ,"") {
            final String run(String[] args) {
                return String.format("Unsupported command: '%s'.", args[0]);
            }
        };
        
        final String usage;
        
        final String comment;
        
        final String prefix;
        
        static final Command search(final String[] args) {
            for (Command cmd : Command.values()) {
                if (cmd != NONCMD && cmd.match(args)) {
                    return cmd;
                }
            }
            return NONCMD;
        }
        
        boolean match(String[] args) {
            return args[0].equals(prefix);
        }
        
        String run(String[] args) throws Exception {
            return "";
        }
        
        Command(String prefix, String usage, String comment) {
            this.prefix = prefix;
            this.usage = usage;
            this.comment = comment;
        }
        
    }
    
    static enum Status {
        UNDEFINED, NO_VM_CONNECTED, CONNECTED
    }
    
    private static final String PROMPT = "> ";
    
    private static Main jdb = new Main();
    
    private boolean isExit = false;
    
    private Status status = Status.UNDEFINED;
    
    private VirtualMachineManager virturalMachineManager = Bootstrap.virtualMachineManager();
    
    private EventRequestManager eventRequestManager = null;
    
    private EventQueue eventQueue = null;
    
    private EventSet eventSet = null;
    
    private VirtualMachine targetVM = null;
    
    private Process VMProcess = null;
    
    private String mainClass = null;
    
    private HashMap<String, List<Integer>> deferringLineBreakpoint = 
                new HashMap<String, List<Integer>>();
    
    private HashMap<String, List<String>> deferringMethodBreakpoint = 
                new HashMap<String, List<String>>();
    
    private String currentPrompt = PROMPT;
    
    private Main() {}
    
    /*
     *  Prepare jdb according to the incoming arguments: decide working mode 
     *  and corresponding parameter, then launch jdb and get ready to process 
     *  commands.
     */
    private void init(String[] args) throws Exception {
        if (args.length >= 2 && args[0].equals("-attach")) {
            SocketAttachingConnectorImpl sac = new SocketAttachingConnectorImpl(
                    (VirtualMachineManagerImpl) Bootstrap.virtualMachineManager());

            Map argMap = sac.defaultArguments();
            Connector.Argument value;
            value = (Connector.Argument) argMap.get("hostname");
            value.setValue("localhost");
            value = (Connector.Argument) argMap.get("port");
            value.setValue(args[1]);
            targetVM = sac.attach(argMap);
            VMProcess = targetVM.process();
        } else {
            System.out.println("Wrong command option. " +
                    "The jdb currently only support jdb -attach [port].");
            System.exit(0);
        }
        
        eventRequestManager = targetVM.eventRequestManager();
        
        ClassPrepareRequest classPrepareRequest 
            = eventRequestManager.createClassPrepareRequest();
        classPrepareRequest.addClassFilter("*");
        classPrepareRequest.setSuspendPolicy(EventRequest.SUSPEND_ALL);
        classPrepareRequest.enable();
    }
    
    private void start() throws Exception {
        eventQueue = targetVM.eventQueue();
        while (true) {
            if (isExit == true) {
                break;
            }
            eventSet = eventQueue.remove();
            EventIterator eventIterator = eventSet.eventIterator();
            while (eventIterator.hasNext()) {
                Event event = (Event) eventIterator.next();
                execute(event);
            }
            if (!isExit) {
                eventSet.resume();
            }
        }
    }
    
    private void execute(Event event) throws Exception {
        boolean keepRunning = true;
        if (event instanceof VMStartEvent) {
            //eventRequestManager = targetVM.eventRequestManager();
            //ExceptionRequest excReq = 
                //eventRequestManager.createExceptionRequest(null, false, true);
            //excReq.setSuspendPolicy(EventRequest.SUSPEND_ALL);
            //excReq.enable();
            //System.out.println("Set uncaught java.lang.Throwable");
            //System.out.println("Set deferred uncaught java.lang.Throwable");
            status = Status.CONNECTED;
            System.out.println("Initializing jdb ...");
            System.out.println(
                    "VM Started: No frames on the current call stack");
            //currentPrompt = ((VMStartEvent)event).thread().name();
            keepRunning = false;
        }
        if (event instanceof ClassPrepareEvent) {
            String className = ((ClassPrepareEvent) event).referenceType().name();
            List<Integer> lineList = deferringLineBreakpoint.get(className);
            if (lineList != null) {
                ReferenceType classType = ((ClassPrepareEvent) event).referenceType();
                for (Integer line : lineList) {
                    List<Location> locations = classType.locationsOfLine(line);
                    if (!locations.isEmpty()) {
                        Location location = locations.get(0);
                        BreakpointRequest breakpointRequest = jdb.eventRequestManager
                                .createBreakpointRequest(location);
                        breakpointRequest.setSuspendPolicy(EventRequest.SUSPEND_ALL);
                        breakpointRequest.enable();
                        System.out.println(String.format("Breakpoint set: " + location));
                    }
                }
            }
        }
        if (event instanceof BreakpointEvent) {
            System.out.println("Reach breakpoint at " + 
                    ((BreakpointEvent) event).location());
            keepRunning = false;
        }
        if (event instanceof VMDisconnectEvent) {
            System.out.println("Application ends.");
            isExit = true;
        }
        
        if (!keepRunning) {
            Scanner cmdScanner = new Scanner(System.in);
            while (true) {
                System.out.print(currentPrompt);
                String line = cmdScanner.nextLine();
                String[] args = line.trim().split(" ");
                if (!line.equals("")) {
                    Command cmd = Command.search(args);
                    if (cmd == Command.EXIT ) {
                        isExit = true;
                        break;
                    }
                    if (cmd == Command.CONT) {
                        break;
                    }
                    String result = cmd.run(args);
                    if (cmd == Command.RUN) {
                        break;
                    }
                    if (!result.equals("")) {
                        System.out.println(result);
                    }
                }
            }
        }
    }
    
    public static void main(String[] args) throws Exception {
        jdb.init(args);
        jdb.start();
        if (jdb.status == Status.CONNECTED) {
            jdb.targetVM.dispose();
        }
    }

}

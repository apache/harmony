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

import com.sun.jdi.Location;
import com.sun.jdi.Method;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.ThreadReference;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.VirtualMachineManager;
import com.sun.jdi.connect.Connector;
import com.sun.jdi.event.BreakpointEvent;
import com.sun.jdi.event.ClassPrepareEvent;
import com.sun.jdi.event.Event;
import com.sun.jdi.event.EventIterator;
import com.sun.jdi.event.EventQueue;
import com.sun.jdi.event.EventSet;
import com.sun.jdi.event.MethodExitEvent;
import com.sun.jdi.event.StepEvent;
import com.sun.jdi.event.ThreadStartEvent;
import com.sun.jdi.event.VMDisconnectEvent;
import com.sun.jdi.event.VMStartEvent;
import com.sun.jdi.request.BreakpointRequest;
import com.sun.jdi.request.ClassPrepareRequest;
import com.sun.jdi.request.EventRequest;
import com.sun.jdi.request.EventRequestManager;
import com.sun.jdi.request.MethodExitRequest;
import com.sun.jdi.request.StepRequest;
import com.sun.jdi.request.ThreadStartRequest;

/**
 * The java implementation of Harmony JDB command line tool.
 */
public class Main {

    /**
     * The JDB command enumeration.
     */
    static enum Command {
        
        // complete
        CONNECTORS("connectors", "connectors", "") {
            @Override
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
        
        // seems complete
        RUN("run", "run [class [args]]", "") {
            @Override
            final String run(String[] args) {
                if (jdb.status == Status.CONNECTED) {
                    jdb.debuggee.resume();
                    jdb.status = Status.STARTED;
                }
                return EMPTY_MSG;
            }
        },
        
        THREADS("threads", "threads [threadgroup]", "") {
            @Override
            final String run(String[] args) {
                if (jdb.status != Status.STARTED) {
                    return String.format(NOT_VALID_UNTIL_STARTED, "threads");
                }
                StringBuilder sb = new StringBuilder("** thread list **\n");
                // TODO print the thread list
                return sb.toString();
            }
        },
        
        THREAD("thread", "thread <thread id>", "") {
            final String run(String[] args) {
                if (jdb.status != Status.STARTED) {
                    return String.format(NOT_VALID_UNTIL_STARTED, "thread");
                }
                // TODO command logic
                return EMPTY_MSG;
            }
        },
        
        SUSPEND("suspend", "suspend [thread id(s)]", "") {
            final String run(String[] args) {
                if (jdb.status != Status.STARTED) {
                    return String.format(NOT_VALID_UNTIL_STARTED, "suspend");
                }
                // TODO command logic
                return EMPTY_MSG;
            }
        },
        
        RESUME("resume", "resume [thread id(s)]", "") {
            final String run(String[] args) {
                if (jdb.status != Status.STARTED) {
                    return String.format(NOT_VALID_UNTIL_STARTED, "resume");
                }
                // TODO command logic
                return EMPTY_MSG;
            }
        },
        
        WHERE("where", "where [<thread id> | all]", "") {
            final String run(String[] args) {
                if (jdb.status != Status.STARTED) {
                    return String.format(NOT_VALID_UNTIL_STARTED, "where");
                }
                // TODO command logic
                return EMPTY_MSG;
            }
        },
        
        WHEREI("wherei", "wherei [<thread id> | all]", "") {
            final String run(String[] args) {
                if (jdb.status != Status.STARTED) {
                    return String.format(NOT_VALID_UNTIL_STARTED, "wherei");
                }
                // TODO command logic
                return EMPTY_MSG;
            }
        },
        
        UP("up", "up [n frames]", "") {
            final String run(String[] args) {
                if (jdb.status != Status.STARTED) {
                    return String.format(NOT_VALID_UNTIL_STARTED, "up");
                }
                // TODO command logic
                return EMPTY_MSG;
            }
        },
        
        DOWN("down", "down [n frames]", "") {
            final String run(String[] args) {
                if (jdb.status != Status.STARTED) {
                    return String.format(NOT_VALID_UNTIL_STARTED, "down");
                }
                // TODO command logic
                return EMPTY_MSG;
            }
        },
        
        KILL("kill", "kill <thread id> <expr>", "") {
            final String run(String[] args) {
                if (jdb.status != Status.STARTED) {
                    return String.format(NOT_VALID_UNTIL_STARTED, "kill");
                }
                // TODO command logic
                return EMPTY_MSG;
            }
        },
        
        INTERRUPT("interrupt", "interrupt <thread id>", "") {
            final String run(String[] args) {
                if (jdb.status != Status.STARTED) {
                    return String.format(NOT_VALID_UNTIL_STARTED, "interrupt");
                }
                // TODO command logic
                return EMPTY_MSG;
            }
        },
        
        PRINT("print", "print <expr>", "") {
            final String run(String[] args) {
                if (jdb.status != Status.STARTED) {
                    return String.format(NOT_VALID_UNTIL_STARTED, "print");
                }
                // TODO command logic
                return EMPTY_MSG;
            }
        },
        
        DUMP("dump", "dump <expr>", "") {
            final String run(String[] args) {
                if (jdb.status != Status.STARTED) {
                    return String.format(NOT_VALID_UNTIL_STARTED, "dump");
                }
                // TODO command logic
                return EMPTY_MSG;
            }
        },
        
        EVAL("eval", "eval <expr>", "") {
            final String run(String[] args) {
                if (jdb.status != Status.STARTED) {
                    return String.format(NOT_VALID_UNTIL_STARTED, "eval");
                }
                // TODO command logic
                return EMPTY_MSG;
            }
        },
        
        SET("set", "set <lvalue> = <expr>", "") {
            final String run(String[] args) {
                if (jdb.status != Status.STARTED) {
                    return String.format(NOT_VALID_UNTIL_STARTED, "set");
                }
                // TODO command logic
                return EMPTY_MSG;
            }
        },
        
        LOCALS("locals", "locals", "") {
            final String run(String[] args) {
                if (jdb.status != Status.STARTED) {
                    return String.format(NOT_VALID_UNTIL_STARTED, "locals");
                }
                // TODO command logic
                return EMPTY_MSG;
            }
        },
        
        CLASSES("classes", "classes", "") {
            final String run(String[] args) {
                if (jdb.status != Status.STARTED) {
                    return String.format(NOT_VALID_UNTIL_STARTED, "classes");
                }
                // TODO command logic
                return EMPTY_MSG;
            }
        },
        
        CLASS("class", "class <class id>", "") {
            final String run(String[] args) {
                if (jdb.status != Status.STARTED) {
                    return String.format(NOT_VALID_UNTIL_STARTED, "class");
                }
                return EMPTY_MSG;
            }
        },
        
        METHODS("methods", "methods <class id>", "") {
            final String run(String[] args) {
                if (jdb.status != Status.STARTED) {
                    return String.format(NOT_VALID_UNTIL_STARTED, "methods");
                }
                return EMPTY_MSG;
            }
        },
        
        FIELDS("fields", "fields <class id>", "") {
            final String run(String[] args) {
                if (jdb.status != Status.STARTED) {
                    return String.format(NOT_VALID_UNTIL_STARTED, "fields");
                }
                return EMPTY_MSG;
            }
        },
        
        THREADGROUPS("threadgroups", "threadgroups", "") {
            final String run(String[] args) {
                if (jdb.status != Status.STARTED) {
                    return String.format(NOT_VALID_UNTIL_STARTED, "threadgroups");
                }
                return EMPTY_MSG;
            }
        },
        
        THREADGROUP("threadgroup", "threadgroup <name>", "") {
            final String run(String[] args) {
                if (jdb.status != Status.STARTED) {
                    return String.format(NOT_VALID_UNTIL_STARTED, "threadgroup");
                }
                // TODO command logic
                return EMPTY_MSG;
            }
        },
        
        // TODO complete, not working well though
        STOP_IN("stop", "stop in <class id>.<method>[(argument_type,...)]", 
                "") {
            @Override
            final boolean match(String[] args) {
                return super.match(args) && args[1].equals("in");
            }
            @Override
            @SuppressWarnings("unchecked")
            final String run(String[] args) {
                String[] params;
                try {
                    params = parseClassMethod(args[2]);
                } catch (Exception e) {
                    return usage;
                }
                List<ReferenceType> classes = 
                    jdb.debuggee.classesByName(params[0]);
                // class not loaded yet
                if (classes.isEmpty()) {
                    if (!jdb.deferringMethodBreakpoint.containsKey(params[0])) {
                        jdb.deferringMethodBreakpoint.put(
                                params[0], new ArrayList<String>());
                    }
                    // method string: "method|argument_type,..."
                    jdb.deferringMethodBreakpoint.get(
                            params[0]).add(params[1] + "|" + params[2]);
                    return String.format(
                            "Deferring breakpoint %s\n" +
                            "it will be set after the class is loaded.", 
                            args[2]);
                } else {
                    // Generate the target argument type string list
                    String[] argTypeNames = params[2].split(",");
                    // Get all methods of the class
                    ReferenceType clazz = classes.get(0);
                    List<Method> methodList = clazz.methodsByName(params[1]);
                    Method matchedMethod = null;
                    /*
                     * As the jdb command argument doesn't supply the result 
                     * value type, it's impossible to generate a jni signature 
                     * for the specified method. I just have to search...
                     */
                    for (Method m : methodList) {
                        List<String> types = m.argumentTypeNames();
                        if (types.size() != argTypeNames.length) {
                            continue;
                        } else {
                            boolean matched = true;
                            for (int i = 0; i < argTypeNames.length; i++) {
                                if (!types.get(i).equals(argTypeNames[i])) {
                                    matched = false;
                                    break;
                                }
                            }
                            if (matched) {
                                matchedMethod = m;
                                break;
                            }
                        }
                    }
                    if (null != matchedMethod) {
                        Location loc = matchedMethod.location();
                        if (null != loc) {
                            BreakpointRequest breakpointRequest = 
                                jdb.eventRequestManager.
                                createBreakpointRequest(loc);
                            breakpointRequest.setSuspendPolicy(
                                    EventRequest.SUSPEND_ALL);
                            breakpointRequest.enable();
                            jdb.breakpointRegisterMap.put(
                                    loc.toString(), breakpointRequest);
                            return String.format("Breakpoint set: " + loc);
                        }
                    }
                }
                return EMPTY_MSG;
            }
        },
        
        // TODO complete, working well, need more check
        STOP_AT("stop", "stop at <class id>:<line>", "") {
            @Override
            final boolean match(String[] args) {
                return super.match(args) && args[1].equals("at");
            }
            @Override
            @SuppressWarnings("unchecked")
            final String run(String[] args) throws Exception {
                String[] params = args[2].split(":");
                List<ReferenceType> classes = 
                    jdb.debuggee.classesByName(params[0]);
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
                    if (!locations.isEmpty()) {
                        Location loc = locations.get(0);
                        BreakpointRequest breakpointRequest = 
                            jdb.eventRequestManager.
                            createBreakpointRequest(loc);
                        breakpointRequest.setSuspendPolicy(
                                EventRequest.SUSPEND_ALL);
                        breakpointRequest.enable();
                        jdb.breakpointRegisterMap.put(
                                loc.toString(), breakpointRequest);
                        return String.format("Breakpoint set: " + loc);
                    } else {
                        return EMPTY_MSG;
                    }
                }
            }
        },
        
        // TODO complete, not working well though
        CLEAR_METHOD("clear", "clear <class id>.<method>[(argument_type,...)]", "") {
            final boolean match(String[] args) {
                return args.length >= 2 
                && args[0].equals(prefix) 
                && args[1].indexOf(':') == -1
                && args[1].indexOf('.') != -1;
            }
            @SuppressWarnings("unchecked")
            final String run(String[] args) {
                String[] params;
                boolean isFound = false;
                try {
                    params = parseClassMethod(args[1]);
                } catch (Exception e) {
                    return usage;
                }
                String methodStr = params[1] + "|" + params[2];
                List<String> methodStrList = 
                    jdb.deferringMethodBreakpoint.get(params[0]);
                for (String str : methodStrList) {
                    if (str.equals(methodStr)) {
                        methodStrList.remove(str);
                        isFound = true;
                        break;
                    }
                }
                if (!isFound) {
                    List<ReferenceType> classes = 
                        jdb.debuggee.classesByName(params[0]);
                    if (!classes.isEmpty()) {
                        // Generate the target argument type string list
                        String[] argTypeNames = params[2].split(",");
                        // Get all methods of the class
                        ReferenceType clazz = classes.get(0);
                        List<Method> methodList = clazz.methodsByName(params[1]);
                        Method matchedMethod = null;
                        /*
                         * As the jdb command argument doesn't supply the result 
                         * value type, it's impossible to generate a jni signature 
                         * for the specified method. I just have to search...
                         */
                        for (Method m : methodList) {
                            List<String> types = m.argumentTypeNames();
                            if (types.size() != argTypeNames.length) {
                                continue;
                            } else {
                                boolean matched = true;
                                for (int i = 0; i < argTypeNames.length; i++) {
                                    if (!types.get(i).equals(argTypeNames[i])) {
                                        matched = false;
                                        break;
                                    }
                                }
                                if (matched) {
                                    matchedMethod = m;
                                    break;
                                }
                            }
                        }
                        if (null != matchedMethod) {
                            Location loc = matchedMethod.location();
                            if (null != loc) {
                                String key = loc.toString();
                                jdb.breakpointRegisterMap.get(key).disable();
                                jdb.breakpointRegisterMap.remove(key);
                                isFound = true;
                            }
                        }
                    }
                }
                if (isFound) {
                    return String.format("Breakpoint cleared: " + args[1]);
                } else {
                    return String.format(
                            "Not found: breakpoint %s", args[1]);
                }
            }
        },
        
        // TODO complete, not working well though
        CLEAR_LINE("clear", "clear <class id>:<line>", "") {
            final boolean match(String[] args) {
                return args.length >= 2 
                    && args[0].equals(prefix) 
                    && args[1].indexOf(':') != -1;
            }
            @SuppressWarnings("unchecked")
            final String run(String[] args) throws Exception {
                String[] params = args[1].split(":");
                int line = Integer.parseInt(params[1]);
                boolean isFound = false;
                List<Integer> lineList = 
                    jdb.deferringLineBreakpoint.get(params[0]);
                for (Integer i : lineList) {
                    if (i.intValue() == line) {
                        lineList.remove(i);
                        isFound = true;
                        break;
                    }
                }
                if (!isFound) {
                    List<ReferenceType> classes = 
                        jdb.debuggee.classesByName(params[0]);
                    if (!classes.isEmpty()) {
                        List<Location> locations = classes.get(0).
                                locationsOfLine(line);
                        if (!locations.isEmpty()) {
                            Location loc = locations.get(0);
                            String key = loc.toString();
                            jdb.breakpointRegisterMap.get(key).disable();
                            jdb.breakpointRegisterMap.remove(key);
                            isFound = true;
                        }
                    }
                }
                if (isFound) {
                    return String.format("Breakpoint cleared: " + args[1]);
                } else {
                    return String.format(
                            "Not found: breakpoint %s", args[1]);
                }
            }
        },
        
        CLEAR("clear", "clear", "") {
            final boolean match(String[] args) {
                return args.length == 1 && args[0].equals(prefix);
            }
            @SuppressWarnings("unchecked")
            final String run(String[] args) {
                /*
                 * TODO have not include deferred breakpoints yet
                 */
                List<BreakpointRequest> list = 
                    jdb.eventRequestManager.breakpointRequests();
                if (list.size() == 0) {
                    return "No breakpoints set.";
                } else {
                    StringBuilder sb = new StringBuilder();
                    for (BreakpointRequest request : list) {
                        sb.append(request.location() + "\n");
                    }
                    return sb.toString();
                }
            }
        },
        
        CATCH("catch", "catch [uncaught|caught|all]", "") {
            final String run(String[] args) {
                if (jdb.status != Status.STARTED) {
                    return String.format(NOT_VALID_UNTIL_STARTED, "catch");
                }
                // TODO command logic
                return EMPTY_MSG;
            }
        },
        
        IGNORE("ignore", "ignore [uncaught|caught|all]", "") {
            final String run(String[] args) {
                if (jdb.status != Status.STARTED) {
                    return String.format(NOT_VALID_UNTIL_STARTED, "ignore");
                }
                // TODO command logic
                return EMPTY_MSG;
            }
        },
        
        WATCH("watch", "watch [access|all] <class id>.<field name>", "") {
            final String run(String[] args) {
                if (jdb.status != Status.STARTED) {
                    return String.format(NOT_VALID_UNTIL_STARTED, "watch");
                }
                // TODO command logic
                return EMPTY_MSG;
            }
        },
        
        UNWATCH("unwatch", "unwatch [access|all] <class id>.<field name>", "") {
            final String run(String[] args) {
                if (jdb.status != Status.STARTED) {
                    return String.format(NOT_VALID_UNTIL_STARTED, "unwatch");
                }
                // TODO command logic
                return EMPTY_MSG;
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
                if (jdb.status != Status.STARTED) {
                    return String.format(NOT_VALID_UNTIL_STARTED, "trace");
                }
                // TODO command logic
                return EMPTY_MSG;
            }
        },
        
        TRACE_METHOD("trace", "trace [go] method exit | exits [thread]", "") {
            final boolean match(String[] args) {
                return false;
            }
            final String run(String[] args) {
                if (jdb.status != Status.STARTED) {
                    return String.format(NOT_VALID_UNTIL_STARTED, "trace");
                }
                // TODO command logic
                return EMPTY_MSG;
            }
        },
        
        UNTRACE("untrace", "untrace [methods]", "") {
            final String run(String[] args) {
                if (jdb.status != Status.STARTED) {
                    return String.format(NOT_VALID_UNTIL_STARTED, "untrace");
                }
                // TODO command logic
                return EMPTY_MSG;
            }
        },
        
        // TODO complete, not working well though
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
                if (jdb.status != Status.STARTED) {
                    return String.format(NOT_VALID_UNTIL_STARTED, "step");
                }
                StepRequest request = 
                    jdb.eventRequestManager.createStepRequest(jdb.thread, 
                            StepRequest.STEP_LINE, 
                            StepRequest.STEP_INTO);
                request.addCountFilter(1);// next step only
                request.enable();
                return "Step one line.";
            }
        },
        
        // TODO complete, not working well though
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
                if (jdb.status != Status.STARTED) {
                    return String.format(NOT_VALID_UNTIL_STARTED, "step up");
                }
                MethodExitRequest request = 
                    jdb.eventRequestManager.createMethodExitRequest();
                request.addThreadFilter(jdb.thread);
                request.addCountFilter(1);
                request.enable();
                return "Step up to the caller.";
            }
        },
        
        // TODO complete, not working well though
        STEPI("stepi", "stepi", "") {
            final String run(String[] args) {
                if (jdb.status != Status.STARTED) {
                    return String.format(NOT_VALID_UNTIL_STARTED, "stepi");
                }
                StepRequest request = 
                    jdb.eventRequestManager.createStepRequest(jdb.thread, 
                            StepRequest.STEP_MIN, 
                            StepRequest.STEP_INTO);
                request.addCountFilter(1);// next step only
                request.enable();
                return "Step one instruction.";
            }
        },
        
        // TODO complete, not working well though
        NEXT("next", "next", "") {
            final String run(String[] args) {
                if (jdb.status != Status.STARTED) {
                    return String.format(NOT_VALID_UNTIL_STARTED, "next");
                }
                StepRequest request = 
                    jdb.eventRequestManager.createStepRequest(jdb.thread, 
                            StepRequest.STEP_LINE, 
                            StepRequest.STEP_OVER);
                request.addCountFilter(1);// next step only
                request.enable();
                return "Step one line (over calls)";
            }
        },
        
        // TODO, complete, working well, need more check
        CONT("cont", "cont", "") {
            final String run(String[] args) {
                if (jdb.status != Status.STARTED) {
                    return String.format(NOT_VALID_UNTIL_STARTED, "cont");
                }
                return EMPTY_MSG;
            }
        },
        
        LIST("list", "list [line number|method]", "") {
            final String run(String[] args) {
                if (jdb.status != Status.STARTED) {
                    return String.format(NOT_VALID_UNTIL_STARTED, "list");
                }
                // TODO command logic
                return EMPTY_MSG;
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
                // TODO command logic
                return EMPTY_MSG;
            }
        },
        
        EXCLUDE("exclude", "exclude [<class pattern>, ... | \"none\"]", "") {
            final String run(String[] args) {
                // TODO command logic
                return EMPTY_MSG;
            }
        },
        
        CLASSPATH("classpath", "classpath", "") {
            final String run(String[] args) {
                // TODO command logic
                return EMPTY_MSG;
            }
        },
        
        MONITOR_CMD("monitor", "monitor <command>", "") {
            final boolean match(String[] args) {
                return false;
            }
            final String run(String[] args) {
                if (jdb.status != Status.STARTED) {
                    return String.format(NOT_VALID_UNTIL_STARTED, "monitor");
                }
                // TODO command logic
                return EMPTY_MSG;
            }
        },
        
        MONITOR("monitor", "monitor", "") {
            final boolean match(String[] args) {
                return false;
            }
            final String run(String[] args) {
                if (jdb.status != Status.STARTED) {
                    return String.format(NOT_VALID_UNTIL_STARTED, "monitor");
                }
                // TODO command logic
                return EMPTY_MSG;
            }
        },
        
        UNMONITOR("unmonitor", "unmonitor <monitor#>", "") {
            final String run(String[] args) {
                if (jdb.status != Status.STARTED) {
                    return String.format(NOT_VALID_UNTIL_STARTED, "unmonitor");
                }
                // TODO command logic
                return EMPTY_MSG;
            }
        },
        
        READ("read", "read <filename>", "") {
            final String run(String[] args) {
                if (jdb.status != Status.STARTED) {
                    return String.format(NOT_VALID_UNTIL_STARTED, "read");
                }
                // TODO command logic
                return EMPTY_MSG;
            }
        },
        
        LOCK("lock", "lock <expr>", "") {
            final String run(String[] args) {
                if (jdb.status != Status.STARTED) {
                    return String.format(NOT_VALID_UNTIL_STARTED, "lock");
                }
                // TODO command logic
                return EMPTY_MSG;
            }
        },
        
        THREADLOCKS("threadlocks", "threadlocks [thread id]", "") {
            final String run(String[] args) {
                if (jdb.status != Status.STARTED) {
                    return String.format(NOT_VALID_UNTIL_STARTED, "threadlocks");
                }
                // TODO command logic
                return EMPTY_MSG;
            }
        },
        
        POP("pop", "pop", "") {
            final String run(String[] args) {
                if (jdb.status != Status.STARTED) {
                    return String.format(NOT_VALID_UNTIL_STARTED, "pop");
                }
                // TODO command logic
                return EMPTY_MSG;
            }
        },
        
        REENTER("reenter", "reenter", "") {
            final String run(String[] args) {
                if (jdb.status != Status.STARTED) {
                    return String.format(NOT_VALID_UNTIL_STARTED, "reenter");
                }
                // TODO command logic
                return EMPTY_MSG;
            }
        },
        
        REDEFINE("redefine", "redefine <class id> <class file name>", "") {
            final String run(String[] args) {
                if (jdb.status != Status.STARTED) {
                    return String.format(NOT_VALID_UNTIL_STARTED, "redefine");
                }
                // TODO command logic
                return EMPTY_MSG;
            }
        },
        
        DISABLEGC("disablegc", "disablegc <expr>", "") {
            final String run(String[] args) {
                if (jdb.status != Status.STARTED) {
                    return String.format(NOT_VALID_UNTIL_STARTED, "disablegc");
                }
                // TODO command logic
                return EMPTY_MSG;
            }
        },
        
        ENABLEGC("enablegc", "enablegc <expr>", "") {
            final String run(String[] args) {
                if (jdb.status != Status.STARTED) {
                    return String.format(NOT_VALID_UNTIL_STARTED, "enablegc");
                }
                // TODO command logic
                return EMPTY_MSG;
            }
        },
        
        REPEAT("!!", "!!", "") {
            final String run(String[] args) {
                // TODO command logic
                return EMPTY_MSG;
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
                // TODO build the info string
                return sb.toString();
            }
        },
        
        VERSION("version", "version", "") {
            final String run(String[] args) {
                return VERSION_STR;
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
            final String run(String[] args) {
                return EMPTY_MSG;
            }
        },
        
        // Returned by the search method when no real command is matched
        NONCMD("", "" ,"") {
            final String run(String[] args) {
                return String.format("Unsupported command: '%s'.", args[0]);
            }
        };
        
        // Expected command prefix
        final String usage;
        
        // Command format and options
        final String comment;
        
        // Help information of the command
        final String prefix;
        
        // Iterate the command list and match the args against them
        static final Command search(final String[] args) {
            for (Command cmd : Command.values()) {
                if (cmd != NONCMD && cmd.match(args)) {
                    return cmd;
                }
            }
            return NONCMD;
        }
        
        // parse the "<class id>.<method>[(argument_type,...)]" string
        static final String[] parseClassMethod(String str) {
            String[] params = new String[3];
            int argumentStart = str.indexOf('(');
            if (argumentStart == -1) {
                // class id
                params[0] = str.substring(
                        0, str.lastIndexOf('.'));
                // method name
                params[1] = str.substring(
                        str.lastIndexOf('.') + 1);
                // empty argument type list
                params[2] = "";
            } else {
                String classMethod = str.substring(argumentStart);
                // class id
                params[0] = classMethod.substring(
                        0, classMethod.lastIndexOf('.'));
                // method name
                params[1] = classMethod.substring(
                        classMethod.lastIndexOf('.') + 1);
                // argument type list
                params[2] = str.substring(
                        argumentStart + 1, str.length() - 1);
            }
            return params;
        }
        
        /*
         * Generally, a command matching only needs to compare its prefix, 
         * override it in enum instance if necessary.
         */
        boolean match(String[] args) {
            return args[0].equals(prefix);
        }
        
        /*
         * The default command running output is do nothing and return an empty 
         * string, override it in enum instance if necessary.
         */
        String run(String[] args) throws Exception {
            return EMPTY_MSG;
        }
        
        Command(String prefix, String usage, String comment) {
            this.prefix = prefix;
            this.usage = usage;
            this.comment = comment;
        }
        
    }
    
    // TODO not decided how many statuses needed yet
    static enum Status {
        UNDEFINED, NO_VM_CONNECTED, CONNECTED, STARTED
    }
    
    // The default prompt
    private static final String PROMPT = "> ";
    
    /*
     * The empty message indicates there's no information coming out with 
     * the command result
     */
    private static final String EMPTY_MSG = "";
    
    // The jdb version information
    private static final String VERSION_STR = 
        "This is jdb version 1.6 <Apache Harmony 6>";
    
    /*
     * The string indicates that a command is not valid until the VM is started 
     * with the 'run' command.
     */
    private static final String NOT_VALID_UNTIL_STARTED = 
        "Command '%s' is not valid until " +
        "the VM is started with the 'run' command";
    
    // The singleton instance of jdb
    private static final Main jdb = new Main();
    
    private boolean isExit = false;
    
    // The debuggee status
    private Status status = Status.UNDEFINED;
    
    private VirtualMachineManager virturalMachineManager = 
        Bootstrap.virtualMachineManager();
    
    private EventRequestManager eventRequestManager = null;
    
    private EventQueue eventQueue = null;
    
    private EventSet eventSet = null;
    
    private VirtualMachine debuggee = null;
    
    //private Process VMProcess = null;
    // Current thread
    private ThreadReference thread = null;
    
    //private String mainClass = null;
    
    private HashMap<String, List<Integer>> deferringLineBreakpoint = 
                new HashMap<String, List<Integer>>();
    
    private HashMap<String, List<String>> deferringMethodBreakpoint = 
                new HashMap<String, List<String>>();
    
    /* 
     * A map that stores references to all breakpoints(not include the deferred 
     * ones). The key of the map is a string indicate the location of the 
     * breakpoint. The map is used by those commands that will clear some 
     * breakpoint in certain location.
     */
    private HashMap<String, BreakpointRequest> breakpointRegisterMap = 
                new HashMap<String, BreakpointRequest>();
    
    // TODO currentPrompt is prompt that indicate current context(fixed for now)
    private String currentPrompt = PROMPT;
    
    private Main() {}
    
    /*
     *  Prepare jdb according to the incoming arguments: decide working mode 
     *  and corresponding parameter, then launch jdb and get ready to process 
     *  commands.
     */
    @SuppressWarnings("unchecked")
    private void init(String[] args) throws Exception {
        /*
         * TODO As currently jdb just supports "jdb -attach [port]" command, 
         * more work to do to parse the jdb launching command option
         */
        if (args.length >= 2 && args[0].equals("-attach")) {
            SocketAttachingConnectorImpl sac = 
                new SocketAttachingConnectorImpl(
                        (VirtualMachineManagerImpl) Bootstrap.
                        virtualMachineManager());
            Map argMap = sac.defaultArguments();
            Connector.Argument value;
            value = (Connector.Argument) argMap.get("hostname");
            value.setValue("localhost");
            value = (Connector.Argument) argMap.get("port");
            value.setValue(args[1]);
            debuggee = sac.attach(argMap);
            //VMProcess = debuggee.process();
        } else {
            System.out.println("Wrong command option. " +
                    "The jdb currently only support jdb -attach [port].");
            System.exit(0);
        }
        
        eventRequestManager = debuggee.eventRequestManager();
        
        // Enable class prepare request for all classes
        ClassPrepareRequest classPrepareRequest 
            = eventRequestManager.createClassPrepareRequest();
        classPrepareRequest.addClassFilter("*");
        classPrepareRequest.setSuspendPolicy(EventRequest.SUSPEND_ALL);
        classPrepareRequest.enable();
        
        // Enable thread start request for all threads
        ThreadStartRequest threadStartRequest 
            = eventRequestManager.createThreadStartRequest();
        threadStartRequest.setSuspendPolicy(EventRequest.SUSPEND_EVENT_THREAD);
        threadStartRequest.enable();
    }
    
    // Start the loop
    private void start() throws Exception {
        eventQueue = debuggee.eventQueue();
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
    
    @SuppressWarnings("unchecked")
    private void execute(Event event) throws Exception {
        /*
         * The variable indicates whether go to command looping after handling 
         * the current event
         */
        boolean keepRunning = true;
        if (event instanceof VMStartEvent) {
            status = Status.CONNECTED;
            System.out.println("Initializing jdb ...");
            System.out.println(
                    "VM Started: No frames on the current call stack");
            // stop after the vm starts
            keepRunning = false;
        }
        if (event instanceof ThreadStartEvent) {
            thread = ((ThreadStartEvent) event).thread();
        }
        if (event instanceof MethodExitEvent) {
            // run command step up -> stop
            keepRunning = false;
        }
        if (event instanceof StepEvent) {
            // run commands step, stepi, or next -> stop
            keepRunning = false;
        }
        /*
         * after a class is loaded, check whether there are deferred 
         * breakpoints(both line and method) related to the class; if yes, 
         * set these breakpoints. The jdb won't stop running.
         */
        if (event instanceof ClassPrepareEvent) {
            // get the class name
            String className = ((ClassPrepareEvent) 
                    event).referenceType().name();
            // set the possible deferred line breakpoints
            List<Integer> lineList = deferringLineBreakpoint.get(className);
            if (lineList != null) {
                ReferenceType classType = ((ClassPrepareEvent) 
                        event).referenceType();
                for (Integer line : lineList) {
                    List<Location> locations = classType.locationsOfLine(line);
                    if (!locations.isEmpty()) {
                        Location loc = locations.get(0);
                        BreakpointRequest breakpointRequest = 
                            jdb.eventRequestManager.
                            createBreakpointRequest(loc);
                        breakpointRequest.setSuspendPolicy(
                                EventRequest.SUSPEND_ALL);
                        breakpointRequest.enable();
                        jdb.breakpointRegisterMap.put(
                                loc.toString(), breakpointRequest);
                        System.out.println(
                                String.format("Breakpoint set: " + loc));
                    }
                }
            }
            // set the possible deferred method breakpoints
            List<String> methodStrList = deferringMethodBreakpoint.get(className);
            if (methodStrList != null) {
                ReferenceType classType = ((ClassPrepareEvent) 
                        event).referenceType();
                for (String methodStr : methodStrList) {
                    String[] params = methodStr.split("|");
                    // Generate the target argument type string list
                    String[] argTypeNames;
                    if (params.length > 1) {
                        argTypeNames = params[1].split(",");
                    } else {
                        argTypeNames = new String[0];
                    }
                    // Get all methods of the class
                    List<Method> methodList = classType.methodsByName(params[0]);
                    Method matchedMethod = null;
                    /*
                     * As the jdb command argument doesn't supply the result 
                     * value type, it's impossible to generate a jni signature 
                     * for the specified method. I just have to search...
                     */
                    for (Method m : methodList) {
                        List<String> types = m.argumentTypeNames();
                        if (types.size() != argTypeNames.length) {
                            continue;
                        } else {
                            boolean matched = true;
                            for (int i = 0; i < argTypeNames.length; i++) {
                                if (!types.get(i).equals(argTypeNames[i])) {
                                    matched = false;
                                    break;
                                }
                            }
                            if (matched) {
                                matchedMethod = m;
                                break;
                            }
                        }
                    }
                    if (null != matchedMethod) {
                        Location loc = matchedMethod.location();
                        if (null != loc) {
                            BreakpointRequest breakpointRequest = 
                                jdb.eventRequestManager.
                                createBreakpointRequest(loc);
                            breakpointRequest.setSuspendPolicy(
                                    EventRequest.SUSPEND_ALL);
                            breakpointRequest.enable();
                            jdb.breakpointRegisterMap.put(
                                    loc.toString(), breakpointRequest);
                            System.out.println(
                                    String.format("Breakpoint set: " + loc));
                        }
                    }
                }
            }
        }
        if (event instanceof BreakpointEvent) {
            // reach breakpoint and stop
            System.out.println("Reach breakpoint at " + 
                    ((BreakpointEvent) event).location());
            keepRunning = false;
        }
        if (event instanceof VMDisconnectEvent) {
            // The jdb will end when disconnected from target vm
            System.out.println("Application ends.");
            isExit = true;
        }
        
        /*
         * If the event requires the jdb to stop running, the jdb turn to the user 
         * interaction mode before resuming.
         */
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
                    String result = cmd.run(args);
                    if (!result.equals(EMPTY_MSG)) {
                        System.out.println(result);
                    }
                    // If the command is one of those execution control
                    if (cmd == Command.STEP 
                            || cmd == Command.STEP_UP 
                            || cmd == Command.STEPI
                            || cmd == Command.RUN
                            || cmd == Command.CONT) {
                        break;
                    }
                }
            }
        }
    }
    
    public static void main(String[] args) throws Exception {
        jdb.init(args);
        jdb.start();
        if (jdb.status == Status.CONNECTED) {
            jdb.debuggee.dispose();
        }
    }

}

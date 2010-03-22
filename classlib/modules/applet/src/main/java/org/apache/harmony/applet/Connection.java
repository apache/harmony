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
/** 
 * @author Pavel Dolgov
 */
package org.apache.harmony.applet;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import org.apache.harmony.awt.ContextStorage;
import org.apache.harmony.applet.internal.nls.Messages;


/**
 * Connection between host application and applet infrastructure
 */
public class Connection implements Callback {
    
    private final Socket socket;
    
    final LineReader reader;
//    final BufferedReader reader;
    final PrintWriter writer;
    
    final Factory factory;

    public static void main(String[] args) {
        ContextStorage.activateMultiContextMode();
        
        int port = 0;
        
        try {
            port = Integer.parseInt(args[0]);
        } catch (Exception e) {
            throw new IllegalArgumentException(Messages.getString("applet.02"));
        }
        
        Connection c = new Connection(port);
        c.listen();
    }
    
    Connection(int port) {
        try {
            socket = new Socket("localhost", port);
            
            reader = new LineReader(socket.getInputStream());
//            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            writer = new PrintWriter(socket.getOutputStream());

            factory = new Factory(this);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    void listen() {

        try {
            for (String cmd = reader.readLine(); cmd != null; cmd = reader.readLine()) {

                if (cmd.equals("exit")) {
                    exit();
                    return;
                }
                if (cmd.equals("dump")) {
                    dump();
                    continue;
                }
                String args[] = cmd.split(" ");

                if (args[0].equals("create")) {
                    create(args);
                    continue;
                }
                if (args[0].equals("unload")) {
                    unload(args);
                    continue;
                }
                if (args[0].equals("start")) {
                    start(args);
                    continue;
                }
                if (args[0].equals("stop")) {
                    stop(args);
                    continue;
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    private void dump() {
        try {
            System.err.println("  -----------THREADS--------------");
            Thread cur = Thread.currentThread();
            dump(cur.getThreadGroup(), "");
            System.err.println("  -----------CONTEXT--------------");
            factory.dump();
            System.err.println("  -----------END DUMP--------------");
        } catch (Exception e) {
            System.err.println(e);
        }
    }
    
    private void dump(ThreadGroup group, String prefix) {
        try {
            System.err.println(prefix + group);
        } catch (Exception e) {
            System.err.println(e);
        }
        
        try {
            Thread threads[] = new Thread[group.activeCount()];
            for (int cnt = group.enumerate(threads), i = 0; i<cnt; i++) {
                if (threads[i].getThreadGroup() == group) {
                    System.err.println(prefix + "| " + threads[i]);
                }
            }
        } catch (Exception e) {
            System.err.println(e);
        }

        try {
            ThreadGroup groups[] = new ThreadGroup[group.activeGroupCount()];
            for (int cnt = group.enumerate(groups), i=0; i<cnt; i++) {
                dump(groups[i], prefix + "> ");
            }
        } catch (Exception e) {
            System.err.println(e);
        }
    }
    
    private void start(String args[]) {
        
        factory.start(Integer.parseInt(args[1]));
    }

    private void stop(String args[]) {
        
        factory.stop(Integer.parseInt(args[1]));
    }

    private void unload(String args[]) {
        
        factory.dispose(Integer.parseInt(args[1]));
    }
    
    /**
     * command synopsis:<br>
     * CREATE id parentWindowId className docBase docId codeBase<br>
     *   NAME name_in_document<br>
     *   PARAM name value<br>
     * END<br>
     * @param args - CREATE command split into tokens
     */
    private void create(String args[]) {
        
        int id;
        long parentWindowId;
        String className;
        URL documentBase;
        int docId;
        URL codeBase;
        String name = null;
        Map<String, String> parameters = new HashMap<String, String>();
        
        try {
            id = Integer.parseInt(args[1]);
            parentWindowId = Long.parseLong(args[2]);
            className = args[3];
            documentBase = new URL(args[4]);
            docId = Integer.parseInt(args[5]);
            codeBase = new URL(args[6]);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        try {
            for (String line = reader.readLine(); line != null; line = reader.readLine()) {
                if (line.equals("exit")) {
                    exit();
                }
                if (line.equals("end")) {
                    break;
                }
                String parts[] = line.split(" ");
                if (parts[0].equals("param")) {
                    parameters.put(parts[1], parts[2]);
                } else if (parts[0].equals("name")) {
                    name = parts[1];
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        
        Parameters params = 
                new Parameters(id,
                               parentWindowId,
                               documentBase,
                               docId,
                               codeBase,
                               className,
                               parameters,
                               name,
                               null); 

        factory.createAndRun(params);
    }
    
    void exit() {
        try {
            sendCommand("exit");
            writer.close();
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.exit(0);
    }
    
    private void sendCommand(String cmd) {
        writer.println(cmd);
        writer.flush();
    }

    public void showDocument(int documentId, URL url, String target) {
        sendCommand("show " + documentId + " " + url + " " + target);
    }

    public void showStatus(int documentId, String status) {
        sendCommand("status " + documentId + " " + status);
    }

    public void appletResize(int appletId, int width, int height) {
        sendCommand("resize " + appletId + " " + width + " " + height);
    }    
    
    static class LineReader {
        
        private final StringBuffer buffer = new StringBuffer();
        private final InputStream in;
        private boolean eof = false;
        
        LineReader(InputStream is) {
            in = is;
        }

        public String readLine() throws IOException {
            if (eof) {
                return null;
            }
            
            int pos = buffer.indexOf("\n");
            if (pos >= 0) {
                return getLine(pos);
            }

            final int BUF_SIZE = 1024;
            byte[] buf = new byte[BUF_SIZE];

            int count = 0;
            while( (count = in.read(buf, 0, BUF_SIZE)) > 0 ) {
                buffer.append(new String(buf, 0, count));
                pos = buffer.indexOf("\n");
                if (pos >= 0) {
                    return getLine(pos);
                }
            }
            
            eof = true;
            return getRemainder();
        }
        
        public void close() throws IOException {
            in.close();
        }

        private String getLine(int endPos) {
            if (endPos > 0) {
                String result = buffer.substring(0, endPos);
                buffer.delete(0, endPos + 1);
                return result;
            }
            if (endPos == 0) {
                buffer.delete(0, 1);
            }
            return new String();
        }
        
        private String getRemainder() {
            String result = buffer.toString();
            buffer.delete(0, buffer.length());
            return (result.length() > 0) ? result : null;
        }
    }

}

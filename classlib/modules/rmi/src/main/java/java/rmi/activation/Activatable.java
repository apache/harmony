/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package java.rmi.activation;

import java.rmi.MarshalledObject;
import java.rmi.NoSuchObjectException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.server.RMIClientSocketFactory;
import java.rmi.server.RMIServerSocketFactory;
import java.rmi.server.RemoteObject;
import java.rmi.server.RemoteServer;
import org.apache.harmony.rmi.common.RMILog;
import org.apache.harmony.rmi.internal.nls.Messages;
import org.apache.harmony.rmi.remoteref.ActivatableServerRef;
import org.apache.harmony.rmi.server.ExportManager;

public abstract class Activatable extends RemoteServer {
    private static final long serialVersionUID = -3120617863591563455L;

    private static final RMILog rlog = RMILog.getActivationLog();

    private ActivationID id;

    protected Activatable(String codebase, MarshalledObject<?> data, boolean restart, int port)
            throws ActivationException, RemoteException {
        super();
        id = exportObject(this, codebase, data, restart, port);
    }

    protected Activatable(String codebase, MarshalledObject<?> data, boolean restart, int port,
            RMIClientSocketFactory csf, RMIServerSocketFactory ssf) throws ActivationException,
            RemoteException {
        super();
        id = exportObject(this, codebase, data, restart, port, csf, ssf);
    }

    protected Activatable(ActivationID id, int port) throws RemoteException {
        super();
        // rmi.log.05=Activatable.<init>[{0}, {1}]
        rlog.log(RMILog.VERBOSE, Messages.getString("rmi.log.05", id, port)); //$NON-NLS-1$
        this.id = id;
        // rmi.log.0E=Activatable >>> Ready to export object:
        rlog.log(RMILog.VERBOSE, Messages.getString("rmi.log.0E")); //$NON-NLS-1$
        exportObject(this, id, port);
    }

    protected Activatable(ActivationID id, int port, RMIClientSocketFactory csf,
            RMIServerSocketFactory ssf) throws RemoteException {
        super();
        this.id = id;
        exportObject(this, id, port, csf, ssf);
    }

    protected ActivationID getID() {
        return id;
    }

    public static Remote register(ActivationDesc desc) throws UnknownGroupException,
            ActivationException, RemoteException {
        ActivationSystem as = ActivationGroup.getSystem();
        ActivationID aid = as.registerObject(desc);
        // rmi.log.0F=aid = {0}
        rlog.log(RMILog.VERBOSE, Messages.getString("rmi.log.0F", aid)); //$NON-NLS-1$
        return org.apache.harmony.rmi.remoteref.ActivatableRef.getStub(desc, aid);
    }

    public static ActivationID exportObject(Remote obj, String location, MarshalledObject<?> data,
            boolean restart, int port, RMIClientSocketFactory csf, RMIServerSocketFactory ssf)
            throws ActivationException, RemoteException {
        ActivationDesc adesc = new ActivationDesc(obj.getClass().getName(), location, data,
                restart);
        ActivationID aid = ActivationGroup.getSystem().registerObject(adesc);
        exportObject(obj, aid, port, csf, ssf);
        return aid;
    }

    public static Remote exportObject(Remote robj, ActivationID aid, int port,
            RMIClientSocketFactory csf, RMIServerSocketFactory ssf) throws RemoteException {
        ActivatableServerRef asr = new ActivatableServerRef(aid, port, csf, ssf);
        return ExportManager.exportObject(robj, asr, false); //asr.exportObject(robj, null, false, true, true);
    }

    public static ActivationID exportObject(Remote robj, String location,
            MarshalledObject<?> data, boolean restart, int port) throws ActivationException,
            RemoteException {
        ActivationDesc adesc = new ActivationDesc(robj.getClass().getName(), location, data,
                restart);
        ActivationID aid = ActivationGroup.getSystem().registerObject(adesc);
        exportObject(robj, aid, port);
        ActivationGroup curAG = ActivationGroup.getCurrentAG();
        // rmi.console.00=CurAG = {0}
        System.out.println(Messages.getString("rmi.console.00", curAG)); //$NON-NLS-1$
        curAG.activeObject(aid, robj);
        return aid;
    }

    public static Remote exportObject(Remote robj, ActivationID aid, int port)
            throws RemoteException {
        // rmi.log.10=Activatable >>> exportObject
        rlog.log(RMILog.VERBOSE, Messages.getString("rmi.log.10")); //$NON-NLS-1$
        ActivatableServerRef asr = new ActivatableServerRef(aid, port);
        if (robj instanceof Activatable) {
            ((Activatable) robj).ref = asr;
        }
        // rmi.log.11=Activatable >>> ActivatableServerRef={0}
        rlog.log(RMILog.VERBOSE, Messages.getString("rmi.log.11", asr)); //$NON-NLS-1$
        ExportManager.exportObject(robj, asr, false, true, true);
        // rmi.log.12=Activatable >>> asr after export: {0}
        rlog.log(RMILog.VERBOSE, Messages.getString("rmi.log.12", asr)); //$NON-NLS-1$
        Remote rmt = RemoteObject.toStub(robj);
        // rmi.log.13=Activatable.exportObject: stub = {0}
        rlog.log(RMILog.VERBOSE, Messages.getString("rmi.log.13", rmt)); //$NON-NLS-1$
        return rmt;
    }

    public static boolean inactive(ActivationID aid) throws UnknownObjectException,
            ActivationException, RemoteException {
        return ActivationGroup.getCurrentAG().inactiveObject(aid);
    }

    public static void unregister(ActivationID aid) throws UnknownObjectException,
            ActivationException, RemoteException {
        ActivationGroup.getSystem().unregisterObject(aid);
    }

    public static boolean unexportObject(Remote obj, boolean force)
            throws NoSuchObjectException {
        return ExportManager.unexportObject(obj, force);
    }

    @Override
    public String toString() {
        return this.getClass() + ": [ActivationID =" + id + "; Ref =" + ref + "]"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    }
}

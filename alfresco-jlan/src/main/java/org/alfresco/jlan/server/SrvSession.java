/*
 * Copyright (C) 2006-2008 Alfresco Software Limited.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.

 * As a special exception to the terms and conditions of version 2.0 of 
 * the GPL, you may redistribute this Program in connection with Free/Libre 
 * and Open Source Software ("FLOSS") applications as described in Alfresco's 
 * FLOSS exception.  You should have recieved a copy of the text describing 
 * the FLOSS exception, and it is also available here: 
 * http://www.alfresco.com/legal/licensing"
 */
package org.alfresco.jlan.server;

import java.net.InetAddress;

import org.alfresco.jlan.debug.Debug;
import org.alfresco.jlan.server.auth.AuthContext;
import org.alfresco.jlan.server.auth.ClientInfo;
import org.alfresco.jlan.server.core.SharedDevice;
import org.alfresco.jlan.server.core.SharedDeviceList;
import org.alfresco.jlan.server.filesys.TransactionalFilesystemInterface;

/**
 * Server Session Base Class
 *
 * <p>
 * Base class for server session implementations for different protocols.
 *
 * @author gkspencer
 */
public abstract class SrvSession {

    // Network server this session is associated with
    private final NetworkServer m_server;
    // Session id/slot number
    private int m_sessId;
    // Unique session id string
    private String m_uniqueId;
    // Process id
    private int m_processId = -1;
    // Session/user is logged on/validated
    private boolean m_loggedOn;
    // Client details
    private final ThreadLocal<ClientInfo> m_clientInfo;
    // Authentication context, used during the initial session setup phase
    private AuthContext m_authContext;
    // Debug flags for this session, and debug output interface.
    private int m_debug;
    private String m_dbgPrefix;
    // List of dynamic/temporary shares created for this session
    private SharedDeviceList m_dynamicShares;
    // Session shutdown flag
    private boolean m_shutdown;
    // Protocol type
    private String m_protocol;
    // Remote client/host name
    private String m_remoteName;
    // Transaction object, for filesystems that implement the TransactionalFilesystemInterface
    private ThreadLocal<Object> m_tx;
    private ThreadLocal<TransactionalFilesystemInterface> m_txInterface;
    // Time of last I/O on this session
    private long m_lastIO;

    /**
     * Class constructor
     *
     * @param sessId int
     * @param srv NetworkServer
     * @param proto String
     * @param remName String
     */
    public SrvSession(int sessId, NetworkServer srv, String proto, String remName) {
        m_sessId = sessId;
        m_server = srv;
        setProtocolName(proto);
        setRemoteName(remName);
        // Allocate the client information thread local
        m_clientInfo = new ThreadLocal<>();
    }

    /**
     * Output a string to the debug device
     *
     * @param str
     */
    public final void debugPrint(String str) {
        Debug.print(str);
    }

    /**
     * Output a string and a newline to the debug device
     *
     * @param str String
     */
    public final void debugPrintln(String str) {
        Debug.print(m_dbgPrefix);
        Debug.println(str);
    }

    /**
     * Output an exception stack trace to the debug device
     *
     * @param ex Exception
     */
    public final void debugPrintln(Exception ex) {
        Debug.println(ex);
    }

    /**
     * Check if the session has an authentication context
     *
     * @return boolean
     */
    public final boolean hasAuthenticationContext() {
        return m_authContext != null;
    }

    /**
     * Return the authentication context for this sesion
     *
     * @return AuthContext
     */
    public final AuthContext getAuthenticationContext() {
        return m_authContext;
    }

    /**
     * Add a dynamic share to the list of shares created for this session
     *
     * @param shrDev SharedDevice
     */
    public final void addDynamicShare(SharedDevice shrDev) {
        // Check if the dynamic share list must be allocated
        if (m_dynamicShares == null) {
            m_dynamicShares = new SharedDeviceList();
        }
        // Add the new share to the list
        m_dynamicShares.addShare(shrDev);
    }

    /**
     * Determine if the session has any dynamic shares
     *
     * @return boolean
     */
    public final boolean hasDynamicShares() {
        return m_dynamicShares != null;
    }

    /**
     * Return the list of dynamic shares created for this session
     *
     * @return SharedDeviceList
     */
    public final SharedDeviceList getDynamicShareList() {
        return m_dynamicShares;
    }

    /**
     * Return the process id
     *
     * @return int
     */
    public final int getProcessId() {
        return m_processId;
    }

    /**
     * Return the remote client network address
     *
     * @return InetAddress
     */
    public abstract InetAddress getRemoteAddress();

    /**
     * Return the session id for this session.
     *
     * @return int
     */
    public final int getSessionId() {
        return m_sessId;
    }

    /**
     * Return the server this session is associated with
     *
     * @return NetworkServer
     */
    public final NetworkServer getServer() {
        return m_server;
    }

    /**
     * Check if the session has valid client information
     *
     * @return boolean
     */
    public final boolean hasClientInformation() {
        return m_clientInfo.get() != null;
    }

    /**
     * Return the client information
     *
     * @return ClientInfo
     */
    public final ClientInfo getClientInformation() {
        return m_clientInfo.get();
    }

    /**
     * Determine if the protocol type has been set
     *
     * @return boolean
     */
    public final boolean hasProtocolName() {
        return m_protocol != null;
    }

    /**
     * Return the protocol name
     *
     * @return String
     */
    public final String getProtocolName() {
        return m_protocol;
    }

    /**
     * Determine if the remote client name has been set
     *
     * @return boolean
     */
    public final boolean hasRemoteName() {
        return m_remoteName != null;
    }

    /**
     * Return the remote client name
     *
     * @return String
     */
    public final String getRemoteName() {
        return m_remoteName;
    }

    /**
     * Determine if the session is logged on/validated
     *
     * @return boolean
     */
    public final boolean isLoggedOn() {
        return m_loggedOn;
    }

    /**
     * Determine if the session has been shut down
     *
     * @return boolean
     */
    public final boolean isShutdown() {
        return m_shutdown;
    }

    /**
     * Return the unique session id
     *
     * @return String
     */
    public final String getUniqueId() {
        return m_uniqueId;
    }

    /**
     * Determine if the specified debug flag is enabled.
     *
     * @param dbgFlag int
     * @return boolean
     */
    public final boolean hasDebug(int dbgFlag) {
        return (m_debug & dbgFlag) != 0;
    }

    /**
     * Return the time of the last I/o on this session
     *
     * @return long
     */
    public final long getLastIOTime() {
        return m_lastIO;
    }

    /**
     * Set the authentication context, used during the initial session setup
     * phase
     *
     * @param ctx AuthContext
     */
    public final void setAuthenticationContext(AuthContext ctx) {
        m_authContext = ctx;
    }

    /**
     * Set the client information
     *
     * @param client ClientInfo
     */
    public final void setClientInformation(ClientInfo client) {
        m_clientInfo.set(client);
    }

    /**
     * Set the debug output interface.
     *
     * @param flgs int
     */
    public final void setDebug(int flgs) {
        m_debug = flgs;
    }

    /**
     * Set the debug output prefix for this session
     *
     * @param prefix String
     */
    public final void setDebugPrefix(String prefix) {
        m_dbgPrefix = prefix;
    }

    /**
     * Set the logged on/validated status for the session
     *
     * @param loggedOn boolean
     */
    public final void setLoggedOn(boolean loggedOn) {
        m_loggedOn = loggedOn;
    }

    /**
     * Set the process id
     *
     * @param id int
     */
    public final void setProcessId(int id) {
        m_processId = id;
    }

    /**
     * Set the protocol name
     *
     * @param name String
     */
    public final void setProtocolName(String name) {
        m_protocol = name;
    }

    /**
     * Set the remote client name
     *
     * @param name String
     */
    public final void setRemoteName(String name) {
        m_remoteName = name;
    }

    /**
     * Set the session id for this session.
     *
     * @param id int
     */
    public final void setSessionId(int id) {
        m_sessId = id;
    }

    /**
     * Set the unique session id
     *
     * @param unid String
     */
    public final void setUniqueId(String unid) {
        m_uniqueId = unid;
    }

    /**
     * Set the time of hte last I/O on this session
     *
     * @param ioTime long
     */
    public final void setLastIOTime(long ioTime) {
        m_lastIO = ioTime;
    }

    /**
     * Set the shutdown flag
     *
     * @param flg boolean
     */
    protected final void setShutdown(boolean flg) {
        m_shutdown = flg;
    }

    /**
     * Close the network session
     */
    public void closeSession() {
        // Release any dynamic shares owned by this session
        if (hasDynamicShares()) {
            // Close the dynamic shares
            getServer().getShareMapper().deleteShares(this);
        }
    }

    /**
     * Initialize the thread local transaction objects
     */
    public final void initializeTransactionObject() {
        if (m_tx == null) {
            m_tx = new ThreadLocal<>();
        }
        if (m_txInterface == null) {
            m_txInterface = new ThreadLocal<>();
        }
    }

    /**
     * Return the transaction context
     *
     * @return ThreadLocal 
     */
    public final ThreadLocal<Object> getTransactionObject() {
        return m_tx;
    }

    /**
     * Set the active transaction and transaction interface
     *
     * @param tx Object
     * @param txIface TransactionalFilesystemInterface
     */
    public final void setTransaction(Object tx, TransactionalFilesystemInterface txIface) {
        m_tx.set(tx);
        m_txInterface.set(txIface);
    }

    /**
     * Set the active transaction interface
     *
     * @param txIface TransactionalFilesystemInterface
     */
    public final void setTransaction(TransactionalFilesystemInterface txIface) {
        m_txInterface.set(txIface);
    }

    /**
     * Clear the stored transaction
     */
    public final void clearTransaction() {
        m_tx.set(null);
        m_txInterface.set(null);
    }

    /**
     * End a transaction
     */
    public final void endTransaction() {
        // Check if there is an active transaction
        if (m_txInterface != null && m_txInterface.get() != null && m_tx != null) {
            // Use the transaction interface to end the transaction
            m_txInterface.get().endTransaction(this, m_tx.get());
        }
    }

    /**
     * Check if there is an active transaction
     *
     * @return boolean
     */
    public final boolean hasTransaction() {
        if (m_tx == null) {
            return false;
        }
        return m_tx.get() != null;
    }
}

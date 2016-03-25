/*
 * Copyright (C) 2006-2007 Alfresco Software Limited.
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
package org.alfresco.jlan.smb.dcerpc.server;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import org.alfresco.jlan.debug.Debug;
import org.alfresco.jlan.server.auth.acl.AccessControlManager;
import org.alfresco.jlan.server.core.ShareType;
import org.alfresco.jlan.server.core.SharedDevice;
import org.alfresco.jlan.server.core.SharedDeviceList;
import org.alfresco.jlan.smb.Dialect;
import org.alfresco.jlan.smb.SMBStatus;
import org.alfresco.jlan.smb.dcerpc.DCEBuffer;
import org.alfresco.jlan.smb.dcerpc.DCEBufferException;
import org.alfresco.jlan.smb.dcerpc.Srvsvc;
import org.alfresco.jlan.smb.dcerpc.info.ServerInfo;
import org.alfresco.jlan.smb.dcerpc.info.ShareInfo;
import org.alfresco.jlan.smb.dcerpc.info.ShareInfoList;
import org.alfresco.jlan.smb.server.CIFSConfigSection;
import org.alfresco.jlan.smb.server.SMBServer;
import org.alfresco.jlan.smb.server.SMBSrvException;
import org.alfresco.jlan.smb.server.SMBSrvSession;

/**
 * Srvsvc DCE/RPC Handler Class
 *
 * @author gkspencer
 */
public class SrvsvcDCEHandler implements DCEHandler {

    /**
     * Process a SrvSvc DCE/RPC request
     *
     * @param sess SMBSrvSession
     * @param inBuf DCEBuffer
     * @param pipeFile DCEPipeFile
     * @exception IOException
     * @exception SMBSrvException
     */
    @Override
    public void processRequest(SMBSrvSession sess, DCEBuffer inBuf, DCEPipeFile pipeFile)
            throws IOException, SMBSrvException {
        //	Get the operation code and move the buffer pointer to the start of the request data
        int opNum = inBuf.getHeaderValue(DCEBuffer.HDR_OPCODE);
        try {
            inBuf.skipBytes(DCEBuffer.OPERATIONDATA);
        } catch (DCEBufferException ex) {
        }
        //	Debug
        if (Debug.EnableInfo && sess.hasDebug(SMBSrvSession.DBG_DCERPC)) {
            sess.debugPrintln("DCE/RPC SrvSvc request=" + Srvsvc.getOpcodeName(opNum));
        }

        //	Create the output DCE buffer and add the response header
        DCEBuffer outBuf = new DCEBuffer();
        outBuf.putResponseHeader(inBuf.getHeaderValue(DCEBuffer.HDR_CALLID), 0);
        //	Process the request
        boolean processed = false;
        switch (opNum) {
            //	Enumerate shares
            case Srvsvc.NetrShareEnum:
                processed = netShareEnum(sess, inBuf, outBuf);
                break;
            //	Enumerate all shares
            case Srvsvc.NetrShareEnumSticky:
                processed = netShareEnum(sess, inBuf, outBuf);
                break;
            //	Get share information
            case Srvsvc.NetrShareGetInfo:
                processed = netShareGetInfo(sess, inBuf, outBuf);
                break;
            //	Get server information
            case Srvsvc.NetrServerGetInfo:
                processed = netServerGetInfo(sess, inBuf, outBuf);
                break;
            //	Unsupported function
            default:
                break;
        }
        //	Return an error status if the request was not processed
        if (processed == false) {
            sess.sendErrorResponseSMB(SMBStatus.SRVNotSupported, SMBStatus.ErrSrv);
            return;
        }
        //	Set the allocation hint for the response
        outBuf.setHeaderValue(DCEBuffer.HDR_ALLOCHINT, outBuf.getLength());
        //	Attach the output buffer to the pipe file
        pipeFile.setBufferedData(outBuf);
    }

    /**
     * Handle a share enumeration request
     *
     * @param sess SMBSrvSession
     * @param inBuf DCEPacket
     * @param outBuf DCEPacket
     * @return boolean
     */
    protected final boolean netShareEnum(SMBSrvSession sess, DCEBuffer inBuf, DCEBuffer outBuf) {
        //	Decode the request
        String srvName;
        ShareInfoList shrInfo;
        try {
            inBuf.skipPointer();
            srvName = inBuf.getString(DCEBuffer.ALIGN_INT);
            shrInfo = new ShareInfoList(inBuf);
        } catch (DCEBufferException ex) {
            return false;
        }

        //	Debug
        if (Debug.EnableInfo && sess.hasDebug(SMBSrvSession.DBG_DCERPC)) {
            sess.debugPrintln("NetShareEnum srvName=" + srvName + ", shrInfo=" + shrInfo.toString());
        }

        //	Get the share list from the server
        SharedDeviceList shareList = sess.getServer().getShareMapper().getShareList(srvName, sess, false);

        //	Check if there is an access control manager configured
        if (sess.getServer().hasAccessControlManager()) {

            //	Filter the list of available shares by applying any access control rules
            AccessControlManager aclMgr = sess.getServer().getAccessControlManager();

            shareList = aclMgr.filterShareList(sess, shareList);
        }

        //	Create a list of share information objects of the required information level
        List infoList = new ArrayList();
        Enumeration enm = shareList.enumerateShares();
        while (enm.hasMoreElements()) {
            //	Get the current shared device details
            SharedDevice share = (SharedDevice) enm.nextElement();
            //	Determine the share type
            int shrTyp = ShareInfo.Disk;
            if (share.getType() == ShareType.PRINTER) {
                shrTyp = ShareInfo.PrintQueue;
            } else if (share.getType() == ShareType.NAMEDPIPE) {
                shrTyp = ShareInfo.IPC;
            } else if (share.getType() == ShareType.ADMINPIPE) {
                shrTyp = ShareInfo.IPC + ShareInfo.Hidden;
            }
            //	Create a share information object with the basic information
            ShareInfo info = new ShareInfo(shrInfo.getInformationLevel(), share.getName(), shrTyp, share.getComment());
            infoList.add(info);
            //	Add additional information
            switch (shrInfo.getInformationLevel()) {
                //	Level 2
                case 2:
                    if (share.getContext() != null) {
                        info.setPath(share.getContext().getDeviceName());
                    }
                    break;
                //	Level 502
                case 502:
                    if (share.getContext() != null) {
                        info.setPath(share.getContext().getDeviceName());
                    }
                    break;
            }
        }
        //	Set the share information list in the server share information and write the
        //	share information to the output DCE buffer.
        shrInfo.setShareList(infoList);
        try {
            shrInfo.writeList(outBuf);
            outBuf.putInt(0);		//	status code
        } catch (DCEBufferException ex) {
        }
        //	Indicate that the request was processed successfully
        return true;
    }

    /**
     * Handle a get share information request
     *
     * @param sess SMBSrvSession
     * @param inBuf DCEPacket
     * @param outBuf DCEPacket
     * @return boolean
     */
    protected final boolean netShareGetInfo(SMBSrvSession sess, DCEBuffer inBuf, DCEBuffer outBuf) {
        //	Decode the request
        String srvName;
        String shrName;
        int infoLevel;
        try {
            inBuf.skipPointer();
            srvName = inBuf.getString(DCEBuffer.ALIGN_INT);
            shrName = inBuf.getString(DCEBuffer.ALIGN_INT);
            infoLevel = inBuf.getInt();
        } catch (DCEBufferException ex) {
            return false;
        }
        //	Debug
        if (Debug.EnableInfo && sess.hasDebug(SMBSrvSession.DBG_DCERPC)) {
            sess.debugPrintln("netShareGetInfo srvname=" + srvName + ", share=" + shrName + ", infoLevel=" + infoLevel);
        }
        //	Find the required shared device
        SharedDevice share = null;
        try {
            //	Get the shared device details 
            share = sess.getServer().findShare(srvName, shrName, ShareType.UNKNOWN, sess, false);
        } catch (Exception ex) {
        }
        //	Check if the share details are valid
        if (share == null) {
            return false;
        }
        //	Determine the share type
        int shrTyp = ShareInfo.Disk;
        if (share.getType() == ShareType.PRINTER) {
            shrTyp = ShareInfo.PrintQueue;
        } else if (share.getType() == ShareType.NAMEDPIPE) {
            shrTyp = ShareInfo.IPC;
        } else if (share.getType() == ShareType.ADMINPIPE) {
            shrTyp = ShareInfo.IPC + ShareInfo.Hidden;
        }
        //	Create the share information
        ShareInfo shrInfo = new ShareInfo(infoLevel, share.getName(), shrTyp, share.getComment());

        //	Pack the information level, structure pointer and share information
        outBuf.putInt(infoLevel);
        outBuf.putPointer(true);
        shrInfo.writeObject(outBuf, outBuf);
        //	Add the status and return a success status
        outBuf.putInt(0);
        return true;
    }

    /**
     * Handle a get server information request
     *
     * @param sess SMBSrvSession
     * @param inBuf DCEPacket
     * @param outBuf DCEPacket
     * @return boolean
     */
    protected final boolean netServerGetInfo(SMBSrvSession sess, DCEBuffer inBuf, DCEBuffer outBuf) {
        //	Decode the request
        String srvName;
        int infoLevel;
        try {
            inBuf.skipPointer();
            srvName = inBuf.getString(DCEBuffer.ALIGN_INT);
            infoLevel = inBuf.getInt();
        } catch (DCEBufferException ex) {
            return false;
        }
        //	Debug
        if (Debug.EnableInfo && sess.hasDebug(SMBSrvSession.DBG_DCERPC)) {
            sess.debugPrintln("netServerGetInfo srvname=" + srvName + ", infoLevel=" + infoLevel);
        }
        //	Create the server information and set the common values
        ServerInfo srvInfo = new ServerInfo(infoLevel);
        SMBServer srv = sess.getSMBServer();
        srvInfo.setServerName(srv.getServerName());
        srvInfo.setComment(srv.getComment());
        srvInfo.setServerType(srv.getServerType());
        //	Determine if the server is using the NT SMB dialect and set the platofmr id accordingly
        CIFSConfigSection cifsConfig = srv.getCIFSConfiguration();
        if (cifsConfig != null && cifsConfig.getEnabledDialects().hasDialect(Dialect.NT) == true) {
            srvInfo.setPlatformId(ServerInfo.PLATFORM_NT);
            srvInfo.setVersion(5, 1);
        } else {
            srvInfo.setPlatformId(ServerInfo.PLATFORM_OS2);
            srvInfo.setVersion(4, 0);
        }

        //	Write the server information to the DCE response
        srvInfo.writeObject(outBuf, outBuf);
        outBuf.putInt(0);

        //	Indicate that the request was processed successfully
        return true;
    }
}

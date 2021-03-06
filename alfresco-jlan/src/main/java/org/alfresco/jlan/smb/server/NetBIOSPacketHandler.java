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
package org.alfresco.jlan.smb.server;

import java.io.IOException;
import java.net.Socket;
import java.net.SocketTimeoutException;

import org.alfresco.jlan.netbios.RFCNetBIOSProtocol;
import org.alfresco.jlan.util.DataPacker;

/**
 * NetBIOS Protocol Packet Handler Class
 *
 * @author gkspencer
 */
public class NetBIOSPacketHandler extends SocketPacketHandler {

    /**
     * Class constructor
     *
     * @param sock Socket
     * @exception IOException	If a network error occurs
     */
    public NetBIOSPacketHandler(Socket sock)
            throws IOException {
        super(sock, SMBSrvPacket.PROTOCOL_NETBIOS, "NetBIOS", "NB");
    }

    /**
     * Read a packet from the input stream
     *
     * @param pkt SMBSrvPacket
     * @return int
     * @throws java.io.IOException
     */
    @Override
    public final int readPacket(SMBSrvPacket pkt) throws IOException {
        //	Read the packet header
        byte[] buf = pkt.getBuffer();
        int len = 0;
        while (len < RFCNetBIOSProtocol.HEADER_LEN && len != -1) {
            try {
                len = readPacket(buf, len, RFCNetBIOSProtocol.HEADER_LEN - len);
            } catch (SocketTimeoutException ex) {
            }
        }
        //	Check if the connection has been closed, read length equals -1
        if (len == -1) {
            return len;
        }

        //	Check if we received a valid NetBIOS header
        if (len < RFCNetBIOSProtocol.HEADER_LEN) {
            throw new IOException("Invalid NetBIOS header, len=" + len);
        }

        //  Get the packet type from the header
        int typ = (int) (buf[0] & 0xFF);
        int flags = (int) buf[1];
        int dlen = (int) DataPacker.getShort(buf, 2);

        if ((flags & 0x01) != 0) {
            dlen += 0x10000;
        }
        //	Check for a session keep alive type message
        if (typ == RFCNetBIOSProtocol.SESSION_KEEPALIVE) {
            return 0;
        }
        //	Check if the packet buffer is large enough to hold the data + header
        if (buf.length < (dlen + RFCNetBIOSProtocol.HEADER_LEN)) {
            //	Allocate a new buffer to hold the data and copy the existing header
            byte[] newBuf = new byte[dlen + RFCNetBIOSProtocol.HEADER_LEN];
            System.arraycopy(buf, 0, newBuf, 0, 4);
            //	Attach the new buffer to the SMB packet
            pkt.setBuffer(newBuf);
            buf = newBuf;
        }

        //  Read the data part of the packet into the users buffer, this may take
        //  several reads
        int offset = RFCNetBIOSProtocol.HEADER_LEN;
        int totlen = offset;

        while (dlen > 0) {
            //  Read the data
            try {
                len = readPacket(buf, offset, dlen);
            } catch (SocketTimeoutException ex) {
                continue;
            }
            //	Check if the connection has been closed
            if (len == -1) {
                return -1;
            }

            //  Update the received length and remaining data length
            totlen += len;
            dlen -= len;

            //  Update the user buffer offset as more reads will be required
            //  to complete the data read
            offset += len;

        } // end while reading data

        //	Return the received packet length
        return totlen;
    }

    /**
     * Send a packet to the output stream
     *
     * @param pkt SMBSrvPacket
     * @param len int
     * @throws java.io.IOException
     */
    @Override
    public final void writePacket(SMBSrvPacket pkt, int len) throws IOException {
        //  Fill in the NetBIOS message header, this is already allocated as
        //  part of the users buffer.
        byte[] buf = pkt.getBuffer();
        buf[0] = (byte) RFCNetBIOSProtocol.SESSION_MESSAGE;
        buf[1] = (byte) 0;
        if (len > 0xFFFF) {
            //	Set the >64K flag
            buf[1] = (byte) 0x01;
            //	Set the low word of the data length
            DataPacker.putShort((short) (len & 0xFFFF), buf, 2);
        } else {
            //	Set the data length
            DataPacker.putShort((short) len, buf, 2);
        }
        //  Output the data packet
        int bufSiz = len + RFCNetBIOSProtocol.HEADER_LEN;
        writePacket(buf, 0, bufSiz);
    }
}

/*
 * Copyright (C) 2016 Alfresco Software Limited.
 *
 * This file is part of Alfresco
 *
 * Alfresco is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Alfresco is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Alfresco. If not, see <http://www.gnu.org/licenses/>.
 */
/***************************************************************************
 *
 * Copyright (C) 2016 SurCloud.
 *
 * This file is part of JLAN for SurFS
 *
 * JLAN for SurFS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * JLAN for SurFS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with JLAN for SurFS. If not, see <http://www.gnu.org/licenses/>.
 */
package org.alfresco.jlan.oncrpc;

import com.autumn.core.log.LogFactory;
import com.autumn.core.log.Logger;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Rpc Packet Pool Class
 *
 * <p>
 * Contains a pool of small and large RpcPacket objects for use by
 * multi-threaded RPC servers.
 *
 * @author gkspencer
 */
public class RpcPacketPool {

    private final static Logger log = LogFactory.getLogger();
    // Default small/large packet sizes
    public static final int DefaultSmallSize = 1024 * 4;
    public static final int DefaultLargeSize = 1024 * 32;

    public static final int DefaultSmallLimit = -1; // no allocation limit
    public static final int DefaultLargeLimit = -1; // " " "

    // Small packet size and maximum allowed packets
    private final int m_smallPktSize;
    private final int m_smallPktLimit;

    // Large packet size and maximum allowed packets
    private final int m_largePktSize;
    private final int m_largePktLimit;

    // Count of allocated small/large packets
    private final AtomicInteger m_smallPktCount = new AtomicInteger(0);
    private final AtomicInteger m_largePktCount = new AtomicInteger(0);
    // Small/large packet lists
    private final LinkedTransferQueue<RpcPacket> m_smallPackets = new LinkedTransferQueue<>();
    private final LinkedTransferQueue<RpcPacket> m_largePackets = new LinkedTransferQueue<>();

    /**
     * Default constructor
     */
    public RpcPacketPool() {
        m_smallPktSize = DefaultSmallSize;
        m_smallPktLimit = DefaultSmallLimit;
        m_largePktSize = DefaultLargeSize;
        m_largePktLimit = DefaultLargeLimit;
    }

    /**
     * Class constructor
     *
     * @param smallSize int
     * @param smallLimit int
     * @param largeSize int
     * @param largeLimit int
     */
    public RpcPacketPool(int smallSize, int smallLimit, int largeSize, int largeLimit) {
        m_smallPktSize = smallSize;
        m_smallPktLimit = smallLimit;
        m_largePktSize = largeSize;//+124
        m_largePktLimit = largeLimit;
    }

    /**
     * Class constructor
     *
     * @param largeSize int
     * @param largeLimit int
     */
    public RpcPacketPool(int largeSize, int largeLimit) {
        m_smallPktSize = DefaultSmallSize;
        m_smallPktLimit = largeLimit;
        m_largePktSize = largeSize;//+124
        m_largePktLimit = largeLimit;
    }

    /**
     * Return the small packet size
     *
     * @return int
     */
    public final int getSmallPacketSize() {
        return m_smallPktSize;
    }

    /**
     * Return the count of allocated small packets
     *
     * @return int
     */
    public final int getSmallPacketCount() {
        return m_smallPktCount.get();
    }

    /**
     * Return the small packet allocation limit
     *
     * @return int
     */
    public final int getSmallPacketAllocationLimit() {
        return m_smallPktLimit;
    }

    /**
     * Return the count of available large packets
     *
     * @return int
     */
    public final int availableLargePackets() {
        return m_largePackets.size();
    }

    /**
     * Return the large packet size
     *
     * @return int
     */
    public final int getLargePacketSize() {
        return m_largePktSize;
    }

    /**
     * Return the count of allocated large packets
     *
     * @return int
     */
    public final int getLargePacketCount() {
        return m_largePktCount.get();
    }

    /**
     * Return the large packet allocation limit
     *
     * @return int
     */
    public final int getLargePacketAllocationLimit() {
        return m_largePktLimit;
    }

    /**
     * Return the count of available small packets
     *
     * @return int
     */
    public final int availableSmallPackets() {
        return m_smallPackets.size();
    }

    /**
     * Allocate a packet from the packet pool
     *
     * @param reqSize int
     * @return RpcPacket
     */
    public final RpcPacket allocatePacket(int reqSize) {
        RpcPacket pkt;// Check if the packet should come from the small or large packet list
        if (reqSize <= m_smallPktSize) {// Allocate a packet from the small packet list
            pkt = allocateSmallPacket();
        } else {// Allocate a packet from the large packet list    
            pkt = allocateLargePacket();
        }
        return pkt;// Return the allocated packet
    }

    /**
     * Release an RPC packet back to the pool
     *
     * @param pkt RpcPacket
     */
    public final void releasePacket(RpcPacket pkt) {
        if (pkt.getBuffer().length >= m_largePktSize) {// Check if the packet should be released to the small or large list
            m_largePackets.offer(pkt);
        } else {
            m_smallPackets.offer(pkt);
        }
    }

    /**
     * Allocate, or create, a small RPC packet
     *
     * @return RpcPacket
     */
    private RpcPacket allocateSmallPacket() {
        RpcPacket pkt = m_smallPackets.poll();
        if (pkt == null) {
            if (m_smallPktLimit == -1 || m_smallPktCount.get() < m_smallPktLimit) {
                pkt = new RpcPacket(m_smallPktSize, this);
                m_smallPktCount.incrementAndGet();
                log.info("RpcPacketPool Allocated (small),len={0}, list={1}/{2}",
                        new Object[]{pkt.getBuffer().length, m_smallPktCount.get(), m_smallPktLimit});
            } else {
                try {
                    pkt = m_smallPackets.take();
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            }
        }
        return pkt;
    }

    /**
     * Allocate, or create, a large RPC packet
     *
     * @return RpcPacket
     */
    private RpcPacket allocateLargePacket() {
        RpcPacket pkt = m_largePackets.poll();
        if (pkt == null) {
            if (m_largePktLimit == -1 || m_largePktCount.get() < m_largePktLimit) {
                pkt = new RpcPacket(m_largePktSize, this);
                m_largePktCount.incrementAndGet();
                log.info("RpcPacketPool Allocated (large),len={0}, list={1}/{2}",
                        new Object[]{pkt.getBuffer().length, m_largePktCount.get(), m_largePktLimit});
            } else {
                try {
                    pkt = m_largePackets.take();
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            }
        }
        return pkt;
    }
}

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
package org.alfresco.jlan.smb;

import java.util.BitSet;

/**
 * SMB dialect selector class.
 *
 * <p>
 * Used to select the SMB/CIFS dialects that a client can use when communicating
 * with a remote server. The dialect list is used by the client/server to agree
 * the protocol level during the initial SMB/CIFS negotiation phase.
 *
 * @author gkspencer
 */
public class DialectSelector {

    // Bit set of selected SMB dialects.
    private final BitSet m_dialects;

    /**
     * Construct a new SMB dialect selector with the SMB core protocol selected.
     */
    public DialectSelector() {
        m_dialects = new BitSet(Dialect.Max);

        //  Select only the core protocol by default
        ClearAll();
        AddDialect(Dialect.Core);
    }

    /**
     * Add a dialect to the list of available SMB dialects.
     *
     * @param idx Index of the dialect to add to the available dialects.
     * @exception java.lang.ArrayIndexOutOfBoundsException Invalid dialect
     * index.
     */
    public final void AddDialect(int idx) throws ArrayIndexOutOfBoundsException {
        m_dialects.set(idx);
    }

    /**
     * Clear all the dialect bits.
     */
    public final void ClearAll() {
        for (int i = 0; i < m_dialects.size(); m_dialects.clear(i++)) {
        }
    }

    /**
     * Set all available dialects
     */
    public void EnableAll() {
        for (int i = 0; i < Dialect.Max; m_dialects.set(i++)) {
        }
    }

    /**
     * Copy the SMB dialect selector settings.
     *
     * @param dsel DialectSelector
     */
    public void copyFrom(DialectSelector dsel) {
        //  Clear all current settings
        ClearAll();
        //  Copy the settings
        for (int i = 0; i < Dialect.Max; i++) {
            //  Check if the dialect is enabled
            if (dsel.hasDialect(i)) {
                AddDialect(i);
            }
        }
    }

    /**
     * Determine if the specified SMB dialect is selected/available.
     *
     * @param idx Index of the dialect to test for.
     * @return true if the SMB dialect is available, else false.
     * @exception java.lang.ArrayIndexOutOfBoundsException Invalid dialect
     * index.
     */
    public boolean hasDialect(int idx)
            throws ArrayIndexOutOfBoundsException {
        return m_dialects.get(idx);
    }

    /**
     * Determine if the core SMB dialect is enabled
     *
     * @return boolean
     */
    public boolean hasCore() {
        return hasDialect(Dialect.Core) || hasDialect(Dialect.CorePlus);
    }

    /**
     * Determine if the LanMan SMB dialect is enabled
     *
     * @return boolean
     */
    public boolean hasLanMan() {
        return hasDialect(Dialect.DOSLanMan1) || hasDialect(Dialect.DOSLanMan2)
                || hasDialect(Dialect.LanMan1) || hasDialect(Dialect.LanMan2)
                || hasDialect(Dialect.LanMan2_1);
    }

    /**
     * Determine if the NT SMB dialect is enabled
     *
     * @return boolean
     */
    public boolean hasNT() {
        return hasDialect(Dialect.NT);
    }

    /**
     * Remove an SMB dialect from the list of available dialects.
     *
     * @param idx Index of the dialect to remove.
     * @exception java.lang.ArrayIndexOutOfBoundsException Invalid dialect
     * index.
     */
    public void RemoveDialect(int idx)
            throws ArrayIndexOutOfBoundsException {
        m_dialects.clear(idx);
    }

    /**
     * Return the dialect selector list as a string.
     *
     * @return String
     */
    @Override
    public String toString() {
        //  Create a string buffer to build the return string
        StringBuilder str = new StringBuilder();
        str.append("[");
        for (int i = 0; i < m_dialects.size(); i++) {
            if (hasDialect(i)) {
                str.append(Dialect.DialectTypeString(i));
                str.append(",");
            }
        }
        //  Trim the last comma and return the string
        if (str.length() > 1) {
            str.setLength(str.length() - 1);
        }
        str.append("]");
        return str.toString();
    }
}

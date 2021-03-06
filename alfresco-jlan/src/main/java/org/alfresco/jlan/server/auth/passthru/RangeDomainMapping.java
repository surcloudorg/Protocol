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

package org.alfresco.jlan.server.auth.passthru;

import org.alfresco.jlan.util.IPAddress;

/**
 * Address Range Domain Mapping Class
 *
 * @author gkspencer
 */
public class RangeDomainMapping extends DomainMapping {

	// Range from/to addresses
	
	private int m_rangeFrom;
	private int m_rangeTo;
	
	/**
	 * class constructor
	 * 
	 * @param domain String
	 * @param rangeFrom int
	 * @param rangeTo int
	 */
	public RangeDomainMapping( String domain, int rangeFrom, int rangeTo)
	{
		super( domain);
		
		m_rangeFrom = rangeFrom;
		m_rangeTo   = rangeTo;
	}
	
	/**
	 * Return the from range address
	 * 
	 * @return int
	 */
	public final int getRangeFrom()
	{
		return m_rangeFrom;
	}
	
	/**
	 * Return the to range address
	 * 
	 * @return int
	 */
	public final int getRangeTo()
	{
		return m_rangeTo;
	}
	
	/**
	 * Check if the client address is a member of this domain
	 * 
	 * @param clientIP int
	 * @return boolean
	 */
	public boolean isMemberOfDomain( int clientIP)
	{
		if (clientIP >= m_rangeFrom && clientIP <= m_rangeTo)
			return true;
		return false;
	}
	
	/**
	 * Return the domain mapping as a string
	 * 
	 * @return String
	 */
	public String toString()
	{
		StringBuilder str = new StringBuilder();
		
		str.append("[");
		str.append(getDomain());
		str.append(",");
		str.append(IPAddress.asString( getRangeFrom()));
		str.append(":");
		str.append(IPAddress.asString( getRangeTo()));
		str.append("]");
		
		return str.toString();
	}
}

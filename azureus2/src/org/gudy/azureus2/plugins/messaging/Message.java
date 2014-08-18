/*
 * Created on Feb 9, 2005
 * Created by Alon Rohter
 * Copyright (C) Azureus Software, Inc, All Rights Reserved.
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
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *
 */

package org.gudy.azureus2.plugins.messaging;

import java.nio.ByteBuffer;



/**
 * Basic peer message.
 * A message is uniquely identified by the combination of ID and version.
 */
public interface Message { 
  
  /**
   * Is a protocol-bearing message, i.e. messaging/overhead data.
   */
  public static final int TYPE_PROTOCOL_PAYLOAD = 0;
  
  /**
   * Is a data-bearing message, i.e. file data.
   */
  public static final int TYPE_DATA_PAYLOAD     = 1;

  
  
  
  /**
   * Get message id.
   * @return id
   */
  public String getID();
  
  
  /**
   * Get message type.
   * @return type
   */
  public int getType();
    
  /**
   * Get textual description of this particular message.
   * @return description
   */
  public String getDescription();
  
  /**
   * Get message payload data.
   * @return message data buffers
   */
  public ByteBuffer[] getPayload();
  
  
  /**
   * Create a new instance of this message by decoding the given byte serialization.
   * @param data to deserialize
   * @return decoded message instance
   * @throws MessageException if the decoding process fails
   */
  public Message create( ByteBuffer data ) throws MessageException;
    
  
  /**
   * Destroy the message; i.e. perform cleanup actions.
   */
  public void destroy();
  
}

/*
 * Created on 15 Jun 2006
 * Created by Paul Gardner
 * Copyright (C) 2006 Aelitis, All Rights Reserved.
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
 * AELITIS, SAS au capital de 46,603.30 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */

package com.aelitis.azureus.core.security.impl;

import java.math.BigInteger;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.KeySpec;

import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.interfaces.ECPrivateKey;
import org.bouncycastle.jce.interfaces.ECPublicKey;
import org.bouncycastle.jce.spec.ECNamedCurveParameterSpec;
import org.bouncycastle.jce.spec.ECParameterSpec;
import org.bouncycastle.jce.spec.ECPrivateKeySpec;
import org.bouncycastle.jce.spec.ECPublicKeySpec;
import org.bouncycastle.math.ec.ECPoint;
import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.util.SystemTime;

import com.aelitis.azureus.core.security.CryptoHandler;
import com.aelitis.azureus.core.security.CryptoManager;
import com.aelitis.azureus.core.security.CryptoManagerException;
import com.aelitis.azureus.core.security.CryptoManagerPasswordException;
import com.aelitis.azureus.core.security.CryptoManagerPasswordHandler;

public class 
CryptoHandlerECC
	implements CryptoHandler
{
	private static final ECNamedCurveParameterSpec ECCparam = ECNamedCurveTable.getParameterSpec("prime192v2");

	private static String	CONFIG_PREFIX = "core.crypto.ecc.";
	
	private static final int	TIMEOUT_DEFAULT_SECS		= 60*60;
	
	private CryptoManagerImpl		manager;
	
	private PrivateKey			use_method_private_key;
	private PublicKey			use_method_public_key;
	
	private long	last_unlock_time;
	
	protected
	CryptoHandlerECC(
		CryptoManagerImpl		_manager,
		int						_instance_id )
	{
		manager	= _manager;
		
		CONFIG_PREFIX += _instance_id + ".";
	}
	
	public void
	unlock(
		char[]		password )
	
		throws CryptoManagerException
	{
		getMyPrivateKey( password, "" );
	}
	
	public synchronized void
	lock()
	{
		use_method_private_key	= null;
	}
	
	public int
	getUnlockTimeoutSeconds()
	{
		return( COConfigurationManager.getIntParameter( CONFIG_PREFIX + "timeout", TIMEOUT_DEFAULT_SECS ));
	}
	
	public void
	setUnlockTimeoutSeconds(
		int		secs )
	{
		COConfigurationManager.setParameter( CONFIG_PREFIX + "timeout", secs );
	}
	
	public byte[]
	sign(
		byte[]		data,
		char[]		password,
		String		reason )
	
		throws CryptoManagerException
	{
		PrivateKey	priv = getMyPrivateKey( password, reason );
		
		Signature sig = getSignature( priv );
		
		try{
			sig.update( data );
			
			return( sig.sign());
			
		}catch( Throwable e ){
			
			throw( new CryptoManagerException( "Signature failed", e ));
		}
	}
	 
	public boolean
	verify(
		byte[]		public_key,
		byte[]		data,
		byte[]		signature )
	
		throws CryptoManagerException
	{
		PublicKey	pub = rawdataToPubkey( public_key );
		
		Signature sig = getSignature( pub );
		
		try{
			sig.update( data );
			
			return( sig.verify( signature ));
			
		}catch( Throwable e ){
			
			throw( new CryptoManagerException( "Signature failed", e ));
		}
	}
	
	public byte[]
	getPublicKey(
		char[]		password,
		String		reason )
	
		throws CryptoManagerException
	{
		return( keyToRawdata( getMyPublicKey( password, reason )));
	}

	public byte[]
	getEncryptedPrivateKey(
		char[]		password,
		String		reason )
	
		throws CryptoManagerException
	{
		getMyPrivateKey( password, reason );
		
		byte[]	res = COConfigurationManager.getByteParameter( CONFIG_PREFIX + "privatekey", null );
		
		if ( res == null ){
			
			throw( new CryptoManagerException( "Private key unavailable" ));
		}
		
		return( res );
	}
	
	public synchronized void
	recoverKeys(
		byte[]		public_key,
		byte[]		encrypted_private_key )
	
		throws CryptoManagerException
	{
		use_method_private_key	= null;
		use_method_public_key	= null;
		
		COConfigurationManager.setParameter( CONFIG_PREFIX + "publickey", public_key );
			
		COConfigurationManager.setParameter( CONFIG_PREFIX + "privatekey", encrypted_private_key );
		
		COConfigurationManager.save();
	}
	
	public synchronized void
	resetKeys(
		char[]		password )
	
		throws CryptoManagerException
	{
		use_method_private_key	= null;
		use_method_public_key	= null;
		
		COConfigurationManager.removeParameter( CONFIG_PREFIX + "publickey" );
			
		COConfigurationManager.removeParameter( CONFIG_PREFIX + "privatekey" );
		
		COConfigurationManager.save();
		
		createAndStoreKeys( password, "" );
	}
	
	public synchronized void
	changePassword(
		char[]		old_password,
		char[]		new_password )
	
		throws CryptoManagerException
	{
			// ensure old password is correct
		
		use_method_private_key	= null;
		use_method_public_key	= null;
		
		getMyPrivateKey( old_password, "" );
		getMyPublicKey( old_password, "" );
		
		storeKeys( new_password );
	}
	
	
	protected synchronized PrivateKey
	getMyPrivateKey(
		char[]		password,
		String		reason )
	
		throws CryptoManagerException
	{
		if ( use_method_private_key != null ){
			
			int	timeout_secs = getUnlockTimeoutSeconds();
			
			if ( timeout_secs > 0 ){
				
				if ( SystemTime.getCurrentTime() - last_unlock_time >= timeout_secs * 1000 ){
					
					use_method_private_key = null;
				}
			}
		}
		
		if ( use_method_private_key == null ){
			
			byte[]	encoded = COConfigurationManager.getByteParameter( CONFIG_PREFIX + "privatekey", null );
			
			if ( encoded == null ){
				
				createAndStoreKeys( password, reason );
				
			}else{
				
				if ( password == null ){
					
					password = manager.getPassword( 
									CryptoManager.HANDLER_ECC, 
									CryptoManagerPasswordHandler.ACTION_DECRYPT, 
									reason );
				}

				use_method_private_key = rawdataToPrivkey( manager.decryptWithPBE( encoded, password ));
				
				last_unlock_time = SystemTime.getCurrentTime();
				
				boolean		ok = false;
				
				try{
					byte[]	test_data = "test".getBytes();
					
					ok = verify( keyToRawdata( getMyPublicKey( password, reason )), test_data,  sign( test_data, password, reason ));
					
					if ( !ok ){
											
						throw( new CryptoManagerPasswordException());
						
					}
					
				}catch( CryptoManagerException e ){
					
					throw( e );
					
				}catch( Throwable e ){
					
					throw( new CryptoManagerException( "Password incorrect", e ));
					
				}finally{
					
					if ( !ok ){
						
						use_method_private_key	= null;
					}
				}
			}
		}
		
		if ( use_method_private_key == null ){
			
			throw( new CryptoManagerException( "Failed to get private key" ));
		}
		
		return( use_method_private_key );
	}
	
	protected synchronized PublicKey
	getMyPublicKey(
		char[]		password,
		String		reason )
	
		throws CryptoManagerException
	{
		if ( use_method_public_key == null ){
			
			byte[]	key_bytes = COConfigurationManager.getByteParameter( CONFIG_PREFIX + "publickey", null );
			
			if ( key_bytes == null ){
				
				createAndStoreKeys( password, reason );
				
			}else{
				
				use_method_public_key = rawdataToPubkey( key_bytes );
			}
		}
		
		if ( use_method_public_key == null ){
			
			throw( new CryptoManagerException( "Failed to get public key" ));
		}
		
		return( use_method_public_key );
	}
	
	protected void
	createAndStoreKeys(
		char[]		password,
		String		reason )
	
		throws CryptoManagerException
	{
		if ( password == null ){
			
			password = manager.getPassword( 
							CryptoManager.HANDLER_ECC,
							CryptoManagerPasswordHandler.ACTION_ENCRYPT,
							reason );
		}
		
		KeyPair	keys = createKeys();
		
		use_method_public_key	= keys.getPublic();
		
		use_method_private_key	= keys.getPrivate();
		
		storeKeys( password );
	}
	
	protected void
	storeKeys(
		char[]		password )
	
		throws CryptoManagerException
	{
		COConfigurationManager.setParameter( CONFIG_PREFIX + "publickey", keyToRawdata( use_method_public_key ));
		
		byte[]	priv_raw = keyToRawdata( use_method_private_key );
		
		byte[]	priv_enc = manager.encryptWithPBE( priv_raw, password );
		
		COConfigurationManager.setParameter( CONFIG_PREFIX + "privatekey", priv_enc );

		COConfigurationManager.save();
	}
	
	protected KeyPair 
	createKeys()
	
		throws CryptoManagerException
	{
		try
		{
			KeyPairGenerator keyGen = KeyPairGenerator.getInstance("ECDSA", "BC");
			
			keyGen.initialize(ECCparam);

			return keyGen.genKeyPair();
			
		}catch(Throwable e){
			
			throw( new CryptoManagerException( "Failed to create keys", e ));
		}
	}

	public Signature 
	getSignature(
		Key key )
	
		throws CryptoManagerException
	{
		try
		{
			Signature ECCsig = Signature.getInstance("SHA1withECDSA", "BC");
			
			if( key instanceof ECPrivateKey ){
				
				ECCsig.initSign((ECPrivateKey)key);
				
			}else if( key instanceof ECPublicKey ){
				
				ECCsig.initVerify((ECPublicKey)key);

			}else{
				
				throw new CryptoManagerException("Invalid Key Type, ECC keys required");
			}
			
			return ECCsig;
			
		}catch( CryptoManagerException e ){
		
			throw( e );
			
		}catch( Throwable e ){
			
			throw( new CryptoManagerException( "Failed to create Signature", e ));
		}
	}

	protected byte[] 
	keyToRawdata( 
		PrivateKey privkey )
	
		throws CryptoManagerException
	{
		if(!(privkey instanceof ECPrivateKey)){
			
			throw( new CryptoManagerException( "Invalid private key" ));
		}
		
		return ((ECPrivateKey)privkey).getD().toByteArray();
	}

	protected PrivateKey 
	rawdataToPrivkey(
		byte[] input )
	
		throws CryptoManagerException
	{
		BigInteger D = new BigInteger(input);
		
		KeySpec keyspec = new ECPrivateKeySpec(D,(ECParameterSpec)ECCparam);
		
		PrivateKey privkey = null;
		
		try{
			privkey = KeyFactory.getInstance("ECDSA","BC").generatePrivate(keyspec);
			
			return privkey;
			
		}catch( Throwable e ){
	
			throw( new CryptoManagerException( "Failed to decode private key" ));
		}
	}
	
	protected byte[] 
	keyToRawdata(
		PublicKey pubkey )
	
		throws CryptoManagerException
	{
		if(!(pubkey instanceof ECPublicKey)){
			
			throw( new CryptoManagerException( "Invalid public key" ));
		}
		
		return ((ECPublicKey)pubkey).getQ().getEncoded();
	}
	
	
	protected PublicKey 
	rawdataToPubkey(
		byte[] input )
	
		throws CryptoManagerException
	{
		ECPoint W = ECCparam.getCurve().decodePoint(input);
		
		KeySpec keyspec = new ECPublicKeySpec(W,(ECParameterSpec)ECCparam);

		try{
			
			return KeyFactory.getInstance("ECDSA", "BC").generatePublic(keyspec);
			
		}catch (Throwable e){
		
			throw( new CryptoManagerException( "Failed to decode private key" ));
		}
	}	
}

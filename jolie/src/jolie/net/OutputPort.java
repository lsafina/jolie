/***************************************************************************
 *   Copyright (C) by Fabrizio Montesi                                     *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU Library General Public License as       *
 *   published by the Free Software Foundation; either version 2 of the    *
 *   License, or (at your option) any later version.                       *
 *                                                                         *
 *   This program is distributed in the hope that it will be useful,       *
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of        *
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the         *
 *   GNU General Public License for more details.                          *
 *                                                                         *
 *   You should have received a copy of the GNU Library General Public     *
 *   License along with this program; if not, write to the                 *
 *   Free Software Foundation, Inc.,                                       *
 *   59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.             *
 *                                                                         *
 *   For details about the authors of this software, see the AUTHORS file. *
 ***************************************************************************/

package jolie.net;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Vector;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.soap.SOAPException;
import javax.xml.transform.TransformerConfigurationException;
import jolie.Constants;
import jolie.Interpreter;
import jolie.process.AssignmentProcess;
import jolie.process.NullProcess;
import jolie.process.Process;
import jolie.process.SequentialProcess;
import jolie.runtime.AbstractIdentifiableObject;
import jolie.runtime.Expression;
import jolie.runtime.Value;
import jolie.runtime.VariablePath;
import jolie.util.Pair;

public class OutputPort extends AbstractIdentifiableObject
{
	final private Collection< String > operations;
	final private Constants.ProtocolId protocolId;
	final private Process configurationProcess;
	final private VariablePath locationVariablePath, protocolConfigurationVariablePath;

	public OutputPort(
			String id,
			Collection< String > operations,
			Constants.ProtocolId protocolId,
			Process protocolConfigurationProcess,
			URI locationURI
			)
	{
		super( id );
		this.operations = operations;
		this.protocolId = protocolId;
		
		// Create the location VariablePath
		Vector< Pair< Expression, Expression > > path =
					new Vector< Pair< Expression, Expression > >();
		path.add( new Pair< Expression, Expression >( Value.create( id ), null ) );
		path.add( new Pair< Expression, Expression >( Value.create( "location" ), null ) );
		this.locationVariablePath = new VariablePath( path, false );
		
		// Create the configuration Process
		Process a = ( locationURI == null ) ? NullProcess.getInstance() : 
			new AssignmentProcess( this.locationVariablePath, Value.create( locationURI.toString() ) );
		SequentialProcess s = new SequentialProcess();
		s.addChild( a );
		s.addChild( protocolConfigurationProcess );
		this.configurationProcess = s;
		
		path = new Vector< Pair< Expression, Expression > >();
		path.add( new Pair< Expression, Expression >( Value.create( id ), null ) );
		path.add( new Pair< Expression, Expression >( Value.create( "protocol" ), null ) );
		this.protocolConfigurationVariablePath = new VariablePath( path, false );
	}
	
	private CommProtocol protocol = null;
	
	protected CommProtocol getProtocol( URI uri )
		throws URISyntaxException, IOException
	{
		if ( protocol == null ) {
			if ( protocolId == null )
				throw new IOException( "Unknown protocol for output port " + id() );
			if ( protocolId.equals( Constants.ProtocolId.SODEP ) ) {
				protocol = new SODEPProtocol( protocolConfigurationVariablePath );
			} else if ( protocolId.equals( Constants.ProtocolId.SOAP ) ) {
				try {
					protocol = new SOAPProtocol(
							protocolConfigurationVariablePath,
							locationVariablePath,
							Interpreter.getInstance()
						);
				} catch( SOAPException e ) {
					throw new IOException( e );
				}
			} else if ( protocolId.equals( Constants.ProtocolId.HTTP ) ) {
				try {
					protocol = new HTTPProtocol(
							protocolConfigurationVariablePath,
							locationVariablePath
						);
				} catch( ParserConfigurationException e ) {
					throw new IOException( e );
				} catch( TransformerConfigurationException e ) {
					throw new IOException( e );
				}
			}
			assert( protocol != null );
		}
		
		
		return protocol.clone();
	}
	
	private CommChannel channel = null;
	private URI channelURI = null;

	public CommChannel getCommChannel()
		throws URISyntaxException, IOException
	{
		Value loc = locationVariablePath.getValue();
		if ( loc.isChannel() )
			channel = loc.channelValue();
		else {
			URI uri = new URI( loc.strValue() );
			if ( !uri.equals( channelURI ) || !channel.isOpen() ) {
				channel = CommChannel.createCommChannel( uri, this );
				channelURI = uri;
			} else
				channel.refreshProtocol();
		}
		
		return channel;
	}
	
	public VariablePath locationVariablePath()
	{
		return locationVariablePath;
	}
	
	public Collection< String > operations()
	{
		return operations;
	}
	
	public Constants.ProtocolId protocolId()
	{
		return protocolId;
	}
	
	public Process configurationProcess()
	{
		return configurationProcess;
	}
	
}

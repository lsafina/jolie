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

package jolie.process;

import java.io.IOException;

import jolie.ExecutionThread;
import jolie.net.CommChannel;
import jolie.net.CommMessage;
import jolie.runtime.FaultException;
import jolie.runtime.VariablePath;
import jolie.runtime.InputHandler;
import jolie.runtime.InputOperation;
import jolie.runtime.OperationChannelInfo;
import jolie.runtime.Value;

public class OneWayProcess implements CorrelatedInputProcess, InputOperationProcess
{
	/*private class ExecutionInChannel extends Execution
	{
		public ExecutionInChannel( OneWayProcess parent )
		{
			super( parent );
		}
		
		public void run()
			throws FaultException
		{
			try {
				parent.operation.signForMessage( this );
				synchronized( this ) {
					if( message == null )
						this.wait();
				}
				parent.channelInfo.channelPath().getValue().setChannel( channel );
			} catch( InterruptedException ie ) {
				parent.operation.cancelWaiting( this );
			}
			parent.runBehaviour( channel, message );
		}
		
		public synchronized void recvMessage( CommChannel channel, CommMessage message )
		{
			if ( parent.correlatedProcess != null )
				parent.correlatedProcess.inputReceived();

			this.message = message;
			this.channel = channel;
			this.notify();
		}
	}
	
	private class ExecutionFromChannel extends Execution
	{
		public ExecutionFromChannel( OneWayProcess parent )
		{
			super( parent );
		}
		
		public void run()
			throws FaultException
		{
			channel = parent.channelInfo.channelPath().getValue().channelValue();
			if ( channel == null )
				throw new FaultException( "ClosedChannel" );
			try {
				message = channel.recv();
			} catch( IOException ioe ) {
				throw new FaultException( "IOException" );
			}
			if ( !message.inputId().equals( parent.operation.id() ) )
				throw new FaultException( "BadOperationName" );
			parent.runBehaviour( channel, message );
		}
	}
	
	private class ExecutionPickChannel extends Execution
	{
		public ExecutionPickChannel( OneWayProcess parent )
		{
			super( parent );
		}
		
		public void run()
			throws FaultException
		{
			ValueVector vec = parent.channelInfo.channelPath().getValueVector();
			for( Value v : vec ) {
				channel = v.channelValue();
				
			}
			
			channel = parent.channelInfo.channelPath().getValue().channelValue();
			if ( channel == null )
				throw new FaultException( "ClosedChannel" );
			try {
				message = channel.recv();
			} catch( IOException ioe ) {
				throw new FaultException( "IOException" );
			}
			parent.runBehaviour( channel, message );
		}
	}*/
	
	private class Execution implements InputProcessExecution
	{
		protected CommMessage message = null;
		protected OneWayProcess parent;
		protected CommChannel channel = null;
		
		public Execution( OneWayProcess parent )
		{
			this.parent = parent;
		}
		
		public Process clone( TransformationReason reason )
		{
			return new Execution( parent );
		}
		
		public Process parent()
		{
			return parent;
		}

		public void run()
			throws FaultException
		{
			try {
				parent.operation.signForMessage( this );
				synchronized( this ) {
					if( message == null )
						this.wait();
				}
				parent.runBehaviour( null, message );
			} catch( InterruptedException ie ) {
				parent.operation.cancelWaiting( this );
			}
		}

		public synchronized void recvMessage( CommChannel channel, CommMessage message )
		{
			if ( parent.correlatedProcess != null )
				parent.correlatedProcess.inputReceived();

			this.message = message;
			this.notify();
			try {
				channel.close();
			} catch( IOException ioe ) {}
		}
	}
	
	protected InputOperation operation;
	protected VariablePath varPath;
	protected CorrelatedProcess correlatedProcess = null;
	protected OperationChannelInfo channelInfo;
	
	//private Class< ? extends Execution > model;

	public OneWayProcess(
			InputOperation operation,
			VariablePath varPath,
			OperationChannelInfo channelInfo )
	{
		this.operation = operation;
		this.varPath = varPath;
		this.channelInfo = channelInfo;

		/*if ( channelInfo == null )
			model = Execution.class;
		else if ( channelInfo.operator() == Constants.ChannelOperator.FROM )
			model = ExecutionFromChannel.class;
		else if ( channelInfo.operator() == Constants.ChannelOperator.IN )
			model = ExecutionInChannel.class;
		else if ( channelInfo.operator() == Constants.ChannelOperator.PICK )
			model = ExecutionPickChannel.class;*/
	}
	
	public Process clone( TransformationReason reason )
	{
		return new OneWayProcess( operation, varPath, channelInfo );
	}
	
	public void setCorrelatedProcess( CorrelatedProcess process )
	{
		this.correlatedProcess = process;
	}
	
	public VariablePath inputVarPath()
	{
		return varPath;
	}

	public void run()
		throws FaultException
	{
		if ( ExecutionThread.currentThread().isKilled() )
			return;
		
		(new Execution( this )).run();
	}
	
	public InputHandler getInputHandler()
	{
		return operation;
	}
	
	public void runBehaviour( CommChannel channel, CommMessage message )
	{
		if ( varPath != null ) {
			Value val = varPath.getValue();
			val.erase();
			val.deepCopy( message.value() );
		}
	}
}

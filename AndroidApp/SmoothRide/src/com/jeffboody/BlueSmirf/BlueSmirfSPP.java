/*
 * Copyright (c) 2012 Jeff Boody
 *
 * Permission is hereby granted, free of charge, to any person obtaining a
 * copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *
 */

package com.jeffboody.BlueSmirf;

import android.util.Log;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;

public class BlueSmirfSPP
{
	private static final String TAG = "BlueSmirfSPP";

	// Bluetooth code is based on this example
	// http://groups.google.com/group/android-beginners/browse_thread/thread/322c99d3b907a9e9/e1e920fe50135738?pli=1

	// well known SPP UUID
	private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

	// Internal state (protected by lock)
	private Lock    mLock;
	private boolean mIsConnected;
	private boolean mIsError;
	private String  mBluetoothAddress;

	// Bluetooth state
	private BluetoothAdapter mBluetoothAdapter;
	private BluetoothSocket  mBluetoothSocket;
	private OutputStream     mOutputStream;
	private InputStream      mInputStream;

	public BlueSmirfSPP()
	{
		mLock             = new ReentrantLock();
		mIsConnected      = false;
		mIsError          = false;
		mBluetoothAdapter = null;
		mBluetoothSocket  = null;
		mOutputStream     = null;
		mInputStream      = null;
		mBluetoothAddress = null;
	}

	public boolean connect(String addr)
	{
		mLock.lock();
		try
		{
			if(mIsConnected)
			{
				Log.e(TAG, "connect: already connected");
				return false;
			}
			mBluetoothAddress = addr;
		}
		finally
		{
			mLock.unlock();
		}

		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		if(mBluetoothAdapter == null)
		{
			Log.e(TAG, "connect: no adapter");
			return false;
		}

		if(mBluetoothAdapter.isEnabled() == false)
		{
			Log.e(TAG, "connect: bluetooth disabled");
			return false;
		}

		try
		{
			// Address must be upper case
			addr.toUpperCase();

			BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(addr);
			mBluetoothSocket = device.createRfcommSocketToServiceRecord(MY_UUID);

			// discovery is a heavyweight process so
			// disable while making a connection
			mBluetoothAdapter.cancelDiscovery();

			mBluetoothSocket.connect();
			mOutputStream = mBluetoothSocket.getOutputStream();
			mInputStream = mBluetoothSocket.getInputStream();
		}
		catch (Exception e)
		{
			Log.e(TAG, "connect: ", e);
			disconnect();
			return false;
		}

		mLock.lock();
		try
		{
			mIsConnected = true;
			mIsError     = false;
		}
		finally
		{
			mLock.unlock();
		}
		return true;
	}

	public void disconnect()
	{
		mLock.lock();
		try
		{
			// don't log error when closing streams
			mIsConnected = false;

			try { mOutputStream.close();    } catch(Exception e) { }
			try { mInputStream.close();     } catch(Exception e) { }
			try { mBluetoothSocket.close(); } catch(Exception e) { }

			mOutputStream     = null;
			mInputStream      = null;
			mBluetoothSocket  = null;
			mBluetoothAdapter = null;
			mIsError          = false;
		}
		finally
		{
			mLock.unlock();
		}
	}

	public boolean isConnected()
	{
		mLock.lock();
		try
		{
			return mIsConnected;
		}
		finally
		{
			mLock.unlock();
		}
	}

	public boolean isError()
	{
		mLock.lock();
		try
		{
			return mIsError;
		}
		finally
		{
			mLock.unlock();
		}
	}

	public String getBluetoothAddress()
	{
		mLock.lock();
		try
		{
			return mBluetoothAddress;
		}
		finally
		{
			mLock.unlock();
		}
	}

	public void writeByte(int b)
	{
		try
		{
			mOutputStream.write(b);
		}
		catch (Exception e)
		{
			mLock.lock();
			try
			{
				if(mIsConnected && (mIsError == false))
				{
					Log.e(TAG, "writeByte: " + e);
					mIsError = true;
				}
			}
			finally
			{
				mLock.unlock();
			}
		}
	}

	public void write(byte[] buffer, int offset, int count)
	{
		try
		{
			mOutputStream.write(buffer, offset, count);
		}
		catch (Exception e)
		{
			mLock.lock();
			try
			{
				if(mIsConnected && (mIsError == false))
				{
					Log.e(TAG, "write: " + e);
					mIsError = true;
				}
			}
			finally
			{
				mLock.unlock();
			}
		}
	}

	public int readByte()
	{
		int b = 0;
		try
		{
			b = mInputStream.read();
			if(b == -1)
			{
				disconnect();
			}
		}
		catch (Exception e)
		{
			mLock.lock();
			try
			{
				if(mIsConnected && (mIsError == false))
				{
					Log.e(TAG, "readByte: " + e);
					mIsError = true;
				}
			}
			finally
			{
				mLock.unlock();
			}
		}
		return b;
	}

	public int read(byte[] buffer, int offset, int length)
	{
		int b = 0;
		try
		{
			b = mInputStream.read(buffer, offset, length);
			if(b == -1)
			{
				disconnect();
			}
		}
		catch (Exception e)
		{
			mLock.lock();
			try
			{
				if(mIsConnected && (mIsError == false))
				{
					Log.e(TAG, "read: " + e);
					mIsError = true;
				}
			}
			finally
			{
				mLock.unlock();
			}
		}
		return b;
	}

	public void flush()
	{
		try
		{
			mOutputStream.flush();
		}
		catch (Exception e)
		{
			mLock.lock();
			try
			{
				if(mIsConnected && (mIsError == false))
				{
					Log.e(TAG, "flush: " + e);
					mIsError = true;
				}
			}
			finally
			{
				mLock.unlock();
			}
		}
	}
}

package com.idevicesinc.sweetblue.utils;

import android.content.ContentResolver;
import android.database.CharArrayBuffer;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.net.Uri;
import android.os.Bundle;

import com.idevicesinc.sweetblue.annotations.Extendable;

/**
 * An implementation of {@link Cursor} used for error conditions and things like that where we don't want to return null.
 */
@Extendable
public class EmptyCursor implements Cursor
{
	/**
	 * Singleton value, safe to reuse.
	 */
	public static final EmptyCursor SINGLETON = new EmptyCursor();

	@Override public int getCount()
	{
		return 0;
	}

	@Override public int getPosition()
	{
		return 0;
	}

	@Override public boolean move(int offset)
	{
		return false;
	}

	@Override public boolean moveToPosition(int position)
	{
		return false;
	}

	@Override public boolean moveToFirst()
	{
		return false;
	}

	@Override public boolean moveToLast()
	{
		return false;
	}

	@Override public boolean moveToNext()
	{
		return false;
	}

	@Override public boolean moveToPrevious()
	{
		return false;
	}

	@Override public boolean isFirst()
	{
		return false;
	}

	@Override public boolean isLast()
	{
		return false;
	}

	@Override public boolean isBeforeFirst()
	{
		return false;
	}

	@Override public boolean isAfterLast()
	{
		return false;
	}

	@Override public int getColumnIndex(String columnName)
	{
		return 0;
	}

	@Override public int getColumnIndexOrThrow(String columnName) throws IllegalArgumentException
	{
		return 0;
	}

	@Override public String getColumnName(int columnIndex)
	{
		return "";
	}

	@Override public String[] getColumnNames()
	{
		return new String[0];
	}

	@Override public int getColumnCount()
	{
		return 0;
	}

	@Override public byte[] getBlob(int columnIndex)
	{
		return P_Const.EMPTY_BYTE_ARRAY;
	}

	@Override public String getString(int columnIndex)
	{
		return "";
	}

	@Override public void copyStringToBuffer(int columnIndex, CharArrayBuffer buffer)
	{

	}

	@Override public short getShort(int columnIndex)
	{
		return 0;
	}

	@Override public int getInt(int columnIndex)
	{
		return 0;
	}

	@Override public long getLong(int columnIndex)
	{
		return 0;
	}

	@Override public float getFloat(int columnIndex)
	{
		return 0;
	}

	@Override public double getDouble(int columnIndex)
	{
		return 0;
	}

	@Override public int getType(int columnIndex)
	{
		return 0;
	}

	@Override public boolean isNull(int columnIndex)
	{
		return false;
	}

	@Override public void deactivate()
	{

	}

	@Override public boolean requery()
	{
		return false;
	}

	@Override public void close()
	{

	}

	@Override public boolean isClosed()
	{
		return false;
	}

	@Override public void registerContentObserver(ContentObserver observer)
	{

	}

	@Override public void unregisterContentObserver(ContentObserver observer)
	{

	}

	@Override public void registerDataSetObserver(DataSetObserver observer)
	{

	}

	@Override public void unregisterDataSetObserver(DataSetObserver observer)
	{

	}

	@Override public void setNotificationUri(ContentResolver cr, Uri uri)
	{

	}

	@Override public Uri getNotificationUri()
	{
		return Uri.EMPTY;
	}

	@Override public boolean getWantsAllOnMoveCalls()
	{
		return false;
	}

	@Override
	public void setExtras(Bundle extras)
	{

	}

	@Override public Bundle getExtras()
	{
		return null;
	}

	@Override public Bundle respond(Bundle extras)
	{
		return null;
	}
}

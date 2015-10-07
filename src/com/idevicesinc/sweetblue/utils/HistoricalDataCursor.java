package com.idevicesinc.sweetblue.utils;

import android.database.Cursor;

/**
 * This interface defines a wrapper around a database cursor (similar to {@link android.database.Cursor})
 * specific to a database representing a series of {@link HistoricalData} serializations.
 * For performance reasons, implementations are not required to return actual {@link HistoricalData} instances,
 * just the underlying <code>byte[]</code> BLOB and <code>long</code> timestamp for each.
 * <br><br>
 * NOTE: This may be wrapping an in-memory list, not just a database.
 */
public interface HistoricalDataCursor
{
	/**
	 * Returns the numbers of rows in the cursor.
	 *
	 * @return the number of rows in the cursor.
	 */
	int getCount();

	/**
	 * Returns the current position of the cursor in the row set.
	 * The value is zero-based. When the row set is first returned the cursor
	 * will be at positon -1, which is before the first row. After the
	 * last row is returned another call to next() will leave the cursor past
	 * the last entry, at a position of count().
	 *
	 * @return the current cursor position.
	 */
	int getPosition();

	/**
	 * Move the cursor by a relative amount, forward or backward, from the
	 * current position. Positive offsets move forwards, negative offsets move
	 * backwards. If the final position is outside of the bounds of the result
	 * set then the resultant position will be pinned to -1 or count() depending
	 * on whether the value is off the front or end of the set, respectively.
	 *
	 * <p>This method will return true if the requested destination was
	 * reachable, otherwise, it returns false. For example, if the cursor is at
	 * currently on the second entry in the result set and move(-5) is called,
	 * the position will be pinned at -1, and false will be returned.
	 *
	 * @param offset the offset to be applied from the current position.
	 * @return whether the requested move fully succeeded.
	 */
	boolean move(int offset);

	/**
	 * Move the cursor to an absolute position. The valid
	 * range of values is -1 &lt;= position &lt;= count.
	 *
	 * <p>This method will return true if the request destination was reachable,
	 * otherwise, it returns false.
	 *
	 * @param position the zero-based position to move to.
	 * @return whether the requested move fully succeeded.
	 */
	boolean moveToPosition(int position);

	/**
	 * Move the cursor to the first row.
	 *
	 * <p>This method will return false if the cursor is empty.
	 *
	 * @return whether the move succeeded.
	 */
	boolean moveToFirst();

	/**
	 * Move the cursor to the last row.
	 *
	 * <p>This method will return false if the cursor is empty.
	 *
	 * @return whether the move succeeded.
	 */
	boolean moveToLast();

	/**
	 * Move the cursor to the next row.
	 *
	 * <p>This method will return false if the cursor is already past the
	 * last entry in the result set.
	 *
	 * @return whether the move succeeded.
	 */
	boolean moveToNext();

	/**
	 * Move the cursor to the previous row.
	 *
	 * <p>This method will return false if the cursor is already before the
	 * first entry in the result set.
	 *
	 * @return whether the move succeeded.
	 */
	boolean moveToPrevious();

	/**
	 * Returns whether the cursor is pointing to the first row.
	 *
	 * @return whether the cursor is pointing at the first entry.
	 */
	boolean isFirst();

	/**
	 * Returns whether the cursor is pointing to the last row.
	 *
	 * @return whether the cursor is pointing at the last entry.
	 */
	boolean isLast();

	/**
	 * Returns whether the cursor is pointing to the position before the first
	 * row.
	 *
	 * @return whether the cursor is before the first result.
	 */
	boolean isBeforeFirst();

	/**
	 * Returns whether the cursor is pointing to the position after the last
	 * row.
	 *
	 * @return whether the cursor is after the last result.
	 */
	boolean isAfterLast();

	/**
	 * Closes the Cursor, releasing all of its resources and making it completely invalid.
	 */
	void close();

	/**
	 * return true if the cursor is closed
	 * @return true if the cursor is closed.
	 */
	boolean isClosed();

	/**
	 * Returns the epoch time of the historical data as a <code>long</code>, equivalent to
	 * {@link HistoricalData#getEpochTime()} but raw <code>long</code> is used for performance reasons.
	 */
	long getEpochTime();

	/**
	 * Returns the BLOB of the historical data as a <code>byte[]</code>, equivalent to {@link HistoricalData#getBlob()}
	 * but a raw <code>byte[]</code> is used for performance reasons.
	 */
	byte[] getBlob();

	/**
	 * Returns the historical data at the current position. Note that this may or may not allocate an instance on demand,
	 * which may be a minor but unneccessary performance hit for your use case. See also {@link #getEpochTime()} and {@link #getBlob()} to directly
	 * access the underlying data without boxing.
	 */
	HistoricalData getHistoricalData();
}

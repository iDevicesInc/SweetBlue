package com.idevicesinc.sweetblue.utils;


import com.idevicesinc.sweetblue.annotations.Extendable;

/**
 * A zero-dependency utility class to track running average time of operations
 * and estimate the completion time of long running operations like firmware updates.
 * Basically takes times as doubles representing seconds, and spits back the same.
 */
@Extendable
public class TimeEstimator
{
	private final int m_totalSteps;
	private final double m_estimatedTimePerStep;
	
	private int m_progress = 0;
	private double m_timeElapsed = 0.0;
	private double m_timeRemaining = 0.0;
	private double m_runningAverage = 0.0;
	
	private final double[] m_times;
	
	/**
	 * Lets this class act as a "time remaining" estimator for long-running operations.
	 */
	public TimeEstimator(int totalSteps, double estimatedTimePerStep, int runningAverageN)
	{
		m_totalSteps = totalSteps;
		m_estimatedTimePerStep = estimatedTimePerStep;
		
		m_times = new double[runningAverageN];
		
		pushTimeStep(0.0);
	}
	
	/**
	 * Lets this class act as a utility for calculating the running average completion time of arbitrary operations.
	 */
	public TimeEstimator(int runningAverageN)
	{
		this(0, 0.0, runningAverageN);
	}
	
	private void pushTimeStep(double timeStep)
	{
		if( m_progress <= m_times.length )
		{
			int index = m_progress-1;
			
			if( index >= 0 )
			{
				m_times[m_progress-1] = timeStep;
				
				double total = 0.0;
				for( int i = 0; i < m_progress; i++ )
				{
					total += m_times[i];
				}
				
				m_runningAverage = m_progress == 0 ? 0.0 : total / ((double)m_progress);
				updateTimeRemaining();
			}
			else
			{
				m_timeRemaining = ((double)m_totalSteps) * m_estimatedTimePerStep;
			}
		}
		else
		{
			double currentTime = timeStep;
			double total = 0.0;
			
			for( int i = m_times.length-1; i >= 0; i-- )
			{
				total += currentTime;
				
				double temp = m_times[i];
				m_times[i] = currentTime;
				currentTime = temp;
			}
			
			m_runningAverage = m_times.length == 0 ? 0.0 : total / m_times.length;
			updateTimeRemaining();
		}
	}
	
	private void updateTimeRemaining()
	{
		double timeRemaining = m_runningAverage * ((double)getStepsRemaining());
		
		if( timeRemaining <= m_timeRemaining || m_timeRemaining == 0.0 )
		{
			m_timeRemaining = timeRemaining;
		}
		else if( timeRemaining > m_timeRemaining )
		{
			if( (timeRemaining - m_timeRemaining) > 60 * 5 )
			{
				m_timeRemaining = timeRemaining;
			}
		}
	}
	
	/**
	 * Adds the time it took for a just-completed operation to finish
	 * and updates the running average time.
	 */
	public void addTime(double timeStep)
	{
		m_timeElapsed+=timeStep;
		m_progress++;
		
		pushTimeStep(timeStep);
	}
	
	public double getTimeElapsed()
	{
		return m_timeElapsed;
	}
	
	public double getTimeRemaining()
	{
		return m_timeRemaining;
	}
	
	public int getStepsCompleted()
	{
		return m_progress;
	}
	
	public int getStepsRemaining()
	{
		return m_totalSteps - m_progress;
	}
	
	public double getRunningAverage()
	{
		return m_runningAverage;
	}
	
	public int getRunningAverageN()
	{
		return m_times.length;
	}
	
	public double getTotalAverage()
	{
		return m_progress == 0 ? 0.0 : m_timeElapsed / m_progress;
	}
}

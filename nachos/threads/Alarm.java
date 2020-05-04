package nachos.threads;

import java.util.*;
import nachos.machine.*;

/**
 * Uses the hardware timer to provide preemption, and to allow threads to sleep
 * until a certain time.
 */
public class Alarm {
	/**
	 * Allocate a new Alarm. Set the machine's timer interrupt handler to this
	 * alarm's callback.
	 * 
	 * <p>
	 * <b>Note</b>: Nachos will not function correctly with more than one alarm.
	 */
	public Alarm() {
		waitingQueue = new LinkedList<>();
		waitingTimeMap = new HashMap<>();
		Machine.timer().setInterruptHandler(new Runnable() {
			public void run() {
				timerInterrupt();
			}
		});
	}

	/**
	 * The timer interrupt handler. This is called by the machine's timer
	 * periodically (approximately every 500 clock ticks). Causes the current
	 * thread to yield, forcing a context switch if there is another thread that
	 * should be run.
	 */
	public void timerInterrupt() {
		boolean disableInterruptResult = Machine.interrupt().disable();
		long curTime = Machine.timer().getTime();
		while (!waitingQueue.isEmpty() && waitingTimeMap.get(waitingQueue.peek()) <= curTime) {
			KThread unblockedThread = waitingQueue.poll();
			unblockedThread.ready();
		}
		Machine.interrupt().restore(disableInterruptResult);
	}

	/**
	 * Put the current thread to sleep for at least <i>x</i> ticks, waking it up
	 * in the timer interrupt handler. The thread must be woken up (placed in
	 * the scheduler ready set) during the first timer interrupt where
	 * 
	 * <p>
	 * <blockquote> (current time) >= (WaitUntil called time)+(x) </blockquote>
	 * 
	 * @param x the minimum number of clock ticks to wait.
	 * 
	 * @see nachos.machine.Timer#getTime()
	 */
	public void waitUntil(long x) {
		// the thread must wait for at least X times
		// for now, cheat just to get something working (busy waiting is bad)
		// yield: give up the current CPU, put it on the ready queue, will not block!
		// TODO: use KThread.sleep() to block, use KThread.ready() to unblock  it
		// TODO: disable interrupt to deal with mutual exclusion
		// TODO: implement your own waiting queue
		long wakeTime = Machine.timer().getTime() + x;
		boolean disableInterruptResult = Machine.interrupt().disable();
		waitingQueue.offer(KThread.currentThread());
		waitingTimeMap.put(KThread.currentThread(), wakeTime);
		KThread.sleep();		// block the current thread
		Machine.interrupt().restore(disableInterruptResult);		// restore the interrupt
	}

        /**
	 * Cancel any timer set by <i>thread</i>, effectively waking
	 * up the thread immediately (placing it in the scheduler
	 * ready set) and returning true.  If <i>thread</i> has no
	 * timer set, return false.
	 * 
	 * <p>
	 * @param thread the thread whose timer should be cancelled.
	 */
	public boolean cancel(KThread thread) {
		return false;
	}

	private Queue<KThread> waitingQueue;
	private Map<KThread, Long> waitingTimeMap;
}

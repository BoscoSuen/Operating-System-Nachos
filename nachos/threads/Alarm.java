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
		waitingPq = new PriorityQueue<>((a,b) -> ((int)(a.wakeUpTime - b.wakeUpTime)));
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
		while (!waitingPq.isEmpty() && waitingPq.peek().getWakeUpTime() <= curTime) {
			KThread unblockedThread = waitingPq.poll().getThread();
			if (unblockedThread.isBlocked()) {
				unblockedThread.ready();
			}
		}
		Machine.interrupt().restore(disableInterruptResult);
		KThread.yield();		// forcing a context switch
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
		// for now, cheat just to get something working (busy waiting is bad)
		// TODO: use KThread.sleep() to block, use KThread.ready() to unblock  it
		// TODO: disable interrupt to deal with mutual exclusion
		// TODO: implement your own waiting queue
		long wakeTime = Machine.timer().getTime() + x;
		boolean disableInterruptResult = Machine.interrupt().disable();
		if (x <= 0) {
			Machine.interrupt().restore(disableInterruptResult);
			return;
		}
		waitingPq.offer(new WaitingThread(wakeTime, KThread.currentThread()));
		KThread.sleep();											// block the current thread
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
		boolean disableInterruptResult = Machine.interrupt().disable();
		for (WaitingThread waitingThread : waitingPq) {
			if (waitingThread.getThread() == thread) {
				KThread curThread = waitingThread.getThread();
				waitingPq.remove(waitingThread);
				curThread.ready();
				Machine.interrupt().restore(disableInterruptResult);
				return true;
			}
		}
		Machine.interrupt().restore(disableInterruptResult);
		return false;
	}


	public class WaitingThread {
		public WaitingThread(long wakeUpTime, KThread thread) {
			this.wakeUpTime = wakeUpTime;
			this.thread = thread;
		}

		public KThread getThread() { return this.thread; }
		public long getWakeUpTime() { return this.wakeUpTime; }

		private long wakeUpTime;
		private KThread thread;
	}

	private PriorityQueue<WaitingThread> waitingPq;

	/**
	 * Self Test for Alarm class
	 */

	public static class AlarmRun implements Runnable {
		public AlarmRun(int threadNum, int waitTime) {
			this.threadNum = threadNum;
			this.waitTime = waitTime;
		}

		public void run() {
			System.out.println("The thread " + threadNum + "block at " + Machine.timer().getTime());
			ThreadedKernel.alarm.waitUntil(waitTime);
			System.out.println("The thread " + threadNum + "unblock at " + Machine.timer().getTime());
		}
		private int threadNum;
		private int waitTime;
	}

	public static void selfTest() {
		alarmTest1();
		alarmTest2();
		System.out.println("\nFinish Alarm Self Test\n");
	}

	public static void alarmTest1() {
		System.out.println("Alarm Test1: base test");
		int durations[] = {1000, 10*1000, 100*1000};
		long t0, t1;
		for (int d : durations) {
			t0 = Machine.timer().getTime();
			ThreadedKernel.alarm.waitUntil(d);
			t1 = Machine.timer().getTime();
			System.out.println ("alarmTest1: waited for " + (t1 - t0) + " ticks");
		}
	}

	public static void alarmTest2() {
		System.out.println("\nAlarm Test2:");
		KThread kThread1 = new KThread(new AlarmRun(1, 500));
		kThread1.fork();
		KThread kThread2 = new KThread(new AlarmRun(2,1000));
		kThread2.fork();
		kThread1.join();
		kThread2.join();
		AlarmRun wait = new AlarmRun(0, 5000);
		wait.run();
	}

}

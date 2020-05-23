package nachos.threads;

import nachos.machine.*;


import java.util.LinkedList;
import java.util.Queue;

/**
 * An implementation of condition variables that disables interrupt()s for
 * synchronization.
 * 
 * <p>
 * You must implement this.
 * 
 * @see nachos.threads.Condition
 */
public class Condition2 {
	/**
	 * Allocate a new condition variable.
	 * 
	 * @param conditionLock the lock associated with this condition variable.
	 * The current thread must hold this lock whenever it uses <tt>sleep()</tt>,
	 * <tt>wake()</tt>, or <tt>wakeAll()</tt>.
	 */
	public Condition2(Lock conditionLock) {
		this.conditionLock = conditionLock;
	}

	/**
	 * Atomically release the associated lock and go to sleep on this condition
	 * variable until another thread wakes it using <tt>wake()</tt>. The current
	 * thread must hold the associated lock. The thread will automatically
	 * reacquire the lock before <tt>sleep()</tt> returns.
	 */
	public void sleep() {
		Lib.assertTrue(conditionLock.isHeldByCurrentThread());

		// add this thread into waiting queue of this CV
		waitQueue.offer(KThread.currentThread());

		// disable interrupt to make atomic operation
		boolean intStatus = Machine.interrupt().disable();
		conditionLock.release();
		// sleep this thread
		KThread.sleep();
		Machine.interrupt().restore(intStatus);
		conditionLock.acquire();
	}

	/**
	 * Wake up at most one thread sleeping on this condition variable. The
	 * current thread must hold the associated lock.
	 */
	public void wake() {
		Lib.assertTrue(conditionLock.isHeldByCurrentThread());
		boolean intStatus = Machine.interrupt().disable();
		while ((threadTobeWake = waitQueue.poll()) != null) {
			if (threadTobeWake.isBlocked()) {
				if (!ThreadedKernel.alarm.cancel(threadTobeWake)) {
					threadTobeWake.ready();
				}
				break;
			} else {
				System.out.println("Current thread is already unblocked!");
			}
		}
		Machine.interrupt().restore(intStatus);
	}

	/**
	 * Wake up all threads sleeping on this condition variable. The current
	 * thread must hold the associated lock.
	 */
	public void wakeAll() {
		Lib.assertTrue(conditionLock.isHeldByCurrentThread());
		boolean intStatus = Machine.interrupt().disable();
		while ((threadTobeWake = waitQueue.poll()) != null) {
			if (threadTobeWake.isBlocked() && !ThreadedKernel.alarm.cancel(threadTobeWake)) {
				threadTobeWake.ready();
			}
		}
		Machine.interrupt().restore(intStatus);
	}

	/**
	 * Atomically release the associated lock and go to sleep on
	 * this condition variable until either (1) another thread
	 * wakes it using <tt>wake()</tt>, or (2) the specified
	 * <i>timeout</i> elapses.  The current thread must hold the
	 * associated lock.  The thread will automatically reacquire
	 * the lock before <tt>sleep()</tt> returns.
	 */
	public void sleepFor(long timeout) {
		Lib.assertTrue(conditionLock.isHeldByCurrentThread());
		waitQueue.offer(KThread.currentThread());

		boolean intStatus = Machine.interrupt().disable();
		conditionLock.release();
		ThreadedKernel.alarm.waitUntil(timeout);
		waitQueue.remove(KThread.currentThread());
		Machine.interrupt().restore(intStatus);

		conditionLock.acquire();
	}

	private Lock conditionLock;
	private Queue<KThread> waitQueue = new LinkedList<>();
	private KThread threadTobeWake;

	/** testing
	 * Example of the "interlock" pattern where two threads strictly
	 * alternate their execution with each other using a condition
	 * variable.  (Also see the slide showing this pattern at the end
	 * of Lecture 6.)
	 */
	private static class InterlockTest {
		private static Lock lock;
		private static Condition2 cv;

		private static class Interlocker implements Runnable {
			public void run () {
				lock.acquire();
				for (int i = 0; i < 10; i++) {
					System.out.println(KThread.currentThread().getName());
					cv.wake();   // signal
					cv.sleep();  // wait
				}
				lock.release();
			}
		}

		public InterlockTest () {
			lock = new Lock();
			cv = new Condition2(lock);

			KThread ping = new KThread(new Interlocker());
			ping.setName("ping");
			KThread pong = new KThread(new Interlocker());
			pong.setName("pong");

			ping.fork();
			pong.fork();

			// We need to wait for ping to finish, and the proper way
			// to do so is to join on ping.  (Note that, when ping is
			// done, pong is sleeping on the condition variable; if we
			// were also to join on pong, we would block forever.)
			// For this to work, join must be implemented.  If you
			// have not implemented join yet, then comment out the
			// call to join and instead uncomment the loop with
			// yields; the loop has the same effect, but is a kludgy
			// way to do it.
//			ping.join();
			 for (int i = 0; i < 50; i++) { KThread.currentThread().yield(); }
		}
	}

	public static class SleepForTest {
		private static Lock lock;
		private static Condition2 cv;

		public static class sleepForTester implements Runnable {
			@Override
			public void run() {
				lock.acquire();
				for (int i = 0; i < 10; i++) {
					System.out.println(KThread.currentThread().getName());
					cv.wake();   // signal
//					System.out.println("sleep for current time" + Machine.timer().getTime());
					cv.sleepFor(i * 100);  // wait
				}
				lock.release();
			}
		}

		public SleepForTest() {
			lock = new Lock();
			cv = new Condition2(lock);
			KThread ping = new KThread(new SleepForTest.sleepForTester());
			ping.setName("ping");
			KThread pong = new KThread(new SleepForTest.sleepForTester());
			pong.setName("pong");

			ping.fork();
			pong.fork();
			for (int i = 0; i < 50; i++) { KThread.currentThread().yield(); }
		}

	}

	public static class WakeAllTest implements Runnable {
		private static Lock lock;
		private static Condition2 cv;

//		public static class WakeAllTester implements Runnable {
		@Override
		public void run() {
			lock.acquire();
			for (int i = 0; i < 10; i++) {
//					System.out.println("Wakeall Method Will Come In");
//					ThreadedKernel.alarm.cancel(KThread.currentThread());
				cv.wakeAll();   // signal
				cv.sleepFor(i * 200);  // wait
			}
			lock.release();
		}
//		}
		public WakeAllTest() {
			lock = new Lock();
			cv = new Condition2(lock);
			KThread ping = new KThread(new SleepForTest.sleepForTester());
			ping.setName("ping");
			KThread pong = new KThread(new SleepForTest.sleepForTester());
			pong.setName("pong");
//			KThread tester = new KThread(new WakeAllTester());
//			tester.setName("tester");

			ping.fork();
			pong.fork();
//			tester.fork();
			for (int i = 0; i < 50; i++) { KThread.currentThread().yield(); }
		}
	}

	public static class DuplicateReadyTest {
		private static Condition2 cv;
		private Lock lock;

		private class DuplicateReadyTestRun implements Runnable {
			@Override
			public void run() {
				lock.acquire();
				KThread curThread = KThread.currentThread();
				cv.sleepFor(1000);
				ThreadedKernel.alarm.cancel(curThread);
				cv.wake();
				lock.release();
			}
		}

		public DuplicateReadyTest() {
			lock = new Lock();
			cv = new Condition2(lock);
			KThread T1 = new KThread(new DuplicateReadyTestRun());
			KThread T2 = new KThread(new DuplicateReadyTestRun());
			T1.fork();
			T2.fork();
			T1.join();
			T2.join();
		}
	}

	// test sleep() and wake()
	public static class ProducerConsumer1 {
		private static Lock lock = new Lock();
		private static Condition2 cvProducer = new Condition2(lock);
		private static Condition2 cvConsumer = new Condition2(lock);

		private static int producerInd; // current ind produced
		private static int consumerInd; // current ind consumed
		private static boolean[] buffer;

		public ProducerConsumer1(int totalSize) {
			producerInd = 0;
			consumerInd = 0;
			buffer = new boolean[totalSize];

			KThread[] producers = new KThread[20];
			for (int i = 0; i < producers.length; i++) {
				producers[i] = new KThread(new Producer1());
				producers[i].setName("producer" + i);
			}

			KThread[] consumers = new KThread[20];
			for (int i = 0; i < consumers.length; i++) {
				consumers[i] = new KThread(new Consumer1());
				consumers[i].setName("consumer" + i);
			}
			for (int i = 0; i < 20; i++) {
				producers[i].fork();
				consumers[i].fork();
			}
			consumers[0].join();
		}

		public static class Producer1 implements Runnable {
			@Override
			public void run() {
				for (int i = 0; i < 20; i++) {
					Lib.assertTrue(producerInd >= consumerInd);
					lock.acquire();

					while (producerInd - consumerInd == buffer.length) {
						cvProducer.sleep();
					}
					Lib.assertTrue(buffer[producerInd % buffer.length] == false);
					buffer[producerInd % buffer.length] = true;
					System.out.println(KThread.currentThread().getName() + " Produce on buffer " + producerInd % buffer.length);
					producerInd++;
					if (producerInd > consumerInd) {
						cvConsumer.wake();
					}
					lock.release();
				}
			}
		}

		public static class Consumer1 implements Runnable {
			@Override
			public void run() {
				for (int i = 0; i < 20; i++) {
					Lib.assertTrue(producerInd >= consumerInd);
					lock.acquire();

					while (producerInd == consumerInd) {
						cvConsumer.sleep();
					}
					Lib.assertTrue(buffer[consumerInd % buffer.length] == true);
					buffer[consumerInd % buffer.length] = false;
					System.out.println(KThread.currentThread().getName() + " Consume on buffer " + consumerInd % buffer.length);
					consumerInd++;
					if (producerInd - consumerInd < buffer.length) {
						cvProducer.wake();
					}
					lock.release();

				}
			}
		}

	}

	// test sleep() and wakeAll()
	public static class ProducerConsumer2 {
		private static Lock lock = new Lock();
		private static Condition2 cvProducer = new Condition2(lock);
		private static Condition2 cvConsumer = new Condition2(lock);

		private static int producerInd; // current ind produced
		private static int consumerInd; // current ind consumed
		private static boolean[] buffer;

		public ProducerConsumer2(int totalSize) {
			producerInd = 0;
			consumerInd = 0;
			buffer = new boolean[totalSize];

			KThread[] producers = new KThread[20];
			for (int i = 0; i < producers.length; i++) {
				producers[i] = new KThread(new Producer2());
				producers[i].setName("producer" + i);
			}

			KThread[] consumers = new KThread[20];
			for (int i = 0; i < consumers.length; i++) {
				consumers[i] = new KThread(new Consumer2());
				consumers[i].setName("consumer" + i);
			}
			for (int i = 0; i < 20; i++) {
				producers[i].fork();
				consumers[i].fork();
			}
			consumers[0].join();
		}

		public static class Producer2 implements Runnable {
			@Override
			public void run() {
				for (int i = 0; i < 20; i++) {
					Lib.assertTrue(producerInd >= consumerInd);
					lock.acquire();

					while (producerInd - consumerInd == buffer.length) {
						cvProducer.sleep();
					}
					Lib.assertTrue(buffer[producerInd % buffer.length] == false);
					buffer[producerInd % buffer.length] = true;
					System.out.println(KThread.currentThread().getName() + " Produce on buffer " + producerInd % buffer.length);
					producerInd++;
					if (producerInd > consumerInd) {
						cvConsumer.wakeAll();
					}
					lock.release();
				}
			}
		}

		public static class Consumer2 implements Runnable {
			@Override
			public void run() {
				for (int i = 0; i < 20; i++) {
					Lib.assertTrue(producerInd >= consumerInd);
					lock.acquire();

					while (producerInd == consumerInd) {
						cvConsumer.sleep();
					}
					Lib.assertTrue(buffer[consumerInd % buffer.length] == true);
					buffer[consumerInd % buffer.length] = false;
					System.out.println(KThread.currentThread().getName() + " Consume on buffer " + consumerInd % buffer.length);
					consumerInd++;
					if (producerInd - consumerInd < buffer.length) {
						cvProducer.wakeAll();
					}
					lock.release();
				}
			}
		}
	}

	// test sleepFor() and wake()
	public static class ProducerConsumer3 {
		private static Lock lock = new Lock();
		private static Condition2 cvProducer = new Condition2(lock);
		private static Condition2 cvConsumer = new Condition2(lock);

		private static int producerInd; // current ind produced
		private static int consumerInd; // current ind consumed
		private static boolean[] buffer;

		public ProducerConsumer3(int totalSize) {
			producerInd = 0;
			consumerInd = 0;
			buffer = new boolean[totalSize];

			KThread[] producers = new KThread[20];
			for (int i = 0; i < producers.length; i++) {
				producers[i] = new KThread(new Producer3());
				producers[i].setName("producer" + i);
			}

			KThread[] consumers = new KThread[20];
			for (int i = 0; i < consumers.length; i++) {
				consumers[i] = new KThread(new Consumer3());
				consumers[i].setName("consumer" + i);
			}
			for (int i = 0; i < 20; i++) {
				producers[i].fork();
				consumers[i].fork();
			}
			consumers[0].join();
		}

		public static class Producer3 implements Runnable {
			@Override
			public void run() {
				for (int i = 0; i < 20; i++) {

					Lib.assertTrue(producerInd >= consumerInd);
					lock.acquire();
					while (producerInd - consumerInd == buffer.length) {
						cvProducer.sleepFor(i * 100);
					}
					Lib.assertTrue(buffer[producerInd % buffer.length] == false);
					buffer[producerInd % buffer.length] = true;
					System.out.println(KThread.currentThread().getName() + " Produce on buffer " + producerInd % buffer.length);
					producerInd++;
					if (producerInd > consumerInd) {
						cvConsumer.wake();
					}

					lock.release();
				}
			}
		}

		public static class Consumer3 implements Runnable {
			@Override
			public void run() {
				for (int i = 0; i < 20; i++) {
					Lib.assertTrue(producerInd >= consumerInd);
					lock.acquire();

					while (producerInd == consumerInd) {
						cvConsumer.sleepFor(i * 100);
					}
					Lib.assertTrue(buffer[consumerInd % buffer.length] == true);
					buffer[consumerInd % buffer.length] = false;
					System.out.println(KThread.currentThread().getName() + " Consume on buffer " + consumerInd % buffer.length);
					consumerInd++;
					if (producerInd - consumerInd < buffer.length) {
						cvProducer.wake();
					}
					lock.release();

				}
			}
		}

	}

	// test sleepFor() and wakeAll()
	public static class ProducerConsumer4 {
		private static Lock lock = new Lock();
		private static Condition2 cvProducer = new Condition2(lock);
		private static Condition2 cvConsumer = new Condition2(lock);

		private static int producerInd; // current ind produced
		private static int consumerInd; // current ind consumed
		private static boolean[] buffer;

		public ProducerConsumer4(int totalSize) {
			producerInd = 0;
			consumerInd = 0;
			buffer = new boolean[totalSize];

			KThread[] producers = new KThread[20];
			for (int i = 0; i < producers.length; i++) {
				producers[i] = new KThread(new Producer4());
				producers[i].setName("producer" + i);
			}

			KThread[] consumers = new KThread[20];
			for (int i = 0; i < consumers.length; i++) {
				consumers[i] = new KThread(new Consumer4());
				consumers[i].setName("consumer" + i);
			}
			for (int i = 0; i < 20; i++) {
				producers[i].fork();
				consumers[i].fork();
			}
			consumers[0].join();
		}

		public static class Producer4 implements Runnable {
			@Override
			public void run() {
				for (int i = 0; i < 20; i++) {
					Lib.assertTrue(producerInd >= consumerInd);
					lock.acquire();

					while (producerInd - consumerInd == buffer.length) {
						cvProducer.sleepFor(i * 100);
					}
					Lib.assertTrue(buffer[producerInd % buffer.length] == false);
					buffer[producerInd % buffer.length] = true;
					System.out.println(KThread.currentThread().getName() + " Produce on buffer " + producerInd % buffer.length);
					producerInd++;
					if (producerInd > consumerInd) {
						cvConsumer.wakeAll();
					}
					lock.release();
				}
			}
		}

		public static class Consumer4 implements Runnable {
			@Override
			public void run() {
				for (int i = 0; i < 20; i++) {
					Lib.assertTrue(producerInd >= consumerInd);
					lock.acquire();

					while (producerInd == consumerInd) {
						cvConsumer.sleepFor(i * 100);
					}
					Lib.assertTrue(buffer[consumerInd % buffer.length] == true);
					buffer[consumerInd % buffer.length] = false;
					System.out.println(KThread.currentThread().getName() + " Consume on buffer " + consumerInd % buffer.length);
					consumerInd++;
					if (producerInd - consumerInd < buffer.length) {
						cvProducer.wakeAll();
					}
					lock.release();

				}
			}
		}

	}

	// Invoke Condition2.selfTest() from ThreadedKernel.selfTest()

	public static void selfTest() {
//		System.out.println("Begin Condition2 InterlockTest test");
//		new InterlockTest();
//		System.out.println("Finish Condition2 InterlockTest test");
//
//		System.out.println("Begin Condition2 SleepFor Test");
//		new SleepForTest();
//		System.out.println("Finish Condition2 SleepFor Test\n");
//
//		System.out.println("Begin Condition2 WakeAll Test");
//		new WakeAllTest();
//		System.out.println("Finish Condition2 WakeAll Test");

		System.out.println("Begin Producer-Consumer Test1 with Condition2");
		new ProducerConsumer1(5);
		System.out.println("End Producer-Consumer Test1 with Condition2");

		System.out.println("Begin Producer-Consumer Test2 with Condition2");
		new ProducerConsumer2(5);
		System.out.println("End Producer-Consumer Test2 with Condition2");

		System.out.println("Begin Producer-Consumer Test3 with Condition2");
		new ProducerConsumer3(5);
		System.out.println("End Producer-Consumer Test3 with Condition2");

		System.out.println("Begin Producer-Consumer Test4 with Condition2");
		new ProducerConsumer4(5);
		System.out.println("End Producer-Consumer Test4 with Condition2");


		System.out.println("Begin Duplicate Wake Test");
		new DuplicateReadyTest();
		System.out.println("Finish Duplicate Wake Test");
	}
}

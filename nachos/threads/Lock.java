package nachos.threads;

import nachos.machine.*;

/**
 * A <tt>Lock</tt> is a synchronization primitive that has two states,
 * <i>busy</i> and <i>free</i>. There are only two operations allowed on a lock:
 * 
 * <ul>
 * <li><tt>acquire()</tt>: atomically wait until the lock is <i>free</i> and
 * then set it to <i>busy</i>.
 * <li><tt>release()</tt>: set the lock to be <i>free</i>, waking up one waiting
 * thread if possible.
 * </ul>
 * 
 * <p>
 * Also, only the thread that acquired a lock may release it. As with
 * semaphores, the API does not allow you to read the lock state (because the
 * value could change immediately after you read it).
 */
public class Lock {
	/**
	 * Allocate a new lock. The lock will initially be <i>free</i>.
	 */
	public Lock() {
	}

	private String name = " ";

	public Lock(String name){
		this.name = name;
	}

	public String getName(){
		return name;
	}

	/**
	 * Atomically acquire this lock. The current thread must not already hold
	 * this lock.
	 */
	public void acquire() {
		Lib.assertTrue(!isHeldByCurrentThread());

		boolean intStatus = Machine.interrupt().disable();
//		System.out.println("enter acquiring lock "+ this.getName() + " " + this.toString());
		KThread thread = KThread.currentThread();

		if (lockHolder != null) {
			waitQueue.waitForAccess(thread);
			KThread.sleep();
		}
		else {
			waitQueue.acquire(thread);
			lockHolder = thread;
		}

		Lib.assertTrue(lockHolder == thread);

		Machine.interrupt().restore(intStatus);
	}

	/**
	 * Atomically release this lock, allowing other threads to acquire it.
	 */
	public void release() {
		Lib.assertTrue(isHeldByCurrentThread());

		boolean intStatus = Machine.interrupt().disable();
//		System.out.println("enter releasing lock "+ this.getName() + " " + this.toString());

		if ((lockHolder = waitQueue.nextThread()) != null){
			lockHolder.ready();
//			System.out.println("relase lock to "+lockHolder.toString());
		}


		Machine.interrupt().restore(intStatus);
	}

	/**
	 * Test if the current thread holds this lock.
	 * 
	 * @return true if the current thread holds this lock.
	 */
	public boolean isHeldByCurrentThread() {
		return (lockHolder == KThread.currentThread());
	}

	private KThread lockHolder = null;

	private ThreadQueue waitQueue = ThreadedKernel.scheduler
			.newThreadQueue(true);
}

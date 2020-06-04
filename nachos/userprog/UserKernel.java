package nachos.userprog;

import nachos.machine.*;
import nachos.threads.*;
import nachos.userprog.*;

import java.util.LinkedList;
import java.util.List;

/**
 * A kernel that can support multiple user processes.
 */
public class UserKernel extends ThreadedKernel {
	/**
	 * Allocate a new user kernel.
	 */
	public UserKernel() {
		super();
	}

	/**
	 * Initialize this kernel. Creates a synchronized console and sets the
	 * processor's exception handler.
	 */
	public void initialize(String[] args) {
		super.initialize(args);

		console = new SynchConsole(Machine.console());

		freePhysicalPages = new LinkedList<>();

		lockOfFreePhysPageList = new Lock();

		Machine.processor().setExceptionHandler(new Runnable() {
			public void run() {
				exceptionHandler();
			}
		});

		// initialize free physical page list
		initialFreePhysicalPageList();
	}

	private void initialFreePhysicalPageList() {
		int numPhysPages = Machine.processor().getNumPhysPages();
		for (int i = 0; i < numPhysPages; i++) {
			freePhysicalPages.add(i);
		}
	}

	public static void increaseLiveProcess() {
//		liveProcessMutex.P();
		boolean status = Machine.interrupt().disable();
		liveProcessCount++;
		Machine.interrupt().restore(status);
//		liveProcessMutex.V();
	}

	public static void decreaseLiveProcess() {
//		liveProcessMutex.P();
		boolean status = Machine.interrupt().disable();
		liveProcessCount--;
		Machine.interrupt().restore(status);
//		liveProcessMutex.V();
	}

	public static int getLiveProcessCount() {
		return liveProcessCount;
	}

	/**
	 * Test the console device.
	 */
	public void selfTest() {
//		super.selfTest();
//
//		System.out.println("Testing the console device. Typed characters");
//		System.out.println("will be echoed until q is typed.");
//
//		char c;
//
//		do {
//			c = (char) console.readByte(true);
//			console.writeByte(c);
//		} while (c != 'q');
//
//		System.out.println("");
	}

	/**
	 * Returns the current process.
	 *
	 * @return the current process, or <tt>null</tt> if no process is current.
	 */
	public static UserProcess currentProcess() {
		if (!(KThread.currentThread() instanceof UThread))
			return null;

		return ((UThread) KThread.currentThread()).process;
	}

	/**
	 * The exception handler. This handler is called by the processor whenever a
	 * user instruction causes a processor exception.
	 *
	 * <p>
	 * When the exception handler is invoked, interrupts are enabled, and the
	 * processor's cause register contains an integer identifying the cause of
	 * the exception (see the <tt>exceptionZZZ</tt> constants in the
	 * <tt>Processor</tt> class). If the exception involves a bad virtual
	 * address (e.g. page fault, TLB miss, read-only, bus error, or address
	 * error), the processor's BadVAddr register identifies the virtual address
	 * that caused the exception.
	 */
	public void exceptionHandler() {
		Lib.assertTrue(KThread.currentThread() instanceof UThread);

		UserProcess process = ((UThread) KThread.currentThread()).process;
		int cause = Machine.processor().readRegister(Processor.regCause);
		process.handleException(cause);
	}

	/**
	 * Start running user programs, by creating a process and running a shell
	 * program in it. The name of the shell program it must run is returned by
	 * <tt>Machine.getShellProgramName()</tt>.
	 *
	 * @see nachos.machine.Machine#getShellProgramName
	 */
	public void run() {
		super.run();

		UserProcess process = UserProcess.newUserProcess();

		String shellProgram = Machine.getShellProgramName();
		if (!process.execute(shellProgram, new String[] {})) {
			System.out.println ("Could not find executable '" +
					shellProgram + "', trying '" +
					shellProgram + ".coff' instead.");
			shellProgram += ".coff";
			if (!process.execute(shellProgram, new String[] {})) {
				System.out.println ("Also could not find '" +
						shellProgram + "', aborting.");
//				Lib.assertTrue(false);
			}

		}

		KThread.currentThread().finish();
	}

	public static int getAFreePhysicalPage() {
		lockOfFreePhysPageList.acquire();

		int result = -1;
		if (freePhysicalPages.size() != 0) {
			result = freePhysicalPages.remove(0);
		}

		lockOfFreePhysPageList.release();
		return result;
	}

	public static void releasePhysicalPage(int ppn) {
		lockOfFreePhysPageList.acquire();

		freePhysicalPages.add(ppn);

		lockOfFreePhysPageList.release();
	}

	/**
	 * Terminate this kernel. Never returns.
	 */
	public void terminate() {
		super.terminate();
	}

	/** Globally accessible reference to the synchronized console. */
	public static SynchConsole console;

	// dummy variables to make javac smarter
	private static Coff dummy1 = null;

	// free physical pages
	private static List<Integer> freePhysicalPages;

	private static Lock lockOfFreePhysPageList;

//	private static Semaphore liveProcessMutex = new Semaphore(1);

//	/**	The lock to deal with pid .*/
//	public static Lock PIDLock = new Lock();

	/**	Use the counter to get a PID for process, which should equal to the number of process .*/
	public static int processCounter = 0;

	private static int liveProcessCount = 0;
}
package nachos.userprog;

import nachos.machine.*;
import nachos.threads.*;
import nachos.userprog.*;
import nachos.vm.*;

import java.io.EOFException;
import java.util.*;

/**
 * Encapsulates the state of a user process that is not contained in its user
 * thread (or threads). This includes its address translation state, a file
 * table, and information about the program being executed.
 *
 * <p>
 * This class is extended by other classes to support additional functionality
 * (such as additional syscalls).
 *
 * @see nachos.vm.VMProcess
 * @see nachos.network.NetProcess
 */
public class UserProcess {
	/**
	 * Allocate a new process.
	 */
	public UserProcess() {
//		int numPhysPages = Machine.processor().getNumPhysPages();
//		pageTable = new TranslationEntry[numPhysPages];
//		for (int i = 0; i < numPhysPages; i++)
//			pageTable[i] = new TranslationEntry(i, i, true, false, false, false);
		initialFDTable();

		// critical section
		PIDLock.acquire();

		PID = UserKernel.processCounter++;

		PIDLock.release();

		UserKernel.increaseLiveProcess();
	}

	/**
	 * Allocate and return a new process of the correct class. The class name is
	 * specified by the <tt>nachos.conf</tt> key
	 * <tt>Kernel.processClassName</tt>.
	 *
	 * @return a new process of the correct class.
	 */
	public static UserProcess newUserProcess() {
		String name = Machine.getProcessClassName ();

		// If Lib.constructObject is used, it quickly runs out
		// of file descriptors and throws an exception in
		// createClassLoader.  Hack around it by hard-coding
		// creating new processes of the appropriate type.

		if (name.equals ("nachos.userprog.UserProcess")) {
			return new UserProcess ();
		} else if (name.equals ("nachos.vm.VMProcess")) {
			return new VMProcess ();
		} else {
			return (UserProcess) Lib.constructObject(Machine.getProcessClassName());
		}
	}

	/**
	 * Execute the specified program with the specified arguments. Attempts to
	 * load the program, and then forks a thread to run it.
	 *
	 * @param name the name of the file containing the executable.
	 * @param args the arguments to pass to the executable.
	 * @return <tt>true</tt> if the program was successfully executed.
	 */
	public boolean execute(String name, String[] args) {
		if (!load(name, args)) {
			unloadSections();
			return false;
		}

		thread = new UThread(this);
		thread.setName(name).fork();

		return true;
	}

	/**
	 * Save the state of this process in preparation for a context switch.
	 * Called by <tt>UThread.saveState()</tt>.
	 */
	public void saveState() {
	}

	/**
	 * Restore the state of this process after a context switch. Called by
	 * <tt>UThread.restoreState()</tt>.
	 */
	public void restoreState() {
		Machine.processor().setPageTable(pageTable);
	}

	/**
	 * Read a null-terminated string from this process's virtual memory. Read at
	 * most <tt>maxLength + 1</tt> bytes from the specified address, search for
	 * the null terminator, and convert it to a <tt>java.lang.String</tt>,
	 * without including the null terminator. If no null terminator is found,
	 * returns <tt>null</tt>.
	 *
	 * @param vaddr the starting virtual address of the null-terminated string.
	 * @param maxLength the maximum number of characters in the string, not
	 * including the null terminator.
	 * @return the string read, or <tt>null</tt> if no null terminator was
	 * found.
	 */
	public String readVirtualMemoryString(int vaddr, int maxLength) {
		Lib.assertTrue(maxLength >= 0);

		byte[] bytes = new byte[maxLength + 1];

		int bytesRead = readVirtualMemory(vaddr, bytes);

		for (int length = 0; length < bytesRead; length++) {
			if (bytes[length] == 0)
				return new String(bytes, 0, length);
		}

		return null;
	}

	/**
	 * Transfer data from this process's virtual memory to all of the specified
	 * array. Same as <tt>readVirtualMemory(vaddr, data, 0, data.length)</tt>.
	 *
	 * @param vaddr the first byte of virtual memory to read.
	 * @param data the array where the data will be stored.
	 * @return the number of bytes successfully transferred.
	 */
	public int readVirtualMemory(int vaddr, byte[] data) {
		return readVirtualMemory(vaddr, data, 0, data.length);
	}

	/**
	 * Transfer data from this process's virtual memory to the specified array.
	 * This method handles address translation details. This method must
	 * <i>not</i> destroy the current process if an error occurs, but instead
	 * should return the number of bytes successfully copied (or zero if no data
	 * could be copied).
	 *
	 * @param vaddr the first byte of virtual memory to read.
	 * @param data the array where the data will be stored.
	 * @param offset the first byte to write in the array.
	 * @param length the number of bytes to transfer from virtual memory to the
	 * array.
	 * @return the number of bytes successfully transferred.
	 */
	public int readVirtualMemory(int vaddr, byte[] data, int offset, int length) {
		Lib.assertTrue(offset >= 0 && length >= 0
				&& offset + length <= data.length);

		byte[] memory = Machine.processor().getMemory();

		// for now, just assume that virtual addresses equal physical addresses
		if (vaddr < 0 || vaddr >= numPages * pageSize) {
			return 0;
		}
//		if (vaddr < 0 || vaddr >= memory.length || vaddr + length >= numPages * pageSize)
//			return 0;

		int currVaddr = vaddr;
		int totalAmount = 0;
		while (currVaddr < vaddr + length && currVaddr < numPages * pageSize) {
			int vpn = Processor.pageFromAddress(currVaddr);
			int ppn = pageTable[vpn].ppn;
			int addrOffset = Processor.offsetFromAddress(currVaddr);
			int paddr = pageSize * ppn + addrOffset;

			int nextPageVaddr = pageSize * (vpn + 1);
			int amount;
			if (nextPageVaddr < vaddr + length && nextPageVaddr < numPages * pageSize) {
				amount = nextPageVaddr - currVaddr;

			} else {
				amount = Math.min(vaddr + length, numPages * pageSize) - currVaddr;
//				amount = vaddr + length - currVaddr;
			}
			System.arraycopy(memory, paddr, data, offset, amount);
			currVaddr = nextPageVaddr;
			offset += amount;
			totalAmount += amount;
		}
//		int vpn = Processor.pageFromAddress(vaddr);
//		int addrOffset = Processor.offsetFromAddress(vaddr);
//		int ppn = pageTable[vpn].ppn;
//		int paddr = pageSize * ppn + addrOffset;
//
//		int amount = Math.min(length, memory.length - vaddr);
//		System.arraycopy(memory, vaddr, data, offset, amount);

		return totalAmount;
	}

	/**
	 * Transfer all data from the specified array to this process's virtual
	 * memory. Same as <tt>writeVirtualMemory(vaddr, data, 0, data.length)</tt>.
	 *
	 * @param vaddr the first byte of virtual memory to write.
	 * @param data the array containing the data to transfer.
	 * @return the number of bytes successfully transferred.
	 */
	public int writeVirtualMemory(int vaddr, byte[] data) {
		return writeVirtualMemory(vaddr, data, 0, data.length);
	}

	/**
	 * Transfer data from the specified array to this process's virtual memory.
	 * This method handles address translation details. This method must
	 * <i>not</i> destroy the current process if an error occurs, but instead
	 * should return the number of bytes successfully copied (or zero if no data
	 * could be copied).
	 *
	 * @param vaddr the first byte of virtual memory to write.
	 * @param data the array containing the data to transfer.
	 * @param offset the first byte to transfer from the array.
	 * @param length the number of bytes to transfer from the array to virtual
	 * memory.
	 * @return the number of bytes successfully transferred.
	 */
	public int writeVirtualMemory(int vaddr, byte[] data, int offset, int length) {
		Lib.assertTrue(offset >= 0 && length >= 0
				&& offset + length <= data.length);

		byte[] memory = Machine.processor().getMemory();

		// for now, just assume that virtual addresses equal physical addresses
//		if (vaddr < 0 || vaddr >= memory.length || vaddr + length >= numPages * pageSize)
//			return 0;
		if (vaddr < 0 || vaddr >= numPages * pageSize) {
			return 0;
		}

		int currVaddr = vaddr;
		int totalAmount = 0;
		while (currVaddr < vaddr + length && currVaddr < numPages * pageSize) {
			int vpn = Processor.pageFromAddress(currVaddr);
			int ppn = pageTable[vpn].ppn;
			int addrOffset = Processor.offsetFromAddress(currVaddr);
			int paddr = pageSize * ppn + addrOffset;

			int nextPageVaddr = pageSize * (vpn + 1);
			int amount;
			if (nextPageVaddr < vaddr + length && nextPageVaddr < numPages * pageSize) {
				amount = nextPageVaddr - currVaddr;

			} else {
				amount = Math.min(vaddr + length, numPages * pageSize) - currVaddr;
//				amount = vaddr + length - currVaddr;
			}
			System.arraycopy(data, offset, memory, paddr, amount);
			currVaddr = nextPageVaddr;
			offset += amount;
			totalAmount += amount;
		}

//		int amount = Math.min(length, memory.length - vaddr);
//		System.arraycopy(data, offset, memory, vaddr, amount);

		return totalAmount;
	}

	private void initialFDTable() {
		fileDescriptorTable = new OpenFile[maxFiles];
		fileDescriptorTable[0] = UserKernel.console.openForReading(); // stdin
		fileDescriptorTable[1] = UserKernel.console.openForWriting(); // stdout
	}

	/**
	 * Load the executable with the specified name into this process, and
	 * prepare to pass it the specified arguments. Opens the executable, reads
	 * its header information, and copies sections and arguments into this
	 * process's virtual memory.
	 *
	 * @param name the name of the file containing the executable.
	 * @param args the arguments to pass to the executable.
	 * @return <tt>true</tt> if the executable was successfully loaded.
	 */
	private boolean load(String name, String[] args) {
		Lib.debug(dbgProcess, "UserProcess.load(\"" + name + "\")");

		OpenFile executable = ThreadedKernel.fileSystem.open(name, false);
		if (executable == null) {
			Lib.debug(dbgProcess, "\topen failed");
			return false;
		}

		try {
			coff = new Coff(executable);
		}
		catch (EOFException e) {
			executable.close();
			Lib.debug(dbgProcess, "\tcoff load failed");
			return false;
		}

		// make sure the sections are contiguous and start at page 0
		numPages = 0;
		for (int s = 0; s < coff.getNumSections(); s++) {
			CoffSection section = coff.getSection(s);
			if (section.getFirstVPN() != numPages) {
				coff.close();
				Lib.debug(dbgProcess, "\tfragmented executable");
				return false;
			}
			numPages += section.getLength();
		}

		// make sure the argv array will fit in one page
		byte[][] argv = new byte[args.length][];
		int argsSize = 0;
		for (int i = 0; i < args.length; i++) {
			argv[i] = args[i].getBytes();
			// 4 bytes for argv[] pointer; then string plus one for null byte
			argsSize += 4 + argv[i].length + 1;
		}
		if (argsSize > pageSize) {
			coff.close();
			Lib.debug(dbgProcess, "\targuments too long");
			return false;
		}

		// program counter initially points at the program entry point
		initialPC = coff.getEntryPoint();

		// next comes the stack; stack pointer initially points to top of it
		numPages += stackPages;
		initialSP = numPages * pageSize;

		// and finally reserve 1 page for arguments
		numPages++;

		// initialize page table
		if (!initialPageTable()) { return false; }

		if (!loadSections())
			return false;

		// store arguments in last page
		int entryOffset = (numPages - 1) * pageSize;
		int stringOffset = entryOffset + args.length * 4;

		this.argc = args.length;
		this.argv = entryOffset;

		for (int i = 0; i < argv.length; i++) {
			byte[] stringOffsetBytes = Lib.bytesFromInt(stringOffset);
			Lib.assertTrue(writeVirtualMemory(entryOffset, stringOffsetBytes) == 4);
			entryOffset += 4;
			Lib.assertTrue(writeVirtualMemory(stringOffset, argv[i]) == argv[i].length);
			stringOffset += argv[i].length;
			Lib.assertTrue(writeVirtualMemory(stringOffset, new byte[] { 0 }) == 1);
			stringOffset += 1;
		}

		return true;
	}

	/**
	 * Initialize the page table, the length of page table is numPages
	 */
	private boolean initialPageTable() {
		pageTable = new TranslationEntry[numPages];
		for (int i = 0; i < numPages; i++) {
			int ppn = UserKernel.getAFreePhysicalPage();  // if ppn == -1, no enough physical pages to use: return false
			if (ppn == -1) { return false; }
			pageTable[i] = new TranslationEntry(i, ppn, true, false, false, false);
		}
		return true;
	}

	/**
	 * Allocates memory for this process, and loads the COFF sections into
	 * memory. If this returns successfully, the process will definitely be run
	 * (this is the last step in process initialization that can fail).
	 *
	 * @return <tt>true</tt> if the sections were successfully loaded.
	 */
	protected boolean loadSections() {
		if (numPages > Machine.processor().getNumPhysPages()) {
			coff.close();
			Lib.debug(dbgProcess, "\tinsufficient physical memory");
			return false;
		}

		// load sections
		for (int s = 0; s < coff.getNumSections(); s++) {
			CoffSection section = coff.getSection(s);

			Lib.debug(dbgProcess, "\tinitializing " + section.getName()
					+ " section (" + section.getLength() + " pages)");

			for (int i = 0; i < section.getLength(); i++) {
				int vpn = section.getFirstVPN() + i;
				// setup the page tale
				if (pageTable[vpn] == null) { return false; }
				pageTable[vpn].readOnly = section.isReadOnly();
				section.loadPage(i, pageTable[vpn].ppn);
			}
		}

		return true;
	}

	/**
	 * Release any resources allocated by <tt>loadSections()</tt>.
	 */
	protected void unloadSections() {
		for (int i = 0; i < pageTable.length; i++) {
			if (pageTable[i] != null) {
				UserKernel.releasePhysicalPage(pageTable[i].ppn);
				pageTable[i] = null;
			}
		}
	}

	/**
	 * Initialize the processor's registers in preparation for running the
	 * program loaded into this process. Set the PC register to point at the
	 * start function, set the stack pointer register to point at the top of the
	 * stack, set the A0 and A1 registers to argc and argv, respectively, and
	 * initialize all other registers to 0.
	 */
	public void initRegisters() {
		Processor processor = Machine.processor();

		// by default, everything's 0
		for (int i = 0; i < processor.numUserRegisters; i++)
			processor.writeRegister(i, 0);

		// initialize PC and SP according
		processor.writeRegister(Processor.regPC, initialPC);
		processor.writeRegister(Processor.regSP, initialSP);

		// initialize the first two argument registers to argc and argv
		processor.writeRegister(Processor.regA0, argc);
		processor.writeRegister(Processor.regA1, argv);
	}

	/**
	 * Handle the halt() system call.
	 */
	private int handleHalt() {

		Machine.halt();

		Lib.assertNotReached("Machine.halt() did not halt machine!");
		return 0;
	}

	/**
	 * Handle the exit() system call.
	 * close all files in the file table
	 * delete memory calling UnloadSections()
	 * close coff by coff.close()
	 * save status for parent
	 * wake up parent
	 * last process: kernel.kernel.terminate()
	 * close KThread
	 * handle abnormal exit
	 * @param status
	 * @return
	 */
	private int handleExit(int status) {
		// Do not remove this call to the autoGrader...
		Machine.autoGrader().finishingCurrentProcess(status);
		// ...and leave it as the top of handleExit so that we
		// can grade your implementation.

		Lib.debug(dbgProcess, "UserProcess.handleExit (" + status + ")");
		// close all files
		for (int i = 2; i < fileDescriptorTable.length; ++i) {
			if (fileDescriptorTable[i] != null) {
				handleClose(i);
			}
		}

		unloadSections();
		coff.close();
//		Any children of the process no longer have a parent process.
//		 parent is shared, critical section
		for (UserProcess child : childProcessLookUpMap.values()) {
			child.parent = null;
		}

		this.exitStatus = status;

		UserKernel.decreaseLiveProcess();

		if (UserKernel.getLiveProcessCount() == 0) {
			Kernel.kernel.terminate();
		}
		UThread.currentThread().finish();

//		// TODO: handle abnormal exit
//		if (status == Integer.MIN_VALUE) { return -1; }

		return 0;
	}

	private int handleCreate(int vaName) {
		String name = readVirtualMemoryString(vaName, maxParameterLength);
		if (name == null || name.length() > maxFileNameLength) { return -1; }
		int fd = openHelper(name, true);
		return fd;
	}

	private int handleOpen(int vaName) {
		String name = readVirtualMemoryString(vaName, maxParameterLength);
		if (name == null || name.length() > maxFileNameLength) { return -1; }
		int fd = openHelper(name, false);
		return fd;
	}

	private int openHelper(String filename, boolean create) {
		OpenFile openFile = UserKernel.fileSystem.open(filename, create);
		if (openFile != null) {
			for (int fd = 2; fd < fileDescriptorTable.length; fd++) {
				if (fileDescriptorTable[fd] == null) {
					fileDescriptorTable[fd] = openFile;
					return fd;
				}
			}
			return -1;  // FDT full
		} else {
			return -1; // file doesn't exist
		}
	}

	private int handleRead(int fd, int bufferPointer, int count) { // TODO: How to check bufferPointer
		if (!(fd >=0 && fd < fileDescriptorTable.length) || fileDescriptorTable[fd] == null || count < 0) { return -1; }

		byte[] localBuffer = new byte[count];
		int recv = fileDescriptorTable[fd].read(localBuffer, 0, count);
		if (recv == -1) { return -1; }

		int transfered = writeVirtualMemory(bufferPointer, localBuffer, 0, recv);
		if (transfered < recv) {
			return -1;
		} else {
			return recv;
		}
	}

	private int handleWrite(int fd, int bufferPointer, int count) {
		if (!(fd >=0 && fd < fileDescriptorTable.length) || fileDescriptorTable[fd] == null || count < 0) { return -1; }

		byte[] localBuffer = new byte[count];
		int transfered = readVirtualMemory(bufferPointer, localBuffer, 0, count);
		if (transfered < count) { return -1; }
		int send =  fileDescriptorTable[fd].write(localBuffer, 0, count);
		if (send < count) {
			return -1;
		} else {
			return send;
		}
	}

	private int handleClose(int fd) {
		if (!(fd >=0 && fd < fileDescriptorTable.length) || fileDescriptorTable[fd] == null) { return -1; }
		fileDescriptorTable[fd].close();
		fileDescriptorTable[fd] = null;
		return 0;
	}

	private int handleUnlink(int vaName) {
		String name = readVirtualMemoryString(vaName, maxParameterLength);
		if (name == null || name.length() > maxFileNameLength) { return -1; }
		boolean result = UserKernel.fileSystem.remove(name);
		return result ? 0:-1;
	}

	/**
	 * read coffName from virtual address
	 * read argvs from virtual address
	 * @param vaCoff
	 * @param argc
	 * @param argv
	 * @return the child process's process ID or -1
	 */
	private int handleExec(int vaCoff, int argc, int argv) {
		if (argc < 0) {
			Lib.debug(dbgProcess, "handleExec: argc = " + argc);
			Lib.debug(dbgProcess, "handleExec: argv = " + argv);
			return -1;
		}
		String coffName = readVirtualMemoryString(vaCoff, maxParameterLength);
		// this string must include the ".coff" extension
		if (coffName == null || coffName.length() == 0 || !coffName.endsWith(".coff")) {
			Lib.debug(dbgProcess, "handleExec: coffName wrong, coffName = " + coffName);
			return -1;
		}

		// get the arguments address
		// each pointer has 4 type, use readvirtualmemory() and track the current vaddr
		String[] argStrs = new String[argc];
		byte[] buffer = new byte[4];
		for (int i = 0; i < argc; ++i) {
			int readCount = readVirtualMemory(argv + i * 4, buffer);
			if (readCount < 4) return -1;

			int vaddr = Lib.bytesToInt(buffer, 0);
			argStrs[i] = readVirtualMemoryString(vaddr,maxParameterLength);
		}

		// create child process
		UserProcess childProcess = new UserProcess();
		childProcess.parent = this;
		childProcessLookUpMap.put(childProcess.PID, childProcess);
		if (childProcess.execute(coffName, argStrs)) {
			return childProcess.PID;
		}

		Lib.debug(dbgProcess, "handleExec: cannot execute child process.");

		return -1;
	}

	/**
	 * use the Kthread.join
	 * get child status and write to *status
	 * @param pid childPID, only a process's parent can join to it.
	 * @param vaStatus
	 * @return the status of join()
	 */
	public int handleJoin(int pid, int vaStatus) {
		if (!childProcessLookUpMap.containsKey(pid)) {
			Lib.debug(dbgProcess, "Doesn't have the child PId with PID = " + pid);
			return -1;
		}

		UserProcess child = childProcessLookUpMap.get(pid);
		child.thread.join();
		childProcessLookUpMap.remove(pid);

		// store the status to the *status
		byte[] buffer = new byte[4];
		// TODO: check exit status
		buffer = Lib.bytesFromInt(child.exitStatus);
		int writeCount = writeVirtualMemory(vaStatus, buffer);
//		return writeCount == 4 ? 1 : 0;
		return (child.exitStatus != Integer.MIN_VALUE && writeCount == 4) ? 1:0;
	}

	private static final int syscallHalt = 0, syscallExit = 1, syscallExec = 2,
			syscallJoin = 3, syscallCreate = 4, syscallOpen = 5,
			syscallRead = 6, syscallWrite = 7, syscallClose = 8,
			syscallUnlink = 9;

	/**
	 * Handle a syscall exception. Called by <tt>handleException()</tt>. The
	 * <i>syscall</i> argument identifies which syscall the user executed:
	 *
	 * <table>
	 * <tr>
	 * <td>syscall#</td>
	 * <td>syscall prototype</td>
	 * </tr>
	 * <tr>
	 * <td>0</td>
	 * <td><tt>void halt();</tt></td>
	 * </tr>
	 * <tr>
	 * <td>1</td>
	 * <td><tt>void exit(int status);</tt></td>
	 * </tr>
	 * <tr>
	 * <td>2</td>
	 * <td><tt>int  exec(char *name, int argc, char **argv);
	 * 								</tt></td>
	 * </tr>
	 * <tr>
	 * <td>3</td>
	 * <td><tt>int  join(int pid, int *status);</tt></td>
	 * </tr>
	 * <tr>
	 * <td>4</td>
	 * <td><tt>int  creat(char *name);</tt></td>
	 * </tr>
	 * <tr>
	 * <td>5</td>
	 * <td><tt>int  open(char *name);</tt></td>
	 * </tr>
	 * <tr>
	 * <td>6</td>
	 * <td><tt>int  read(int fd, char *buffer, int size);
	 * 								</tt></td>
	 * </tr>
	 * <tr>
	 * <td>7</td>
	 * <td><tt>int  write(int fd, char *buffer, int size);
	 * 								</tt></td>
	 * </tr>
	 * <tr>
	 * <td>8</td>
	 * <td><tt>int  close(int fd);</tt></td>
	 * </tr>
	 * <tr>
	 * <td>9</td>
	 * <td><tt>int  unlink(char *name);</tt></td>
	 * </tr>
	 * </table>
	 *
	 * @param syscall the syscall number.
	 * @param a0 the first syscall argument.
	 * @param a1 the second syscall argument.
	 * @param a2 the third syscall argument.
	 * @param a3 the fourth syscall argument.
	 * @return the value to be returned to the user.
	 */
	public int handleSyscall(int syscall, int a0, int a1, int a2, int a3) {
		switch (syscall) {
			case syscallHalt:
				return handleHalt();
			case syscallExit:
				return handleExit(a0);
			case syscallCreate:
				return handleCreate(a0);
			case syscallOpen:
				return handleOpen(a0);
			case syscallRead:
				return handleRead(a0, a1, a2);
			case syscallWrite:
				return handleWrite(a0, a1, a2);
			case syscallClose:
				return handleClose(a0);
			case syscallUnlink:
				return handleUnlink(a0);
			case syscallExec:
				return handleExec(a0, a1, a2);
			case syscallJoin:
				return handleJoin(a0, a1);
			default:
				Lib.debug(dbgProcess, "Unknown syscall " + syscall);
//				Lib.assertNotReached("Unknown system call!");
				return handleExit(Integer.MIN_VALUE);

		}
	}

	/**
	 * Handle a user exception. Called by <tt>UserKernel.exceptionHandler()</tt>
	 * . The <i>cause</i> argument identifies which exception occurred; see the
	 * <tt>Processor.exceptionZZZ</tt> constants.
	 *
	 * @param cause the user exception that occurred.
	 */
	public void handleException(int cause) {
		Processor processor = Machine.processor();

		switch (cause) {
			case Processor.exceptionSyscall:
				int result = handleSyscall(processor.readRegister(Processor.regV0),
						processor.readRegister(Processor.regA0),
						processor.readRegister(Processor.regA1),
						processor.readRegister(Processor.regA2),
						processor.readRegister(Processor.regA3));
				processor.writeRegister(Processor.regV0, result);
				processor.advancePC();
				break;

			default:
				handleExit(Integer.MIN_VALUE);
				return;
//				Lib.debug(dbgProcess, "Unexpected exception: "
//						+ Processor.exceptionNames[cause]);
//				Lib.assertNotReached("Unexpected exception");

		}
	}

	/** The program being run by this process. */
	protected Coff coff;

	/** This process's page table. */
	protected TranslationEntry[] pageTable;

	/** The number of contiguous pages occupied by the program. */
	protected int numPages;

	/** The number of pages in the program's stack. */
	protected final int stackPages = 8;

	/** The thread that executes the user-level program. */
	protected UThread thread;

	/** Parent process of child process .*/
	protected UserProcess parent = null;

	/** Current Process's PID .*/
	protected int PID;

	protected OpenFile[] fileDescriptorTable;

	/** The relationship between PID and process .*/
	private static Map<Integer, UserProcess> childProcessLookUpMap = new HashMap<>();

	/**	The lock to deal with pid .*/
	public static Lock PIDLock = new Lock();

	private int initialPC, initialSP;

	private int argc, argv;

	/** status of the process exited or not. */
	private int exitStatus;

	private static final int pageSize = Processor.pageSize;

	private static final int maxFiles = 16;

	private static final int maxParameterLength = 256;

	private static final int maxFileNameLength = 255;

	private static final char dbgProcess = 'a';
}

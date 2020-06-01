package nachos.userprog;

import nachos.machine.*;
import nachos.threads.*;
import nachos.userprog.*;
import nachos.vm.*;

import javax.jws.soap.SOAPBinding;
import java.io.EOFException;
import java.io.FileDescriptor;
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
		int numPhysPages = Machine.processor().getNumPhysPages();
		pageTable = new TranslationEntry[numPhysPages];
		for (int i = 0; i < numPhysPages; i++) {
			pageTable[i] = new TranslationEntry(i, i, true, false, false, false);
		}
		fileTable = new OpenFile[maxFiles];
		fileTable[0] = UserKernel.console.openForReading();	// stdin
		fileTable[1] = UserKernel.console.openForWriting(); // stdout

		// FIXME: critical section
		UserKernel.PIDLock.acquire();

		PID = UserKernel.getNextPID();

		UserKernel.PIDLock.release();

		lock = new Lock();
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
		if (!load(name, args))
			return false;

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
		// read length in memory: maxLength

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
	// copy data from memory starting from paddr, copy it to the data
	// offset: offset in data array
	public int readVirtualMemory(int vaddr, byte[] data, int offset, int length) {
		Lib.assertTrue(offset >= 0 && length >= 0
				&& offset + length <= data.length);

		byte[] memory = Machine.processor().getMemory();

		// TODO: for now, just assume that virtual addresses equal physical addresses
		if (vaddr < 0 || vaddr >= memory.length)
			return 0;
		// TODO: we need to get ppn from vpn using pageTable
		// TODO: deal with page boundary, add a pointer to track current vaddr read

		int count = 0;
		while (count < length) {
			int vpn = Machine.processor().pageFromAddress(vaddr + count);
			int pageOffset = Machine.processor().offsetFromAddress(vaddr + count);
			int ppn = pageTable[vpn].ppn;
			// FIXME
			if (ppn < 0 || ppn > Machine.processor().getNumPhysPages()) break;
			pageTable[vpn].used = true;
			int paddr = ppn * pageSize + pageOffset;
			int curPageLeft = pageSize - pageOffset;
			int amount = Math.min(curPageLeft, length - count);
			System.arraycopy(memory, paddr, data, offset + count, amount);
			// arrayCopy(source array, starting position of source array, destination array, starting position in des array, len)
			count += amount;
		}
		return count;
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
		if (vaddr < 0 || vaddr >= memory.length)
			return 0;

		int count = 0;
		while (count < length) {
			int vpn = Machine.processor().pageFromAddress(vaddr + count);
			int pageOffset = Machine.processor().offsetFromAddress(vaddr + count);
			pageTable[vpn].used = true;
			// FIXME
			if (pageTable[vpn].readOnly) return 0;
			pageTable[vpn].dirty = true;
			int ppn = pageTable[vpn].ppn;
			int paddr = ppn * pageSize + pageOffset;
			int amount = Math.min(pageSize - pageOffset, length - count);
			System.arraycopy(data, offset, memory, paddr, amount);
		}

		return count;
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
	 * Allocates memory for this process, and loads the COFF sections into
	 * memory. If this returns successfully, the process will definitely be run
	 * (this is the last step in process initialization that can fail).
	 * 
	 * @return <tt>true</tt> if the sections were successfully loaded.
	 */
	protected boolean loadSections() {
		UserKernel.sectionLock.acquire();

		if (numPages > Machine.processor().getNumPhysPages()) {
			coff.close();
			Lib.debug(dbgProcess, "\tinsufficient physical memory");
			UserKernel.sectionLock.release();
			return false;
		}

		// create pageTable
		// * critical section
		pageTable = new TranslationEntry[numPages];
		for (int i = 0; i < numPages; ++i) {
			if (UserKernel.freePageList.size() > 0) {
				int phyPage = UserKernel.freePageList.pollFirst();
				pageTable[i] = new TranslationEntry(i, phyPage, true, false, false, false);
			} else {
				Lib.debug(dbgProcess, "\tinsufficient free physical pages");
				return false;
			}
		}

		UserKernel.sectionLock.release();

		// load sections
		for (int s = 0; s < coff.getNumSections(); s++) {
			CoffSection section = coff.getSection(s);

			Lib.debug(dbgProcess, "\tinitializing " + section.getName()
					+ " section (" + section.getLength() + " pages)");

			for (int i = 0; i < section.getLength(); i++) {
				int vpn = section.getFirstVPN() + i;
				pageTable[vpn].readOnly = section.isReadOnly();
				// for now, just assume virtual addresses=physical addresses
				section.loadPage(i, pageTable[vpn].ppn);
			}
		}

		return true;
	}

	/**
	 * Release any resources allocated by <tt>loadSections()</tt>.
	 */
	protected void unloadSections() {
		// critical section
		UserKernel.sectionLock.release();
		for (int i = 0; i < numPages; ++i) {
			UserKernel.freePageList.offer(pageTable[i].ppn);
			pageTable[i] = null;
		}
		UserKernel.sectionLock.release();
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

	private int getFreeFileDescriptor() {
		for (int i  = 0; i < maxFiles; ++i) {
			if (fileTable[i] == null) return i;
		}
		return -1;
	}

	/**
	 * Following method handles the system calls.
	 */

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
		for (int i = 2; i < fileTable.length; ++i) {
			if (fileTable[i] != null) {
				handleClose(i);
			}
		}

		unloadSections();
//		Any children of the process no longer have a parent process.
//		 parent is shared, critical section
		for (UserProcess child : childProcessLookUpMap.values()) {
			child.parent = null;
		}

		this.exitStatus = status;

		if (PID == 0) {
			Kernel.kernel.terminate();
		} else {
			UThread.currentThread().finish();
		}

		// TODO: handle abnormal exit

		return 0;
	}

	/**
	 * Handle the creat(char *name) system call.
	 * Attempt to open the named disk file, creating it if it does not exist,
	 * we need to get the file name string from virtual address
	 * Notice that: maximum length for strings passed as arguments to system calls is 256 bytes (not including the terminating null).
	 * @param vaName
	 * @return a file descriptor referring to a stream.
	 */
	private int handleCreate(int vaName) {
		String fileName = readVirtualMemoryString(vaName, maxParameterLength);
		if (fileName == null || fileName.length() == 0) return -1;

		int fileDescriptor = getFreeFileDescriptor();
		if (fileDescriptor == -1) return -1;

		fileTable[fileDescriptor] = ThreadedKernel.fileSystem.open(fileName, true);
		return fileDescriptor;
	}

	/**
	 * Same logic as create, but do not create file
	 * If OpenFile == null, we need to return -1
	 * @param vaName
	 * @return fileDescriptor
	 */
	private int handleOpen(int vaName) {
		String fileName = readVirtualMemoryString(vaName, maxParameterLength);
		if (fileName == null) return -1;

		int fileDescriptor = getFreeFileDescriptor();
		if (fileDescriptor == -1) return -1;

		OpenFile openFile = ThreadedKernel.fileSystem.open(fileName, false);
		if (openFile == null) {
			Lib.debug(dbgProcess, "\topen failed");
			return -1;
		}
		fileTable[fileDescriptor] = openFile;
		return fileDescriptor;
	}

	private boolean validFileWithFileDescriptor(int fileDescriptor) {
		return fileDescriptor < 0 || fileDescriptor >= maxFiles || fileTable[fileDescriptor] == null;
	}

	/**
	 * read from file to a local buffer of limited size
	 * then write it into the user inputted buffer using writeVirtualMemory()
	 * @param fileDescriptor
	 * @param vaBuffer
	 * @param count number of bytes requested
	 * @return the number of bytes read
	 */
	private int handleRead(int fileDescriptor, int vaBuffer, int count) {
		if (!validFileWithFileDescriptor(fileDescriptor)) return -1;

		// read passes byte[] buffer, int offset and length
		byte[] buffer = new byte[count];
		int readResult = fileTable[fileDescriptor].read(buffer, 0, count);
		if (readResult <= 0) return 0;
		return writeVirtualMemory(vaBuffer, buffer);
	}


	/**
	 * read data from the user inputted buffer to a local buffer of limited size
	 * then use write() to write to file from the local buffer
	 * @param fileDescriptor
	 * @param vaBuffer
	 * @param count
	 * @return the number of bytes written
	 */
	private int handleWrite(int fileDescriptor, int vaBuffer, int count) {
		if (!validFileWithFileDescriptor(fileDescriptor)) return -1;

		byte[] buffer = new byte[count];
		int readResult = readVirtualMemory(vaBuffer, buffer);
		if (readResult <= 0) return 0;

		int writeResult = fileTable[fileDescriptor].write(buffer, 0, count);
//		It IS an error if this number is smaller than the number of bytes requested.
//		For disk files, this indicates that the disk is full.
		if (writeResult < count) {
			System.out.println("The disk is full");
			return -1;
		}
		return writeResult;
	}


	/**
	 * find the OpenFile associated with fd
	 * @param fileDescriptor
	 * @return Returns 0 on success
	 */
	private int handleClose(int fileDescriptor) {
		if (!validFileWithFileDescriptor(fileDescriptor)) return -1;

		fileTable[fileDescriptor].close();;
		fileTable[fileDescriptor] = null;
		return 0;
	}


	/**
	 * get the fileName and use remove()
	 * @param vaName
	 * @return Returns 0 on success
	 */
	private int handleUnlink(int vaName) {
		String fileName = readVirtualMemoryString(vaName, maxParameterLength);
		if (fileName == null) return 0;	// like already unlinked?
		return ThreadedKernel.fileSystem.remove(fileName) ? 0 : -1;
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
		if (argc < 1) return -1;
		String coffName = readVirtualMemoryString(vaCoff, maxParameterLength);
		// this string must include the ".coff" extension
		if (coffName == null || coffName.length() == 0 || !coffName.contains(".coff")) return -1;

		// get the arguments address
		// each pointer has 4 type, use readvirtualmemory() and track the current vaddr
		String[] argStrs = new String[argc];
		byte[] buffer = new byte[4];
		for (int i = 0; i < argc; ++i) {
			int readCount = readVirtualMemory(argv, buffer);
			argv += 4;
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
		buffer = Lib.bytesFromInt(child.exitStatus);
		int writeCount = writeVirtualMemory(vaStatus, buffer);
		return writeCount == 4 ? 1 : 0;
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
			return handleCreate(a0);	// vaddr
		case syscallOpen:
			return handleOpen(a0);		// vaddr
		case syscallRead:
			return handleRead(a0, a1, a2);	// int, vaddr, int
		case syscallWrite:
			return handleWrite(a0, a1, a2);	// int, vaddr, int
		case syscallClose:
			return handleClose(a0);		// int
		case syscallUnlink:
			return handleUnlink(a0);	// vaddr
		case syscallExec:
			return handleExec(a0, a1, a2);
		case syscallJoin:
			return handleJoin(a0, a1);


		default:
			Lib.debug(dbgProcess, "Unknown syscall " + syscall);
			Lib.assertNotReached("Unknown system call!");
		}
		return 0;
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
			Lib.debug(dbgProcess, "Unexpected exception: "
					+ Processor.exceptionNames[cause]);
			Lib.assertNotReached("Unexpected exception");
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

	/** OpenFile table for the process. */
	protected OpenFile[] fileTable;

	/** The relationship between PID and process .*/
	private static Map<Integer, UserProcess> childProcessLookUpMap = new HashMap<>();

	private static Lock lock;


	private int initialPC, initialSP;

	private int argc, argv;

	/** status of the process exited or not. */
	private int exitStatus;

	private static final int pageSize = Processor.pageSize;

	private static final int maxFiles = 16;

	private static final int maxParameterLength = 256;

	private static final char dbgProcess = 'a';

}

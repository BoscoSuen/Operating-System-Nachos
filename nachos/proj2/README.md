# Project2
## Task1
### Implementation  
1. Creat  
1.1 get the filename via readVirtualMemoryString(vaName, maxParameterLength)  
1.2 check the filename, if the filename is invalid, return -1  
1.3 call the UserKernel.fileSystem.open(filename, true) to creat a file  
1.4 add this file into process's fileDescription Table   
2. Open  
2.1 get the filename via readVirtualMemoryString(vaName, maxParameterLength)    
2.2 check the filename, if the filename is invalid, return -1   
2.3 call the UserKernel.fileSystem.open(filename, false) to open a file  
2.4 add this file into process's fileDescription Table  
3. Read  
3.1 check whether the fileDescription is valid, if not return -1  
3.2 fileDescriptorTable[fd].read() to read specific bytes into local buffer, if failed(return -1 from read()), then return -1.  
3.3 use writeVirtualMemory() to write localBuffer into process address, if transfered smaller than received from read(), return -1, otherwise succeed, return received bytes. 
4. Write  
4.1 check whether the fileDescription is valid, if not return -1  
4.2 use readVirtualMemory() read bytes from use process into local buffer, if transfered smaller than the required, return -1  
4.3 use fileDescriptorTable[fd].write() write bytes into file, if send smaller than required, return -1, otherwise return required write bytes.  
5. Close  
5.1 check whether the fileDescription is valid, if not return -1  
5.2 fileDescriptorTable[fd].close() let the file closed   
5.3 fileDescriptorTable[fd] = null  release the reference   
6. Unlink  
6.1 get the filename via readVirtualMemoryString(vaName, maxParameterLength)   
6.2 check the filename, if the filename is invalid, return -1  
6.4 use UserKernel.fileSystem.remove(name) remove a file in the filesystem, return the result. 
### Testing  
1. creat 
we write several test c program to test this. We check whether the creat run normally, and examinate this function with 
invalid arguments, such as filename which 256 characters. 
2. open  
First, we check whether the open syscall can execute normally, then we test open with invaid arguments, such as 
non-exist file, invalid filename, etc.   
3. read  
We test read with valid argument to test whether it can execute normally, and test it with invalid arguments, such as 
invalid file descripter, bufferPointer.   
4. write  
We test write with valid argument to test whether it can execute normally, and test it with invalid arguments, such as 
invalid file descripter, bufferPointer.    
5. close  
we test close with valid file descripter to see whether it can run normally, and test it with invalid file descripter to test it.   
6. unlink  
We test it with valid filename, invalid filename, and exist filename, non-exist filename to see wheter this syscall implement correctly.  
## Task2
### Implementation
In this part, first, in the UserKernel class, 
we implemented a linked-list to manage the free
physical pages and a lock to prevent race conditions. 
We also implemented [getAFreePhysicalPage](https://github.com/UCSD-CSE120-SP20-A/nachos_sp20_CathyWang53_Yukinichi_isguoqiang/blob/d6739fd554c02ba5051762f5c5fa7c5b84bedff6/nachos/userprog/UserKernel.java#L152-L162)
 and [releasePhysicalPage](https://github.com/UCSD-CSE120-SP20-A/nachos_sp20_CathyWang53_Yukinichi_isguoqiang/blob/d6739fd554c02ba5051762f5c5fa7c5b84bedff6/nachos/userprog/UserKernel.java#L164-L170) 
 methods.
Second, in the UserProcess class, we maintained a pageTable 
to translate virtual address to physical address. 
We then modified the [loadSections](https://github.com/UCSD-CSE120-SP20-A/nachos_sp20_CathyWang53_Yukinichi_isguoqiang/blob/d6739fd554c02ba5051762f5c5fa7c5b84bedff6/nachos/userprog/UserProcess.java#L387-L411)
 and 
 [unloadSections](https://github.com/UCSD-CSE120-SP20-A/nachos_sp20_CathyWang53_Yukinichi_isguoqiang/blob/d6739fd554c02ba5051762f5c5fa7c5b84bedff6/nachos/userprog/UserProcess.java#L416-L423)
  functions to support 
the usage of pageTable and virtual memory for multiprogramming. 
That is, we translate the virtual address to physical address 
and then load sections to the corresponding physical memory.
Finally, we modified the [readVirtualMemory](https://github.com/UCSD-CSE120-SP20-A/nachos_sp20_CathyWang53_Yukinichi_isguoqiang/blob/d6739fd554c02ba5051762f5c5fa7c5b84bedff6/nachos/userprog/UserProcess.java#L157)
 and [writeVirtualMemory](https://github.com/UCSD-CSE120-SP20-A/nachos_sp20_CathyWang53_Yukinichi_isguoqiang/blob/d6739fd554c02ba5051762f5c5fa7c5b84bedff6/nachos/userprog/UserProcess.java#L229)
  methods. 
Here we translate the virtual address to the physical address 
and read/write data per page from/to the virtual memory.

### Testing
We write several test cases to see whether each process can execute normally without access other process's address space.  
## Task3
### Implementation
To deal with the parent process and child process, we use a pointer to track the parent process and use a Map to link the PID of child Process and the correspoind process. As for PID, we use the counter of the process. All implementation with critical section have been considered. And all the excepions and faults have been considered.
- [handle exec]() We can get the coffName from coff virtual address by calling *readVirtualMemoryString*, and we build an array of String to store the argvs to be read, and we read the virtual memory from argv by calling *readVirtualMemory*, and then read the corresponding argv string through the virtual memorty. After that, we pass to next argv(argv + 4), and do same process. Then we create a new process, lined its parent to this process and put the PID and process in the map, and then execute the coffName and argument string array.
- [handle join]() We use the map to look up the process by the PID, and call join() in the KThread that have been implemented before, and then remove the key-val in the map, write the exit status to the status virtual address.
- [handle exit]() We close all OpenFile in file table and unload section, and then link parent pointer to null, close the coff, set exit status, call *UThread.currentThread().finish()* and wake the parent, and we also handled abnormal exit to check if the status == INT_MIN(we set the value as abnormal exit)
 ### Testing
We test the exec by executing some program we have implemented, and test some bad address, bad file and some exceptions. And we test join by testing if the child process can wrongly join the parent. And we test exit in the exec and join to check if there is any abnormal exit. All the test cases perform well.
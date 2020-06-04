# Project1
## Task1
[Alarm class](https://github.com/UCSD-CSE120-SP20-A/nachos_sp20_CathyWang53_Yukinichi_isguoqiang/blob/50b3724f101f3039d62f7514bd3ebdf3de5b4b00/nachos/threads/Alarm.java) implements a PriorityQueue to list all waiting threads with corresponding *KThread and wake uptime*, and order in their wake up time. The class includes 
- [WaitUntil()](https://github.com/UCSD-CSE120-SP20-A/nachos_sp20_CathyWang53_Yukinichi_isguoqiang/blob/750b887b60eb206e76b2b8a5237862aa8c4e6320/nachos/threads/Alarm.java#L61) to block the current thread, we disable the interrupt, set wake up time and add to the waiting queue, and put the current thread to sleep and restore the interrupt;
- [timerInterrupt()](https://github.com/UCSD-CSE120-SP20-A/nachos_sp20_CathyWang53_Yukinichi_isguoqiang/blob/750b887b60eb206e76b2b8a5237862aa8c4e6320/nachos/threads/Alarm.java#L36) handle the interrupt to check the wake up time, we disable the interrupt, and before unblock the thread, first check if the thread is already unblocked in other function calls and check is the thread need to unblock, unblock the thread and restore the interrupt and force a context switch;
- [cancel()](https://github.com/UCSD-CSE120-SP20-A/nachos_sp20_CathyWang53_Yukinichi_isguoqiang/blob/750b887b60eb206e76b2b8a5237862aa8c4e6320/nachos/threads/Alarm.java#L86) we disable the interrupt and remove the corresponding thread in the priorityqueue and place the thread in the ready set and restore the interrupt. And all methods work well.  
  
***Test***: Using a list of durations and call waitUntil() to check the (wake up time - sleep time) >= x. And also use multi-thread to call different waitUntil() time, and check if the running order is as expected. The smaller waitUntil time will make the thread wake up earlier. And all tests performs well.
## Task2
### Implementation
We wrote the [join()](https://github.com/UCSD-CSE120-SP20-A/nachos_sp20_CathyWang53_Yukinichi_isguoqiang/blob/750b887b60eb206e76b2b8a5237862aa8c4e6320/nachos/threads/KThread.java#L287) function in the Kthread.java. 
If this thread's status is finished, currentThread will go sleeping; or currentThread will return directly.
We used [two global variables](https://github.com/UCSD-CSE120-SP20-A/nachos_sp20_CathyWang53_Yukinichi_isguoqiang/blob/750b887b60eb206e76b2b8a5237862aa8c4e6320/nachos/threads/KThread.java#L605-L606) to check if this thread has been joined, and record the joined thread.
In the finish method, we will wake up the joined thread if it's sleeping.
### Evaluation
Join function can handle all situations mentioned in the project description, 
in over 1000 different randomly-interrupted environments.

### Testing
We tested the join method by implementing four [joinTesters](https://github.com/UCSD-CSE120-SP20-A/nachos_sp20_CathyWang53_Yukinichi_isguoqiang/blob/750b887b60eb206e76b2b8a5237862aa8c4e6320/nachos/threads/KThread.java#L457).
The testers cover the situations mentioned above. 
In addition, we ran the code in over 1000 different randomly-interrupted environments.

## Task3
In this task, we implement condition variable in Condition2.java using interrupt enable and disable 
to provide atomicity. 
#### Implementation
we uses an Queue of KThread (LinkedList in java) to represent a **waitQueue**.
- [sleep()](https://github.com/UCSD-CSE120-SP20-A/nachos_sp20_CathyWang53_Yukinichi_isguoqiang/blob/50b3724f101f3039d62f7514bd3ebdf3de5b4b00/nachos/threads/Condition2.java#L36)  
When a thread call Condition2.sleep(), it will first put it self into 
the waitQueue of Condition2.   
Then it will disable the interrupt of machine, release the lock the thread hold,
call KThread.sleep() to block itself and wait other thread wake it up.  
When it is waked up, it will enable the interrupt first and then re-acquire the lock 
before finish this Condition2.sleep() function.  
The position of interrupt disable and enable is critical and it depends on 
which part we should regard it as an atomic operation. In the sleep(), we 
think the atomic operation should be lock release and sleep. Since if it is not 
an atomic operation, if another thread acquires the lock before this thread goes to 
sleep and wake this thread, this wakeup information will be lost.
- [wake()](https://github.com/UCSD-CSE120-SP20-A/nachos_sp20_CathyWang53_Yukinichi_isguoqiang/blob/50b3724f101f3039d62f7514bd3ebdf3de5b4b00/nachos/threads/Condition2.java#L55)   
In this function, we first disable and enable the interrupt at the begin and end respectively.
And then pop a thread from waitQueue, if it is been blocked using 
KThread.isBlocked() (we added in the KThread) we can ready it otherwise we pop another thread.
For how to do ready() correctly, we should first using ThreadKernel.alarm.cancel(thread) to cancel
this thread's waiting, if canceled successfully, it means this thead is waiting in alarm by
 calling waitUtil(ticks) and been readied, so that in this wake function, we just don't do
ready() again. If the cancel function return false, we need to ready this thread here.

- [wakeAll()](https://github.com/UCSD-CSE120-SP20-A/nachos_sp20_CathyWang53_Yukinichi_isguoqiang/blob/50b3724f101f3039d62f7514bd3ebdf3de5b4b00/nachos/threads/Condition2.java#L75)   
It is much similar to what wake() do but wake all the threads in the waitQueue
by a while loop.

#### Testing
We write a [producer-consumer model](https://github.com/UCSD-CSE120-SP20-A/nachos_sp20_CathyWang53_Yukinichi_isguoqiang/blob/50b3724f101f3039d62f7514bd3ebdf3de5b4b00/nachos/threads/Condition2.java#L255) using lock and condition variable implemented in 
this part. We have 20 producers and 20 consumers who are working on the fixed size buffers.
And using assert statements to raise Exceptions when producers and consumers work on the 
invalid buffer. And also add a test that after sleep the thread for a specific time duration, and before it wakes up, we call cancel() method to wake up the thread to ready manually and check the wake() thread will not call duplicate ready(). 
We use a auto test shell scripts to do test with different numbers in nachos -s <numbers> to
ensure our producer-consumer run correctly.
## Task4
#### Implementation
In this part we implemented sleepFor(ticks). This function is much similar to 
the sleep() function.  
The main difference is that after release the lock, it will call ThreadKernel.alarm.waitUtil(timeout) 
to sleep. Note that this thread will be added both into waitQueues of Condition2 and Alarm. 
So that when it ready by no matter Alarm or Condition2, the thread should be both removed by waitQueue 
in Alarm and Condition2. We realized this by:   
- In [sleepFor()](https://github.com/UCSD-CSE120-SP20-A/nachos_sp20_CathyWang53_Yukinichi_isguoqiang/blob/50b3724f101f3039d62f7514bd3ebdf3de5b4b00/nachos/threads/Condition2.java#L159), after ThreadKernel.alarm.waitUtil(timeout) finished, we believe the thread is remove from 
Alarm's waitQueue. So what should we do is just remove from waitQueue in Condition2. And then enable interrupt 
and re-acquire lock.
- In wake() & wakeAll(), when we decided to wake up (ready) a thread, this thread maybe is sleep in Alarm 
by waitUtil(timeout). So we first call ThreadKernel.alarm.cancel(thread), which will return false if this thread 
is not waiting in alarm right now or return true if this thread is wait in alarm and has been waked up by 
calling this cancel() function. So in wake() & wakeAll(), if cancel() return true, we don't do ready again, otherwise 
we should ready this thread here in wake() or wakeAll(). [LINK](https://github.com/UCSD-CSE120-SP20-A/nachos_sp20_CathyWang53_Yukinichi_isguoqiang/blob/50b3724f101f3039d62f7514bd3ebdf3de5b4b00/nachos/threads/Condition2.java#L60)

#### Testing
This part uses Producer-Consumer model in Task3 as well. Just using sleepFor(timeout) instead of sleep() in code.

## Task5
### Implementation
In this part, we implemented the [Rendezvous](https://github.com/UCSD-CSE120-SP20-A/nachos_sp20_CathyWang53_Yukinichi_isguoqiang/blob/f3d96f75beda73e6aee9aaedd6689206b4c4bc1e/nachos/threads/Rendezvous.java#L10) class.
To achieve the independence between different tags, we created a data segment class [dataSegEachTag](https://github.com/UCSD-CSE120-SP20-A/nachos_sp20_CathyWang53_Yukinichi_isguoqiang/blob/f3d96f75beda73e6aee9aaedd6689206b4c4bc1e/nachos/threads/Rendezvous.java#L50) for each tag. 
In each dataSegClass, different number of threads can exchange data, paired in order.
The shared data (the pointer of two threads and exchanged values) is protected by a lock and a condition variable. 
A hash map resultValMap is also used to store the result value for each thread. 
Since each thread has to release the lock before returning, 
it's possible that other threads may modify the shared exchanged data, 
so we use this hash map to store the returned values separately.

The main logic is in the [exchange()](https://github.com/UCSD-CSE120-SP20-A/nachos_sp20_CathyWang53_Yukinichi_isguoqiang/blob/f3d96f75beda73e6aee9aaedd6689206b4c4bc1e/nachos/threads/Rendezvous.java#L72) 
function. Every time when a thread comes, it first acquire the lock, then checks if the tag is empty. 
This decides which if-else code segment it may go into. 
For the odd number of thread, it rewrites the shared variables, goes to sleep and releases the lock.
For the even number of thread, it first rewrites the shared variables, stores the results into the resultValMap, 
wakes up the paired thread, releases the lock, and then returns the result.
At last, the odd thread wakes up and returns the result.

In the Rendezvous class, we maintained a hash map tagDataMap to mapping the tag to its data segment class.
This hash map is also protected by a lock. In its [exchange()](https://github.com/UCSD-CSE120-SP20-A/nachos_sp20_CathyWang53_Yukinichi_isguoqiang/blob/f3d96f75beda73e6aee9aaedd6689206b4c4bc1e/nachos/threads/Rendezvous.java#L36)
function, the thread first finds the corresponding tag, then calls the corresponding exchange function.

### Evaluation
During the running process, the threads can be interrupted by any time. 
Our Rendezvous can handle all the tested situations mentioned in the project description 
in over 1000 different randomly-interrupted environments. 

### Testing
We found three bugs during the testing. 

* First, the lock protecting the shared data in each dataSegEachTag, [lockIsEmpty](https://github.com/UCSD-CSE120-SP20-A/nachos_sp20_CathyWang53_Yukinichi_isguoqiang/blob/f3d96f75beda73e6aee9aaedd6689206b4c4bc1e/nachos/threads/Rendezvous.java#L75), 
should be above the if-else, or two threads may both go into the empty code seg.

* Second, we need to use the hash map [resultValMap](https://github.com/UCSD-CSE120-SP20-A/nachos_sp20_CathyWang53_Yukinichi_isguoqiang/blob/f3d96f75beda73e6aee9aaedd6689206b4c4bc1e/nachos/threads/Rendezvous.java#L107)
to store the result value for each thread separately, 
or some other threads may modify the shared variables before this thread returns.

* Third, for the odd thread, it shouldn't modify the shared variable from being waked up to returning.
Or, if multiple threads are sleeping, and one of them is still waiting for the paired thread, which means, 
its result value hasn't been stored into the resultValMap, the shared variable is meaningful to it.
If other thread modifies the shared variable, it will be influenced.

We wrote five [testers](https://github.com/UCSD-CSE120-SP20-A/nachos_sp20_CathyWang53_Yukinichi_isguoqiang/blob/f3d96f75beda73e6aee9aaedd6689206b4c4bc1e/nachos/threads/Rendezvous.java#L135)
 covering all the situations mentioned in the project description. We also runs in over 1000 different randomly-interrupted environments.
 
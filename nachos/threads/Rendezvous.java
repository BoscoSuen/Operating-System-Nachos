package nachos.threads;

import nachos.machine.*;

import java.util.HashMap;

/**
 * A <i>Rendezvous</i> allows threads to synchronously exchange values.
 */
public class Rendezvous {
    /**
     * Allocate a new Rendezvous.
     */
    public Rendezvous() {
        tagDataMap = new HashMap<>();
        lock = new Lock();
    }

    /**
     * Synchronously exchange a value with another thread.  The first
     * thread A (with value X) to exhange will block waiting for
     * another thread B (with value Y).  When thread B arrives, it
     * will unblock A and the threads will exchange values: value Y
     * will be returned to thread A, and value X will be returned to
     * thread B.
     * <p>
     * Different integer tags are used as different, parallel
     * synchronization points (i.e., threads synchronizing at
     * different tags do not interact with each other).  The same tag
     * can also be used repeatedly for multiple exchanges.
     *
     * @param tag   the synchronization tag.
     * @param value the integer to exchange.
     */

    public int exchange(int tag, int value) {
        lock.acquire();
//        System.out.println("enter lock 1");
        if (!tagDataMap.containsKey(tag)) {
//            System.out.println(
//                    "put new data segment"
//            );
            tagDataMap.put(tag,new dataSegEachTag(tag));
        }
        dataSegEachTag tagData = tagDataMap.get(tag);
        lock.release();
        return tagData.exchange(value);
    }

    private class dataSegEachTag {
        //shared variables
        private int exchgVal1, exchgVal2;
        private KThread thread1, thread2;
        private boolean isEmpty;
        private int tag;

        //condition variable and lock
        private Condition2 waitForThread2;
        private Lock lock;

        //shared data, but the key-val pair is unique for each thread
        public HashMap<KThread, Integer> resultValMap;

        public dataSegEachTag(int tag) {
            resultValMap = new HashMap<>();
            lock = new Lock();
            waitForThread2 = new Condition2(lock);
            isEmpty = true;
            this.tag = tag;
        }

        public int exchange(int value) {
            // current status: supporting multiple threads call on the same tag only
            // TODO: need code review, not sure about the correctness
            if (isEmpty) {// this only runs in thread 1
                System.out.println("tag "+ tag +" is empty");
                lock.acquire();
                thread1 = KThread.currentThread();
                exchgVal1 = value;
                isEmpty = false;

                // thread 1 sleep
                waitForThread2.sleep();

                //re-initialize
                thread1 = null;

                lock.release();

                //return to thread 1

                //Since we have leaved the locked area,
                // it can't guarantee the value of shared variables
                return resultValMap.get(KThread.currentThread());
            } else { // this only runs in thread 2
                System.out.println("tag "+ tag +" is full");
                lock.acquire();
                thread2 = KThread.currentThread();
                exchgVal2 = value;

                //set the resultVal
                //TODO question: every time a new thread call this function,
                // new data putted into the map without being removed...
                resultValMap.put(thread1, exchgVal2);
                resultValMap.put(thread2, exchgVal1);

                //wake up thread 1
                waitForThread2.wake();

                //re-initialize
                isEmpty = true;
                thread2 = null;
                lock.release();

                //return to thread 2
                return resultValMap.get(KThread.currentThread());
            }
        }
    }

    //shared data, unique for each tag
    private HashMap<Integer, dataSegEachTag> tagDataMap;
    Lock lock;


    public static void rendezTest1() {
        System.out.println("Test 1: four threads on two tags");
        final Rendezvous r = new Rendezvous();

        KThread t1 = new KThread(new Runnable() {
            public void run() {
                int tag = 0;
                int send = -1;

                System.out.println("Thread " + KThread.currentThread().getName() + " exchanging " + send);
                int recv = r.exchange(tag, send);
                Lib.assertTrue(recv == 1, "Was expecting " + 1 + " but received " + recv);
                System.out.println("Thread " + KThread.currentThread().getName() + " received " + recv);
            }
        });
        t1.setName("t1");
        KThread t2 = new KThread(new Runnable() {
            public void run() {
                int tag = 0;
                int send = 1;

                System.out.println("Thread " + KThread.currentThread().getName() + " exchanging " + send);
                int recv = r.exchange(tag, send);
                Lib.assertTrue(recv == -1, "Was expecting " + -1 + " but received " + recv);
                System.out.println("Thread " + KThread.currentThread().getName() + " received " + recv);
            }
        });
        t2.setName("t2");
        KThread t3 = new KThread(new Runnable() {
            public void run() {
                int tag = 1;
                int send = -2;

                System.out.println("Thread " + KThread.currentThread().getName() + " exchanging " + send);
                int recv = r.exchange(tag, send);
                Lib.assertTrue(recv == 2, "Was expecting " + 2 + " but received " + recv);
                System.out.println("Thread " + KThread.currentThread().getName() + " received " + recv);
            }
        });
        t3.setName("t3");
        KThread t4 = new KThread(new Runnable() {
            public void run() {
                int tag = 1;
                int send = 2;

                System.out.println("Thread " + KThread.currentThread().getName() + " exchanging " + send);
                int recv = r.exchange(tag, send);
                Lib.assertTrue(recv == -2, "Was expecting " + -2 + " but received " + recv);
                System.out.println("Thread " + KThread.currentThread().getName() + " received " + recv);
            }
        });
        t4.setName("t4");

        t1.fork();
        t3.fork();
        t2.fork();

        t4.fork();
        // assumes join is implemented correctly
        t1.join();
        t2.join();
        t3.join();
        t4.join();
    }

    public static void rendezTest2() {
        System.out.println(" ");
        System.out.println("Test 2: four threads on two rendezvous");
        final Rendezvous r1 = new Rendezvous();
        final Rendezvous r2 = new Rendezvous();

        KThread t1 = new KThread(new Runnable() {
            public void run() {
                int tag = 0;
                int send = -1;

                System.out.println("Thread " + KThread.currentThread().getName() + " exchanging " + send);
                int recv = r1.exchange(tag, send);
                Lib.assertTrue(recv == 1, "Was expecting " + 1 + " but received " + recv);
                System.out.println("Thread " + KThread.currentThread().getName() + " received " + recv);
            }
        });
        t1.setName("t1");
        KThread t2 = new KThread(new Runnable() {
            public void run() {
                int tag = 0;
                int send = 1;

                System.out.println("Thread " + KThread.currentThread().getName() + " exchanging " + send);
                int recv = r1.exchange(tag, send);
                Lib.assertTrue(recv == -1, "Was expecting " + -1 + " but received " + recv);
                System.out.println("Thread " + KThread.currentThread().getName() + " received " + recv);
            }
        });
        t2.setName("t2");
        KThread t3 = new KThread(new Runnable() {
            public void run() {
                int tag = 1;
                int send = -2;

                System.out.println("Thread " + KThread.currentThread().getName() + " exchanging " + send);
                int recv = r2.exchange(tag, send);
                Lib.assertTrue(recv == 2, "Was expecting " + 2 + " but received " + recv);
                System.out.println("Thread " + KThread.currentThread().getName() + " received " + recv);
            }
        });
        t3.setName("t3");
        KThread t4 = new KThread(new Runnable() {
            public void run() {
                int tag = 1;
                int send = 2;

                System.out.println("Thread " + KThread.currentThread().getName() + " exchanging " + send);
                int recv = r2.exchange(tag, send);
                Lib.assertTrue(recv == -2, "Was expecting " + -2 + " but received " + recv);
                System.out.println("Thread " + KThread.currentThread().getName() + " received " + recv);
            }
        });
        t4.setName("t4");

        t1.fork();
        t3.fork();
        t2.fork();

        t4.fork();
        // assumes join is implemented correctly
        t1.join();
        t2.join();
        t3.join();
        t4.join();
    }

    // Invoke Rendezvous.selfTest() from ThreadedKernel.selfTest()

    public static void selfTest() {
        // place calls to your Rendezvous tests that you implement here
        rendezTest1();
        rendezTest2();
    }
}


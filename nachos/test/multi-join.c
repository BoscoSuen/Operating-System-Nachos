#include "syscall.h"
#include "stdio.h"
#include "stdlib.h"

/**
 * This program tests whether the parent can join (multiple) child processes, whether they before
 * or after they finish.
 */

int main()
{
    char * argv1 [1];
    argv1[0] = "hello.coff";
    char * argv2 [1];
    argv2[0] = "runAWhile.coff";
    char * argv3 [1];
    argv3[0] = "exit1.coff";

    int cpid1, cpid2;
    int statusStore = -99;
    int* status = &statusStore;
    int joinRes, failNum;




    printf("Test 1: exec two hello.coff, join in reversed order\n");
    cpid1 = exec("hello.coff",1,argv1);
    cpid2 = exec("hello.coff",1,argv1);

    joinRes = join(cpid2, status);
    printf("Join cpid2: join status: %d; exit status: %d\n", joinRes, *status);
    if (joinRes != 1) {
        printf("Join cpid2 error! join status: %d\n", joinRes);
    }
    joinRes = join(cpid1, status);
    printf("Join cpid1: join status: %d; exit status: %d\n", joinRes, *status);
    if (joinRes != 1) {
        printf("Join cpid1 error! join status: %d\n", joinRes);
    }

    printf("test 1 ends!\n\n");



    printf("Test 2: exec runAWhile.coff then hello.coff, join in order\n");
    cpid1 = exec("runAWhile.coff",1,argv2);
    cpid2 = exec("hello.coff",1,argv1);

    joinRes = join(cpid1, status);
    printf("Join cpid2: join status: %d; exit status: %d\n", joinRes, *status);
    if (joinRes != 1) {
        printf("Join cpid2 error! status: %d\n", joinRes);
    }
    joinRes = join(cpid2, status);
    printf("Join cpid1: join status: %d; exit status: %d\n", joinRes, *status);
    if (joinRes != 1) {
        printf("Join cpid1 error! status: %d\n", joinRes);
    }

    printf("test 2 ends!\n\n");



    printf("Test 3: join exit1.coff\n");
    cpid1 = exec("exit1.coff",1,argv3);

    joinRes = join(cpid1, status);
    if (joinRes != 1) {
        printf("Exit1 exits abnormally. join status: %d; exit status: %d\n", joinRes, *status);
    }
    else
        printf("Exit1 exits normally! join status: %d; exit status: %d\n", joinRes, *status);

    printf("test 3 ends!\n\n");



    printf("Test 4: join except1.coff. Should exit abnormally.\n");
    cpid1 = exec("except1.coff",0,0);

    joinRes = join(cpid1, status);
    if (joinRes != 1) {
        printf("except1 exits abnormally (PASS). join status: %d; exit status: %d\n", joinRes, *status);
    }
    else
        printf("except1 exits normally (FAIL)! join status: %d; exit status: %d\n", joinRes, *status);

    printf("test 4 ends!\n\n");




    printf("Test 5: join test.coff\n");
    cpid1 = exec("test.coff",0,0);

    joinRes = join(cpid1, status);
    if (joinRes != 1) {
        printf("test exits abnormally. join status: %d; exit status: %d\n", joinRes, *status);
    }
    else
        printf("test exits normally! join status: %d; exit status: %d\n", joinRes, *status);

    printf("test 5 ends!\n\n");

    return 0;
}
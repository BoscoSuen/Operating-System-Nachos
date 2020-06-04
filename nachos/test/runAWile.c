#include "syscall.h"
#include "stdio.h"
#include "stdlib.h"

/**
 * This program tests whether the parent can join (multiple) child processes, whether they before
 * or after they finish.
 */

int main()
{
    printf("runAWhile starts\n");
    int i;
    int t = 100000;
    for (i = 0; i < t; i++) {}
    printf("runAWhile ends\n");
    return 0;
}
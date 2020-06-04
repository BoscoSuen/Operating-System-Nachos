/* test open a exist file */
#include "syscall.h"
#include "stdlib.h"
#include "stdio.h"

int main() {

    char *str = "testFile.txt";
    int fd = open(str);
    if (fd != -1) { printf("Test Passed: open 'testFile.txt' in test directory. "); }
    else { printf("Test Failed."); }
    assert(fd != -1);
    return 0;
}

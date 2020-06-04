/* test open a non-exist file */
#include "syscall.h"
#include "stdlib.h"
#include "stdio.h"

int main() {

    char *str = "non_exist.txt";
    int fd = open(str);
    if (fd == -1) { printf("Test Passed: open a non-exist file. "); }
    else { printf("Test Failed."); }
    assert(fd == -1);
    return 0;
}

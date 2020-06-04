/*
The test is used for check multi process status and after all processes done, the kernel need to be terminated
*/

#include "syscall.h"
#include "stdio.h"

int
main() {
    int status = 0;
    int pid;
    for (int i = 0; i < 5; ++i) {
        pid = exec("simpleTest.coff", 0, 0);
        if (pid <= 0) {
            printf("exec failed.\n");
            break;
        }
        // ##################
        // execute in order #
        // ##################
//        if (join(pid, &status) != 1) {
//            printf("join failed.\n");
//            break;
//        }
        printf("i = %d, status = %d\n", i, status);
    }
    exit(0);
    return 0;
}

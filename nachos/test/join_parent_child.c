/*
The test is used to check the child process cannot join the parent process.
*/

#include "syscall.h"
// join(pid, *status)

int
main(int argc, char* argv[]) {
    int pid = 0, status = 0;
    if (argc == 0) {
        // in the root process, we try to create the child process
        char* args[1];
        args[0] = "NULL";
        pid = exec("join_parent_child.coff", 1, args);
        int result = join(pid, &status);
        if (result > 0) {
            printf("Join pass! result = %d. \n", result);
        } else {
            // return 0 or 1
            printf("Join failed! result = %d. \n", result);
            exit(-1);
        }
    } else {
        // child process
        int result = join(0, &status);
        if (result < 0) {
            //If processID does not refer to a child process of the current process, returns -1.
            printf("Child process cannot join the parent process! \n");
            printf("\t Test child join parent: pass");
        }
    }
    return 0;
}
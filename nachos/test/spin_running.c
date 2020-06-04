#include "syscall.h"
#include "stdio.h"

int
main() {
    printf("wait! \n");
    for (int i = 0; i < 666666; ++i) {}
    printf("waiting finish. \n");
    return 0;
}
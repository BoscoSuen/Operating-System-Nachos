/* read1.c
** read a non-exist file with correct arguments (fd is invalid)
*/
#include "syscall.h"
#include "stdio.h"
int main() {
    int fd = -1;
    char buff[10]="";
    int recv = 0;
    while ((recv = read(fd, buff, 10)) != 0) {
        if (recv == -1) {
            printf("read Failed. Exit");
            exit(-1);
        }
        printf("%s", buff);
    }
    printf("\n");
    return 0;
}

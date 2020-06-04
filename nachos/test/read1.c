/* read1.c
** read a exist file with correct arguments
*/
#include "syscall.h"
#include "stdio.h"
int main() {
    char *str = "testFile.txt";
    int fd = open(str);
    char buff[10];
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

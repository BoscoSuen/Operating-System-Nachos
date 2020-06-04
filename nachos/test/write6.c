/* write2.c
** write str into an exist file with correct arguments -address
*/
#include "syscall.h"
#include "stdio.h"
#include "stdlib.h"
int main() {
    char *str = "testFile.txt";
    char *inputStr = "write something here";
    int fd = open(str);
    int send = write(fd, inputStr+100, strlen(inputStr));
    if (send == -1) {
        printf("Error");
        exit(-1);
    } else {
        printf("send %d", send);
    }
    return 0;
}
/* write2.c
 ** write str into an exist file with invalid arguments -fd.
 */
 #include "syscall.h"
 #include "stdio.h"
 #include "stdlib.h"
 int main() {
     char *inputStr = "write something here";
     int fd = 20;
     int send = write(fd, inputStr, strlen(inputStr));
     if (send == -1) {
         printf("Error");
         exit(-1);
     } else {
         printf("send %d", send);
     }
     return 0;
 }
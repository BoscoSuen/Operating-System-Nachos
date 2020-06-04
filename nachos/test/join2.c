#include "syscall.h"

int
main (int argc, char *argv[])
{
    int pid, r, status = 0;

    if (argc > 0) {
      // we are in child, try to join the parent
      r = join(0, &status);
      if (r < 0) {
        // child was denied access! Nice!
        printf("the child is unable to join its parent!\n");
        return 0;
      }
      else {
        // oops! we can join the parent (which makes no sense)!
        printf("the child was able to join its parent!\n");
        exit(-1);
      }
    }
    else {

    printf ("creating child...\n");
    char *arg[1];
    arg[0] = "None";
    pid = exec ("join2.coff", 1, arg);
    r = join (pid, &status);
    if (r <= 0) {
    	printf ("...failed (r = %d)\n", r);
      exit(-1);
    }
    else {
      printf("passed! (r = %d)\n", r);
    }
    }
    // the return value from main is used as the status to exit
    return 0;
}
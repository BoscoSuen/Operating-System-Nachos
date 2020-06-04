/* close1.c
** open a file and then close it
*/
#include "syscall.h"
#include "stdio.h"
#include "stdlib.h"
int main() {
    char *str = "testFile.txt";
    int res1 = unlink(str);
    assert(res1 == 0);
    if (res1 == 0) { printf("Test Passed: unlinked a exist file"); }
    else { printf("Test Failed: failed to unlinked a exist file"); }

    int res2 = unlink(str);
    if (res2 == -1) { printf("Test Passed: unlinked a non-exist file"); }
    else { printf("Test Failed:  unlinked a non-exist file"); }
    return 0;
}

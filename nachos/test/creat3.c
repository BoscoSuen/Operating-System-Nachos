/* test filename length of creat function */
#include "syscall.h"
#include "stdio.h"
#include "stdlib.h"
int main() {

    /* filename length : 256 chars */
	char* filename="0123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345";
	int result = creat(filename);
	if (result == -1) { printf("Test Passed: create a file with name length 256 failed. "); }
	else { printf("Test Failed."); }
	return 0;
}

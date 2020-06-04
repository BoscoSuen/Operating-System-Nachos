/* test filename length of creat function */
#include "syscall.h"
#include "stdio.h"
#include "stdlib.h"
int main() {

    /* filename length : 257 chars */
	char* filename="01234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456";
	int result = creat(filename);
	if (result == -1) { printf("Test Passed: create a file with invalid name length. "); }
	else { printf("Test Failed."); }
	return 0;
}

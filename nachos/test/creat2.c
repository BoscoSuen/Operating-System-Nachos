/* test filename length of creat function */
#include "syscall.h"
#include "stdio.h"
#include "stdlib.h"
int main() {

    /* filename length : 255 chars */
	char* filename="012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234";
	int result = creat(filename);
	if (result != -1) { printf("Test Passed: create a file with name length 255. "); }
	else { printf("Test Failed."); }
	return 0;
}

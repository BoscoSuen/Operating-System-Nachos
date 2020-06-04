/*
fage_fault generator that a wrong pointer cannot fit in memory
*/

#include "syscall.h"

int
main (int argc, char *argv[])
{
    int *ptr = (int*)0xBADFFFFF;
    return *ptr;
}
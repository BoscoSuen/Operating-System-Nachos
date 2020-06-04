
#include "syscall.h"
#include "stdio.h"

int main()
{
    printf("This is test.coff speaking\n");
    halt();
    printf("Should print out this line if this program wasn't executed directly\n");
    return 0;
}

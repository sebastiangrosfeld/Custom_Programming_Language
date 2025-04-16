#include<stdio.h>

int test(int n) {
    printf("Hello, World! %d\n", n);
    return 0;
}

int main() {
    int a = test(5);
    printf("%d\n", a);
    return 0;
}

#include <stdio.h>
#include <iostream>
#include <vector>

#include "lexer.h"

int main(int argc, char *argv[]) {
    if (argc > 1) {
        std::string file_name(argv[1]);
        Lexer lex(file_name);
    } else {
        std::cout << "[Error] No input file provided\n";
    }
    return 0;
}
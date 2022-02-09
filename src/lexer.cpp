#include <stdio.h>
#include <iostream>
#include <fstream>
#include "../include/lexer.h"

char c;
FILE* input_file;

Lexer::Lexer(std::string &file_name) {
    input_file = fopen(file_name.c_str(),"r");
    if(input_file == nullptr) {
        std::cout << "[Error] Script " << input_file << " could not be read\n";
        exit(1);
    } 

    c = getc(input_file);
    if (c == EOF) {
        //do something related to eof here?
        fclose(input_file);
    }
}

char nextChar() {
    if (c != EOF) {
        c = getc(input_file);
    } else {
        c = EOF;
    }
    return c;
}
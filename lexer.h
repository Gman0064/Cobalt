#include <iostream>

#ifndef LEXER_H
#define LEXER_H

class Lexer
{
    private:
        std::string extension = ".prog";
    public:
        Lexer(std::string &file_name);
};

#endif
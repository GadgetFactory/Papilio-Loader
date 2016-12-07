/* Xilinx .bit file parser

Copyright (C) 2004 Andrew Rogers

This program is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; either version 2 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA

Changes:
Dmitry Teytelman [dimtey@gmail.com] 14 Jun 2006 [applied 13 Aug 2006]:
    Code cleanup for clean -Wall compile.
Mike FIeld [hamster@snap.net.nz] 15 Oct 2012
    Add appendZeros() method
*/



#include "bitfile.h"
#include "io_exception.h"

#include <sys/types.h>
#include <sys/stat.h>
#include <unistd.h>
#include <errno.h>
#include <string.h>
#include <stdio.h>

using namespace std;

BitFile::BitFile()
  : length(0), buffer(0), Error(false), logfile(stderr) {

  // Initialize bit flip table
  initFlip();
}

// print information of the bit file
void BitFile::print()
{
    printf("Created from NCD file: %s\n",getNCDFilename());
    printf("Target device: %s\n",getPartName());
    printf("Created: %s %s\n",getDate(),getTime());
    printf("Bitstream length: %lu bits\n", getLength());
}

// Read in file
void BitFile::readFile(char const * fname, bool flip)
{
    FILE *const  fp=fopen(fname,"rb");
    if(!fp)
        throw  io_exception(std::string("Cannot open file ") );
    filename = fname;
    size_t rsize;

    try
    {
        // Skip the header
        fseek(fp, 13, 0);

        char         key;
        std::string *field;
        std::string  dummy;

        while(!feof(fp))
        {
            rsize = fread(&key, 1, 1, fp);
            if (rsize != 1)
                throw io_exception("Unexpected end of file");
            switch(key)
            {
                case 'a': field = &ncdFilename; break;
                case 'b': field = &partName;    break;
                case 'c': field = &date;        break;
                case 'd': field = &time;        break;
                case 'e':
                    processData(fp, flip);
                    fclose(fp);
                    return;
                default:
                    fprintf(stderr, "Ignoring unknown field '%c'\n", key);
                    field = &dummy;
            }
            readField(*field, fp);
        }
        throw  io_exception("Unexpected end of file");
    }
    catch(...)
    {
        fclose(fp);
        throw;
    }
}

void BitFile::processData(FILE *fp, bool flip)
{
    byte t[4];
    size_t rsize;
    rsize = fread(t,1,4,fp);
    if (rsize != 4)
        throw  io_exception("Unexpected end of file");
    length=(t[0]<<24)+(t[1]<<16)+(t[2]<<8)+t[3];
    if(buffer)
        delete [] buffer;
    buffer=new byte[length];

    for(unsigned int i=0; i<length&&!feof(fp); i++)
    {
        byte b;
        rsize = fread(&b,1,1,fp);
	if (rsize != 1)
            throw  io_exception("Unexpected end of file");
        buffer[i]=(flip?bitRevTable[b]:b); // Reverse the bit order.
    }
    if(feof(fp))
        throw  io_exception("Unexpected end of file");

    rsize = fread(t,1,1,fp);
    if (rsize != 0)
        throw  io_exception("Expected end of file");

    if(!feof(fp))
        error("Ignoring extra data at end of file");
}

void BitFile::appendZeros(unsigned cnt)
{
    size_t i;
    size_t const  nlen = length + cnt;
    byte  *const  nbuf = new byte[nlen];

    // copy old part
    for(i = 0; i < length; i++)
        nbuf[i] = buffer[i];
    delete [] buffer;
    buffer = nbuf;

    // append new contents
    for(i = length; i < nlen; i += 4)
    {
        buffer[i] = 0;
    }
    length = nlen;
}

void BitFile::append(unsigned long val, unsigned cnt)
{
    size_t i;
    size_t const  nlen = length + 4*cnt;
    byte  *const  nbuf = new byte[nlen];

    // copy old part
    for(i = 0; i < length; i++)
        nbuf[i] = buffer[i];
    delete [] buffer;
    buffer = nbuf;

    // append new contents
    for(i = length; i < nlen; i += 4)
    {
        buffer[i+0] = bitRevTable[0xFF & (val >> 24)];
        buffer[i+1] = bitRevTable[0xFF & (val >> 16)];
        buffer[i+2] = bitRevTable[0xFF & (val >>  8)];
        buffer[i+3] = bitRevTable[0xFF & (val >>  0)];
    }
    length = nlen;
}

void BitFile::append(char const *fname, bool flip)
{
    size_t rsize;
    FILE *const  fp=fopen(fname,"rb");
    if(!fp)
        throw  io_exception(std::string("Cannot open file ") + fname);

    try
    {
        struct stat  stats;
        stat(fname, &stats);
        size_t i;

        size_t const  nlen = length + stats.st_size;
        byte  *const  nbuf = new byte[nlen];

        // copy old part
        for(i = 0; i < length; i++)
            nbuf[i] = buffer[i];
        delete [] buffer;
        buffer = nbuf;

        // append new contents
        for(i = length; i < nlen; i++)
        {
            if(feof(fp))
                throw  io_exception("Unexpected end of file");
            byte  b;
            rsize = fread(&b, 1, 1, fp);
            if (rsize != 1)
                throw  io_exception("Unexpected end of file");
            buffer[i]=(flip?bitRevTable[b]:b); // Reverse the bit order.
        }
        length = nlen;

        fclose(fp);
    }
    catch(...)
    {
        fclose(fp);
        throw;
    }
}

void BitFile::setLength(unsigned int size)
{
    length = (size+7)>>3;
    if(buffer)
        delete [] buffer;
    buffer=new byte[length];
}

unsigned long BitFile::saveAs(int style, const char  *device, const char *fname)
{
    if(length<=0)
        return length;
    unsigned int clip;
    /* Don't store 0xff bytes from the end of the flash */
    for(clip=length-1; (buffer[clip] == 0xff) && clip>0; clip--){};
    FILE *fp=fopen(fname,"wb");
    if(fp == 0)
    {
        printf("Unable to open %s: %s\n", fname, strerror(errno));
        fclose(fp);
        return 0;
    }
    if(style != 0)
    {
        printf("Bitfile Style not jet implemented\n");
        return 0;
    }
    for(unsigned int i=0; i<clip; i++)
    {
        byte b=bitRevTable[buffer[i]]; // Reverse bit order
        fwrite(&b,1,1,fp);
    }
    fclose(fp);
    return clip;
}

void BitFile::error(const string &str)
{
    errorStr=str;
    Error=true;
    fprintf(logfile,"%s\n",str.c_str());
}

void BitFile::readField(string &field, FILE *fp)
{
    size_t rsize;
    byte t[2];
    rsize = fread(t,1,2,fp);
    if (rsize != 2)
        throw  io_exception("Unexpected end of file");
    unsigned short len=(t[0]<<8)+t[1];
    for(int i=0; i<len; i++)
    {
        byte b;
        rsize = fread(&b,1,1,fp);
        if (rsize != 1)
            throw  io_exception("1 Unexpected end of file");
        field+=(char)b;
    }
}

void BitFile::initFlip()
{
  for(int i=0; i<256; i++){
    int num=i;
    int fnum=0;
    for(int k=0; k<8; k++){
      int bit=num&1;
      num=num>>1;
      fnum=(fnum<<1)+bit;
    }
    bitRevTable[i]=fnum;
  }
}


BitFile::~BitFile()
{
  if(buffer)
    delete [] buffer;
}

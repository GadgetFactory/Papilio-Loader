/* AT45DB SPI JTAG programming algorithms

Copyright (C) 2010 Jochem Govers

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

*/



#ifndef PROGALGSPI_H
#define PROGALGSPI_H

#include "bitfile.h"
#include "jtag.h"
#include "iobase.h"
#include "tools.h"

#define SSTFLASH 1
#define MacronixFLASH 2
#define GENERIC 3

class ProgAlgSpi
{
    private:
        byte JPROGRAM;
        byte BYPASS;
        byte USER1;
        byte IDCODE;
        unsigned int SectorSize;//in bytes
        unsigned int PageSize;//in bytes
        unsigned int Pages;//Total number of pages
        int SpiAddressShift;//how many times is the address shifted
        unsigned int Max_Retries;
		int FlashType;
        int tP;// Page Programming Time (256/264 bytes) 2 4 ms
        int tPE;// Page Erase Time (256/264 bytes) 13 32 ms
		int tCE;// Chip Erase Time 50 ms
		int MacronixtCE;// Macronix Chip Erase Time 50 s
		int tBP;// Time to program a byte on SST. 10 us
        unsigned int BulkErase; // Max time in seconds to do a chip erase
        unsigned int SectorErase; // Max time in seconds to do a sector erase

        Jtag *jtag;
        IOBase *io;
        int family;

        void flow_array_program(BitFile &file);
        void flow_program_legacy(BitFile &file);
        void Spi_Command(const byte *tdi, byte *tdo, int length);
        bool Spi_Check(bool verbose=false);
		bool Spi_Write_Check(bool verbose=false);
        bool Spi_Identify(bool verbose=false);
        bool Spi_Erase(bool verbose=false);
        bool Spi_PartialErase(int length, bool verbose=false);
        bool Spi_Write(const byte *write_data, int length, bool verbose=false);
        bool Spi_Verify(const byte *verify_data, int length, bool verbose);
        void Spi_SetCommand(const byte *command, byte *data, const int bytes);
        void Spi_SetCommandRW(const byte command, byte *data, const int address);
    public:
        enum Spi_Options_t
        {
            ERASE_ONLY,
            VERIFY_ONLY,
            WRITE_ONLY,
            FULL
        };
        ProgAlgSpi(Jtag &j, IOBase &i, int family);
        bool ProgramSpi(BitFile &file, Spi_Options_t options);
        bool EraseSpi();
};


#endif

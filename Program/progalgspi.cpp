/* AT45DB JTAG programming algorithms

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
1   Added support for all AT45DB devices..
*/

#include "ProgAlgSpi.h"
#include <windows.h>
#include <time.h>



ProgAlgSpi::ProgAlgSpi(Jtag &j, IOBase &i, int fam)
{
    jtag=&j;
    io=&i;
    family = fam;

    PageSize=264;
    Pages=1024;
    tPE=10;
    tP=4;
    Max_Retries=4;
    SpiAddressShift=9;

    JPROGRAM=0x0b;
    BYPASS=0x3f;
    USER1=0x02;
    IDCODE=0x09;
}

void ProgAlgSpi::Spi_Command(const byte *tdi, byte *tdo, int length)
{
    byte *spi_out;
    byte *spi_in;
    int bytes=(length+7)/8;//bytes in
    // tdo comes 8 clock later as expected..
    int bytes_s=bytes+1+4;//1 byte post + 4 bytes pre
    int i;

    if(tdo)
    {
        spi_out=(byte*)malloc(sizeof(byte)*bytes_s);
        memset(spi_out,0,bytes_s);
    }
    spi_in=(byte*)malloc(sizeof(byte)*bytes_s);
    memset(spi_in,0,bytes_s);

    spi_in[0]=0x59;
    spi_in[1]=0xa6;
    spi_in[2]=(length)>>8;//bit count (bits 15:8)
    spi_in[3]=(length&0xff);//bit count (bits 7:0)

    for(i=0;i<4;i++)
        spi_in[i]=bRevTable[spi_in[i]];

    for(i=0;i<bytes;i++)
        spi_in[i+4]=bRevTable[tdi[i]];

    jtag->shiftDR(spi_in,(tdo?spi_out:0),8*bytes_s);

    if(tdo)
    {
        for(i=0;i<bytes;i++)
            tdo[i]=bRevTable[spi_out[i+5]];
    }

    free(spi_in);
    if(tdo)
        free(spi_out);
}

void ProgAlgSpi::Spi_SetCommand(const byte *command, byte *data, const int bytes)
{
    int i;
    for(i=0;i<bytes;i++)
        data[i]=command[i];
}
void ProgAlgSpi::Spi_SetCommandRW(const byte command, byte *data, const int address)
{
    // command length is 4 bytes...
    // 10 address bits
    // cccc cccc xxxx xppp pppp pppb bbbb bbbb
    // c=command bit
    // x=dont care
    // p=page address
    // b=byte address
    byte tmp[4];
    memset(tmp, 0, sizeof(tmp));
    int na=(address << SpiAddressShift);
    int i;
    tmp[0]=command;
    tmp[1]=(na&0x70000)>>16;
    tmp[2]=(na&0xfe00)>>8;
    tmp[3]=0;

    for(i=0;i<4;i++)
        data[i]=tmp[i];

}

bool ProgAlgSpi::Spi_Identify(bool verbose)
{
    byte tdo[5];
    byte Cmd[5]={0x9f,0x0,0x0,0x0,0x0};

    Spi_Command(Cmd,tdo,40);

    switch(tdo[1])
    {
        case 0x1f: /* Atmel */
            switch(tdo[2]&0x1f)
            {
                case 0x2: /* AT45DB011 */
                    Pages=512;
                    PageSize=264;
                    break;
                case 0x3: /* AT45DB021 */
                    Pages=1024;
                    PageSize=264;
                    break;
                case 0x4: /* AT45DB041 */
                    Pages=2048;
                    PageSize=264;
                    break;
                case 0x5: /* AT45DB081 */
                    Pages=4096;
                    PageSize=264;
                    break;
                case 0x6: /* AT45DB161 */
                    Pages=4096;
                    PageSize=528;
                    break;
                case 0x7: /* AT45DB321 */
                    Pages=8192;
                    PageSize=528;
                    break;
                case 0x8: /* AT45DB641 */
                    Pages=8192;
                    PageSize=1056;
                    break;
                default:
                    printf("Uknown Flash Size\n");
                    return false;
            }
            if(verbose)
                printf("Found Atmel Flash (Pages=%d, Page Size=%d bytes, %d bits).\n",Pages,PageSize,Pages*PageSize*8);
            break;
        case 0x20: /* Numonyx */
        case 0xef: /* Winbond */
        default:
            printf("Uknown Flash Manufacturer\n");
            return false;
    }

    if(tdo[3] || tdo[4])
    {
        /* Unexpected, but no hard error? */
    }

    return true;
}

bool ProgAlgSpi::Spi_Check(bool verbose)
{
    byte tdo[4];
    byte StatusReg_Mask=0xbd;// Ready and static bits ok
    byte StatusReg_Val=0x94;
    byte StatusReg_Cmd[2]={0xd7,0x0};

    byte testi[8]={"\x9f\x0\x0\x0\x0"};
    byte testo[8];
    Spi_Command(testi,testo,40);

    Spi_Command(StatusReg_Cmd, tdo, 16);
    if((tdo[1]&StatusReg_Mask) != StatusReg_Val)
    {
        if(verbose)
            printf("Error: SPI Status Register [0x%02X] mismatch (Wrong device or device not ready)..\n",tdo[1]);
        return false;
    }
    return true;
}

bool ProgAlgSpi::Spi_Erase(bool verbose)
{
    int i,x;
    bool fail=false;
    byte data[4];

    if(verbose)
        printf("Erasing    :");
    for(i=0;i<Pages&&!fail;i++)
    {
        memset(data,0, sizeof(data));
        Spi_SetCommandRW('\x81',data,i);

        Spi_Command(data,0,32);
        for(x=0;x<=Max_Retries;x++)
        {
            fail=!Spi_Check();
            if(!fail)
                break;
            Sleep(tPE);
        }
        if((i%64)==0&&verbose)
            printf(".");
    }

    if(verbose)
    {
        if(!fail)
            printf("Ok\n");
        else
            printf("Failed (@ Page: %d)\n", i);
    }

    return !fail;
}

bool ProgAlgSpi::Spi_Write(const byte *write_data, int length, bool verbose)
{
    int i,x;
    bool fail=false;
    byte *data;
    int wBytes=(length/8)+((length%8)?(8-(length%8)):0);
    int bufsize=sizeof(byte)*(PageSize+4);
    int DoPages=wBytes/PageSize;

    data=(byte*)malloc(bufsize);

    // full Pages
    if(verbose)
        printf("Pogramming :");
    for(i=0;i<DoPages&&!fail;i++)
    {
        memset(data, 0, bufsize);
        Spi_SetCommand((byte*)"\x84",data,1);

        memcpy(&data[4], &write_data[PageSize*i],PageSize);
        Spi_Command(data,0, 8*(bufsize));

        // Write buffer to mem
        memset(data, 0, bufsize);
        Spi_SetCommandRW('\x88',data,i);

        Spi_Command(data,0,4*8);
        for(x=0;x<=Max_Retries;x++)
        {
            fail=!Spi_Check();
            if(!fail)
                break;
            Sleep(tP);
        }
        if((i%64)==0&&verbose)
            printf(".");

    }

    // partial Page
    if(!fail&&(DoPages*PageSize)<wBytes)
    {
        int remBytes=(wBytes-DoPages*PageSize);
        memset(data, 0, bufsize);
        Spi_SetCommand((byte*)"\x84",data,1);

        memcpy(&data[4], &write_data[PageSize*DoPages],remBytes);
        Spi_Command(data,0, 8*(bufsize));

        // Write buffer to mem
        memset(data, 0, bufsize);
        Spi_SetCommandRW('\x88',data,DoPages);


        Spi_Command(data,0,4*8);
        for(x=0;x<=Max_Retries;x++)
        {
            fail=!Spi_Check();
            if(!fail)
                break;
            Sleep(tP);
        }
    }

    if(verbose)
    {
        if(!fail)
            printf("Ok\n");
        else
            printf("Failed (@ Page: %d)\n", i);
    }
    return !fail;
}

bool ProgAlgSpi::Spi_Verify(const byte *verify_data, int length, bool verbose)
{
    int i;
    bool fail=false;
    byte *data;
    byte *tdo;
    int wBytes=(length/8)+((length%8)?(8-(length%8)):0);
    int bufsize=sizeof(byte)*(PageSize+4);
    int DoPages=wBytes/PageSize;

    data=(byte*)malloc(bufsize);
    tdo=(byte*)malloc(bufsize);

    // full Pages
    if(verbose)
        printf("Verifying  :");
    for(i=0;i<DoPages&&!fail;i++)
    {
        // Read from mem
        memset(data, 0, bufsize);
        memset(tdo, 0, bufsize);
        Spi_SetCommandRW('\x03',data,i);

        Spi_Command(data,tdo,(bufsize)*8);
        if(memcmp(&tdo[4],&verify_data[i*PageSize],PageSize))
            fail=true;
        if((i%64)==0&&verbose)
            printf(".");
    }

    // partial Page
    if(!fail&&(DoPages*PageSize)<wBytes)
    {
        int remBytes=(wBytes-DoPages*PageSize);
        // Read from mem
        memset(data, 0, bufsize);
        memset(tdo, 0, bufsize);
        Spi_SetCommandRW('\x03',data,DoPages);

        Spi_Command(data,tdo,(bufsize)*8);
        if(memcmp(&tdo[4],&verify_data[DoPages*PageSize],remBytes))
            fail=true;
    }

    if(verbose)
    {
        if(!fail)
            printf("Pass\n");
        else
            printf("Failed (@ Page: %d)\n", i);
    }

    return !fail;
}

bool ProgAlgSpi::EraseSpi()
{
    struct timeval tv[2];
    bool verbose=io->getVerbose();
    bool res;
    gettimeofday(tv, NULL);
    // Switch to USER1 register, to access SPI FLash..
    jtag->shiftIR(&USER1,0);

    // Check Status and Device
    if(!Spi_Check(true))
        return false;

    if(!Spi_Erase(verbose))
        return false;
    byte *empty;
    int emptylen=PageSize*Pages;
    empty=(byte*)malloc(emptylen);
    memset(empty, 0xff, emptylen);
    res=Spi_Verify(empty, 8*emptylen, verbose);
    free(empty);
    if(!res)
        return false;


    jtag->shiftIR(&BYPASS);

    if (verbose)
    {
        gettimeofday(tv+1, NULL);
        printf("Done.\nSPI execution time %.1f ms\n", (double)deltaT(tv, tv + 1)/1.0e3);
    }

  return true;
}

bool ProgAlgSpi::ProgramSpi(BitFile &file, Spi_Options_t options)
{
    struct timeval tv[2];
    bool verbose=io->getVerbose();
    bool res;
    gettimeofday(tv, NULL);
    // Switch to USER1 register, to access SPI FLash..
    jtag->shiftIR(&USER1,0);

    // Check Status and Device
    if(!Spi_Identify(true) && !Spi_Check(true))
        return false;

    // do some sanity checking
    if(Pages*PageSize*8 < file.getLength())
    {
        printf("Bit file does not fit into Flash memory.\n");
        return false;
    }

    if(options==FULL||options==ERASE_ONLY)
    {
        if(!Spi_Erase(verbose))
            return false;
        byte *empty;
        int emptylen=PageSize*Pages;
        empty=(byte*)malloc(emptylen);
        memset(empty, 0xff, emptylen);
        res=Spi_Verify(empty, 8*emptylen, verbose);
        free(empty);
        if(!res)
            return false;

    }


    if(options==FULL||options==WRITE_ONLY)
        if(!Spi_Write(file.getData(), file.getLength(), verbose))
            return false;

    if(options==FULL||options==VERIFY_ONLY)
        if(!Spi_Verify(file.getData(), file.getLength(), verbose))
            return false;


    /* JPROGAM: Trigerr reconfiguration, not explained in ug332, but
     DS099 Figure 28:  Boundary-Scan Configuration Flow Diagram (p.49) */
    if(options==FULL)
    {
        jtag->shiftIR(&JPROGRAM);
        Sleep(1000);//just wait a bit to make sure everything is done..
    }

    jtag->shiftIR(&BYPASS);

    if (verbose)
    {
        gettimeofday(tv+1, NULL);
        printf("Done.\nSPI execution time %.1f ms\n", (double)deltaT(tv, tv + 1)/1.0e3);
    }

  return true;
}

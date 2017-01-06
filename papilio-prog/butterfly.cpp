/* JTAG programming tools for Butterfly One and others
   from http://www.gadgetfactory.net/gf

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

Mike Field [hamster@snap.net.nz] 15 Oct 2012
    Add commandline arg to Allow binary data to be appended after
    the FPGA bitstream.  Can be used to put data into flash.

*/



// C standard libraries
#include <stdio.h> // fprintf() printf() stderr
#include <stdlib.h>  // exit()
#include <string.h> // strlen() strcpy()


// C POSIX
#include <unistd.h> // getopt()

// C++ standard libraries
#include <iostream>
#include <memory>



#include "io_exception.h"
#include "ioftdi.h"
#include "jtag.h"
#include "devicedb.h"
#include "progalgxc3s.h"
#include "progalgspi.h"
#include "bitfile.h"


unsigned int get_id(Jtag &jtag, DeviceDB &db, int chainpos, bool verbose)
{
    int num=jtag.getChain();
    unsigned int id;

    // Make sure we found at least one JTAG device in the chain
    if (num == 0)
    {
      fprintf(stderr, "No JTAG device found.\n");
      return 0;
    }
    
    // Synchronise database with chain of devices.
    for(int i=0; i<num; i++)
    {
        int length=db.loadDevice(jtag.getDeviceID(i));
        if(length>0)
            jtag.setDeviceIRLength(i,length);
        else
        {
            id=jtag.getDeviceID(i);
            fprintf(stderr,"Cannot find device having IDCODE=%08x\n",id);
            return 0;
        }
    }

    if(jtag.selectDevice(chainpos)<0)
    {
        fprintf(stderr,"Invalid chain position %d, position must be less than %d (but not less than 0).\n",chainpos,num);
        return 0;
    }

    const char *dd=db.getDeviceDescription(chainpos);
    id = jtag.getDeviceID(chainpos);
    if (verbose)
    {
        printf("JTAG chainpos: %d Device IDCODE = 0x%08x\tDesc: %s\n", chainpos,id, dd);
        fflush(stdout);
    }
    return id;
}

void usage(char *name)
{
    fprintf(stderr,
      "\nUsage:\%s [-v] [-j] [-f <bitfile>] [-b <bitfile>] [-s e|v|p|a] [-c] [-C] [-r] [-A <addr>:<binfile>]\n"
      "   -h\t\t\tprint this help\n"
      "   -v\t\t\tverbose output\n"
      "   -j\t\t\tDetect JTAG chain, nothing else\n"
      "   -d\t\t\tFTDI device name\n"
      "   -f <bitfile>\t\tMain bit file\n"
      "   -b <bitfile>\t\tbscan_spi bit file (enables spi access via JTAG)\n"
      "   -s [e|v|p|a]\t\tSPI Flash options: e=Erase Only, v=Verify Only,\n"
      "               \t\tp=Program Only or a=ALL (Default)\n"
      "   -c\t\t\tDisplay current status of FPGA\n"
      "   -C\t\t\tDisplay STAT Register of FPGA\n"
      "   -r\t\t\tTrigger a reconfiguration of FPGA\n"
      "   -a <addr>:<binfile>\tAppend binary file at addr (in hex)\n"
      "   -A <addr>:<binfile>\tAppend binary file at addr, bit reversed\n",name);
    exit(-1);
}

int append_data(BitFile &fpga_bit, char *append_str, bool flip, int verbose)
{
    int addr = 0, padding;
    while(1)
    {
        char c = *append_str;
        append_str++;

        if(c == ':') break;
        else if(c >= '0' && c <= '9') addr = addr * 16 + c - '0';
        else if(c >= 'A' && c <= 'F') addr = addr * 16 + c - 'A'+10;
        else if(c >= 'a' && c <= 'f') addr = addr * 16 + c - 'a'+10;
        else {
            printf("Invalid address for appending data\n");
            return 0;
        }
    }
    padding = addr - fpga_bit.getLength()/8;
    if(padding < 0) { 
        printf("Aborting - Appended data would overwrite FPGA bitstream\n");
        return 0;
    } 
    if(verbose)
        printf("Appending file %s at address %X\n",append_str,addr);

    if(padding > 0) 
        fpga_bit.appendZeros(padding);
    fpga_bit.append(append_str,flip);
    if(verbose)
        printf("Final Length is %lx\n",fpga_bit.getLength()/8);
    return 1;
}

int main(int argc, char **argv)
{
    int chainpos = 0;
    int vendor = 0;
    int product = 0;
    unsigned int id;
    bool verbose = false;
    bool spiflash = false;
    bool reconfigure = false;
    bool detectchain = false;
    int displaystatus = 0; // 0=no status, 1=JTAG IR data, 2=STAT Register readback
    bool result;
    char *desc = 0;
    char const *serial = 0;
    int subtype = FTDI_NO_EN;
    char *devicedb = NULL;
    int c;
    char *cFpga_fn=0;
    char *cBscan_fn=0;
    char *append_str = 0;
    bool append_flip = true;
    ProgAlgSpi::Spi_Options_t spi_options=ProgAlgSpi::FULL;
    DeviceDB db(devicedb);

    std::auto_ptr<IOBase>  io;


    while ((c = getopt (argc, argv, "hd:b:f:s:A:a:jvcCr")) != EOF)
        switch (c)
        {
        case 'r':
            reconfigure=true;
            break;
        case 'C':
            displaystatus=2;
            break;
        case 'c':
            displaystatus=1;
            break;
        case 'v':
            verbose=true;
            break;
        case 'j':
            detectchain=true;
            break;
        case 'd':
            desc=(char*)malloc(strlen(optarg)+1);
            strcpy(desc,optarg);
            break;
        case 'f':
            cFpga_fn=(char*)malloc(strlen(optarg)+1);
            strcpy(cFpga_fn,optarg);
            break;
        case 'A':
            append_str=(char*)malloc(strlen(optarg)+1);
            strcpy(append_str,optarg);
            append_flip = true;
            break;
        case 'a':
            append_str=(char*)malloc(strlen(optarg)+1);
            strcpy(append_str,optarg);
            append_flip = false;
            break;
        case 'b':
            cBscan_fn=(char*)malloc(strlen(optarg)+1);
            strcpy(cBscan_fn,optarg);
            break;
        case 's':
            switch(optarg[0])
            {
                case 'e':
                case 'E':
                    spi_options=ProgAlgSpi::ERASE_ONLY;
                    break;
                case 'p':
                case 'P':
                    spi_options=ProgAlgSpi::WRITE_ONLY;
                    break;
                case 'v':
                case 'V':
                    spi_options=ProgAlgSpi::VERIFY_ONLY;
                    break;
                case 'a':
                case 'A':
                    spi_options=ProgAlgSpi::FULL;
                    break;
                default:
                    printf("Unknown argument: \"%c\" to option: \"%c\"\n",c, optarg[0]);
                    usage(argv[0]);
            }
            break;
        case '?':
            if (optopt == 'i')
                fprintf (stderr, "Option -%c requires an argument.\n", optopt);
            else if (isprint (optopt))
                fprintf (stderr, "Unknown option `-%c'.\n", optopt);
            else
                fprintf (stderr,
                         "Unknown option character `\\x%x'.\n",
                         optopt);
            usage(argv[0]);
            return 1;
        default:
            usage(argv[0]);
        }

    if(cBscan_fn)
    {
        // Erase only does not need any main fpga bit file only bscan_spi
        spiflash=true;
        if(spi_options!=ProgAlgSpi::ERASE_ONLY&&!cFpga_fn)
        {
            printf("Please specify main bit file (-f <bitfile>)\n");
            return 1;
        }

    }
    else if( !cFpga_fn && !displaystatus && !detectchain && !reconfigure)
    {
        //no option specified
        printf("No or ambiguous options specified.\n");
        usage(argv[0]);
    }
    else
    {
        //nothing todo here..
    }

    try
    {
        if (vendor == 0)
            vendor = VENDOR;
        if(product == 0)
            product = DEVICE;
        io.reset(new IOFtdi(vendor, product, desc, serial, subtype));
        io->setVerbose(verbose);
    }
    catch(io_exception& e)
    {
	//Try the Papilio DUO before failing
	try
	{
		io.reset(new IOFtdi(vendor, 0x7bc0, desc, serial, subtype));
		io->setVerbose(verbose);
	}
	catch(io_exception& e2)
	{
 		fprintf(stderr, "Could not access USB device %04x:%04x."
		  " If this is linux then make sure you can access the "
		  " device or use sudo.\n",vendor, product);
        	return 1;
	}
    }

    Jtag jtag = Jtag(io.operator->());
    unsigned int family, manufacturer;
    fprintf(stderr, "Using %s\n", db.getFile().c_str());

    id = get_id (jtag, db, chainpos, true);
    if (id == 0)
      return 1;
    family = (id>>21) & 0x7f;
    manufacturer = (id>>1) & 0x3ff;
    if(detectchain)
        return 0;


    ProgAlgXC3S alg(jtag,io.operator*(), family);
    //alg.getStatusRegister();

    if(displaystatus)
    {
        if(displaystatus==1)
            alg.DisplayStatus();
        else if(displaystatus==2)
            alg.getStatusRegister();
        return 0;
    }
    try
    {
        if(spiflash)
        {
            BitFile fpga_bit;
            fpga_bit.readFile(cBscan_fn);
            //fpga_bit.print();
        
            printf("\nUploading \"%s\". ", cBscan_fn);
            alg.array_program(fpga_bit);

            BitFile flash_bit;

            ProgAlgSpi alg1(jtag,io.operator*(), 0);

            if(spi_options!=ProgAlgSpi::ERASE_ONLY)
            {

                flash_bit.readFile(cFpga_fn, false);

                if(append_str && !append_data(flash_bit, append_str,append_flip, verbose)) /* Try to append data */
                    return 1;
                
                //flash_file.print();
                printf("\nProgramming External Flash Memory with \"%s\".\n", cFpga_fn);
                result=alg1.ProgramSpi(flash_bit, spi_options);
                if (reconfigure)
                {
                  alg.Reconfigure();
                }
            }
            else
            {
                printf("Erasing External Flash Memory.\n");
                result=alg1.EraseSpi();
            }

            if(!result)
                printf("Error occured.\n");
        }
        else
        {
            if(reconfigure)
            {
                printf("Triggering a reconfiguration of the FPGA.\n");
                alg.Reconfigure();
                return 0;
            }
            BitFile fpga_bit;
            fpga_bit.readFile(cFpga_fn);

            if(append_str && !append_data(fpga_bit, append_str, append_flip, verbose)) /* Try to append data */
                return 1;

            fpga_bit.print();

            printf("\nUploading \"%s\". ", cFpga_fn);
            alg.array_program(fpga_bit);
            return 0;
        }
    }
    catch(io_exception& e)
    {
        fprintf(stderr, "IOException: %s\n", e.getMessage().c_str());
        return  1;
    }
}

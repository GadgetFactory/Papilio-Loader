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
10/15/2010 Added support for SST25VF040B Jack Gassett
10/17/2012 Added support for Micron and Winbond devices Magnus Karlsson
*/

#include "config.h"

#ifdef WINDOWS
	#include <windows.h>
#else
  #define Sleep(ms) usleep((ms * 1000))
#endif

#include <sys/time.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>

#include "progalgspi.h"



ProgAlgSpi::ProgAlgSpi(Jtag &j, IOBase &i, int fam)
{
    jtag=&j;
    io=&i;
    family = fam;

    SectorSize=65536;
    PageSize=264;
    Pages=1024;
    tPE=10;
    tP=4;
    tCE=50;
    MacronixtCE=1000;
    tBP=10;
    BulkErase=100;
    SectorErase=4;
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
    else
    	spi_out = NULL;
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
	int na, i;
    memset(tmp, 0, sizeof(tmp));
	if ((FlashType==SSTFLASH) || (FlashType==MacronixFLASH) || (FlashType==GENERIC))
	{
		na=address;
		tmp[0]=command;
		tmp[1]=(na&0xff0000)>>16;
		tmp[2]=(na&0xff00)>>8;
		tmp[3]=(na&0xff);		
	}
	else
	{
		na=(address << SpiAddressShift);
		tmp[0]=command;
		tmp[1]=(na&0x70000)>>16;
		tmp[2]=(na&0xfe00)>>8;
		tmp[3]=0;
	}

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
                    printf("Unknown Atmel Flash Size (0x%.2x)\n", (tdo[2]&0x1f));
                    return false;
            }
            if(verbose)
                printf("Found Atmel Flash (Pages=%d, Page Size=%d bytes, %d bits).\n",Pages,PageSize,Pages*PageSize*8);
            break;
        case 0xc2: /* Macronix */
            switch(tdo[3])
            {
                case 0x17: /* MX25L6445E */
                    Pages=32768;
                    PageSize=256;
                    FlashType=MacronixFLASH;
                    break;
                default:
                    printf("Unknown Macronix Flash Size (0x%.2x)\n", tdo[3]);
                    return false;
            }
            if(verbose)
                printf("Found Macronix Flash (Pages=%d, Page Size=%d bytes, %d bits).\n",Pages,PageSize,Pages*PageSize*8);
            break; 
        case 0x20: /* Numonyx/Micron */
            if (tdo[2] == 0xBA) { //N25QXXX
                switch(tdo[3])
                {
                    case 0x16: /* N25Q32 */
                        Pages=16384;
                        PageSize=256;
                        BulkErase=60;
                        SectorErase=3;
                        FlashType=GENERIC;
                        break;
                    case 0x17: /* N25Q64 */
                        Pages=32768;
                        PageSize=256;
                        BulkErase=250;
                        SectorErase=3;
                        FlashType=GENERIC;
                        break;
                    case 0x18: /* N25Q128 */
                        Pages=65536;
                        PageSize=256;
                        BulkErase=250;
                        SectorErase=3;
                        FlashType=GENERIC;
                        break;
                    case 0x19: /* N25Q256 */
                        Pages=131072;
                        PageSize=256;
                        BulkErase=480;
                        SectorErase=3;
                        FlashType=GENERIC;
                        break;
                    default:
                        printf("Unknown Numonyx/Micron N25Q Flash Size (0x%.2x)\n", tdo[3]);
                        return false;
                }
                if(verbose)
                    printf("Found Numonyx/Micron Flash (Pages=%d, Page Size=%d bytes, %d bits).\n",Pages,PageSize,Pages*PageSize*8);
                break;
            } if (tdo[2] == 0x20) { //N25PXXX
                switch(tdo[3])
                {
                    case 0x14: /* N25P80 */
                        Pages=4096;
                        PageSize=256;
                        BulkErase=20;
                        SectorErase=3;
                        FlashType=GENERIC;
                        break;
                    default:
                        printf("Unknown Numonyx/Micron N25P Flash Size (0x%.2x)\n", tdo[3]);
                        return false;
                }
                if(verbose)
                  printf("Found Numonyx/Micron Flash (Pages=%d, Page Size=%d bytes, %d bits).\n",Pages,PageSize,Pages*PageSize*8);
                break;
            } else {
                printf("Unknown Numonyx/Micron Flash Type (0x%.2x)\n", tdo[2]);
                return false;
            }
            break;
        case 0xef: /* Winbond */
            if (tdo[2] == 0x30) { //W25X
                switch(tdo[3])
                {
                    case 0x13: /* W25X40 */
                        Pages=2048;
                        PageSize=256;
                        BulkErase=4;
                        SectorErase=1;
                        FlashType=GENERIC;
                        break;
                    default:
                        printf("Unknown Winbond W25X Flash Size (0x%.2x)\n", tdo[3]);
                        return false;
                }
                if(verbose)
                    printf("Found Winbond Flash (Pages=%d, Page Size=%d bytes, %d bits).\n",Pages,PageSize,Pages*PageSize*8);
                break;
            } else if (tdo[2] == 0x40) { //W25Q
                switch(tdo[3])
                {
                    case 0x14: /* W25Q80 */
                        Pages=4096;
                        PageSize=256;
                        BulkErase=6;
                        SectorErase=1;
                        FlashType=GENERIC;
                        break;
                    case 0x15: /* W25Q16 */
                        Pages=8192;
                        PageSize=256;
                        BulkErase=10;
                        SectorErase=1;
                        FlashType=GENERIC;
                        break;
                    default:
                        printf("Unknown Winbond W25Q Flash Size (0x%.2x)\n", tdo[3]);
                        return false;
                }
                if(verbose)
                    printf("Found Winbond Flash (Pages=%d, Page Size=%d bytes, %d bits).\n",Pages,PageSize,Pages*PageSize*8);
                break;
            } else {
                printf("Unknown Winbond Flash Type (0x%.2x)\n", tdo[2]);
                return false;
            }
            break;
        case 0xbf: /* SST */
            switch(tdo[3])
            {
                case 0x8d: /* SST25VF040B */
                    Pages=2048;
                    PageSize=264;
                    FlashType=SSTFLASH;
                    break;
                case 0x8e: /* SST25VF080B */
                    Pages=4096;
                    PageSize=264;
                    FlashType=SSTFLASH;
                    break;
                case 0x41: /* SST25VF016B */
                    Pages=8192;
                    PageSize=264;
                    FlashType=SSTFLASH;
                    break;
                case 0x4a: /* SST25VF032B */
                    Pages=16384;
                    PageSize=264;
                    FlashType=SSTFLASH;
                    break;
                case 0x4b: /* SST25VF064C */
                    Pages=32768;
                    PageSize=264;
                    FlashType=SSTFLASH;
                    break;
                default:
                    printf("Unknown SST Flash Size (0x%.2x)\n", tdo[3]);
                    return false;
            }
            if(verbose)
                printf("Found SST Flash (Pages=%d, Page Size=%d bytes, %d bits).\n",Pages,PageSize,Pages*PageSize*8);
            break;			
        default:
            printf("Uknown Flash Manufacturer (0x%.2x)\n", tdo[1]);
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
    byte StatusReg_Mask=0x83;// Ready and static bits ok
    byte StatusReg_Val=0x80;
    byte StatusReg_Cmd[2]={0xd7,0x0};

	if ((FlashType==SSTFLASH) || (FlashType==MacronixFLASH) || (FlashType==GENERIC))
	{
		StatusReg_Mask=0x01;
		StatusReg_Val=0x00;
		StatusReg_Cmd[0]=0x05;
		StatusReg_Cmd[1]=0x0;
	}

	Spi_Command(StatusReg_Cmd, tdo, 16);
		if((tdo[1]&StatusReg_Mask) != StatusReg_Val)
		{
			if(verbose)
				printf("Error: SPI Status Register [0x%02X] mismatch (Wrong device or device not ready)..\n",tdo[1]);
			return false;
		}
    return true;
}

bool ProgAlgSpi::Spi_Write_Check(bool verbose)
{
    byte tdo[4];
    byte StatusReg_Mask=0x83;// Ready and static bits ok
    byte StatusReg_Val=0x80;
    byte StatusReg_Cmd[2]={0xd7,0x0};

	if ((FlashType==SSTFLASH) || (FlashType==MacronixFLASH) || (FlashType==GENERIC))
	{
		StatusReg_Mask=0x1f;
		StatusReg_Val=0x02;
		StatusReg_Cmd[0]=0x05;
		StatusReg_Cmd[1]=0x0;
	}

	Spi_Command(StatusReg_Cmd, tdo, 16);
		if((tdo[1]&StatusReg_Mask) != StatusReg_Val)
		{
			if(verbose)
				printf("Error: SPI Write Check Status Register [0x%02X] mismatch (Wrong device or device not ready)..\n",tdo[1]);
			return false;
		}
    return true;
}

bool ProgAlgSpi::Spi_Erase(bool verbose)
{
	unsigned int i,x;
	bool fail=false;
	byte data[4];
	byte WRSR_Cmd[2]={0x01,0x00};

	if(verbose)
	{	
		printf("Erasing    :\n");
		fflush(stdout);
	}
	if (FlashType==SSTFLASH)
	{
		fail=!Spi_Write_Check();
		
		Spi_Command((byte*)"\x06",0,8);	//WREN
		Sleep(tCE);
		Spi_Command((byte*)"\x80",0,8);	//DBSY
		Sleep(tCE);
		Spi_Command((byte*)"\x50",0,8);	//EWSR
		Sleep(tCE);
		Spi_Command(WRSR_Cmd,0,16);		//WRSR
		Sleep(tCE);
		Spi_Command((byte*)"\x06",0,8);	//WREN
		Sleep(tCE);		
		for(x=0;x<=Max_Retries;x++)
		{
			fail=!Spi_Write_Check();
			if(fail==false)
				break;
			Sleep(tCE);
		}			
		
		Spi_Command((byte*)"\x60",0,8);	//Chip Erase
		for(x=0;x<=Max_Retries;x++)
		{
			fail=!Spi_Check();
			if(fail==false)
				break;
			Sleep(tCE);
		}	

		if(verbose)
		{
			if(fail==false)
				printf("Ok\n");
			else
				printf("Failed Erasing SST Flash.\n");
		}			
	}
	else if (FlashType==MacronixFLASH){

		Spi_Command((byte*)"\x06",0,7);	//Write Enable
		for(x=0;x<=Max_Retries;x++)
		{
			fail=!Spi_Write_Check(verbose);
			if(fail==false)
				break;
			Sleep(tCE);
		}	
		Spi_Command((byte*)"\x60",0,7);	//Chip Erase
		for(x=0;x<=100;x++)
		{
			fail=!Spi_Check();
			if(fail==false)
				break;
			Sleep(MacronixtCE);
			printf(".");
			fflush(stdout);
		}	

		if(verbose)
		{
			if(fail==false)
				printf("Ok\n");
			else
				printf("Failed Erasing Macronix Flash.\n");
		}			
	}
	else if (FlashType==GENERIC){
		Spi_Command((byte*)"\x06",0,7);	//Write Enable
		for(x=0;x<=Max_Retries;x++)
		{
			fail=!Spi_Write_Check(verbose);
			if(fail==false)
				break;
			Sleep(tCE);
		}

		Spi_Command((byte*)"\xc7",0,7);	//Bulk Erase
		for(x=0;x<=BulkErase;x++)
		{
			fail=!Spi_Check();
			if(fail==false)
				break;
			Sleep(1000);
			printf(".");
			fflush(stdout);
		}	

		if(verbose)
		{
			if(fail==false)
				printf("Ok\n");
			else
				printf("Failed Erasing Generic Flash.\n");
		}			
	}
	else
	{
		for(i=0;i<Pages&&!fail;i++)
		{
			memset(data,0, sizeof(data));
			Spi_SetCommandRW('\x81',data,i);

			Spi_Command(data,0,32);
			Sleep(tCE);		
			for(x=0;x<=Max_Retries;x++)
			{
				fail=!Spi_Write_Check(verbose);
				if(!fail)
					break;
				Sleep(tPE);
			}
			if((i%256)==0&&verbose)
			{
				printf(".");
				fflush(stdout);
			}
		}
		if(verbose)
		{
			if(!fail)
				printf("Ok\n");
			else
				printf("Failed (@ Page: %d)\n", i);
		}
	}

    return !fail;
}

bool ProgAlgSpi::Spi_PartialErase(int length, bool verbose)
{
	unsigned int i,x;
	bool fail=false;
	byte data[4];
	byte WRSR_Cmd[2]={0x01,0x00};

	if(verbose)
	{	
		printf("Erasing    :\n");
		fflush(stdout);
	}
	if (FlashType==SSTFLASH)
	{
    // use full chip erase
		fail=!Spi_Write_Check();
		
		Spi_Command((byte*)"\x06",0,8);	//WREN
		Sleep(tCE);
		Spi_Command((byte*)"\x80",0,8);	//DBSY
		Sleep(tCE);
		Spi_Command((byte*)"\x50",0,8);	//EWSR
		Sleep(tCE);
		Spi_Command(WRSR_Cmd,0,16);		//WRSR
		Sleep(tCE);
		Spi_Command((byte*)"\x06",0,8);	//WREN
		Sleep(tCE);		
		for(x=0;x<=Max_Retries;x++)
		{
			fail=!Spi_Write_Check();
			if(fail==false)
				break;
			Sleep(tCE);
		}			
		
		Spi_Command((byte*)"\x60",0,8);	//Chip Erase
		for(x=0;x<=Max_Retries;x++)
		{
			fail=!Spi_Check();
			if(fail==false)
				break;
			Sleep(tCE);
		}	

		if(verbose)
		{
			if(fail==false)
				printf("Ok\n");
			else
				printf("Failed Erasing SST Flash.\n");
		}			
	}
	/*else if (FlashType==MacronixFLASH)
  {
    // use full chip erase
		Spi_Command((byte*)"\x06",0,7);	//Write Enable
		for(x=0;x<=Max_Retries;x++)
		{
			fail=!Spi_Write_Check(verbose);
			if(fail==false)
				break;
			Sleep(tCE);
		}	
		Spi_Command((byte*)"\x60",0,7);	//Chip Erase
		for(x=0;x<=100;x++)
		{
			fail=!Spi_Check();
			if(fail==false)
				break;
			Sleep(MacronixtCE);
			printf(".");
			fflush(stdout);
		}	

		if(verbose)
		{
			if(fail==false)
				printf("Ok\n");
			else
				printf("Failed Erasing Macronix Flash.\n");
		}			
	} */
	else if ((FlashType==MacronixFLASH) || (FlashType==GENERIC))
  {
    printf("Doing Partial Erase\n");
    fflush(stdout);  
    // use partial erase
    unsigned int wBytes=(length+7)/8;
    unsigned int DoPages=(wBytes+PageSize-1)/PageSize;    
    for(i=0;i<DoPages&&!fail;i++)
    {
        if ((i*PageSize)%SectorSize == 0)
        {
            Spi_Command((byte*)"\x06",0,7);	//Write Enable
            for(x=0;x<=Max_Retries;x++)
            {
                fail=!Spi_Write_Check(verbose);
                if(fail==false)
                    break;
                Sleep(tCE);
            }
            Spi_SetCommandRW('\xd8',data,i*PageSize);	//Sector Erase
            Spi_Command(data,0,31);
            Sleep(tCE);		
            for(x=0;x<=SectorErase;x++)
            {
                fail=!Spi_Check();
                if(!fail)
                    break;
                Sleep(1000);
            }
        }
        if((i%256)==0&&verbose)
        {
            printf(".");
            fflush(stdout);
        }
    }        

		if(verbose)
		{
			if(fail==false)
				printf("Ok\n");
			else
				printf("Failed Erasing Generic Flash.\n");
		}			
	}
	else
	{
		for(i=0;i<Pages&&!fail;i++)
		{
			memset(data,0, sizeof(data));
			Spi_SetCommandRW('\x81',data,i);

			Spi_Command(data,0,32);
			Sleep(tCE);		
			for(x=0;x<=Max_Retries;x++)
			{
				fail=!Spi_Write_Check(verbose);
				if(!fail)
					break;
				Sleep(tPE);
			}
			if((i%256)==0&&verbose)
			{
				printf(".");
				fflush(stdout);
			}
		}
		if(verbose)
		{
			if(!fail)
				printf("Ok\n");
			else
				printf("Failed (@ Page: %d)\n", i);
		}
	}

    return !fail;
}

bool ProgAlgSpi::Spi_Write(const byte *write_data, int length, bool verbose)
{
    unsigned int i,x;
    bool fail=false;
    byte *data;
    unsigned int wBytes=(length+7)/8;
    unsigned int bufsize=sizeof(byte)*(PageSize+4);
    unsigned int DoPages=wBytes/PageSize;
	byte WRSR_Cmd[2]={0x01,0x00};
	byte AAIP_Cmd[6]={0xad,0x00,0x00,0x00,0xaa,0xaa};
	
	if (FlashType==SSTFLASH)
	{
		if(verbose)
			printf("Programming :\n");
		
		Spi_Command((byte*)"\x06",0,8);	//WREN
		Sleep(tCE);
		Spi_Command((byte*)"\x80",0,8);	//DBSY
		Sleep(tCE);
		Spi_Command((byte*)"\x50",0,8);	//EWSR
		Sleep(tCE);
		Spi_Command(WRSR_Cmd,0,16);		//WRSR
		Sleep(tCE);
		Spi_Command((byte*)"\x06",0,8);	//WREN
		Sleep(tCE);
		for(x=0;x<=Max_Retries;x++)
		{
			fail=!Spi_Check();
			if(fail==false)
				break;
			Sleep(tCE);
		}		
		
		memcpy(&AAIP_Cmd[4], &write_data[0],2);
		
		Spi_Command(AAIP_Cmd,0,48);
		for(x=0;x<=Max_Retries;x++)
		{
			fail=!Spi_Write_Check(verbose);
			if(!fail)
				break;
			Sleep(tCE);
		}
		for(i=2;i<=wBytes&&!fail;i=i+2)
		{
			memcpy(&AAIP_Cmd[1], &write_data[i],2);
			Spi_Command(AAIP_Cmd,0,24);

			usleep(tBP);

			if((i%20000)==0&&verbose)
			{
				printf(".");			
				fflush(stdout);
			}
		}
	
		printf("Finished Programming\n");
		Spi_Command((byte*)"\x04",0,8);	//WRDI
		
		for(x=0;x<=Max_Retries;x++)
		{
			fail=!Spi_Check(verbose);
			if(!fail)
				break;
			usleep(tBP);
		}		
	}
	else
	{
		data=(byte*)malloc(bufsize);

		// full Pages
		if(verbose){
			printf("Programming :\n");
			fflush(stdout);
		}
		for(i=0;i<DoPages&&!fail;i++)
		{
			memset(data, 0, bufsize);
			if ((FlashType==MacronixFLASH) || (FlashType==GENERIC)){
				Spi_Command((byte*)"\x06",0,7);	//Write Enable
				for(x=0;x<=Max_Retries;x++)
				{
					fail=!Spi_Write_Check(verbose);
					if(fail==false)
						break;
					Sleep(tCE);
				}	
				Spi_SetCommandRW('\x02',data,i*PageSize);
			}
			else{
				Spi_SetCommand((byte*)"\x84",data,1);
			}
			memcpy(&data[4], &write_data[PageSize*i],PageSize);
			Spi_Command(data,0, 8*(bufsize)-1);

			// Write buffer to mem
			memset(data, 0, bufsize);
			if (FlashType!=MacronixFLASH){
				Spi_SetCommandRW('\x88',data,i);
				Spi_Command(data,0,4*8);
			}
			for(x=0;x<=Max_Retries;x++)
			{
				fail=!Spi_Check();
				if(!fail)
					break;
				Sleep(tP);
			}
			if((i%256)==0&&verbose)
			{
				printf(".");			
				fflush(stdout);
			}

		}

		// partial Page
		if(!fail&&(DoPages*PageSize)<wBytes)
		{
			int remBytes=(wBytes-DoPages*PageSize);
			memset(data, 0, bufsize);
			Spi_SetCommand((byte*)"\x84",data,1);
			if ((FlashType==MacronixFLASH) || (FlashType==GENERIC)){
				Spi_Command((byte*)"\x06",0,7);	//Write Enable
				for(x=0;x<=Max_Retries;x++)
				{
					fail=!Spi_Write_Check(verbose);
					if(fail==false)
						break;
					Sleep(tCE);
				}	
				Spi_SetCommandRW('\x02',data,i*PageSize);
			}
			else{
				Spi_SetCommand((byte*)"\x84",data,1);
			}

			memcpy(&data[4], &write_data[PageSize*DoPages],remBytes);
			Spi_Command(data,0, 8*(bufsize)-1);

			// Write buffer to mem
			memset(data, 0, bufsize);
			if (FlashType!=MacronixFLASH){
				Spi_SetCommandRW('\x88',data,DoPages);
				Spi_Command(data,0,4*8);
			}

			for(x=0;x<=Max_Retries;x++)
			{
				fail=!Spi_Check();
				if(!fail)
					break;
				Sleep(tP);
			}
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
    unsigned int i;
    bool fail=false;
    byte *data;
    byte *tdo;
    unsigned int wBytes=(length+7)/8;
    unsigned int bufsize=sizeof(byte)*(PageSize+4);
    unsigned int DoPages=wBytes/PageSize;
	//unsigned int address;

    data=(byte*)malloc(bufsize);
    tdo=(byte*)malloc(bufsize);

    // full Pages
    if(verbose)
        printf("Verifying  :\n");
    for(i=0;i<DoPages&&!fail;i++)
    {
        // Read from mem
        memset(data, 0, bufsize);
        memset(tdo, 0, bufsize);
		if ((FlashType==SSTFLASH) || (FlashType==MacronixFLASH) || (FlashType==GENERIC))
			Spi_SetCommandRW('\x03',data,i*PageSize);
		else
			Spi_SetCommandRW('\x03',data,i);

        Spi_Command(data,tdo,(bufsize)*8);
        if(memcmp(&tdo[4],&verify_data[i*PageSize],PageSize))
		{
            fail=true;
			printf("Error in Verify: first byte of data [0x%02X] ..\n",tdo[4]);
		}
        if((i%256)==0&&verbose)
			{
				printf(".");			
				fflush(stdout);
			}
    }

    // partial Page
    if(!fail&&(DoPages*PageSize)<wBytes)
    {
        int remBytes=(wBytes-DoPages*PageSize);
        // Read from mem
        memset(data, 0, bufsize);
        memset(tdo, 0, bufsize);
		if ((FlashType==SSTFLASH) || (FlashType==MacronixFLASH) || (FlashType==GENERIC))
			Spi_SetCommandRW('\x03',data,DoPages*PageSize);
		else
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
    if(!Spi_Identify(true) && !Spi_Check(true))
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

    if(options==FULL)
    {
        if(!Spi_PartialErase(file.getLength(), verbose))
            return false;
        byte *empty;
        int emptylen=file.getLength()/8 + 1;
        empty=(byte*)malloc(emptylen);
        memset(empty, 0xff, emptylen);
        res=Spi_Verify(empty, file.getLength(), verbose);
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

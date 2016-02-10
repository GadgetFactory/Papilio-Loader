/* Spartan3 JTAG programming algorithms

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
    Added programming time measurements.
*/

#include <sys/time.h>

#include "progalgxc3s.h"

const byte ProgAlgXC3S::JPROGRAM=0x0b;
const byte ProgAlgXC3S::CFG_IN=0x05;
const byte ProgAlgXC3S::CFG_OUT=0x04;
const byte ProgAlgXC3S::JSHUTDOWN=0x0d;
const byte ProgAlgXC3S::JSTART=0x0c;
const byte ProgAlgXC3S::ISC_PROGRAM=0x11;
const byte ProgAlgXC3S::ISC_DNA=0x31;
const byte ProgAlgXC3S::ISC_ENABLE=0x10;
const byte ProgAlgXC3S::ISC_DISABLE=0x16;
const byte ProgAlgXC3S::BYPASS=0x3f;

ProgAlgXC3S::ProgAlgXC3S(Jtag &j, IOBase &i, int fam)
{
  jtag=&j;
  io=&i;
  family = fam;
  switch(family)
    {
    case 0x0e: /* XC3SE*/
    case 0x11: /* XC3SA*/
    case 0x13: /* XC3SAN*/
    case 0x1c: /* SC3SADSP*/
    case 0x20: /* XC6S*/
      tck_len = 16;
      array_transfer_len = 16;
      break;
    default:
      tck_len = 12;
      array_transfer_len = 64;
    }
}

void ProgAlgXC3S::flow_enable()
{
  byte data[1];

  jtag->shiftIR(&ISC_ENABLE);
  data[0]=0x0;
  jtag->shiftDR(data,0,5);
  io->cycleTCK(tck_len);
}

void ProgAlgXC3S::flow_disable()
{
  byte data[1];

  jtag->shiftIR(&ISC_DISABLE);
  io->cycleTCK(tck_len);
  jtag->shiftIR(&BYPASS);
  data[0]=0x0;
  jtag->shiftDR(data,0,1);
  io->cycleTCK(1);

}

void ProgAlgXC3S::flow_array_program(BitFile &file)
{
  struct timeval tv[2];
  unsigned int i;
  gettimeofday(tv, NULL);
  for(i=0; i<file.getLength(); i= i+ array_transfer_len)
    {
      jtag->shiftIR(&ISC_PROGRAM);
      jtag->shiftDR(&(file.getData())[i/8],0,array_transfer_len);
      io->cycleTCK(1);
      if((i % (10000*array_transfer_len)) == 0)
	{
	  fprintf(stdout,".");
	  fflush(stdout);
	}
    }
  gettimeofday(tv+1, NULL);

  // Print the timing summary
  if (io->getVerbose())
    printf(" Done.\nProgramming time %.1f ms\n", (double)deltaT(tv, tv + 1)/1.0e3);
}

void ProgAlgXC3S::flow_program_legacy(BitFile &file)
{
  byte data[2];
  struct timeval tv[2];
  gettimeofday(tv, NULL);

  jtag->shiftIR(&JSHUTDOWN);
  io->cycleTCK(tck_len);
  jtag->shiftIR(&CFG_IN);
  jtag->shiftDR((file.getData()),0,file.getLength());
  io->cycleTCK(1);
  jtag->shiftIR(&JSTART);
  io->cycleTCK(2*tck_len);
  jtag->shiftIR(&BYPASS);
  data[0]=0x0;
  jtag->shiftDR(data,0,1);
  io->cycleTCK(1);

  // Print the timing summary
  if (io->getVerbose())
    {
      gettimeofday(tv+1, NULL);
      printf("Done.\nProgramming time %.1f ms\n", (double)deltaT(tv, tv + 1)/1.0e3);
    }

}
void ProgAlgXC3S::array_program(BitFile &file)
{
  unsigned char buf[1] = {0};
  int i = 0;
  
  flow_enable();
  /* JPROGAM: Trigerr reconfiguration, not explained in ug332, but
     DS099 Figure 28:  Boundary-Scan Configuration Flow Diagram (p.49) */
  jtag->shiftIR(&JPROGRAM);

  do
    jtag->shiftIR(&CFG_IN, buf);
  while (! (buf[0] & 0x10)); /* wait until configuration cleared */
  switch(family)
    {
    case 0x11: /* XC3SA*/
    case 0x13: /* XC3SAN*/
    case 0x1c: /* SC3SADSP*/
    case 0x20: /* XC6S*/
      {
	byte data[8];
	jtag->shiftIR(&ISC_ENABLE);
	jtag->shiftIR(&ISC_DNA);
	jtag->shiftDR(0, data, 64);
	io->cycleTCK(1);
	if (*(long*)data != -1L)
	  /* ISC_DNA only works on a unconfigured device, see AR #29977*/
    if (io->getVerbose())
  	  printf("DNA is 0x%02x%02x%02x%02x%02x%02x%02x%02x\n",
		  data[0], data[1], data[2], data[3],
		  data[4], data[5], data[6], data[7]);
	break;
      }
    }

  /* use leagcy, if large transfers are faster then chunks */
  flow_program_legacy(file);
  /*flow_array_program(file);*/
  flow_disable();

  /* Wait until device comes up */
  while ((( buf[0] & 0x23) != 0x21) && (i <50))
    {
      jtag->shiftIR(&BYPASS, buf);
      jtag->Usleep(1000);
      i++;
    }
  if (i == 50)
    fprintf(stderr, 
	    "Device failed to configure, INSTRUCTION_CAPTURE is 0x%02x\n",
	    buf[0]);
}
void ProgAlgXC3S::Reconfigure()
{
//    jtag->shiftIR(&JPROGRAM);
//    jtag->shiftIR(&BYPASS);
  switch(family)
    {
      case 0x0e: /* XC3SE*/
      case 0x11: /* XC3SA*/
      case 0x13: /* XC3SAN*/
      case 0x20: /* XC6S*/
      break;
    default:
      fprintf(stderr, "Device does not support reconfiguration.\n");
      return;
    }

    /* Sequence is from AR #31913
       FFFF Dummy Word
       9955 SYNC
       850c Type 1 Write to CMD
       7000 REBOOT command
       0004 NOOP
       0004 NOOP
    */
    byte xc3sbuf[12]= {0xff, 0xff, 0x55, 0x99, 0x0c,
                       0x85, 0x00, 0x70, 0x04, 0x00, 0x04, 0x00};
    /* xtp038.pdf
       FFFF Dummy Word
       AA99 Sync Word
       5566 Sync Word
       30A1 Type 1 Write 1 Word to CMD
       000E IPROG Command
       2000 Type 1 NO OP
    */
    byte xc6sbuf[12]= {0xff, 0xff, 0x55, 0x99, 0xaa, 0x66, 0x0c,
                       0x85, 0x00, 0x70, 0x04, 0x00};

  jtag->shiftIR(&JSHUTDOWN);
  io->cycleTCK(16);
  jtag->shiftIR(&CFG_IN);
  if(io->getVerbose())
      fprintf(stderr, "Trying reconfigure\n");
  if(family == 0x20) /*XC6S*/
     jtag->shiftDR(xc6sbuf, NULL, 12*8 );
  else
     jtag->shiftDR(xc3sbuf, NULL, 12*8 );
  jtag->shiftIR(&JSTART);
  io->cycleTCK(32);
  jtag->shiftIR(&BYPASS);
  io->cycleTCK(1);
//  jtag->setTapState(Jtag::TEST_LOGIC_RESET);
}

void ProgAlgXC3S::DisplayStatus()
{
    /*
    attribute INSTRUCTION_CAPTURE of XC3SxxxE : entity is
-- Bit 5 is 1 when DONE is released (part of startup sequence)
-- Bit 4 is 1 if house-cleaning is complete
-- Bit 3 is ISC_Enabled
-- Bit 2 is ISC_Done
  "XXXX01" ;
  */
  byte data[4];
  jtag->shiftIR(&BYPASS,data);
  printf("\n");
  if((data[0]&0x3)!=0x01)
    printf("Response incompatible with mask xxxx01\n");
  printf("ISC_Done       = %d\n",(data[0]&0x4)?1:0);
  printf("ISC_Enabled    = %d\n",(data[0]&0x8)?1:0);
  printf("House Cleaning = %d\n",(data[0]&0x10)?1:0);
  printf("DONE           = %d\n",(data[0]&0x20)?1:0);
}

void ProgAlgXC3S::getStatusRegister()
{
/*
Data Description Data Field
Dummy word 0xFFFFFFFF
Sync Word 0xAA995566
Status Register Read 0x2800E002
Flush pipe (2 words) 0x00000000
CMD Write Packet Header 0x30008001
CMD Write Packet Data (RCRC)(1)
0x00000007
Flush pipe (2 words) 0x00000000
*/
    byte data[28]={
    0xFF,0xFF,0xFF,0xFF,
    0xAA,0x99,0x55,0x66,
    0x28,0x00,0xE0,0x02,
    0x00,0x00,0x00,0x00,
    0x30,0x00,0x80,0x01,
    0x00,0x00,0x00,0x07,
    0x00,0x00,0x00,0x00};
    byte tdo[4]={0,0,0,0};
    int i;
    for(i=0;i<28;i++)
        data[i]=bRevTable[data[i]];

    //jtag->shiftIR(&BYPASS);
    //io->cycleTCK(12);
    jtag->shiftIR(&CFG_IN);
    jtag->shiftDR(data,0,8*28);
    //io->cycleTCK(1);
    jtag->shiftIR(&CFG_OUT,tdo);

    jtag->shiftDR(data,tdo,32);
    for(i=0;i<4;i++)
        tdo[i]=bRevTable[tdo[i]];


/*
ID_ERROR 13 IDCODE not validated while trying to write the FDRIregister
DONE 12 Input from the DONE pin
INIT 11 Input from the INIT pin
MODE 10:8 Input from the MODE pins (M2:M0)
GHIGH_B 7 Status of GHIGH_B (0 = asserted)
GWE 6 Status of GWE (0 = all FFs and Block RAMs are write-disabled)
GTS_CFG 5 Status of GTS_CFG_B (0 = all I/Os are 3-stated)
IN_ERROR 4 Legacy input error. This error occurs when serial data is loaded too fast.
DCI_MATCH 3 DCI is matched. This bit is a logical AND function of all the MATCH signals (one per bank). If no DCI I/Os are in a particular bank, then a 1 is used.
DCM_LOCK 2 DCMs are locked. This bit is a logical AND function of all the LOCKED signals. If DCM is not used, then a 1 is used.
CRC_ERROR 0 CRC error
*/
    printf("\nSTAT Register\n");
    if(tdo[0] || tdo[1] || tdo[2]&0xC0)
        printf("Unexpected error (bits 31-14 not all 0).\n");
    printf("ID_ERROR  = %d\t\tIDCODE not validated.\n", ((tdo[2]&0x20)?1:0));
    printf("DONE      = %d\t\tInput from the DONE pin.\n", ((tdo[2]&0x10)?1:0));
    printf("INIT      = %d\t\tInput from the INIT pin.\n", ((tdo[2]&0x08)?1:0));
    printf("MODE      = %d%d%db\tInput from the MODE pins (M2:M0).\n", ((tdo[2]&0x04)?1:0), ((tdo[2]&0x02)?1:0), ((tdo[2]&0x01)?1:0));
    printf("GHIGH_B   = %d\t\t0 = asserted.\n", ((tdo[3]&0x80)?1:0));
    printf("GWE       = %d\t\t0 = all FFs and Block RAMs are write-disabled.\n", ((tdo[3]&0x40)?1:0));
    printf("GTS_CFG   = %d\t\t0 = all I/Os are 3-stated.\n", ((tdo[3]&0x20)?1:0));
    printf("IN_ERROR  = %d\t\tLegacy input error.\n", ((tdo[3]&0x10)?1:0));
    printf("DCI_MATCH = %d\t\tDCI is matched.\n", ((tdo[3]&0x08)?1:0));
    printf("DCM_LOCK  = %d\t\tDCMs are locked.\n", ((tdo[3]&0x04)?1:0));
    printf("CRC_ERROR = %d\t\tCRC error.\n", ((tdo[3]&0x01)?1:0));


    jtag->shiftIR(&BYPASS);
    io->cycleTCK(12);

}

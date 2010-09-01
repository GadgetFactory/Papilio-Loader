#ifndef __TOOLS_H
#define __TOOLS_H

#ifdef WINDOWS
	#include <windows.h>
	#include <time.h>
#endif
typedef unsigned char byte;

extern byte bRevTable[256];

#define deltaT(tvp1, tvp2) (((tvp2)->tv_sec-(tvp1)->tv_sec)*1000000 + \
                              (tvp2)->tv_usec - (tvp1)->tv_usec)
#endif

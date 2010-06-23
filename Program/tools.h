#ifndef __TOOLS_H
#define __TOOLS_H

#include <windows.h>
#include <time.h>
typedef unsigned char byte;

extern byte bRevTable[256];

struct timezone
{
  int  tz_minuteswest; /* minutes W of Greenwich */
  int  tz_dsttime;     /* type of dst correction */
} ;
#define deltaT(tvp1, tvp2) (((tvp2)->tv_sec-(tvp1)->tv_sec)*1000000 + \
                              (tvp2)->tv_usec - (tvp1)->tv_usec)
int gettimeofday(struct timeval *tv, struct timezone *tz);
#endif

AC_INIT([papilio-prog], [1.5])
AC_CONFIG_HEADER([config.h])

AM_INIT_AUTOMAKE

CPPFLAGS="-g -Wall"
AC_PROG_CXX
AC_PROG_CC
AC_PROG_CPP
AC_PROG_INSTALL
AC_PROG_LN_S

if test -n "$host_alias" ; then
	AC_DEFINE([WINDOWS], 1, [Cross-compiling for windows])
	LIBS="$LIBS -L. -lftd2xx"
else
	PKG_CHECK_MODULES([libftdi], [libftdi >= 0.16],
		[CFLAGS="$CFLAGS $libftdi_CFLAGS";
		LIBS="$LIBS $libftdi_LIBS"])
fi

AC_CONFIG_FILES([Makefile])
AC_OUTPUT


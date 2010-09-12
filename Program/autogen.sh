#!/bin/sh

echo "Generating build system..."
aclocal \
&& autoheader \
&& automake --add-missing --copy --foreign \
&& autoconf

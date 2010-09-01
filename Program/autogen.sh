echo "Generating build system..."
aclocal || exit 1
autoheader || exit 1
automake --add-missing --copy --foreign || exit 1
autoconf || exit 1


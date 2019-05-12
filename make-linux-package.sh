#!/bin/sh

# will need sudo apt-get install default-jdk libftdi-dev

# Build papilio-prog
cd papilio-prog
./autogen.sh
./configure
make clean
make
cp papilio-prog ../Java-GUI/programmer/linux/
cd ..

# Build Java and copy everything to linux-package folder for zipping
cd Java-GUI
sudo ./build.sh
rm -rf ../linux-package/GadgetFactory
mkdir -p ../linux-package/GadgetFactory/papilio-loader
cp -pr papilio-loader.jar papilio-loader.sh programmer/ images/ help/ ../linux-package/GadgetFactory/papilio-loader
cd ..

cd linux-package
rm -f ../papilio-loader-linux.zip
zip -r ../papilio-loader-linux.zip *
cd ..







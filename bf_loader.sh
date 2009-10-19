#!/bin/bash -x

CMDFile=Butterfly_loader.cmd
SVFFile=$1
TMPFile=svftemp.svf

cp $1 bin/$TMPFile
cd bin
echo cable ft2232 vid=0403 pid=6010 > $CMDFile
echo bsdl path . >> $CMDFile
echo detect >> $CMDFile
echo svf $TMPFile progress >> $CMDFile
sudo ./jtag_lin $CMDFile
rm $CMDFile
rm $TMPFile
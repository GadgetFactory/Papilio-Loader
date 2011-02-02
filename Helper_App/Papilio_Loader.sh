bitfile=bitfile

./dialog --title "Papilio Loader" \
		--pause "Bitstream will be temporarily loaded over JTAG.\nTo permanently write to SPI Flash select 'Cancel'" 15 55 5 
return_value=$?
echo $return_value	

if [ $return_value == 0 ]
then	
./butterfly_prog.exe -v -f "$1" -v
fi
	
if [ $return_value == 1 ]
then	
./dialog --title "Papilio SPI Flash Programmer" \
        --menu "Please choose the size of your Papilio board:" 15 55 5 \
        "bscan_spi_xc3s250e.bit" "250K Papilio" \
        "bscan_spi_xc3s500e.bit" "500K Papilio" \
        "bscan_spi_xc3s100e.bit" "100K Papilio" 2> $bitfile
return_value=$?

./butterfly_prog.exe -v -f "$1" -b `./cat $bitfile` -sa -r
fi
This dir contains simple cores to access the internal SPI-flash via BSCAN/USER1.



The (compressed) bitfiles are provided for your convenience.



All IOBs are unused and set to float.

The config + JTAG pins have their default pull-up/downs enabled. 

You have to make your own bitstream if you don't like that.



bscan_spi.vhd              - main VHDL file
constraints_s3e_vq100.ucf  - constraints for the VQ100 package

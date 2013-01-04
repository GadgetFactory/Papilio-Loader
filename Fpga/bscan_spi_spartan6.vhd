----------------------------------------------------------------------------------
-- Company: -
-- Engineer: Jochem Govers
-- 
-- Create Date:    13:00:21 05/13/2010 
-- Design Name: 
-- Module Name:    bscan_spi - Behavioral 
-- Project Name: 
-- Target Devices: Spartan 3e 100/250/500 VQ100
-- Tool versions: ISE 11.4
-- Description: a simple implementation of the BSCAN_SPARTAN3E module to access 
--              external SPI Flash via JTAG.
--
-- Dependencies: None
--
-- Revision: 
-- Revision 0.01 - File Created
-- Revision 0.02 - Added header recognation of header and TDO alignment (requires
--                 4 bytes of preamble and 1 byte post). Based on design
--                 of xc3sprog (changes made to CS handling and header length).
-- Additional Comments: tested on Butterfly One with xc3s250e
--
----------------------------------------------------------------------------------
library IEEE;
use IEEE.STD_LOGIC_1164.ALL;
use IEEE.STD_LOGIC_UNSIGNED.ALL;
use IEEE.NUMERIC_STD.ALL;
--use IEEE.STD_LOGIC_ARITH.ALL;

---- Uncomment the following library declaration if instantiating
---- any Xilinx primitives in this code.
library UNISIM;
use UNISIM.VComponents.all;

entity bscan_spi is
    Port ( SPI_MISO : in  STD_LOGIC;
           SPI_MOSI : inout  STD_LOGIC;
           SPI_CS : inout  STD_LOGIC;
           SPI_SCK : inout  STD_LOGIC);
end bscan_spi;

architecture Behavioral of bscan_spi is
--component BSCAN_SPARTAN6
--	port (CAPTURE : out STD_ULOGIC;
--			DRCK1 : out STD_ULOGIC;
--			DRCK2 : out STD_ULOGIC;
--			RESET : out STD_ULOGIC;
--			SEL1 : out STD_ULOGIC;
--			SEL2 : out STD_ULOGIC;
--			SHIFT : out STD_ULOGIC;
--			TDI : out STD_ULOGIC;
--			UPDATE : out STD_ULOGIC;
--			TDO1 : in STD_ULOGIC;
--			TDO2 : in STD_ULOGIC);
--	end component;
	
	signal user_CAPTURE : std_ulogic;
	signal user_DRCK1 : std_ulogic;
	signal user_DRCK2 : std_ulogic;
	signal user_RESET : std_ulogic;
	signal user_SEL1 : std_ulogic;
	signal user_SEL2 : std_ulogic;
	signal user_SHIFT : std_ulogic;
	signal user_TDI : std_ulogic;
	signal user_UPDATE : std_ulogic;
	signal user_TDO1 : std_ulogic;
	signal user_TDO2 : std_ulogic;
	
	signal tdi_mem : std_logic_vector(31 downto 0);
	signal tdo_mem : std_logic_vector(7 downto 0);
	signal len : std_logic_vector(15 downto 0);
	signal CS_GO_PREP : std_logic;
	signal CS_GO : std_logic;
	signal CS_STOP_PREP : std_logic;
	signal CS_STOP : std_logic;
	signal reset : std_logic;
	signal have_header : std_logic;
begin

reset<=user_CAPTURE or user_RESET or user_UPDATE or not(user_SEL1);

BS : BSCAN_SPARTAN6
	port map (
		CAPTURE => user_CAPTURE,
		DRCK => user_DRCK1,
		--DRCK2 => user_DRCK2,
		RESET => user_RESET,
		SEL => user_SEL1,
		--SEL2 => user_SEL2,
		SHIFT => user_SHIFT,
		TDI => user_TDI,
		UPDATE => user_UPDATE,
		TDO => user_TDO1
		--TDO2 => user_TDO2
		);


process(SPI_MISO, user_SEL1, user_TDI, user_SHIFT, user_DRCK1)
begin
	-- default assignments (put outputs in High-Z state if not in USER1)
	--user_TDO1<=SPI_MISO;
	user_TDO1<=tdo_mem(tdo_mem'high);
	SPI_MOSI<='Z';
	SPI_SCK<='Z';
	SPI_CS<='Z';

	if (user_SEL1='1') then
		SPI_MOSI<='0';
		SPI_SCK<='1';
		--SPI_CS<='1';
		SPI_CS<=not(CS_GO and not(CS_STOP));
	
		if(user_SHIFT='1') then
			SPI_SCK<=user_DRCK1;
			--SPI_CS<='0';
			SPI_MOSI<=user_TDI;
		end if;
	end if;
end process;


process(user_DRCK1)
variable i : integer;
begin
	if(reset = '1')then
		have_header<='0';
		CS_GO<='0';
	elsif(falling_edge(user_DRCK1))then
		if ( have_header='0') then
	      if (tdi_mem(tdi_mem'high downto tdi_mem'high-15)="0101100110100110") then
				len <= tdi_mem(15 downto 0);
				have_header <= '1';
				if (to_integer(unsigned(tdi_mem(15 downto 0)))> 0 ) then
					CS_GO <= '1';
		      end if;
			end if;
		elsif (len /= 0) then
			len <= len - 1;
		end if;
	end if;
end process;

process(user_DRCK1)
variable i : integer;
variable j : integer;
begin
	if(reset ='1') then
		tdo_mem<=(others => '0');
		tdi_mem<=(others => '0');
		CS_STOP<='0';
	elsif(rising_edge(user_DRCK1))then
		tdi_mem(0)<=user_TDI;
		for j in 1 to tdi_mem'high loop
			tdi_mem(j)<=tdi_mem(j-1);
		end loop;

		tdo_mem(0)<=SPI_MISO;
		for i in 1 to tdo_mem'high loop
			tdo_mem(i)<=tdo_mem(i-1);
		end loop;

		if(CS_GO='1' and len=0)then
			CS_STOP<='1';
		end if;
	end if;
end process;

end Behavioral;


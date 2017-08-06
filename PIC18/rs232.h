#include <p18f4520.h>
#include <stdio.h>

//========================================================================================
//  Port Serie RS232   8bits, no parity, 1 bit de stop 
//========================================================================================
void init232(char debit);
void tx232(char val);
char rx232(void);
void tx232_puts(const char * s);		// ecrit une string de caracteres sur le port serie
void tx232_2ascii(unsigned char car);

void init232(char debit){
	// Baud Rate = Fosc/(16(SPBRG+1))
	SPBRG = debit;   					
	// 103 : 9600bds, 51 : 19200bds, 16 : 57600bds, 8: 115kbds, 255 : 1Mbps!!! pour 16MHz
	// 155 : 9600bds, 77 : 19200bds, 25 : 57600bds, 12 : 115kbds pour 24MHz
	TXSTA = 0b00100100;//0b10100100;     		// Asynchronous, 8bits, TXEN, Asynchronous, High speed
	RCSTA = 0b10010000;				// SPEN : enable port, CREN : reception continue
	rx232();								// correction bug bootloader (sinon port bloque ?)
}

//========================================================================================
// Emission d'un octet RS232
//========================================================================================
void tx232(char val){
	while(TXSTAbits.TRMT==0); 					// attendre que le tampon soit vide
	TXREG = val;						// emet le caractere
}

//========================================================================================
// Reception d'un octet RS232
//========================================================================================
char rx232(void){
  unsigned char tmp; // #### UTILE ??? cf doc MidRange
	if (RCSTAbits.OERR==1) { 					//overrun Error
		//RCSTAbits.CREN = 0;
		RCSTAbits.CREN = 1;
		tmp = RCREG;
	}
	PIR1bits.RCIF = 0;							// reset flag ISR
	return RCREG;						// retourne le caractere recu
}

//========================================================================================
// ecrit une string de caracteres sur le port serie
//========================================================================================
void tx232_puts(const char * s){
	while(*s)
		tx232(*s++);
}
 
//========================================================================================
// detection de reception
//========================================================================================
/*
char
rx232_detc(void){
	while(!RCIF);
	return rx232();
}*/

//========================================================================================
// ecrit 2 caracteres ascii
//========================================================================================
/*void tx232_2ascii(unsigned char car){
	tx232(hexa2ascii(car >> 4));
	tx232(hexa2ascii(car & 0x0F));
}
*/
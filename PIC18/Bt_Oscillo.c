#include <p18f4520.h>
#include <rs232.h>
#include <delays.h>

#pragma config OSC = HS // choisir le oscillateur HS

void high_isr(void);
char R;

#pragma code high_vector=0x08
void interrupt_at_high_vector(void)
{
	_asm goto high_isr _endasm
}

void high_isr(void)
{	
	if (PIR1bits.RCIF == 1){
		rx232();
	}	
}	

void main(void){
	TRISD=0x00;
	TRISC=0x80;
	TRISB=0x00;
	TRISA=0x00;
	//ADCON0=0b00000001;
	ADCON1|=0x0F;
	PIR1 = 0x00;
	PIE1 = 0x20;
	init232(15);
	INTCONbits.GIEH = 1;
	INTCONbits.PEIE = 1;
	Delay10KTCYx(125);
	tx232(0x01);
	
	while(1){	
		
		/*ADCON0=0b11;
		while(ADCON0==0b11);
		tx232(ADRESH);
		tx232(ADRESL);*/
		tx232(0x55);
		//tx232(RCREG);
	}
}
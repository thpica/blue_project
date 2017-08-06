package com.blueproject;

class AcquisitionThread extends Thread{
	public int[] buffer;
	private boolean _run = false;
	public boolean Acq = false;
	public boolean drawFlag = false;

	@Override
	public void run() {
		while (_run) {
			if(Acq){
    			for(int i=0; i<buffer.length; i++){
    				buffer[i]=(int) (Math.sin((double)i/20)*(float)40.96);
    				//buffer[i]=(float) Math.random()*100;
    				/*for(int j=0;j<10;j++){
    					buffer[i+j]= 2;
    				}
    				for (int k=0;k<10;k++){
    					buffer[i+10+k]=-2; //1V=205
    				}*/
            	}
			}
		}	
	}
	public void setRunning(boolean run) {
        _run = run;
	}
	public void setAcq(boolean acq){
		Acq = acq;
	}
}

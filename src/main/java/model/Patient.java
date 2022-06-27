package model;

import java.time.LocalTime;

public class Patient implements Comparable<Patient>{

	//info che servono sul singolo paziente, di modo da metterlo in lista/coda proritaria
	//devo conoscere: codice colore, tempo di arrivo
	public enum ColorCode{
		NEW, //in triage
		WHITE,YELLOW,RED, BLACK, //in sala d'attesa
		TREATING, //nello studio medico
		OUT //abbandonato o curato
	}
	
	private int num;
	private LocalTime arrivalTime;
	private ColorCode color;
	public Patient(int num, LocalTime arrivalTime, ColorCode color) {
		super();
		this.num = num;
		this.arrivalTime = arrivalTime;
		this.color = color;
	}
	public LocalTime getArrivalTime() {
		return arrivalTime;
	}
	public void setArrivalTime(LocalTime arrivalTime) {
		this.arrivalTime = arrivalTime;
	}
	public ColorCode getColor() {
		return color;
	}
	public void setColor(ColorCode color) {
		this.color = color;
	}
	@Override
	public String toString() {
		return "Patient [num=" + num + ", arrivalTime=" + arrivalTime + ", color=" + color + "]";
	}
	@Override
	public int compareTo(Patient o) {
		if(this.color.equals(o.color)) {
			//il confronto dipende dal tempo di arrivo
			return this.arrivalTime.compareTo(arrivalTime);
		}else if(this.color.equals(Patient.ColorCode.RED)) {
			return -1;
		}else if(o.color.equals(Patient.ColorCode.RED)) {
			return +1;
		}else if(this.color.equals(Patient.ColorCode.YELLOW)) { //sicuramente abbiamo Yellow - White
			return -1;
		}else //Abbiamo White - Yellow
			return +1;
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + num;
		return result;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Patient other = (Patient) obj;
		if (num != other.num)
			return false;
		return true;
	}
	
	
}

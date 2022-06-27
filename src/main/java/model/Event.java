package model;

import java.time.LocalTime;

public class Event implements Comparable<Event>{

	//eventi che variano lo stato del mondo, ovvero dei pazienti
	public enum EventType{
		ARRIVAL, //arriva nuovo paziente che entra in triage
		TRIAGE, //entro in sala d'attesa con un certo colore
		TIME_OUT, //passaggio del tempo di attesa
		FREE_STUDIO, //vengo chiamato in uno studio medico che si è liberato
		TREATED, //paziente curato
		TICK //timer per controllare se ci sono studi liberi --> per evitare che studio rimanga vuoto
	}
	
	private LocalTime time;
	private EventType type;
	private Patient p;
	public LocalTime getTime() {
		return time;
	}
	public void setTime(LocalTime time) {
		this.time = time;
	}
	public EventType getType() {
		return type;
	}
	public void setType(EventType type) {
		this.type = type;
	}
	public Patient getP() {
		return p;
	}
	public void setP(Patient p) {
		this.p = p;
	}
	public Event(LocalTime time, EventType type, Patient p) {
		super();
		this.time = time;
		this.type = type;
		this.p = p;
	}
	@Override
	public int compareTo(Event o) {
		return this.time.compareTo(o.getTime());
	}
	@Override
	public String toString() {
		return "Event [time=" + time + ", type=" + type + ", p=" + p + "]";
	}
}

package model;

import java.time.Duration;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;

import model.Event.EventType;
import model.Patient.ColorCode;

public class Simulator {

	//coda degli eventi
	private PriorityQueue<Event> queue;
	
	//modello del mondo
	private List<Patient> patients; //alternativa: coda prioritaria dei pazienti, che sono selezionati in base a criterio di priorità dei colori
	private PriorityQueue<Patient> waitingRoom; //contiene SOLO i pazienti in attesa (WHITE/YELLOW/RED)
	//Stato degli studi medici ---> devo sapere quanti no ho in ottale, quanti sono liberi e quanti sono occupati
	private int freeStudios;
	private Patient.ColorCode ultimoColore;
	
	//parametri di input
	private int totStudios = 3; //NS
	private int numPatiens = 120; //ND
	private Duration T_ARRIVAL = Duration.ofMinutes(5);
	private Duration DURATION_TRAIGE = Duration.ofMinutes(5);
	private Duration DURATION_WHITE = Duration.ofMinutes(10);
	private Duration DURATION_YELLOW = Duration.ofMinutes(15);
	private Duration DURATION_RED = Duration.ofMinutes(30);
	private Duration TIMEOUT_WHITE = Duration.ofMinutes(60);
	private Duration TIMEOUT_YELLOW = Duration.ofMinutes(30);
	private Duration TIMEOUT_RED = Duration.ofMinutes(30);
	
	private LocalTime startTime = LocalTime.of(8, 00);
	private LocalTime endTime = LocalTime.of(20, 00);
	//parametri di output ---> valori calcolati dal simulatore
	private int patientsTreated;
	private int patientsDead;
	private int patientsAbandoned;
	
	//inizializza il simulatore preparando tutte le strutture dati che servono
	public void init() {
		this.queue = new PriorityQueue<>();
		this.patients = new ArrayList<>();
		this.waitingRoom = new PriorityQueue<>();
		this.freeStudios = this.totStudios; //inizialmente sono tutti liberi
		this.patientsAbandoned = 0;
		this.patientsDead = 0;
		this.patientsTreated = 0;
		//inietto gli eventi: creo i pazienti dalle 8 alle 20 fino al numero massimo
		//eventi di input (ARRIVAL)
		LocalTime ora = this.startTime;
		int inseriti = 0;
		ultimoColore = ColorCode.RED;
		this.queue.add(new Event(ora, EventType.TICK, null));
		while(ora.isBefore(endTime) && inseriti < this.numPatiens) {
			Patient p = new Patient(inseriti, ora, ColorCode.NEW);
			this.patients.add(p);
			Event e = new Event(ora, EventType.ARRIVAL, p);
			this.queue.add(e);
			ora = ora.plus(T_ARRIVAL);
			inseriti++;
		}
	}
	
	//esegue la simulazione vera e propria
	public void run() {
		while(!this.queue.isEmpty()) {
			Event e  = this.queue.poll();
			processEvent(e);
			System.out.println(e);
		}
	}

	private void processEvent(Event e) {
		//Estraggo le info che servono per decidere
		Patient p = e.getP();
		LocalTime ora = e.getTime();
		
		switch(e.getType()) {
		case ARRIVAL:
			//devo prevedere evento di fine triage fra 5 min
			this.queue.add(new Event(ora.plus(this.DURATION_TRAIGE), EventType.TRIAGE, p));
			break;
		case TRIAGE:
			//devo dare colore a paziente, poi devo schedulare il timeout relativo
			p.setColor(this.proxColor());
			if(p.getColor().equals(Patient.ColorCode.WHITE)) {
				this.queue.add(new Event(ora.plus(TIMEOUT_WHITE), EventType.TIME_OUT, p));
				this.waitingRoom.add(p);
			}else if(p.getColor().equals(Patient.ColorCode.YELLOW)) {
				this.queue.add(new Event(ora.plus(TIMEOUT_YELLOW), EventType.TIME_OUT, p));
				this.waitingRoom.add(p);
			}else {
				this.queue.add(new Event(ora.plus(TIMEOUT_RED), EventType.TIME_OUT, p));
				this.waitingRoom.add(p);
			}
			break;
		case FREE_STUDIO:
			//controllo che studio sia veramente libero per risolvere il caso in cui studio è vuoto, ma in quell'istante è uscito un paziente
			//per cui free studio schedulato ma non ancora eseguito
			if(this.freeStudios == 0) {
				return;
			}
			//qui implemento la gestione della priorità
			//quale paziente ha diritto di entrare?? dovrei partire da quelli più gravi
			Patient primo = this.waitingRoom.poll(); //prendo il primo che è presente nella lista di attesa
			if(primo != null) {
				//ammetti il paziente nello studio
				if(primo.getColor().equals(ColorCode.WHITE)) {
					this.queue.add(new Event(ora.plus(DURATION_WHITE), EventType.TREATED, primo));
				}else if(primo.getColor().equals(ColorCode.YELLOW)) {
					this.queue.add(new Event(ora.plus(DURATION_YELLOW), EventType.TREATED, primo));
				}else if(primo.getColor().equals(ColorCode.RED)) {
					this.queue.add(new Event(ora.plus(DURATION_RED), EventType.TREATED, primo));
				}
				primo.setColor(ColorCode.TREATING);
				this.freeStudios--;
			}
			break;
		case TIME_OUT:
			//mi tolgo da uno stato e vado in uno stato diverso
			//dipende dal colore
			Patient.ColorCode colore = p.getColor();
			switch(colore) {
			case WHITE:
				this.waitingRoom.remove(p);
				//qui vado a casa: non genero eventi, setto solo che è andato a casa
				p.setColor(ColorCode.OUT);
				this.patientsAbandoned++;
				break;
			case YELLOW:
				//diventa rosso
				//cambiare il colore gli fa cambiare la priorità, ma così non si sposta da solo: problema: devo togliere e reinserire
				this.waitingRoom.remove(p);
				p.setColor(ColorCode.RED);
				//Schedulo timeout di rosso
				this.queue.add(new Event(ora.plus(TIMEOUT_RED), EventType.TIME_OUT, p));
				this.waitingRoom.add(p);
				break;
			case RED:
				this.waitingRoom.remove(p);
				//diventa nero e muore
				p.setColor(ColorCode.BLACK);
				this.patientsDead++;
				break;
			//se paziente non è più in waiting room non c'è timeout
			default:
				//non faccio nulla
			}
			break;
		case TREATED:
			//conto come guarito
			this.patientsTreated++;
			p.setColor(ColorCode.OUT);
			this.freeStudios++;
			this.queue.add(new Event(ora, EventType.FREE_STUDIO, null));
			break;
		case TICK:
			if(this.freeStudios > 0 && !this.waitingRoom.isEmpty()) {
				//Schedulo evento di tipo freestudio
				this.queue.add(new Event(ora, EventType.FREE_STUDIO, null));
			}
			//evento che si rigenera da solo solo se sono nell'orario di apertura
			if(ora.isBefore(endTime))
				this.queue.add(new Event(ora.plus(Duration.ofMinutes(5)), EventType.TICK, null));
			break;
		}
		
		
		
	}
	
	private Patient.ColorCode proxColor(){
		if(ultimoColore.equals(ColorCode.WHITE)) {
			ultimoColore = ColorCode.YELLOW;
		}else if(ultimoColore.equals(ColorCode.YELLOW)) {
			ultimoColore = ColorCode.RED;
		}else {
			ultimoColore = ColorCode.WHITE;
		}
		return ultimoColore;
	}

	public int getPatientsTreated() {
		return patientsTreated;
	}

	public int getPatientsDead() {
		return patientsDead;
	}

	public int getPatientsAbandoned() {
		return patientsAbandoned;
	}

	public void setTotStudios(int totStudios) {
		this.totStudios = totStudios;
	}

	public void setNumPatiens(int numPatiens) {
		this.numPatiens = numPatiens;
	}

	public void setT_ARRIVAL(Duration t_ARRIVAL) {
		T_ARRIVAL = t_ARRIVAL;
	}

	public void setDURATION_TRAIGE(Duration dURATION_TRAIGE) {
		DURATION_TRAIGE = dURATION_TRAIGE;
	}

	public void setDURATION_WHITE(Duration dURATION_WHITE) {
		DURATION_WHITE = dURATION_WHITE;
	}

	public void setDURATION_YELLOW(Duration dURATION_YELLOW) {
		DURATION_YELLOW = dURATION_YELLOW;
	}

	public void setDURATION_RED(Duration dURATION_RED) {
		DURATION_RED = dURATION_RED;
	}

	public void setTIMEOUT_WHITE(Duration tIMEOUT_WHITE) {
		TIMEOUT_WHITE = tIMEOUT_WHITE;
	}

	public void setTIMEOUT_YELLOW(Duration tIMEOUT_YELLOW) {
		TIMEOUT_YELLOW = tIMEOUT_YELLOW;
	}

	public void setTIMEOUT_RED(Duration tIMEOUT_RED) {
		TIMEOUT_RED = tIMEOUT_RED;
	}

	public void setStartTime(LocalTime startTime) {
		this.startTime = startTime;
	}

	public void setEndTime(LocalTime endTime) {
		this.endTime = endTime;
	}



}

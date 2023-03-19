package es.um.redes.nanoChat.server;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import es.um.redes.nanoChat.messageML.NCMessage;
import es.um.redes.nanoChat.server.roomManager.NCRoomDescription;
import es.um.redes.nanoChat.server.roomManager.NCRoomManager;

/**
 * Esta clase contiene el estado general del servidor (sin la lógica relacionada
 * con cada sala particular)
 */
class NCServerManager {

	// Primera habitación del servidor
	final static byte INITIAL_ROOM = 'A';
	final static byte FINAL_ROOM = 'Z';
	final static String ROOM_PREFIX = "Room";
	// Siguiente habitación que se creará
	byte nextRoom;
	// Usuarios registrados en el servidor
	private Set<String> users = new HashSet<String>();
	// Habitaciones actuales asociadas a sus correspondientes RoomManagers
	private Map<String, NCRoomManager> rooms = new HashMap<String, NCRoomManager>();

	NCServerManager() {
		nextRoom = INITIAL_ROOM;
	}

	// Método para registrar un RoomManager
	public String registerRoomManager(NCRoomManager rm) {
		// TODO Dar soporte para que pueda haber más de una sala en el servidor
		String roomName = ROOM_PREFIX + (char) nextRoom;
		rm.setRoomName(roomName);
		rooms.put(roomName, rm);
		nextRoom = (byte) ((int) nextRoom + 1);
		return roomName;
	}

	// Devuelve la descripción de las salas existentes
	public synchronized List<NCRoomDescription> getRoomList() {
		List<NCRoomDescription> roomDescriptions = new ArrayList<>();
		for (NCRoomManager manager : rooms.values()) {
			// TODO (hecho) Pregunta a cada RoomManager cuál es la descripción actual de su
			// sala
			NCRoomDescription description = manager.getDescription();
			// TODO (hecho) Añade la información al ArrayList
			roomDescriptions.add(description);
		}
		return roomDescriptions;
	}

	// Intenta registrar al usuario en el servidor.
	public synchronized boolean addUser(String user) {
		return users.add(user);

	}

	// Elimina al usuario del servidor
	public synchronized void removeUser(String user) {
		// TODO (hecho) Elimina al usuario del servidor
		users.remove(user);
		System.out.println("users updates, values :" + users);
	}

	// Un usuario solicita acceso para entrar a una sala y registrar su conexión en
	// ella
	public synchronized NCRoomManager enterRoom(String u, String room, Socket s) {
		// TODO (hecho) Verificamos si la sala existe
		if (rooms.keySet().contains(room)) {
			// TODO (hecho)Si la sala existe y si es aceptado en la sala entonces devolvemos
			// el RoomManager de la sala
			// añadimos al usuario
			users.add(u);
			this.rooms.get(room).registerUser(u, s);
			try {
				this.rooms.get(room).roomNotifications(NCMessage.OP_SEND_ENTRY, u);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return this.rooms.get(room);
		}
		// TODO (hecho)Decidimos qué hacer si la sala no existe (devolver error O crear
		// la sala)
		// DEVOLVEMOS ERROR MEDIANTE EL NULL
		return null;
	}

	// Un usuario deja la sala en la que estaba
	public synchronized void leaveRoom(String u, String room) {
		// TODO (hecho) Verificamos si la sala existe
		if (rooms.containsKey(room)) {
			// TODO (hecho)Si la sala existe sacamos al usuario de la sala
			this.rooms.get(room).removeUser(u);
			try {
				this.rooms.get(room).roomNotifications(NCMessage.OP_SEND_EXIT, u);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		// TODO Decidir qué hacer si la sala se queda vacía
		// SE QUEDA
	}

	public synchronized void sendMessage(String room, String u, String text) throws IOException {
		this.rooms.get(room).broadcastMessage(u, text);
	}

	/*
	 * public synchronized void sendPrivate(String room, String u, String d, String
	 * text) throws IOException { this.rooms.get(room).sendPrivate(u, d, text); }
	 */

	public synchronized boolean sendPrivate(String room, String u, String d, String text) throws IOException {
		if (rooms.get(room).getUserNames().contains(d)) {
			this.rooms.get(room).sendPrivate(u, d, text);
			return true;
		}
		return false;
	}
}

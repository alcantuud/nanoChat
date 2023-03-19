package es.um.redes.nanoChat.server.roomManager;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import es.um.redes.nanoChat.messageML.NCMessage;
import es.um.redes.nanoChat.messageML.NCRoomMessage;
import es.um.redes.nanoChat.messageML.NCTwoStringMessage;

// Unica clase hija de NCRoomManager

public class NCChatRoom extends NCRoomManager {

	private HashMap<String, Socket> users;
	private long lastTimeMessage;
	private List<String> userNames;

	public NCChatRoom() {
		this.roomName = null;
		this.users = new HashMap<String, Socket>();
		this.lastTimeMessage = 0;
		this.userNames = new LinkedList<String>();
	}

	public String getName() {
		return this.roomName;
	}

	public Map<String, Socket> getUsers() {
		return new HashMap<>(this.users);
	}

	public long getLastTimeMessage() {
		return lastTimeMessage;
	}

	public List<String> getUserNames() {
		return userNames;
	}

	@Override
	// Método para enviar un mensaje a los usuarios de la sala
	public void broadcastMessage(String u, String text) throws IOException {
		NCTwoStringMessage message = (NCTwoStringMessage) NCMessage.makeTwoStringMessage(NCMessage.OP_RECEIVE_MESSAGE,
				u, text);
		String rawMessage = message.toEncodedString();
		// Lo mandamos a todos menos al que lo envía
		for (String us : this.users.keySet()) {
			if (!us.equals(u)) {
				DataOutputStream dos = new DataOutputStream(this.users.get(us).getOutputStream());
				dos.writeUTF(rawMessage);
			}
		}
		// actualizamos la hora
		lastTimeMessage = System.currentTimeMillis();
	}

	// Metodo para enviar un mensaje privado
	public void sendPrivate(String u, String d, String text) throws IOException {
		NCMessage message = (NCTwoStringMessage) NCMessage.makeTwoStringMessage(NCMessage.OP_RCV_PRIVATE, u, text);
		String rawMessage = ((NCTwoStringMessage) message).toEncodedString();
		DataOutputStream dos = new DataOutputStream(this.users.get(d).getOutputStream());
		dos.writeUTF(rawMessage);
		// Actualizamos la hora del ultimo mensaje enviado
		lastTimeMessage = System.currentTimeMillis();
	}

	@Override
	// Método para enviar a todos los usuarios de la sala un mensaje
	public void roomNotifications(byte code, String u) throws IOException {
		NCRoomMessage message = (NCRoomMessage) NCMessage.makeRoomMessage(code, u);
		String rawMessage = message.toEncodedString();
		for (String us : this.users.keySet()) {
			if (!us.equals(u)) {
				DataOutputStream dos = new DataOutputStream(this.users.get(us).getOutputStream());
				dos.writeUTF(rawMessage);
			}
		}
	}

	@Override
	// Metodo para devolver la descripción de la sala
	public NCRoomDescription getDescription() {
		LinkedList<String> nicks = new LinkedList<>(this.userNames);
		return new NCRoomDescription(roomName, nicks, lastTimeMessage);
	}

	@Override
	// Método para registra a un usuaio en la sala
	public boolean registerUser(String u, Socket s) {
		if (!users.containsKey(u)) {
			userNames.add(u);
			users.put(u, s);
			return true;
		}
		return false;
	}

	@Override
	// Metodo para eliminar a un usuario de la sala
	public void removeUser(String u) {
		userNames.remove(u);
		users.remove(u);
	}

	@Override
	// Metodo para cambiar el nombre de la sala
	public void setRoomName(String roomName) {
		this.roomName = roomName;

	}

	@Override
	// Método pra obtener el nñumero de usuarios de una sala
	public int usersInRoom() {
		return this.userNames.size();
	}

}

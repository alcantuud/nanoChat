package es.um.redes.nanoChat.server.roomManager;

import java.io.IOException;
import java.net.Socket;
import java.util.List;
import java.util.Map;

//public abstract class NCRoomManager {
public abstract class NCRoomManager {
	String roomName;

	// Método para registrar a un usuario u en una sala (se anota también su socket
	// de comunicación)
	public abstract boolean registerUser(String u, Socket s);

	// Método para hacer llegar un mensaje enviado por un usuario u
	public abstract void broadcastMessage(String u, String message) throws IOException;

	// Método para obtener el nombre de la sala
	public abstract String getName();

	// Método para obtener el mapa de usuarios con su socket
	public abstract Map<String, Socket> getUsers();

	// Método para obtener la hora del último mensaje
	public abstract long getLastTimeMessage();

	// Método para obtener la lista de usuarios de la sala
	public abstract List<String> getUserNames();

	// Método para eliminar un usuario de una sala
	public abstract void removeUser(String u);

	// Método para nombrar una sala
	public abstract void setRoomName(String roomName);

	// Método para devolver la descripción del estado actual de la sala
	public abstract NCRoomDescription getDescription();

	// Método para devolver el número de usuarios conectados a una sala
	public abstract int usersInRoom();

	// Metodo para enviar un mensaje a los integrantes de una sala
	public abstract void roomNotifications(byte code, String u) throws IOException;

	// Metodo para enviar un mensaje privado
	public abstract void sendPrivate(String u, String d, String text) throws IOException;
}

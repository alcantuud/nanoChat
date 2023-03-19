package es.um.redes.nanoChat.server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import es.um.redes.nanoChat.messageML.NCMessage;
import es.um.redes.nanoChat.messageML.NCOneMessage;
import es.um.redes.nanoChat.messageML.NCRoomListMessage;
import es.um.redes.nanoChat.messageML.NCRoomMessage;
import es.um.redes.nanoChat.messageML.NCThreeStringMessage;
import es.um.redes.nanoChat.messageML.NCTwoStringMessage;
import es.um.redes.nanoChat.server.roomManager.NCChatRoom;
import es.um.redes.nanoChat.server.roomManager.NCRoomDescription;
import es.um.redes.nanoChat.server.roomManager.NCRoomManager;

/**
 * A new thread runs for each connected client
 */
public class NCServerThread extends Thread {

	private Socket socket = null;
	// Manager global compartido entre los Threads
	private NCServerManager serverManager = null;
	// Input and Output Streams
	private DataInputStream dis;
	private DataOutputStream dos;
	// Usuario actual al que atiende este Thread
	String user;
	// RoomManager actual (dependerá de la sala a la que entre el usuario)
	NCRoomManager roomManager;
	// Sala actual
	String currentRoom;

	// Inicialización de la sala
	public NCServerThread(NCServerManager manager, Socket socket) throws IOException {
		super("NCServerThread");
		this.socket = socket;
		this.serverManager = manager;
	}

	// Main loop
	public void run() {
		try {
			// Se obtienen los streams a partir del Socket
			dis = new DataInputStream(socket.getInputStream());
			dos = new DataOutputStream(socket.getOutputStream());
			// En primer lugar hay que recibir y verificar el nick
			receiveAndVerifyNickname();
			// Mientras que la conexión esté activa entonces...
			while (true) {
				// TODO Obtenemos el mensaje que llega y analizamos su código de operación
				NCMessage message = NCMessage.readMessageFromSocket(dis);
				switch (message.getOpcode()) {
				case NCMessage.OP_NEW_ROOM: {
					newRoom();
					break;
				}
				// TODO 1) si se nos pide la lista de salas se envía llamando a sendRoomList();
				case NCMessage.OP_ENTER_ROOM: {
					// TODO 2) Si se nos pide entrar en la sala entonces obtenemos el RoomManager de
					// la sala,
					// String room = ((NCRoomMessage)message).getName();
					NCRoomManager newRoomManager = this.serverManager.enterRoom(user,
							((NCRoomMessage) message).getName(), socket);
					// TODO 2) notificamos al usuario que ha sido aceptado y procesamos mensajes con
					// processRoomMessages()
					if (newRoomManager == null) {
						NCOneMessage resp = (NCOneMessage) NCMessage.makeOneMessage(NCMessage.OP_NO_ROOM);
						String rawResp = resp.toEncodedString();
						dos.writeUTF(rawResp);
					} else {
						NCMessage resp = NCMessage.makeOneMessage(NCMessage.OP_IN_ROOM);
						String rawResp = ((NCOneMessage) resp).toEncodedString();
						dos.writeUTF(rawResp);
						roomManager = (NCChatRoom) newRoomManager;
						currentRoom = roomManager.getDescription().roomName;
						processRoomMessages();
					}
					break;
				}
				// Procesamos que quiera obtener una lista de salas
				case NCMessage.OP_GET_ROOMS: {
					sendRoomList();
					break;
				}

				}

			}
		} catch (Exception e) {
			// If an error occurs with the communications the user is removed from all the
			// managers and the connection is closed
			System.out.println("* User " + user + " disconnected.");
			serverManager.leaveRoom(user, currentRoom);
			serverManager.removeUser(user);
		} finally {
			if (!socket.isClosed())
				try {
					socket.close();
				} catch (IOException e1) {
					e1.printStackTrace();
				}
		}
	}

	// Obtenemos el nick y solicitamos al ServerManager que verifique si está
	// duplicado
	private void receiveAndVerifyNickname() throws IOException {
		// La lógica de nuestro programa nos obliga a que haya un nick registrado antes
		// de proseguir
		boolean userOk = false;
		String rawMessage;
		NCRoomMessage message;
		String nick;
		String rawResponse;
		NCMessage response;
		// TODO Entramos en un bucle hasta comprobar que alguno de los nicks
		// proporcionados no está duplicado
		while (!userOk) {
			rawMessage = dis.readUTF();
			message = NCRoomMessage.readFromString(NCMessage.OP_NICK, rawMessage);
			// TODO (hecho) Extraer el nick del mensaje
			nick = message.getName();
			// TODO (hecho) Validar el nick utilizando el ServerManager - addUser()
			userOk = serverManager.addUser(nick);
			if (userOk) {
				// TODO (hecho)Contestar al cliente con el resultado (éxito o duplicado)
				user = nick;
				response = NCMessage.makeOneMessage(NCMessage.OP_NICK_OK);
			} else
				response = NCMessage.makeOneMessage(NCMessage.OP_NICK_DUPLICATED);

			rawResponse = ((NCOneMessage) response).toEncodedString();
			dos.writeUTF(rawResponse);
		}
	}

	// Mandamos al cliente la lista de salas existentes
	private void sendRoomList() throws IOException {
		// TODO (hecho) La lista de salas debe obtenerse a partir del RoomManager y
		// después enviarse mediante su mensaje correspondiente
		LinkedList<NCRoomDescription> salas = new LinkedList<>(serverManager.getRoomList());
		NCRoomListMessage rooms = (NCRoomListMessage) NCMessage.makeRoomListMessage(NCMessage.OP_ROOMS_INFO, salas);
		String rawResponse = rooms.toEncodedString();
		dos.writeUTF(rawResponse);
	}

	// Creamos una sala nueva
	private void newRoom() throws IOException {
		// Creamos el mensaje con el opcode correspondiente
		String name = serverManager.registerRoomManager(new NCChatRoom());
		NCMessage resp = NCMessage.makeRoomMessage(NCMessage.OP_ROOM_CREATED, name);
		String rawResp = ((NCRoomMessage) resp).toEncodedString();
		dos.writeUTF(rawResp);
	}

	// Metodo para interretar los comandos dentro de una sala
	private void processRoomMessages() {
		// TODO Comprobamos los mensajes que llegan hasta que el usuario decida salir de
		// la sala
		boolean exit = false;
		// Seguimos en el bucle hasta que se salga de la sala
		while (!exit) {
			NCMessage message;
			try {
				// TODO Se recibe el mensaje enviado por el usuario
				message = NCMessage.readMessageFromSocket(dis);
				// TODO Se analiza el código de operación del mensaje y se trata en consecuencia
				switch (message.getOpcode()) {
				// El usuario pide la información de la sala en la que está
				case NCMessage.OP_GET_ROOM_INFO: {
					NCRoomDescription description = ((NCChatRoom) roomManager).getDescription();
					List<NCRoomDescription> rooms = new ArrayList<>();
					rooms.add(description);
					NCMessage resp = NCMessage.makeRoomListMessage(NCMessage.OP_ROOM_INFO, rooms);
					String rawResponse = ((NCRoomListMessage) resp).toEncodedString();
					dos.writeUTF(rawResponse);
					break;
				}
				// El usuario dice que quiere salir
				case NCMessage.OP_EXIT: {
					serverManager.leaveRoom(user, currentRoom);
					exit = true;
					break;
				}
				// El usuario comunica que quiere enviar un mensaje
				case NCMessage.OP_SEND_MESSAGE: {
					String user = ((NCTwoStringMessage) message).getName();
					String text = ((NCTwoStringMessage) message).getText();
					serverManager.sendMessage(currentRoom, user, text);
					break;
				}

				case NCMessage.OP_SEND_PRIVATE: {
					String user = ((NCThreeStringMessage) message).getName();
					String dest = ((NCThreeStringMessage) message).getDest();
					String text = ((NCThreeStringMessage) message).getText();
					if (!serverManager.sendPrivate(currentRoom, user, dest, text)) {
						NCMessage resp = NCMessage.makeOneMessage(NCMessage.OP_PRIVATE_WRONG);
						String rawResponse = ((NCOneMessage) resp).toEncodedString();
						dos.writeUTF(rawResponse);
						break;
					} else {
						NCMessage resp = NCMessage.makeOneMessage(NCMessage.OP_PRIVATE_SENT);
						String rawResponse = ((NCOneMessage) resp).toEncodedString();
						dos.writeUTF(rawResponse);
						break;
					}
				}
				}
			} catch (IOException e) {
				// Tratamos la excepción
				exit = true;
				System.out.println("The user " + user + " left unexpectedly");
			}
		}
	}
}

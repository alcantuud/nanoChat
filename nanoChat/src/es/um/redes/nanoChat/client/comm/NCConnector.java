package es.um.redes.nanoChat.client.comm;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
//import java.sql.Date;
//import java.text.SimpleDateFormat;
import java.util.List;

import es.um.redes.nanoChat.messageML.NCMessage;
import es.um.redes.nanoChat.messageML.NCOneMessage;
import es.um.redes.nanoChat.messageML.NCRoomListMessage;
import es.um.redes.nanoChat.messageML.NCRoomMessage;
import es.um.redes.nanoChat.messageML.NCThreeStringMessage;
import es.um.redes.nanoChat.messageML.NCTwoStringMessage;
import es.um.redes.nanoChat.server.roomManager.NCRoomDescription;

//Esta clase proporciona la funcionalidad necesaria para intercambiar mensajes entre el cliente y el servidor de NanoChat
public class NCConnector {
	//private static final boolean VERBOSE_MODE = true; //poner a false cuando ya esté la práctica
	//private static final SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yy HH:mm:ss.SSS");
	
	private Socket socket;
	protected DataOutputStream dos;
	protected DataInputStream dis;

	public NCConnector(InetSocketAddress serverAddress) throws UnknownHostException, IOException {
		//TODO (hecho) Se crea el socket a partir de la dirección proporcionada
		socket = new Socket(serverAddress.getAddress(), serverAddress.getPort());
		//TODO (hecho) Se extraen los streams de entrada y salida
		dos = new DataOutputStream(socket.getOutputStream());
		dis = new DataInputStream(socket.getInputStream());
	}


	//Método para registrar el nick en el servidor. Nos informa sobre si la inscripción se hizo con éxito o no.
	public boolean registerNickname_UnformattedMessage(String nick) throws IOException {
		//Funcionamiento resumido: SEND(nick) and RCV(NICK_OK) or RCV(NICK_DUPLICATED)
		//TODO (hecho)Enviamos una cadena con el nick por el flujo de salida
		dos.writeUTF(nick);
		//TODO (hecho)Leemos la cadena recibida como respuesta por el flujo de entrada
		String rcv = dis.readUTF();
		//TODO (hecho) Si la cadena recibida es NICK_OK entonces no está duplicado (en función de ello modificar el return)
		boolean resp = false;
		if(rcv.equals("NICK_OK")) {
			resp = true;
		}
		return resp;
	}

	//Método para registrar el nick en el servidor. Nos informa sobre si la inscripción se hizo con éxito o no.
	public boolean registerNickname(String nick) throws IOException {
		//Funcionamiento resumido: SEND(nick) and RCV(NICK_OK) or RCV(NICK_DUPLICATED)
		//Creamos un mensaje de tipo RoomMessage con opcode OP_NICK en el que se inserte el nick
		NCRoomMessage message = (NCRoomMessage) NCMessage.makeRoomMessage(NCMessage.OP_NICK, nick);
		//Obtenemos el mensaje de texto listo para enviar
		String rawMessage = message.toEncodedString();
		//Escribimos el mensaje en el flujo de salida, es decir, provocamos que se envíe por la conexión TCP
		dos.writeUTF(rawMessage);
		//TODO Leemos el mensaje recibido como respuesta por el flujo de entrada
		NCMessage resp = NCMessage.readMessageFromSocket(dis);
		//TODO (hecho) Analizamos el mensaje para saber si está duplicado el nick (modificar el return en consecuencia)
		if (resp.getOpcode() == NCMessage.OP_NICK_OK)
			return true;
		return false;
	}
	
	// Metodo que devuelve todas las salas disponibles 
	public List<NCRoomDescription> getRooms() throws IOException {
		// Funcionamiento resumido: SND(GET_ROOMS) and RCV(ROOM_LIST)
		NCOneMessage message = (NCOneMessage) NCMessage.makeOneMessage(NCMessage.OP_GET_ROOMS);
		// Obtenemos el mensaje de texto listo para enviar
		String rawMessage = message.toEncodedString();
		// Escribimos el mensaje en el flujo de salida, es decir, provocamos que se
		// envíe por la conexión TCP
		dos.writeUTF(rawMessage);
		NCMessage response = NCMessage.readMessageFromSocket(dis);
		if (response.getOpcode() == NCMessage.OP_ROOMS_INFO) {
			return ((NCRoomListMessage )response).getRooms();
		}
		return null;
	}

	//Método para solicitar la entrada en una sala
	public boolean enterRoom(String room) throws IOException {
		//Funcionamiento resumido: SND(ENTER_ROOM<room>) and RCV(IN_ROOM) or RCV(REJECT)
		NCRoomMessage message = (NCRoomMessage) NCMessage.makeRoomMessage(NCMessage.OP_ENTER_ROOM, room);
		String rawMessage = message.toEncodedString();
		dos.writeUTF(rawMessage);
		NCMessage resp = NCMessage.readMessageFromSocket(dis);
		if (resp.getOpcode() == NCMessage.OP_IN_ROOM) 
			return true;
		return false;
	}

	//Método para salir de una sala
	public void leaveRoom(String room) throws IOException {
		//Funcionamiento resumido: SND(EXIT_ROOM)
		//TODO (hecho) completar el método
		NCMessage message = NCMessage.makeOneMessage(NCMessage.OP_EXIT);
		String rawMessage = ((NCOneMessage)message).toEncodedString();
		dos.writeUTF(rawMessage);
	}

	//Método que utiliza el Shell para ver si hay datos en el flujo de entrada
	public boolean isDataAvailable() throws IOException {
		return (dis.available() != 0);
	}

	//Método para pedir la descripción de una sala
	public NCRoomDescription getRoomInfo(String room) throws IOException {
		//Funcionamiento resumido: SND(GET_ROOMINFO) and RCV(ROOMINFO)
		//TODO (hecho) Construimos el mensaje de solicitud de información de la sala específica
		NCRoomMessage message = new NCRoomMessage(NCMessage.OP_GET_ROOM_INFO, room);
		String rawMessage = message.toEncodedString();
		dos.writeUTF(rawMessage);
		//TODO (hecho) Recibimos el mensaje de respuesta
		NCMessage resp =  NCMessage.readMessageFromSocket(dis);
		if (resp.getOpcode() == NCMessage.OP_ROOM_INFO) {
			return ((NCRoomListMessage)resp).getRooms().get(0);

		}
		return null;
	}
	
	// Método para crear una sala nueva
	public String newRoom () throws IOException {
		// Construimos un mensaje que informa de que se quiere crear una nueva sala
		NCOneMessage message = (NCOneMessage) NCMessage.makeOneMessage(NCMessage.OP_NEW_ROOM);
		String rawMessage = message.toEncodedString();
		dos.writeUTF(rawMessage);
		// Recibimos el mensaje de respuesta
		NCMessage resp =  NCMessage.readMessageFromSocket(dis);
		if (resp.getOpcode() == NCMessage.OP_ROOM_CREATED) {
			return ((NCRoomMessage)resp).getName();
		}
		return null;
	}

	//Método para cerrar la comunicación con la sala
	//TODO (Opcional) Enviar un mensaje de salida del servidor de Chat
	public void disconnect() {
		try {
			if (socket != null) {
				socket.close();
			}
		} catch (IOException e) {
		} finally {
			socket = null;
		}
	}
	

	//IMPORTANTE!!
	//TODO (hecho) Es necesario implementar métodos para recibir y enviar mensajes de chat a una sala
	
	//Método para enviar mensages
	public void sendMessage(String user, String text) throws IOException {
		// Creamos y mandamos un mensaje que codifica el usuario con el código correspondiente, quien lo envía y que envía
		NCTwoStringMessage message = (NCTwoStringMessage) NCMessage.makeTwoStringMessage(NCMessage.OP_SEND_MESSAGE, user, text);
		String rawMessage = message.toEncodedString();
		dos.writeUTF(rawMessage);
	}
	
	//Método para recibir mensajes
	public NCMessage receiveMessage() {
		try {
			NCMessage message = NCMessage.readMessageFromSocket(dis);
			return message;
		} catch (IOException e) {
			return null;
		}
		
	}

	
	// Metodo para enviar mensajes privador
	public boolean sendPrivate(String user,String dest, String text) throws IOException {
		// Crea y envía el mensaje que se envía de forma privada con el usuario que lo manda, el destinatario y el propio mensaje
		NCThreeStringMessage message = (NCThreeStringMessage) NCMessage.makeThreeStringMessage(NCMessage.OP_SEND_PRIVATE, user, dest, text);
		String rawMessage = message.toEncodedString();
		dos.writeUTF(rawMessage);
		NCMessage resp = NCMessage.readMessageFromSocket(dis);
		if (resp.getOpcode() == NCMessage.OP_PRIVATE_WRONG) 
			return false;
		return true;
	}

	
	/*private void showMessageInConsole(String message) {
		if (VERBOSE_MODE) {
			Date currentDateTime = new Date(System.currentTimeMillis());
			String currentDateTimeText = formatter.format(currentDateTime);
			System.out.println("\nMESSAGE (" + currentDateTimeText + ") ··········");
			System.out.println(message);
			System.out.println("·················(end of message\n");
		}
	}*/

}

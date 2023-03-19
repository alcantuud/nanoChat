package es.um.redes.nanoChat.messageML;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import es.um.redes.nanoChat.server.roomManager.NCRoomDescription;

public abstract class NCMessage {
	protected byte opcode;

	// TODO (hecho)IMPLEMENTAR TODAS LAS CONSTANTES RELACIONADAS CON LOS CODIGOS DE
	// OPERACION
	public static final byte OP_INVALID_CODE = 0;
	// Codigos relacionados con el nick
	public static final byte OP_NICK = 1;
	public static final byte OP_NICK_OK = 2;
	public static final byte OP_NICK_DUPLICATED = 3;

	// Codigo para entrar a una sala y las posibles respuestas
	public static final byte OP_ENTER_ROOM = 4;
	public static final byte OP_IN_ROOM = 5;
	public static final byte OP_NO_ROOM = 6;

	// Codigos para solicitar la informacion de una sala
	public static final byte OP_GET_ROOM_INFO = 7;
	public static final byte OP_ROOM_INFO = 8;

	// Codigos para solicitar la lista de todas las salas
	public static final byte OP_ROOMS_INFO = 9;
	public static final byte OP_GET_ROOMS = 10;

	// Codigo para crear una nueva sala y la respuesta
	public static final byte OP_NEW_ROOM = 11;
	public static final byte OP_ROOM_CREATED = 12;

	// Codigo para salir de una sala
	public static final byte OP_EXIT = 13;

	// Codigo para enviar un mensaje
	public static final byte OP_SEND_MESSAGE = 14;

	// Codigo que noifica que has recibido un mensaje
	public static final byte OP_RECEIVE_MESSAGE = 15;

	// Codigos para notificar la entrada y salida de usuarios
	public static final byte OP_SEND_EXIT = 16;
	public static final byte OP_SEND_ENTRY = 17;

	// Codigo para enviar un mensaje privado
	public static final byte OP_SEND_PRIVATE = 18;

	// Codigo que notifica que has recibido un mensaje privado
	public static final byte OP_RCV_PRIVATE = 19;
	// Código que notifica que un mensaje privado no se pudo enviar
	public static final byte OP_PRIVATE_WRONG = 20;
	public static final byte OP_PRIVATE_SENT = 21;

	public static final char DELIMITER = ':'; // Define el delimitador
	public static final char END_LINE = '\n'; // Define el carácter de fin de línea

	public static final String OPERATION_MARK = "operation";
	public static final String MESSAGE_MARK = "message";

	/**
	 * Códigos de los opcodes válidos El orden es importante para relacionarlos con
	 * la cadena que aparece en los mensajes
	 */
	private static final Byte[] _valid_opcodes = { OP_NICK, OP_NICK_OK, OP_NICK_DUPLICATED, OP_ENTER_ROOM, OP_IN_ROOM,
			OP_NO_ROOM, OP_GET_ROOM_INFO, OP_ROOM_INFO, OP_ROOMS_INFO, OP_GET_ROOMS, OP_NEW_ROOM, OP_ROOM_CREATED,
			OP_EXIT, OP_SEND_MESSAGE, OP_RECEIVE_MESSAGE, OP_SEND_EXIT, OP_SEND_ENTRY, OP_SEND_PRIVATE, OP_RCV_PRIVATE,
			OP_PRIVATE_WRONG, OP_PRIVATE_SENT };

	/**
	 * cadena exacta de cada orden
	 */
	private static final String[] _valid_operations_str = { "Nick", "NickOk", "DuplicatedNick", "EnterRoom", "InRoom",
			"NoRoom", "GetRoomInfo", "RoomInfo", "RoomsInfo", "GetRooms", "NewRoom", "RoomCreated", "Exit",
			"SendMessage", "ReceiveMessage", "SendExit", "SendEntry", "SendPrivate", "RcvPrivate", "PrivateWrong",
			"PrivateSent" };

	private static Map<String, Byte> _operation_to_opcode;
	private static Map<Byte, String> _opcode_to_operation;

	static {
		_operation_to_opcode = new TreeMap<>();
		_opcode_to_operation = new TreeMap<>();
		for (int i = 0; i < _valid_operations_str.length; ++i) {
			_operation_to_opcode.put(_valid_operations_str[i].toLowerCase(), _valid_opcodes[i]);
			_opcode_to_operation.put(_valid_opcodes[i], _valid_operations_str[i]);
		}
	}

	/**
	 * Transforma una cadena en el opcode correspondiente
	 */
	protected static byte stringToOpcode(String opStr) {
		return _operation_to_opcode.getOrDefault(opStr.toLowerCase(), OP_INVALID_CODE);
	}

	/**
	 * Transforma un opcode en la cadena correspondiente
	 */
	protected static String opcodeToString(byte opcode) {
		return _opcode_to_operation.getOrDefault(opcode, null);
	}

	// Devuelve el opcode del mensaje
	public byte getOpcode() {
		return opcode;
	}

	// Método que debe ser implementado por cada subclase de NCMessage
	protected abstract String toEncodedString();

	// Analiza la operación de cada mensaje y usa el método readFromString() de cada
	// subclase para parsear
	public static NCMessage readMessageFromSocket(DataInputStream dis) throws IOException {
		String message = dis.readUTF();
		String regexpr = "<" + MESSAGE_MARK + ">(.*?)</" + MESSAGE_MARK + ">";
		Pattern pat = Pattern.compile(regexpr, Pattern.DOTALL);
		Matcher mat = pat.matcher(message);
		if (!mat.find()) {
			System.out.println("Mensaje mal formado:\n" + message);
			return null;
			// Message not found
		}
		String inner_msg = mat.group(1); // extraemos el mensaje

		String regexpr1 = "<" + OPERATION_MARK + ">(.*?)</" + OPERATION_MARK + ">";
		Pattern pat1 = Pattern.compile(regexpr1);
		Matcher mat1 = pat1.matcher(inner_msg);
		if (!mat1.find()) {
			System.out.println("Mensaje mal formado:\n" + message);
			return null;
			// Operation not found
		}
		String operation = mat1.group(1); // extraemos la operación

		byte code = stringToOpcode(operation);
		if (code == OP_INVALID_CODE)
			return null;

		switch (code) {
		// TODO Parsear el resto de mensajes
		case OP_NICK: {
			return NCRoomMessage.readFromString(code, message);

		}
		case OP_NICK_OK: {
			return makeOneMessage(code);

		}
		case OP_NICK_DUPLICATED: {
			return makeOneMessage(code);

		}
		case OP_ENTER_ROOM: {
			return NCRoomMessage.readFromString(code, message);

		}
		case OP_IN_ROOM: {
			return makeOneMessage(code);

		}
		case OP_NO_ROOM: {
			return makeOneMessage(code);

		}
		case OP_GET_ROOMS: {
			return makeOneMessage(code);

		}
		case OP_ROOMS_INFO: {
			return NCRoomListMessage.readFromString(code, message);

		}
		case OP_GET_ROOM_INFO: {
			return NCRoomMessage.readFromString(code, message);

		}
		case OP_ROOM_INFO: {
			return NCRoomListMessage.readFromString(code, message);
		}
		case OP_NEW_ROOM: {
			return makeOneMessage(code);

		}
		case OP_ROOM_CREATED: {
			return NCRoomMessage.readFromString(code, message);

		}
		case OP_EXIT: {
			return makeOneMessage(code);

		}
		case OP_SEND_MESSAGE: {
			return NCTwoStringMessage.readFromString(code, message);

		}
		case OP_RECEIVE_MESSAGE: {
			return NCTwoStringMessage.readFromString(code, message);

		}
		case OP_SEND_EXIT: {
			return NCRoomMessage.readFromString(code, message);

		}
		case OP_SEND_ENTRY: {
			return NCRoomMessage.readFromString(code, message);

		}
		case OP_SEND_PRIVATE: {
			return NCThreeStringMessage.readFromString(code, message);

		}
		case OP_RCV_PRIVATE: {
			return NCTwoStringMessage.readFromString(code, message);

		}
		case OP_PRIVATE_WRONG: {
			return makeOneMessage(code);
		}
		case OP_PRIVATE_SENT: {
			return makeOneMessage(code);
		}
		default:
			System.err.println("Unknown message type received:" + code);
			return null;
		}

	}

	// TODO Programar el resto de métodos para crear otros tipos de mensajes

	// Metodos para crear mensajes de cada Sublase del propio método
	public static NCMessage makeOneMessage(byte code) {
		return new NCOneMessage(code);
	}

	public static NCMessage makeRoomMessage(byte code, String room) {
		return new NCRoomMessage(code, room);
	}

	public static NCMessage makeRoomListMessage(byte code, List<NCRoomDescription> rooms) {
		return new NCRoomListMessage(code, rooms);
	}

	public static NCMessage makeTwoStringMessage(byte code, String user, String text) {
		return new NCTwoStringMessage(code, user, text);
	}

	public static NCMessage makeThreeStringMessage(byte code, String user, String dest, String text) {
		return new NCThreeStringMessage(code, user, dest, text);
	}
}

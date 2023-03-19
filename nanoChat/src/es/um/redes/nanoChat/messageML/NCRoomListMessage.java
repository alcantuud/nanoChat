package es.um.redes.nanoChat.messageML;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import es.um.redes.nanoChat.server.roomManager.NCRoomDescription;

public class NCRoomListMessage extends NCMessage {

	private static final String RE_ROOMS = "<rooms>(.*?)</rooms>";
	private static final String ROOMS_MARK = "rooms";
	private static final String RE_ROOM = "<room>(.*?)</room>";
	private static final String ROOM_MARK = "room";
	private static final String RE_NAME = "<name>(.*?)</name>";
	private static final String NAME_MARK = "name";
	private static final String RE_TIME = "<time>(.*?)</time>";
	private static final String TIME_MARK = "time";
	private static final String RE_NICK = "<nick>(.*?)</nick>";
	private static final String NICK_MARK = "nick";
	private static final String RE_NICKS = "<nicks>(.*?)</nicks>";
	private static final String NICKS_MARK = "nicks";

	private List<NCRoomDescription> rooms;

	/**
	 * Creamos un mensaje de tipo TwoList a partir del código de operación, del
	 * nombre, la lista de participantes de la sala y la hora
	 */
	public NCRoomListMessage(byte opcode, List<NCRoomDescription> rooms) {
		this.opcode = opcode;
		this.rooms = new ArrayList<NCRoomDescription>();
		for (int i = 0; i < rooms.size(); i++) {
			this.rooms.add(rooms.get(i));
		}
	}

	public List<NCRoomDescription> getRooms() {
		return Collections.unmodifiableList(this.rooms);
	}

	@Override
	public String toEncodedString() {

		StringBuffer sb = new StringBuffer();
		String member;
		NCRoomDescription room;

		sb.append("<" + MESSAGE_MARK + ">" + END_LINE);
		sb.append("<" + OPERATION_MARK + ">" + opcodeToString(opcode) + "</" + OPERATION_MARK + ">" + END_LINE); // Construimos
																													// el
		sb.append("<" + ROOMS_MARK + ">" + END_LINE);

		for (int i = 0; i < rooms.size(); i++) { // campo
			room = rooms.get(i);
			sb.append("<" + ROOM_MARK + ">" + END_LINE);
			sb.append("<" + NAME_MARK + ">" + room.getRoomName() + "</" + NAME_MARK + ">" + END_LINE);
			sb.append("<" + TIME_MARK + ">" + room.getTimeLastMessage() + "</" + TIME_MARK + ">" + END_LINE);

			sb.append("<" + NICKS_MARK + ">" + END_LINE);
			for (int j = 0; j < room.getMembers().size(); j++) {
				member = room.getMembers().get(j);
				sb.append("<" + NICK_MARK + ">" + member + "</" + NICK_MARK + ">" + END_LINE);
			}
			sb.append("</" + NICKS_MARK + ">" + END_LINE);
			sb.append("</" + ROOM_MARK + ">" + END_LINE);

		}
		sb.append("</" + ROOMS_MARK + ">" + END_LINE);
		sb.append("</" + MESSAGE_MARK + ">" + END_LINE);

		return sb.toString(); // Se obtiene el mensaje

	}

	public static NCRoomListMessage readFromString(byte code, String message) {
		List<NCRoomDescription> salas = new ArrayList<>();

		Pattern pat_rooms = Pattern.compile(RE_ROOMS, Pattern.DOTALL);
		Matcher mat_rooms = pat_rooms.matcher(message);

		// Comprobamos que se encentra una lista de salas
		if (mat_rooms.find()) {
			String found_rooms = mat_rooms.group(1);
			// Vemos si la lista no es vacia ni nula para poder pasar a buscar las salas que hay dentro
			if (found_rooms != null && !found_rooms.isEmpty()) {
				Pattern pat_room = Pattern.compile(RE_ROOM, Pattern.DOTALL);
				Matcher mat_room = pat_room.matcher(found_rooms);

				Pattern pat_name = Pattern.compile(RE_NAME);
				Pattern pat_time = Pattern.compile(RE_TIME);
				Pattern pat_nicks = Pattern.compile(RE_NICKS, Pattern.DOTALL);
				Pattern pat_nick = Pattern.compile(RE_NICK);
				boolean found_room = true;
				// Mientras encontremos salas vamos sacando los datos y creando nuevos NCRoomDesciptions que añadimos a un ArrayList
				while (found_room) {
					found_room = mat_room.find();
					if (found_room) {
						String room = mat_room.group(1);
						Matcher mat_name = pat_name.matcher(room);
						Matcher mat_time = pat_time.matcher(room);
						if (mat_name.find() && mat_time.find()) {
							String found_name = mat_name.group(1);
							long found_time = Long.parseLong(mat_time.group(1));
							List<String> users = new ArrayList<String>();
							Matcher mat_nicks = pat_nicks.matcher(room);
							if (mat_nicks.find()) {
								String found_nicks = mat_nicks.group(1);
								if (found_nicks != null && !found_nicks.isEmpty()) {
									Matcher mat_nick = pat_nick.matcher(found_nicks);
									boolean found_nick = true;
									while (found_nick) {
										found_nick = mat_nick.find();
										if (found_nick) {
											String nick = mat_nick.group(1);
											users.add(nick);
										}
									}
								}
							}
							salas.add(new NCRoomDescription(found_name, users, found_time));
						}
					}
				}

			}
		} else {
			System.out.println("Error en RoomListMessage: no se ha encontrado parametro.");
			return null;
		}
		return new NCRoomListMessage(code, salas);
	}

}

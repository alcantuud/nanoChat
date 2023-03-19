package es.um.redes.nanoChat.messageML;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NCTwoStringMessage extends NCMessage {

	private String name;
	private String text;

	// Constantes asociadas a las marcas específicas de este tipo de mensaje
	private static final String RE_NAME = "<name>(.*?)</name>";
	private static final String NAME_MARK = "name";
	private static final String RE_TEXT = "<text>(.*?)</text>";
	private static final String TEXT_MARK = "text";

	/**
	 * Creamos un mensaje de tipo TwoString a partir del código de operación y del
	 * nombre
	 */
	public NCTwoStringMessage(byte opcode, String name, String text) {
		this.opcode = opcode;
		this.name = name;
		this.text = text;
	}

	@Override
	// Pasamos los campos del mensaje a la codificación correcta en lenguaje de
	// marcas
	public String toEncodedString() {
		StringBuffer sb = new StringBuffer();

		sb.append("<" + MESSAGE_MARK + ">" + END_LINE);
		sb.append("<" + OPERATION_MARK + ">" + opcodeToString(opcode) + "</" + OPERATION_MARK + ">" + END_LINE); // Construimos
																													// el
																													// campo
		sb.append("<" + NAME_MARK + ">" + name + "</" + NAME_MARK + ">" + END_LINE);
		sb.append("<" + TEXT_MARK + ">" + text + "</" + TEXT_MARK + ">" + END_LINE);
		sb.append("</" + MESSAGE_MARK + ">" + END_LINE);

		return sb.toString(); // Se obtiene el mensaje

	}

	// Parseamos el mensaje contenido en message con el fin de obtener los distintos
	// campos
	public static NCTwoStringMessage readFromString(byte code, String message) {
		String found_name = null;
		String found_text = null;

		// Tienen que estar los campos porque el mensaje es de tipo TwoStringMessage
		Pattern pat_name = Pattern.compile(RE_NAME);
		Matcher mat_name = pat_name.matcher(message);
		Pattern pat_text = Pattern.compile(RE_TEXT);
		Matcher mat_text = pat_text.matcher(message);
		if (mat_name.find() && mat_text.find()) {
			// Name found
			found_name = mat_name.group(1);
			found_text = mat_text.group(1);
		} else {
			System.out.println("Error en TwoStringMessage: no se ha encontrado parametro.");
			return null;
		}

		return new NCTwoStringMessage(code, found_name, found_text);
	}

	// Devolvemos el nombre contenido en el mensaje
	public String getName() {
		return name;
	}

	// Devolvemos el texto contenido en el mensaje
	public String getText() {
		return text;
	}

}

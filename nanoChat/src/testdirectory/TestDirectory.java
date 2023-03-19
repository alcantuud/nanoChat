package testdirectory;

import java.io.IOException;

import es.um.redes.nanoChat.directory.connector.DirectoryConnector;

public class TestDirectory {
	
	public static void main(String[] args) throws IOException {
		
		String strToConvert = new String ("Hola 123");
		DirectoryConnector dc = new DirectoryConnector("localhost");
		String resp = dc.convertToUpper(strToConvert);
		System.out.println(resp);
	}

}

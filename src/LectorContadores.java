import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class Main {
    public static void main(String[] args) {

        File fichero = new File("src/lecturas_20261019.txt");

        // Leemos el archivo en su codificación correspondiente
        try (FileReader fr = new FileReader(fichero, StandardCharsets.ISO_8859_1)) {
            int c;
            while ((c = fr.read()) != -1) {
                System.out.print((char) c);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}

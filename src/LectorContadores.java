import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.TreeSet;


public class LectorContadores {
    public static void main(String[] args) {

        // Comprobamos que se haya indicado un fichero de entrada.
        if (args.length != 1) {
            System.out.println("Error: debes indicar el fichero de entrada.");
            System.exit(2);
        }

        File fichero = new File(args[0]);

        // Comprobamos que el fichero exista.
        if (!fichero.exists() || !fichero.isFile()) {
            System.out.println("Error: el fichero no existe.");
            System.exit(2);
        }

        // Comprobamos que el fichero no esté vacío.
        if (fichero.length() == 0) {
            System.out.println("Error: el fichero está vacío.");
            System.exit(2);
        }

        // Obtenemos la carpeta donde se encuentra el fichero de entrada.
        File carpeta = fichero.getAbsoluteFile().getParentFile();

        // Creamos los archivos de salida en la misma carpeta.
        File lecturas = new File(carpeta, "lecturas.dat");
        File incidencias = new File(carpeta, "incidencias.log");
        File resumen = new File(carpeta, "resumen.txt");

        // Creamos las colecciones necesarias para guardar los datos y organizar las lecturas de cada contador.
        ArrayList<String> numeros = new ArrayList<>();
        HashMap<Integer, ArrayList<Double>> contadores = new HashMap<>();
        HashMap<Integer, ArrayList<String>> nombresConEspacios = new HashMap<>();

        // Declaramos las variables que utilizaremos para almacenar los datos de cada línea y controlar el número de línea leído.
        String[] partes;
        int contLinea = 1;
        int contador;
        String abonado, estado = "";
        long fecha;
        double lectura;

        // Abrimos los archivos indicando la codificación correspondiente.
        // El fichero de entrada se lee en ISO-8859-1, mientras que los archivos de texto de salida se escriben en UTF-8.
        try (BufferedReader br = new BufferedReader(new FileReader(fichero, StandardCharsets.ISO_8859_1));
             RandomAccessFile raf = new RandomAccessFile(lecturas, "rw");
             BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(incidencias, true), StandardCharsets.UTF_8));
             BufferedWriter bw2 = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(resumen), StandardCharsets.UTF_8))) {

            // Recorremos el fichero binario(historico) para contar cuántos registros existen actualmente en el histórico.
            int totalRegistros = 0;

            while (raf.getFilePointer() < raf.length()) {
                raf.readInt();
                raf.readLong();
                raf.readDouble();
                raf.readUTF();

                totalRegistros++;
            }

            System.out.println("Registros anteriores: " + totalRegistros);

            //  Nos situamos al final del fichero binario(historico) para que las nuevas lecturas se añadan sin sobrescribir los registros anteriores.
            raf.seek(raf.length());

            // Leemos el fichero de entrada línea a línea para no tener que cargar todo su contenido en memoria.
            String linea;
            while ((linea = br.readLine()) != null) {
                boolean escribir = false;

                // Dividimos la línea en partes (;) y el -1 hace que puedan haber campos vacíos al final
                partes = linea.split(";", -1);

                // Hacemos las comprobaciones para decidir si van en "lecturas.dat" o "incidencias.log"
                if (linea.isEmpty()) {

                } else if (partes.length != 4) {
                    bw.write(contLinea + ";campos incompletos\n");
                } else if (!partes[0].matches("\\d{5}") || partes[0].equals("00000")) {
                    bw.write(contLinea + ";contador no válido\n");
                } else if (partes[1].trim().isEmpty()) {
                    bw.write(contLinea + ";abonado vacío\n");
                } else if (!comprobarFecha(partes[2])) {
                    bw.write(contLinea + ";fecha no válida\n");
                } else if (!partes[3].matches("\\d+,\\d{3}")) {
                    bw.write(contLinea + ";lectura no válida\n");
                } else if (numeros.contains(partes[0])) {
                    bw.write(contLinea + ";lectura duplicada\n");
                } else if (!numeros.contains(partes[0])) {
                    numeros.add(partes[0]);
                    escribir = true;
                }

                // Si la línea ha superado todas las comprobaciones, guardamos sus datos en el fichero binario(historico).
                if (escribir) {

                    // Guardamos los nombres de los abonados de cada contador para así después coger el nombre del último abonado
                    if (!nombresConEspacios.containsKey(Integer.parseInt(partes[0]))) {
                        nombresConEspacios.put(Integer.parseInt(partes[0]), new ArrayList<>());
                        nombresConEspacios.get(Integer.parseInt(partes[0])).add(partes[1]);
                    } else {
                        nombresConEspacios.get(Integer.parseInt(partes[0])).add(partes[1]);
                    }

                    raf.writeInt(Integer.parseInt(partes[0]));
                    raf.writeLong(Long.parseLong(partes[2].replace("-", "")));
                    raf.writeDouble(Double.parseDouble(partes[3].replace(",", ".")));
                    raf.writeUTF(partes[1].replace(" ", ""). replace(", ", ""));
                }

                contLinea++;
            }

            // Nos posicionamos al principo del archivo "lecturas.dat" y leemos el archivo
            raf.seek(0);

            while (raf.getFilePointer() < raf.length()) {
                contador = raf.readInt();
                fecha = raf.readLong();
                lectura = raf.readDouble();
                abonado = raf.readUTF();

                // Guardamos las lecturas de cada contador
                if (!contadores.containsKey(contador)) {
                    contadores.put(contador, new ArrayList<>());
                    contadores.get(contador).add(lectura);
                } else {
                    contadores.get(contador).add(lectura);
                }

            }

            // Utilizamos un TreeSet para ordenar los números de contador de menor a mayor antes de generar el resumen.
            TreeSet<Integer> contadoresOrdenados = new TreeSet<>(contadores.keySet());


            // Recorremos los contadores ordenados y obtenemos el número de lecturas y el último abonado de cada uno.
            for (int a : contadoresOrdenados) {
                int var = contadores.get(a).size();
                String ultimoNombre = nombresConEspacios.get(a).getLast();

                // Si el contador tiene más de una lectura, calculamos el consumo restando la penúltima lectura a la última.
                if (var > 1) {
                    double consumo = contadores.get(a).get(var - 1) - contadores.get(a).get(var - 2);
                    String consumoTexto = String.format(java.util.Locale.US, "%.3f", consumo).replace(".", ",");

                    // Según el consumo obtenido, asignamos un estado al contador
                    if (consumo > 0) {
                        estado = "OK";
                    } else if (consumo == 0) {
                        estado = "SIN_CONSUMO";
                    } else {
                        estado = "POSIBLE_SUSTITUCION";
                    }

                    bw2.write(String.format("%05d", a) + ";" + ultimoNombre + ";" + var + ";" + consumoTexto + ";" + estado + "\n");

                // Si solamente existe una lectura del contador, no podemos calcular el consumo, por lo que indicamos SIN_HISTORICO.
                } else if (var == 1) {
                    estado = "SIN_HISTORICO";
                    bw2.write(String.format("%05d", a) + ";" + ultimoNombre + ";" + var + ";-;" + estado + "\n");
                }
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    // Método que comprueba si una fecha tiene el formato AAAA-MM-DD y si es una fecha válida del calendario.
    private static boolean comprobarFecha(String parte) {

        DateTimeFormatter formato = DateTimeFormatter.ofPattern("uuuu-MM-dd").withResolverStyle(ResolverStyle.STRICT);

        try {
            LocalDate.parse(parte, formato);
            return true;
        } catch (DateTimeParseException e) {
            return false;
        }
    }
}

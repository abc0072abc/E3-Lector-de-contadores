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

        File fichero = new File("src/lecturas_20261019.txt");
        File lecturas = new  File("src/lecturas.dat");
        File incidencias = new  File("src/incidencias.log");
        File resumen = new  File("src/resumen.txt");

        String[] partes;
        ArrayList<String> numeros = new ArrayList<>();
        HashMap<Integer, ArrayList<Double>> contadores = new HashMap<>();
        HashMap<Integer, ArrayList<String>> ultimoAbonado = new HashMap<>();
        HashMap<Integer, ArrayList<String>> nombresConEspacios = new HashMap<>();


        int contLinea = 1;
        int contador;
        String abonado, estado = "";
        long fecha;
        double lectura;

        // Leemos el archivo en su codificación correspondiente
        try (BufferedReader br = new BufferedReader(new FileReader(fichero, StandardCharsets.ISO_8859_1));
             RandomAccessFile raf = new RandomAccessFile(lecturas, "rw");
             BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(incidencias, true), StandardCharsets.UTF_8));
             BufferedWriter bw2 = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(resumen), StandardCharsets.UTF_8))) {

            // Contamos los registros que ya existen en el histórico
            int totalRegistros = 0;

            while (raf.getFilePointer() < raf.length()) {
                raf.readInt();
                raf.readLong();
                raf.readDouble();
                raf.readUTF();

                totalRegistros++;
            }

            System.out.println("Registros anteriores: " + totalRegistros);

            // Nos posicionamos al final del archivo .dat para escribir
            raf.seek(raf.length());

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

                //Escribimos en "lecturas.dat"
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

            // Ordenamos los contadores de menor a mayor
            TreeSet<Integer> contadoresOrdenados = new TreeSet<>(contadores.keySet());


            for (int a : contadoresOrdenados) {
                int var = contadores.get(a).size();
                String ultimoNombre = nombresConEspacios.get(a).getLast();

                if (var > 1) {
                    double consumo = contadores.get(a).get(var - 1) - contadores.get(a).get(var - 2);
                    String consumoTexto = String.format(java.util.Locale.US, "%.3f", consumo).replace(".", ",");

                    if (consumo > 0) {
                        estado = "OK";
                    } else if (consumo == 0) {
                        estado = "SIN_CONSUMO";
                    } else {
                        estado = "POSIBLE_SUSTITUCION";
                    }

                    bw2.write(String.format("%05d", a) + ";" + ultimoNombre + ";" + var + ";" + consumoTexto + ";" + estado + "\n");

                } else if (var == 1) {
                    estado = "SIN_HISTORICO";
                    bw2.write(String.format("%05d", a) + ";" + ultimoNombre + ";" + var + ";-;" + estado + "\n");
                }
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

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

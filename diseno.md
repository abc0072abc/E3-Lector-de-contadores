# Diseño del registro binario

El fichero `lecturas.dat` almacena los registros de forma consecutiva, sin separadores ni relleno entre ellos.

Cada registro contiene los siguientes campos y ocupa los bytes indicados:

| Campo | Método de escritura | Bytes |
|---|---|---:|
| Contador | `writeInt()` | 4 |
| Fecha | `writeLong()` | 8 |
| Lectura | `writeDouble()` | 8 |
| Abonado | `writeUTF()` | 2 + longitud del texto |

## Orden de los campos

He elegido este orden porque primero se almacenan los datos numéricos, que tienen un tamaño fijo, y al final el nombre del abonado, que tiene una longitud variable (Y también es el orden en el que aparece en el enunciado).

Para leer cada registro se utiliza el mismo orden en el que se escribió:

1. `readInt()`
2. `readLong()`
3. `readDouble()`
4. `readUTF()`

De esta manera, se pueden recorrer todos los registros del fichero hasta llegar al final.
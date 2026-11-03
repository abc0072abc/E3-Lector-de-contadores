# Decisiones del programa

## 1. ¿Qué haces con una línea incompleta y por qué?

Cuando una línea no tiene exactamente cuatro campos separados por punto y coma, la considero incorrecta y la registro en `incidencias.log` con el motivo `campos incompletos`.

No la guardo en el histórico porque faltan datos necesarios para crear un registro válido. El programa continúa leyendo las siguientes líneas para no interrumpir el procesamiento del fichero.

## 2. ¿Cómo distingues un contador sustituido de una lectura errónea?

Comparo las dos últimas lecturas de cada contador y calculo el consumo restando la anterior a la última.

- Si el consumo es positivo, el estado es `OK`.
- Si es cero, el estado es `SIN_CONSUMO`.
- Si es negativo, el estado es `POSIBLE_SUSTITUCION`.

Un consumo negativo puede indicar que se ha cambiado el contador o que se ha introducido una lectura incorrecta. El programa no puede saber con seguridad cuál de las dos situaciones ha ocurrido, por lo que deja constancia para que se revise.

## 3. ¿Qué pasaría si el binario se abriera en modo sobrescribir?

Si el fichero se abriera en un modo que eliminase su contenido anterior, se perdería todo el histórico de lecturas almacenado.

Por eso utilizo `RandomAccessFile` en modo `rw` y me sitúo al final del fichero con `seek(raf.length())` antes de escribir los nuevos registros. Así se conservan los datos anteriores y las nuevas lecturas se añaden al final.
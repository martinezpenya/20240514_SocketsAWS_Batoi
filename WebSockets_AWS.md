---
title: WebSockets de Java en la nube (AWS)
subtitle: II Jornades d'intercanvi d'experiències en FPCloud
language: ES
author: David Martínez Peña [www.martinezpenya.es]
subject: FPCloud
keywords: [PRG, 2024, Programacion, Java]
IES: IES Eduardo Primo Marqués (Carlet) [www.ieseduardoprimo.es]
header: ${title} - ${subject} (ver. ${today}) 
footer:${currentFileName}.pdf - ${author} - ${IES} - ${pageNo}/${pageCount}
typora-root-url:${filename}/../
typora-copy-images-to:${filename}/../assets
---

[toc]

# Introducción

La intención de este documento es la de dar una perspectiva más realista del uso de sockets, ya que en lugar de usar la misma máquina del alumno como cliente y servidor, vamos a desplegar el servidor del socket en una máquina alojada en la nube de Amazon (AWS).

# Requisitos

Para realizar esta práctica guiada necesitamos:

- Acceso al Learner Lab proporcionado por el profesor. (https://awsacademy.instructure.com)
- Conocimientos sobre los websockets, IP's y puertos.
- Un dispositivo local con capacidad de ejecutar un cliente de socket, con acceso a los puertos e Ip's de AWS (Ojo con la red de conselleria)

# Guía paso a paso

## Preparar el entorno de la nube

### Iniciar Laboratorio

Lo primero que necesitamos es arrancar el laboratorio, para ello Accedemos al LMS del awsacademy, buscamos el Curso facilitado por el docente, accedemos a sus contenidos y a continuación al Learner Lab. (Si es la primera vez que accedemos debemos aceptar los términos de uso).

Inicialmente el laboratorio está en rojo:

![Estado inicial del laboratorio](/assets/LAB01.png)

Elegimos la opción `Start Lab` y esperamos a que aparezca el laboratorio en verde:

![Laboratorio iniciado correctamente](/assets/LAB02.png)

Por defecto el Learner Lab nos proporciona 100 dolares de saldo, y un tiempo de 4 horas, tras el cual se detendrán la mayoría de servicios que tengamos en marcha. Pero mientras quede saldo podemos volver a iniciar el Laboratorio y dispondremos de 4 horas más.

Una vez aparece en verde podemos hacer click sobre las letras AWS y aparecerá el Dashboard de AWS (debemos permitir las ventanas emergentes):

![Dashboard AWS](/assets/LAB03.png)

### Crear un entorno Cloud9

Cloud9 es un entorno de desarrollo en la nube que proporciona AWS asociado a una instáncia EC2 (máquina virtual en la nube).

El primer paso es crear este entorno, para ello buscamos Cloud9 en la parte superior del Dashboard:

![Buscar cloud9 en el dashboard](/assets/LAB04.png)

A continuación seleccionamos `Create environment`:

![Crear el entorno](/assets/LAB05.png)

En la siguiente ventana debemos especificar el **nombre** (`Name`), cambiaremos la **plataforma** a `Ubuntu Server 22.04 LTS`, también podemos ampliar el tiempo de Timeout para no tener problemas a `4 horas` i por último dentro de los **Network settings** elegiremos la conexión por `SSH`, el resto de opciones se quedan por defecto y pulsamos el botón naranja del final `Create`.

![Propiedades del cloud9](/assets/LAB06.png)

Si todo ha ido bien podemos seleccionar el botón `Open`:

![Abrir el entorno creado](/assets/LAB07.png)

Y deberíamos ver algo parecido a esto:

![Entorno cloud9 en ejecución](/assets/LAB08.png)

### Creación del servidor de Sockets

Primero cerraremos la ventana de bienvenida, a continuación creamos un nuevo fichero, por ejemplo `AWSServerSocket.java` con el siguiente código java:

```java
import java.io.*;
import java.net.*;

public class AWSServerSocket {
    
    private static final int PORT=11000;

    public static void main(String[] args) throws IOException, ClassNotFoundException {
        String FraseClient;
        String FraseMajuscules;
        ServerSocket serverSocket;
        Socket clientSocket;
        ObjectInputStream entrada;
        ObjectOutputStream eixida;
        serverSocket = new ServerSocket(PORT);
        System.out.println("Server iniciado y escuchando en el puerto "+ PORT);
        while (true) {
            clientSocket = serverSocket.accept();
            entrada = new ObjectInputStream(clientSocket.getInputStream());
            FraseClient = (String) entrada.readObject();

            System.out.println("La frase recibida es: " + FraseClient);

            eixida = new ObjectOutputStream(clientSocket.getOutputStream());
            FraseMajuscules = FraseClient.toUpperCase();
            System.out.println("El server devuelve la frase: " + FraseMajuscules);
            eixida.writeObject(FraseMajuscules);

            clientSocket.close();
            System.out.println("Server esperando una nueva conexión...");
        }
    }
}
```

Debería quedar algo así:

![Código fuente en cloud9](/assets/LAB09.png)

Y si iniciamos el servidor:

![Servidor en la nube en marcha](/assets/LAB10.png)

### Abrir el puerto en la instancia EC2 del cloud9

Ahora debemos volver a la pestaña donde tenemos el Dashboard de AWS y buscar EC2 (donde antes buscamos cloud9):

![Buscar EC2 en el dashboard](/assets/LAB11.png)

Una vez abierto elegimos la opción `Instancias (en ejecución)`:

![Elegir instancias en ejecución](/assets/LAB12.png)

Deberíamos tener al menos una Instancia, si tenemos más de una debemos buscar la que contenga el nombre de nuestra instancia cloud9, debemos marcar el check que tiene justo delante del nombre y a continuación elegir la pestaña `Seguridad`:

![Seleccionar la instancia y la pestaña seguridad](/assets/LAB13.png)

Si nos fijamos en las reglas de entrada del grupo de seguridad, solo tiene habilitada la entrada para el puerto 22 (SSH), a continuación hacemos click sobre el enlace del Grupo de seguridad:

![Reglas de entrada](/assets/LAB14.png)

Y añadiremos el puerto 11000 (o el que hayamos elegido para nuestro servidor) a las reglas de entrada, elegimos el botón `Editar reglas de entrada`, a continuación `Agregar regla` Elegimos `TCP Personalizado`, puerto 11000 y `AnywhereIpv4` y añadimos una descripción si lo deseamos:

![Editar reglas](/assets/LAB15.png)

Una vez hecho esto si volvemos a la pestaña Seguridad de nuestra instancia EC2 veremos la regla añadida.

### Dirección pública de la EC2

Necesitamos saber la DNS de IPv4 pública de nuestra instancia EC2 para acceder desde el cliente, marcamos el check de nuestra instancia y accedemos a la primera pestaña `Detalles`, y nos fijamos en la parte derecha y pulsaremos el botón de copiar y guardaremos esta información para más adelante:

![Dirección pública de la EC2](/assets/LAB16.png)

## Preparar el cliente local

En nuestro IDE preferido creamos un nuevo archivo `AWSClienteSocket.java` con el siguiente código:

```java
import java.io.*;
import java.net.Socket;
import java.util.Scanner;

public class AWSClienteSocket {

    private final static int PUERTO = 11000;
    private static final String DNSAWS = "ec2-54-173-21-231.compute-1.amazonaws.com";
    //private static final String DNSAWS = "127.0.0.1";

    public static void main(String[] args) throws IOException, ClassNotFoundException {
        Scanner in = new Scanner(System.in);
        System.out.print("Introduce la frase a enviar en minúsculas: ");
        String frase = in.nextLine();

        try (Socket socket = new Socket(DNSAWS, PUERTO)) {
            ObjectOutputStream salida = new ObjectOutputStream(new BufferedOutputStream(socket.getOutputStream()));
            System.out.println("Se envia la frase: " + frase);
            salida.writeObject(frase);
            salida.flush(); //vaciamos el buffer

            ObjectInputStream entrada = new ObjectInputStream(new BufferedInputStream(socket.getInputStream()));
            System.out.println("La frase recibida es: " + (String) entrada.readObject());
        } catch (IOException ex) {
            System.err.println("Error. De entrada salida.");
        }
    }
}
```

> Recuerda cambiar la constante `DNSAWS` por el `String` que corresponde con la dirección DNS IPv4 de tu instancia EC2 obtenida en el punto 3.1.5.

## Ejecución de prueba

### Desde el punto de vista del cliente

Una vez ejecutado el cliente debe aparecer algo similar a esto:

```sh
Introduce la frase a enviar en minúsculas
```

Escribimos nuestra frase, y al pular INTRO obtenemos el siguiente resultado:

```sh
Introduce la frase a enviar en minúsculas
esta frase está en minúsculas
Se envia la frase esta frase está en minúsculas
La frase recibida es: ESTA FRASE ESTÁ EN MINÚSCULAS
```

### Desde el punto de vista del servidor

La consola de salida del servidor por su parte debe haber registrado la conexión del cliente, la recepción de la frase, y la frase devuelta:

```sh
Server iniciado y escuchando en el puerto 11000
La frase recibida es: esta frase está en minúsculas
El server devuelve la frase: ESTA FRASE ESTÁ EN MINÚSCULAS
Server esperando una nueva conexión...
```

# Fuentes de información

- https://awsacademyinstructure.com

# Detección y Traducción de Lenguaje de Señas Chileno

Este repositorio contiene el código fuente y recursos asociados a la aplicación desarrollada como parte de un proyecto de tesis para la [detección y traducción de la lengua de señas chilena](https://app.roboflow.com/lengua-de-seas-chilena/deteccion-y-traduccion-de-lenguaje-de-senas-chilena/overview).

## Descripción General del Conjunto de Datos

El conjunto de datos es una recopilación de gestos del lenguaje de señas chileno, abarcando una diversidad demográfica significativa. Incluye gestos realizados por personas de edades comprendidas entre los 17 y 72 años, con diferentes tonos de piel, contexturas, marcas de edad, accesorios y tatuajes. Esta diversidad tiene como objetivo proporcionar un conjunto de datos amplio y representativo.

## Tecnologías Utilizadas

- Pytorch: Librería de aprendizaje profundo utilizada para implementar y entrenar modelos de redes neuronales.
- YoloV8: Modelo de detección de objetos utilizado para la identificación de gestos en tiempo real.
- Roboflow: Plataforma utilizada para la creación del conjunto de datos, el hosting de redes neuronales y el entrenamiento de modelos adicionales.
- Google Colab: Entorno en línea de Google para la ejecución de cuadernos Jupyter, utilizado para el entrenamiento de redes neuronales.
- Android Studio: Entorno de desarrollo integrado para la creación de la aplicación móvil en Android, implementada en el lenguaje Kotlin.
- Android 8: Versión del sistema operativo móvil para el cual se desarrolló la aplicación.

## Funciones Principales

- Detección y Traducción: Capacidad para detectar y traducir señas del alfabeto de la lengua de señas chilena a texto.
- Zoom y Selección de Cámara: Botones disponibles para ajustar el zoom y cambiar la cámara utilizada.
- Opciones de Accesibilidad: Posibilidad de cambiar el tema de la aplicación y ajustar el tamaño de las letras.
- Ajuste de Umbral de Detección: Permite modificar el umbral de detección para aceptar automáticamente las letras.
- Modos de Funcionamiento: Funciona tanto a través de internet utilizando los servidores de Roboflow como sin conexión mediante el modelo almacenado en la aplicación.

## Rendimiento

Se ha identificado que la aplicación presenta desafíos en la detección de las señas correspondientes a las letras O, P, R, S, V y Y. Estamos comprometidos a abordar y mejorar este rendimiento en futuras actualizaciones.

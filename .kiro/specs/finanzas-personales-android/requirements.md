# Requirements Document

## Introduction

Esta aplicación móvil para Android permite a los usuarios gestionar sus finanzas personales de manera offline, incluyendo el seguimiento de tarjetas de crédito, cuentas de ahorros, gastos y cálculos automáticos de disponibilidad financiera. La aplicación no requiere conexión a servicios bancarios externos, ya que el usuario ingresa manualmente toda la información.

## Glossary

- **Sistema**: La aplicación móvil de finanzas personales para Android
- **Usuario**: La persona que utiliza la aplicación para gestionar sus finanzas
- **Tarjeta de Crédito**: Instrumento de pago que permite compras diferidas con un cupo límite
- **Cupo Disponible**: Monto restante que el usuario puede gastar en una tarjeta de crédito
- **Diferido**: Número de cuotas en las que se divide una compra
- **Corte**: Fecha en la que se cierra el ciclo de facturación de una tarjeta de crédito
- **Cuenta de Ahorros**: Cuenta bancaria donde el usuario mantiene su dinero disponible
- **Gasto**: Transacción que reduce el saldo disponible del usuario
- **Disponibilidad Total**: Suma del saldo en cuenta de ahorros menos las obligaciones de tarjetas de crédito

## Requirements

### Requirement 1

**User Story:** Como usuario, quiero registrar múltiples tarjetas de crédito con su información básica, para poder gestionar todas mis tarjetas en un solo lugar

#### Acceptance Criteria

1. THE Sistema SHALL permitir crear un registro de tarjeta de crédito con nombre, cupo total, cupo disponible, fecha de corte y fecha de pago
2. THE Sistema SHALL permitir almacenar múltiples tarjetas de crédito simultáneamente
3. THE Sistema SHALL permitir editar la información de una tarjeta de crédito existente
4. THE Sistema SHALL permitir eliminar una tarjeta de crédito del sistema
5. THE Sistema SHALL mostrar una lista visual de todas las tarjetas registradas

### Requirement 2

**User Story:** Como usuario, quiero registrar mis cuentas de ahorros con su saldo disponible, para conocer cuánto dinero tengo realmente disponible

#### Acceptance Criteria

1. THE Sistema SHALL permitir crear un registro de cuenta de ahorros con nombre y saldo actual
2. THE Sistema SHALL permitir almacenar múltiples cuentas de ahorros simultáneamente
3. THE Sistema SHALL permitir editar el saldo de una cuenta de ahorros existente
4. THE Sistema SHALL permitir eliminar una cuenta de ahorros del sistema
5. WHEN el usuario registra un gasto, THE Sistema SHALL actualizar automáticamente el saldo de la cuenta de ahorros asociada

### Requirement 3

**User Story:** Como usuario, quiero registrar compras en mis tarjetas de crédito con opción de diferido, para llevar un control detallado de mis obligaciones

#### Acceptance Criteria

1. THE Sistema SHALL permitir registrar una compra con monto, descripción, fecha, tarjeta asociada y número de cuotas
2. WHEN el usuario registra una compra, THE Sistema SHALL reducir el cupo disponible de la tarjeta asociada por el monto total
3. THE Sistema SHALL calcular el valor de cada cuota dividiendo el monto total entre el número de cuotas
4. THE Sistema SHALL permitir registrar compras con diferido de 1 a 36 cuotas
5. THE Sistema SHALL almacenar el historial completo de compras realizadas

### Requirement 4

**User Story:** Como usuario, quiero que la aplicación calcule automáticamente cuánto debo pagar en cada corte de tarjeta, para saber mis obligaciones mensuales

#### Acceptance Criteria

1. THE Sistema SHALL calcular la suma de todas las cuotas que vencen en el próximo corte de cada tarjeta
2. THE Sistema SHALL mostrar el monto total a pagar por tarjeta en la fecha de corte correspondiente
3. WHEN una compra diferida tiene múltiples cuotas, THE Sistema SHALL distribuir las cuotas en los cortes subsecuentes según la fecha de compra
4. THE Sistema SHALL actualizar automáticamente los cálculos cuando se registren nuevas compras
5. THE Sistema SHALL mostrar un desglose detallado de qué compras componen el pago del corte

### Requirement 5

**User Story:** Como usuario, quiero ver cuánto dinero puedo gastar realmente, para tomar decisiones financieras informadas

#### Acceptance Criteria

1. THE Sistema SHALL calcular el disponible total sumando los saldos de todas las cuentas de ahorros
2. THE Sistema SHALL calcular la deuda total sumando todos los pagos pendientes de tarjetas de crédito
3. THE Sistema SHALL calcular el disponible real restando la deuda total del disponible en cuentas
4. THE Sistema SHALL mostrar estos tres valores de forma prominente en la pantalla principal
5. THE Sistema SHALL actualizar estos cálculos en tiempo real cuando se registren transacciones

### Requirement 6

**User Story:** Como usuario, quiero registrar gastos directos desde mi cuenta de ahorros, para mantener un control completo de mis finanzas

#### Acceptance Criteria

1. THE Sistema SHALL permitir registrar un gasto con monto, descripción, fecha y cuenta asociada
2. WHEN el usuario registra un gasto, THE Sistema SHALL reducir el saldo de la cuenta de ahorros asociada
3. THE Sistema SHALL permitir categorizar los gastos por tipo (alimentación, transporte, entretenimiento, servicios, otros)
4. THE Sistema SHALL almacenar el historial completo de gastos realizados
5. THE Sistema SHALL permitir editar o eliminar gastos registrados

### Requirement 7

**User Story:** Como usuario, quiero ver reportes visuales de mis finanzas, para entender mejor mis patrones de gasto

#### Acceptance Criteria

1. THE Sistema SHALL mostrar un resumen mensual de gastos por categoría
2. THE Sistema SHALL mostrar gráficos visuales del uso de tarjetas de crédito versus cupo total
3. THE Sistema SHALL mostrar la evolución del saldo en cuentas de ahorros en el tiempo
4. THE Sistema SHALL mostrar un calendario con las fechas de corte y pago de todas las tarjetas
5. THE Sistema SHALL permitir filtrar reportes por rango de fechas

### Requirement 8

**User Story:** Como usuario, quiero que la aplicación tenga un diseño intuitivo y agradable, para que sea fácil y placentero usarla diariamente

#### Acceptance Criteria

1. THE Sistema SHALL utilizar Material Design 3 como guía de diseño visual
2. THE Sistema SHALL utilizar una paleta de colores consistente y profesional
3. THE Sistema SHALL mostrar iconos claros para cada tipo de transacción y cuenta
4. THE Sistema SHALL proporcionar navegación intuitiva entre las diferentes secciones
5. THE Sistema SHALL responder a las interacciones del usuario con animaciones fluidas

### Requirement 9

**User Story:** Como usuario, quiero que mis datos se guarden localmente en el dispositivo, para mantener privacidad y poder usar la app sin conexión

#### Acceptance Criteria

1. THE Sistema SHALL almacenar todos los datos localmente en el dispositivo Android
2. THE Sistema SHALL persistir los datos entre sesiones de la aplicación
3. THE Sistema SHALL funcionar completamente sin conexión a internet
4. THE Sistema SHALL proteger los datos con las capacidades de seguridad del dispositivo
5. THE Sistema SHALL permitir exportar los datos a un archivo de respaldo

### Requirement 10

**User Story:** Como usuario, quiero recibir notificaciones sobre fechas importantes, para no olvidar los pagos de mis tarjetas

#### Acceptance Criteria

1. THE Sistema SHALL permitir configurar notificaciones para fechas de corte de tarjetas
2. THE Sistema SHALL permitir configurar notificaciones para fechas de pago de tarjetas
3. THE Sistema SHALL enviar notificaciones con 3 días de anticipación a las fechas configuradas
4. THE Sistema SHALL permitir al usuario activar o desactivar las notificaciones
5. THE Sistema SHALL mostrar el monto a pagar en la notificación

# Revisión de la rúbrica Lab_1

Fuentes: hoja `Lab_1` de `Notas Generales 2-2026.xlsx` y enunciado `TBD-Laboratorio 1.pdf`, secciones 4–6 y dominio Grupo 5.
Las secciones Civil y Ejecución contienen los mismos criterios y máximos:
200 puntos de código y 85 de presentación. Esta revisión no asigna una nota.
Los ceros de la planilla no se interpretan como resultados de una evaluación.

## Código y entregables

| Criterio | Máximo | Evidencia y resultado |
| --- | ---: | --- |
| Script de creación de BD | 5 | `createDatabase.sql`, equivalente a `database/01` a `05`; instalación comprobada en base vacía. |
| Dump con datos de prueba | 5 | `database/dump-demo.sql`, generado desde los datos originales y restaurado en una segunda base vacía con `ON_ERROR_STOP=1`; 5 usuarios, 2 centros y 5 paquetes. |
| README de instalación | 5 | `README.md`: Java, PostgreSQL, Keycloak, backend, frontend y restauración alternativa del dump. |
| Servicio REST, web y PostgreSQL | 20 | Spring Boot + React/Vite + PostgreSQL; compilaciones verificadas e integración HTTP con JWT reales. La revisión no incluye una nueva prueba automatizada de navegador. |
| CRUD de todas las tablas, paginación y consultas simples | 20 | Ocho recursos con operaciones CRUD; los nueve listados, incluida auditoría, tienen paginación y filtros. **Salvedad:** `auditoria_direccion` ofrece solo lectura; POST/PUT/DELETE devuelven 405 y un trigger impide alterar el historial. El criterio literal exige CRUD de todas las tablas: confirmar con el docente si acepta esta excepción. No eliminar la protección solo para aparentar cumplimiento. |
| Registro y autenticación | 20 | `RegistroPage.jsx`, `AuthController`, Keycloak y validación JWT/RBAC; registro de clientes y gestión del personal cubiertos por integración. |
| Consultas específicas | 80 | Cotejado con el Grupo 5: rastreo completo por código (punto 9) y alertas de repartidores/centros bajo 85 % (punto 10), además de la vista del punto 7. Implementación y casos ejecutados coinciden; la rúbrica no desglosa los 80 puntos por consulta. |
| Procedimientos almacenados | 15 | `database/03-storedProced.sql`: `asignar_ruta` y `registrar_entrega_fallida` coinciden con los puntos 3–4. Incluyen múltiples tablas y rollback; el cuarto fallo genera el retorno. Hay procedimientos adicionales para entrega exitosa y CRUD de asignaciones. |
| Triggers | 15 | `database/04-triggers.sql`: capacidad por peso/volumen y auditoría posterior al despacho coinciden con los puntos 5–6. Auditoría guarda dirección anterior/nueva, usuario y fecha; SQL e integración aprobadas. |
| Vista | 15 | `database/02-materialViews.sql`: `mv_rendimiento_entregas` agrupa por centro y repartidor, con porcentaje de entregas a tiempo e intentos promedio (punto 7). Incluye función de refresco concurrente y tarea diaria a las 03:00 de America/Santiago; refresco manual probado, no se esperó al disparo diario. |

## Contraste con los diez requisitos del Grupo 5

| Punto del enunciado | Estado | Evidencia concreta |
| --- | --- | --- |
| 1. CRUD completo | Parcial por excepción de auditoría | Ocho entidades con CRUD, auditoría con GET y filtros. El PDF tampoco declara una excepción al CRUD para la tabla de auditoría. Resolver con el docente antes de afirmar cumplimiento total. |
| 2. JWT y operaciones por rol | Coincide | `SecurityConfig` valida firma, issuer y audiencia, enlaza `sub` con usuario SQL y verifica rol. Cliente crea y rastrea sus envíos; repartidor actúa sobre sus rutas; despachador asigna y consulta todas. Pruebas incluyen 401, 403 y acceso ajeno. |
| 3. Asignar ruta | Coincide | `asignar_ruta` crea ruta, asociaciones, estado En reparto y un evento por paquete en la misma transacción; exceso de capacidad revierte los cambios. |
| 4. Entrega fallida | Coincide | Incrementa intentos, reprograma los primeros tres y genera paquete de retorno enlazado al cuarto (`intentos > 3`, no al tercero). La fecha compromiso original se conserva. |
| 5. Trigger de capacidad | Coincide | `check_route_capacity` suma peso y volumen de los paquetes de la ruta, compara con el vehículo y bloquea el exceso. Pruebas de ambos límites, igualdad exacta y concurrencia. |
| 6. Trigger de auditoría | Coincide | `audit_destination_change` actúa cuando `despachado_at` no es NULL y la dirección cambia; exige actor y guarda valores anterior/nuevo, usuario y fecha. La API configura el actor desde la identidad validada. |
| 7. Rendimiento por centro/repartidor | Coincide | Vista con agrupación solicitada, porcentaje de entregas a tiempo e intentos promedio. Los intentos son asignaciones resueltas y los pendientes se excluyen; porcentaje NULL cuando no hay entregas completadas. El enunciado no define un denominador diferente. Refresco por función y tarea diaria habilitada con `@EnableScheduling`. |
| 8. Índices | Coincide | HASH en `paquete.codigo`, B-tree en `paquete.estado`; índice compuesto en `evento_seguimiento(paquete_id, fecha, id)` también cubre el mínimo transversal de tres columnas críticas. |
| 9. Rastreo por código | Coincide | `GET /api/tracking/{codigo}` obtiene todos los eventos por igualdad de código, ordenados por fecha e ID y con comprobación de propiedad. Prueba de línea de tiempo de nueve eventos y rechazo al cliente ajeno. |
| 10. Alertas bajo 85 % | Coincide | `GET /api/reportes/alertas` usa 85 por defecto y consulta la vista para repartidores y centros. Centros agregan conteos, sin promediar porcentajes individuales. Comparación estricta `< 85`; exactamente 85 no alerta. |

### Requisitos transversales y aceptación

PostgreSQL, API REST sin ORM (JdbcTemplate con SQL explícito), frontend React y RBAC coinciden con la arquitectura exigida. Hay más de dos triggers y procedimientos; los dos procedimientos pedidos modifican múltiples tablas de forma atómica. README documenta arquitectura, despliegue, variables, endpoints y JSON; las diapositivas están presentes.

**Criterio crítico del PDF:** el sistema debe poder instalarse usando exclusivamente el README, o corresponde nota mínima. La configuración cloud requirió añadir un JDK completo, proxy Maven y almacén de certificados propios de esta máquina; estas son particularidades del entorno cloud, no sustitutos del requisito local de instalar JDK 21 del README. Se verificaron compilación, servicios y API en esta máquina, y el SQL se volvió a cargar desde cero en una base aislada. **No se ha verificado una máquina local limpia siguiendo únicamente el README, ni una sesión web completa en navegador.** Ese ensayo sigue recomendado antes de entregar.

El PDF también indica que la nota final promedia código y presentación solo cuando código es al menos 4,0. La existencia de artefactos y pruebas aprobadas no garantiza esa nota ni elimina la salvedad del CRUD.

## Presentación

| Criterio | Máximo | Evidencia y pendiente |
| --- | ---: | --- |
| Introducción | 5 | Diapositiva 2: problema, objetivo y usuarios. |
| Descripción del avance | 5 | Diapositiva 3; corregir la frase «CRUD de todas las entidades» para aclarar la auditoría de solo lectura. |
| Diagrama de arquitectura | 10 | Diapositiva 5; cuatro componentes y sus comunicaciones. |
| Demostración | 20 | Diapositivas 20–21, capturas y guion. Ensayar el flujo en vivo; las capturas no prueban que la demostración oral se haya realizado. |
| Fragmentos de código | 10 | Diapositivas 8–9 y 11–19. |
| Contribuciones | 5 | La diapositiva 22 y el guion incluyen el dump y la revisión de Mario, y las responsabilidades de cierre de Héctor (revisión de entrega, preparación de demostración y exposición de seguridad, registro y paginación). La sección ya no usa el historial de commits como criterio de contribución. Las responsabilidades asignadas deben distinguirse de las tareas realizadas. |
| Conclusiones | 10 | Diapositiva 23. |
| Máximo 15 minutos | 10 | Guion planificado de 13:55. Falta cronometrar una exposición real, incluyendo login y cambios de expositor. |
| Enlace al repositorio | 5 | Portada y cierre incluyen `github.com/bbastias7/Laboratorio-1-BDA`. |
| Archivo de diapositivas | 5 | `presentacion/Grupo5_Logistica.pptx` y `.pdf`, 24 diapositivas. Si se corrige la presentación, actualizar ambos para mantenerlos sincronizados. |

## Validación realizada

En el mismo checkout revisado, durante la configuración del entorno:

- Compilación del frontend: `npm ci` y `npm run build`, aprobadas.
- Backend: `mvnw package`, 27 pruebas; cero fallos, errores y omisiones.
- SQL: tres suites aprobadas (integridad, reportes y capacidad total).
- API: 183 respuestas verificadas con JWT reales, permisos, CRUD y procedimientos.
- Concurrencia: la edición no pisa una entrega confirmada y dos asignaciones simultáneas no exceden la capacidad.

Durante esta revisión se generó el dump desde una base nueva, se restauró en otra base nueva y se repitieron allí las tres suites SQL, todas aprobadas. Las bases se mantuvieron separadas de la aplicación. El dump no procede de la base utilizada para pruebas HTTP.

Tras recibir el enunciado se creó otra base vacía (`lab1_enunciado_check`) usando `createDatabase.sql` y se inició una API temporal en el puerto 8082. Se repitieron las tres suites SQL, las 183 comprobaciones HTTP y las pruebas de concurrencia, todas aprobadas. Se comprobó en las pruebas SQL que exactamente 85 % no genera alerta, y un porcentaje inferior sí. La API temporal se detuvo y la base temporal se eliminó al terminar.

Estos resultados prueban los casos ejecutados, no todos los escenarios posibles ni una nota garantizada.

## Reparto propuesto para dos responsables

Este reparto describe responsabilidades para el cierre; no atribuye retrospectivamente la implementación.

1. **Mario — base de datos y backend:** revisar/restaurar el dump, explicar procedimientos, triggers y vista, y explicar el cotejo de las consultas con el enunciado. Bloque de commit: dump de demostración e instrucciones de restauración.
2. **Héctor — revisión de entrega y presentación:** usar esta matriz para preparar la exposición, confirmar aportes reales y ensayar el flujo web. Bloque de commit: revisión documentada de Lab1 y corrección del resultado esperado del README. Completar y revisar los materiales de presentación sigue pendiente; este documento no acredita que Héctor ya haya realizado esas tareas.

La sección de contribuciones presenta aportes y responsabilidades del equipo, sin usar el historial de commits como criterio. Las responsabilidades de cierre no acreditan por sí solas que las tareas estén terminadas.

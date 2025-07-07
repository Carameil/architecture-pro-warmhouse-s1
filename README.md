📝 [Инструкция по развертыванию приложения](apps/README-DOCKER.md)  
🧪 [Postman коллекция для E2E тестирования](e2e-testing.postman_collection.json)

# Задание 1. Анализ и планирование

<aside>

Чтобы составить документ с описанием текущей архитектуры приложения, можно часть информации взять из описания компании и условия задания. Это нормально.

</aside>

### 1. Описание функциональности монолитного приложения

**Управление отоплением:**

- Пользователи могут удаленно управлять датчиками температуры через REST API
- Система поддерживает полный жизненный цикл сенсоров: создание, чтение, обновление, удаление (CRUD операции)
- Возможность обновления показаний температуры датчиков через отдельный endpoint
- Интеграция с внешним Temperature API для получения актуальных данных о температуре по местоположению
- Автоматическое обогащение данных сенсоров информацией из внешнего API при запросах

**Мониторинг температуры:**

- Пользователи могут просматривать список всех температурных датчиков в системе
- Система поддерживает получение детальной информации о конкретном датчике по ID
- Мониторинг состояния датчиков (active/inactive)
- Отслеживание местоположения датчиков (комнаты: Living Room, Bedroom, Kitchen)
- Система предоставляет health endpoint для проверки работоспособности приложения
- Автоматическое получение текущих показаний температуры из внешних источников

### 2. Анализ архитектуры монолитного приложения

**Технологический стек:**
- **Язык программирования:** Go 1.21
- **База данных:** PostgreSQL с использованием pgxpool для connection pooling
- **HTTP Framework:** Gin для обработки REST API запросов
- **Архитектура:** Монолитная трехслойная архитектура

**Архитектурные слои:**
- **Presentation Layer (handlers/):** Обработка HTTP запросов/ответов, валидация данных, сериализация JSON, оркестрация бизнес-логики
- **Business Logic Layer (services/):** Бизнес-логика интеграции с внешними API, обработка температурных данных
- **Data Access Layer (db/):** Прямая работа с PostgreSQL через нативные SQL запросы, управление подключениями

**Характеристики взаимодействия:**
- **Взаимодействие:** Полностью синхронное, все запросы обрабатываются последовательно
- **Интеграции:** HTTP клиент для обращения к внешнему Temperature API
- **Dependency Injection:** Ручное внедрение зависимостей через конструкторы
- **Обработка ошибок:** Централизованное логирование ошибок, стандартные HTTP коды ответов

**Ограничения:**
- **Масштабируемость:** Ограничена возможностями вертикального масштабирования
- **Развертывание:** Требует полной остановки приложения для обновлений
- **Fault Tolerance:** Отсутствие изоляции сбоев - падение любого компонента влияет на всю систему
- **Технологическая гибкость:** Привязка к единому технологическому стеку

### 3. Определение доменов и границы контекстов

**Основной домен:** Smart Home Management (Управление умным домом)

**Текущие поддомены (As-Is):**
- **Device Registry** - управление датчиками и устройствами (Core Domain)
- **Temperature Monitoring** - мониторинг и получение температурных данных (Core Domain)

**Будущие поддомены (To-Be):**
- **User Management** - управление пользователями и аутентификация (Supporting Domain)
- **Home Management** - управление домами и их конфигурацией (Core Domain)
- **Device Control** - управление различными типами устройств (освещение, ворота, камеры) (Core Domain)
- **Telemetry Management** - сбор и обработка телеметрии от устройств (Core Domain)
- **Scenario Management** - управление автоматическими сценариями работы (Core Domain)
- **External Integration** - интеграция с внешними API и сервисами (Supporting Domain)
- **Notification Service** - уведомления и алерты (Supporting Domain)

**Bounded Contexts:**
- **Device Context** - управление устройствами, их состоянием и конфигурацией
- **Monitoring Context** - сбор, обработка и хранение телеметрических данных
- **Control Context** - выполнение команд и управление устройствами
- **Scenario Context** - управление автоматическими сценариями работы устройств на основе показателей
- **User Context** - управление пользователями и доступами
- **House Context** - управление домами и локациями
- **Integration Context** - взаимодействие с внешними системами и API

### 4. Проблемы монолитного решения

**Технические проблемы:**
- **Tight Coupling (Тесная связанность):** Handlers напрямую зависят от DB и Services, изменения в одном компоненте требуют модификации других
- **Single Point of Failure:** Сбой любого компонента (БД, внешний API) приводит к недоступности всей системы
- **Отсутствие масштабируемости:** Невозможно масштабировать отдельные функции независимо
- **Технологические ограничения:** Привязка к Go и PostgreSQL ограничивает выбор оптимальных технологий для разных задач

**Бизнес-проблемы:**
- **Медленная разработка:** Команды не могут работать над разными функциями независимо
- **Сложность развертывания:** Обновление любой функции требует остановки всей системы
- **Ограниченная функциональность:** Текущий монолит поддерживает только температурные датчики, что не соответствует планам расширения

**Операционные проблемы:**
- **Сложная диагностика:** Логи всех компонентов смешаны, трудно изолировать проблемы
- **Ограниченный мониторинг:** Отсутствие детального мониторинга отдельных функций
- **Проблемы с нагрузкой:** Высокая нагрузка на одну функцию влияет на производительность всей системы

### 5. Визуализация контекста системы — диаграмма С4

[C4 Context Diagram](https://www.planttext.com?text=hLLHZzeu47xdLqpHFS2gsRJNtijA9vK2Mz2nkvHahxedP8X9B3bsQZkmKNN__MQS1CB2LVTm-sGyavdl--ORxoVhc75bBdjlXDhACk6GELVOkp0qx72R2fULcz9oizNASULkjpeF2yaKVHDObqYn2SSVWoLTFJyimfnPnmrUhSVqnOkxBQXwxqt2Tq9nc4p_d8-V7gF_qCQL1KlCXrCxWT5WXnc1BOnmtBRT4hwdc3rskNzwZ4VVfj7Jm_H_RUJls85RdVFWjvDcBQimrJNVoqYXhKiDjz_bgumErf2GsP_47SyBRfWh3Lzd8ir-ahgLkamQyHUZlvwUu_MtUJnRBwPVfe-JmR3NSy993kEFq4mA9eAbXkKGG9IZAs26BE51AorHDXYCR32t-DqICMero32g3ugRjz2VjUS-_fYg18W8738DELCivHOe6pBEd50fCCp8jH9E6miTJc1uhXsN5PRvfPR-zxxNf3zisx8a5mgxH1LE98SH_HJMOG7jkpyq3MqTnqdkH5fYM6ZLROmuhdNfrNNNdtMf4cQgWIKfY5yTeCudSQPRNVVf3-573_2lpXhaZMhdwQsMXFEB1bOKKcnhP2vZxYXMLnBBbIMP27fXz5uacB2QHZ6CbdFOQTE8TXINd6sqx99BDJBx-yXj-k86dHPOXwRsWWPNJ4BSQOuXmXfdmYH1WJmgn6DmsNY3XHeQpcJW1FBhuawOOx2fGEgQOHUFzoP2keudgXbJtqwzvfRWBBFAecuIvZe6l_ieoU3zktTGucZj48J2PqrAdk39EyzweRhmhhlWzNuTCKus7YMHsZ3B2KIzpz7N1KoQMy8MOL5odqVhHNXxxi5waeqx4QRZNG2dLuAykEAoksOTKnrTtcgr5_nWBmP0Y6Pnl8p2rZ5ObgesMzIjsEvZ0mtj92dGUlACnB_MFHMyCDWzTzkC2fN6uCjxXlNS9tNffk8rOtHWhtYXZNlzYZ0ofcrBNM3hZB6ULsYuCuBlpoVdv_br4SPZAj5Jj6K6NU50xuuOSEjlRiODf_jIROa4mqXrTtE3mG36g-S9Q8_Ep_-r5PiP-jpLrLqwcHN0fytnmKwRD-r0ZpDljzUTRw_FdlbHTgjmbHHQAEV3KQMscK4wfKNflfVSEjfkN2fBkLh9geSV0F5lL91yvqMu78wNepx3eXcfRPLOY3rLN6cTms0-kOFtGt__cIVcUFVhi27vXo33gFhsGvqPuSAKzWuOohpdeL3reOFl4nxfdzu_)

Диаграмма контекста показывает взаимодействие монолитного приложения Smart Home с внешними участниками: пользователями (веб-клиенты), температурными датчиками и внешним Temperature API. Диаграмма отражает текущее состояние системы (As-Is) и демонстрирует основные потоки данных и управления в экосистеме.

# Задание 2. Проектирование микросервисной архитектуры

В этом задании вам нужно предоставить только диаграммы в модели C4. Мы не просим вас отдельно описывать получившиеся микросервисы и то, как вы определили взаимодействия между компонентами To-Be системы. Если вы правильно подготовите диаграммы C4, они и так это покажут.

### Выбор технологического стека

Учитывая размер команды разработки (5 человек) и необходимость обеспечить эффективную поддержку и развитие системы, было принято стратегическое решение ограничить технологический стек двумя основными языками программирования:

**1. Go** - для высоконагруженных сервисов, требующих высокой производительности:
- **Device Registry Service** - управление каталогом устройств, частые запросы от всех других сервисов
- **Device Control Service** - критически важное управление устройствами в реальном времени
- **Telemetry Service** - обработка больших потоков данных от множества IoT устройств

**2. Java Spring Boot** - для бизнес-сервисов со сложной логикой:
- **User Management Service** - стандартная бизнес-логика управления пользователями
- **House Management Service** - управление домами и локациями
- **Scenario Service** - сложная бизнес-логика автоматизации и правил

**Преимущества такого подхода:**
- Снижение операционных расходов на CI/CD инфраструктуру
- Упрощение процессов найма и обучения разработчиков
- Возможность глубокой экспертизы команды в выбранных технологиях
- Унификация инструментов мониторинга и отладки
- Снижение когнитивной нагрузки при переключении между сервисами

### Архитектурные улучшения для отказоустойчивости

Для устранения single points of failure и повышения отказоустойчивости системы применены следующие паттерны:

**1. Local Cache Pattern**
- Каждый сервис имеет локальный кэш критических данных (permissions, locations, ownership)
- Кэш инициализируется при старте сервиса и обновляется через события
- При недоступности основных сервисов используются кэшированные данные

**2. Event-Driven Cache Synchronization**
- House Management и User Management публикуют события об изменениях
- Остальные сервисы подписываются на события и обновляют локальные кэши
- Синхронные вызовы используются только при старте сервиса для начальной загрузки

**3. Graceful Degradation**
- При недоступности House/User Management сервисы продолжают работать с кэшем
- Операции логируются для последующей синхронизации
- Circuit Breaker pattern для автоматического переключения на кэш

**Результат**: Система продолжает функционировать даже при отказе критических сервисов

### Паттерн взаимодействия между сервисами

В архитектуре используется гибридный подход к межсервисному взаимодействию:

**1. API Gateway** - для внешних запросов:
- Все запросы от пользователей проходят через API Gateway
- Централизованная аутентификация и rate limiting
- Единая точка входа для frontend приложений

**2. Service-to-Service** - для внутренних вызовов:
- Сервисы общаются напрямую друг с другом
- Снижение latency по сравнению с проксированием через API Gateway

**3. Event-Driven** - для синхронизации данных:
- Асинхронные события через Message Broker (RabbitMQ)
- Обновление локальных кэшей без прямых вызовов
- Eventual consistency для некритических данных

**Диаграмма контейнеров (Containers)**

[C4 Container Diagram - Smart Home MVP](https://www.planttext.com?text=fLbRRnit5dxNhs25nLOEh0W5qXS31QfOhktMIcHBxku-1TGCDI8wGqv9ZXrriV-zv_0oDyx4JZOFpWo7_CxXkNpdaFfP6wfCLUIJxxX8yYfbv6XCgIyM2qMVtsRS7AjzfPbAf31CcBU9B1PbJWMkcMkJvdo_k7mtt-3Gu_fkKL1jc8AXJvUmWdB1rDiIyITto_zyVDnz-jVjxkRJtTNrrOVLx6moCTpaZ5o-8_Lqik8qKxGWSx8jG3bo8mj6hXAfJm1TaDdwzmrv1iN8bXTbpW-SfUI1fLoJIveSkSW0TiEKbc9sXANo6K3FoVGcF4_XvKga1BT5de-I55JGZ6cYhJXSGrBsn1EcpqcY63NuBM62AYureR2qa88RgJHC1XIYM24Doq_Jio2OfWKN86VhT5xYAzT6KLY5yjT1fFiErZtmh8BFN0ehmIgdgxAKoW3in05zklfiPkmpM5ZGVCUASbbot8qV8JZ40ALIZ2mtjoXfe-GJJq7KCspa2A28RbhpKceeEKX5SfbO-HhajSHPgwo2FQRE9_uT1TpANJ3NHGTOCw71HkSavzdHe2rf0T_WmHawGyEszlPULYAbwkGaehjgfzTokx4mFIF_dH3uLmVFZ9Ry6d2VwGdMW0c8VqDDVvCYMtoulltmRtpRGgH0w45GgnCf9HVcd2XP6HW-9xG2Mmd3dKNW0w30FWgEdq5d56h_V0-oOQyhMO1yicNA6eBCVf5vBfzXCbcjLiHk3S8KzGJtQN9GiY0xglyWFvprTk1i-S0oZ1VSXRCr2ID12ExXMawR2Q0hpIKeNp13qQld98GA1WE17RBCXTXqM2Y-Avata7vWNAGi87bITAb12xbCAfYr9QFvtF02PrcKI2Ya3Co5BNbumDs5eLZa9LYK9SR6hpPeT9hdi5Ug8TXGf4jD5tKsgYE1c5JhbehJHyooPgGjz5VwHCcsLEZ2zrAQQPZU3urIoGFFCJTe0gxNORjaPgTxbvz5obZgw6XpOydazUhO-HgZLXQOPY5_hKwGXkRK8vQPap2gKY2vnXzRF_AIAilAG0PO8QgongW9yvmOnRCCUB6RDxVY06HWL9LOjWYfqrEf08k2ETuh-OTbyRLx9su0HJ_G_PwRzRrLGvz4SbHIm3vjS5QYzXDxGfE2G4OBc-C9L0jWAR-2_zNE3AzZL66M2YgCOKvb94qeHbsDL_jkCc5DOd56sM6hFkAsKiec4fAbIotoPyKgDf1DWn4EmlMHAfO6gLlxDY1j2tRqiS9KmSFBGJ4GA4n-yj4CYrk2l5bMCBAd6dakF29Cy27PK8Ey0nMyeS4-1SQQTubljH-WljLxMzAaDfbYs_kxBxEV0oJcLA91Erpeya8FsAyc9Qj0mqTXexRaYKDUVNRAxC0dSqXq3jPeaH7fg73ao5Yd0N58ILPIdug6jefpT5oS6WPg-N90bcKSAonYcY5fNqilGo832rYfDPcqRTcL6VD880_C295ohu5hZmB5WE3_JIRV4sl3MuWlHHCxSVB0ydOJsEaE7hNjYlQBGkx1VF2n7EZth2LskyrsyN2rtO4aXFJjNGUksz95oq2zLZj2Z6mr2xuLkRAjo0PR4I-cXH_LhcKJ1O1HkT08hMm8YQ16H_aMQbo4mZRi5rTIcyRm1RIeTN6Fe9d5yIcgGxluEhYcAV5FkkdEeTYdkkv2Ni8Rw3SU60M0-eFZkXTWuYePR4QRw8tYfnK7jUCX2C27N3M4QeNrpJX0Z5jiW88XWc79FnkI2mI7S-_l5jSV7zP-Ax4LkfJdeTkDLPlpo1saDe5JXP69xUaGin-SDQjvC1kTDWQwt1NK-tMpz4XHG3QavR760hEB4pcfoqGEYlROfWi3dcgEbN2USjtThEa9pbhUggDqiFY704dRDRwknozPirlIQvxn5NmK9Z9b5yV-Z-OqTTGFmlIZSGG5P2s2FxuC60VW8AItLIZdet2nfuVGi20yiMvQHd3WyVeeVTkSbbnMTi_10roojMJarGugh-ztkyMbN6wcWz7IFrn7V6RN8t-CU2E-3FY56RlZSDwlp_TCA9uSCHG6EEaAk-FvIe6H15JmLkTCPlQ4YxSkTf8UysA_LTzK-vphOwylicsutTfoVJzYbN6aTW9z2Qenq8jOoarrUns4Z19f7BER1mj_8Xn4xMVvE2ZEN3IjtJ1Sb0Pzl6srrudYUrztN5iLOuLO87v-QJF01f3ZmSSoHVQoGdioelf_qwM5v4X5DkAtGQYczc_gxGUZVWNOGBAwdFicrUBw_n6xIttavJVeDdmdKLaZlmfWx1vX3A9fGEP6piEHhPVoAwxmhYMXERHRUsQU6T22zdtB6K3qJsPFDEUfDoKk6Y-DhV3utIrgSf5zndPHk7k6VWyMU_5bm7RrwED5VepWhfcfaRY052ZghgNVqeMZJQkbQEmm8gc9xfq_v8JbUEfsOlr1vbNwZ_QthPfa4ncR7GWCcbTZ-xaLSDA2AeYDARcJDCKZ_6nhVomeoKUHduRPDWx6WEHkMtA0weex9_ymDDC_JTql4ULmHOYo9C2sJhpV33wkj4zdPI-mNuyVEs9SHCD0NsUWoFkZ9fA1dVuFy77ztIyWtb2l5X3XHTetgMpF-lKcNeIV20cqjvS6Ccdovath2i__F3HuYNVnvira7bYjSqbt0QEfFMM7Iq2RGlLzNAUd1upsbTk5vLLmRhDG3r-yXJOS05mhbhfMp4M5ut9DdZWbuRuJPdwGH3unDMSYWqCPGx40ElSyxOLo0Qg9nEgsOffd0gQX9Z_EI_jZ5Uw0vRAqxPbXj23iSE091nu-JQ0X9MZGoUHdUAoAdFmD)

Диаграмма показывает микросервисную архитектуру с фокусом на ключевые бизнес-функции: **Device Registry & Control**, **Telemetry**, **User Management** и **Scenario Service** как главную ценность автоматизации умного дома. External integrations встроены в Device Registry для упрощения MVP.

**Диаграммы компонентов (Components)**

1. **Device Registry Service (Device Context)** - [диаграмма](https://www.planttext.com?text=ZLTRRzis57xth-2C0Kk0j7xfqu01TUpKpPQqZksk6p0WeALZcGX5QYHbnntsttTuaKHTx3KFGNH8VkVsdKlUQqELANC--e69X9SfaBqnXVvvCb7q-3PZPbzkImqgaSA0C6yJcKyAJeLzyqQRbBFjPFRkpTAAlZm-J7Ag3IWKFSzaNaXXtnGMV_Gm_VFpbytpr_lDn-U7kyNTf_dLzMXacE50Pkz8VPtC6SqKpSaRCeS3Iu2i86FQg1DPWtA2gt0miqQz6hovPdd1sOv1IcOqsJEH8ROzfao0kg85Mr03Htgw8UFfyfw4hp5-_YP5DlcqkF_qX_rQoTA09Wh-Ba4RJOmaEKkKr5wr7bz7iAapmtuhoV5nP1OAAdCjx4AEctECOKv5gWd-8fW00mI_QGOvkjzIO82Z45t7nvlgxpvkSvHIGn6VSqWCaw85fXCGL34PdkExTP34YB_I0oNhGc48oQsKnigcfP4vjO2a0ca7OY-H8u-r3_ZaevMGHlH_Alnz6n0k4w-gYGcvyZGjY3w9X4Z1JzUeVxG-8TdovxjNSuKq02Ketq1UJ0k6Y7T1Ggm853Mb0eAvjzg-0ZLxK4wVYOvj-COnSvvlPIbIgauXrnKLcsHsoJc-9l-C2FxKX0vFlLNXLR1ZmGJvW5o7erGlZdvtwuqz93kfI1heWaPncKNn-4lKjQ6yUXUnI8nie8uBizMNUIGtfmA6WJHGbUnTH7QCO-WmEnWAwqd7dM2FpwsAQ1_i30SDFMSIGujT10EgDUOtOyc0VqFM3rhgV4XXnmIpNklew40vIprxLCd17LLcIPLHmRxHK1U3ZYLxQDooZGJwtgptL66RSPSx1ZeIJse2-sAqzxmTza5Pe1GRp4x9dC017peZNVy2ijaya49obZ1l-mVA-POcBxODMLOa-As7ZQyAu1vROkRv41T196vSYOJaYAsUL21McJEWKpiauIpuMfMGSnA4Oic-awSTK_aHOrNvR8-MMCO2kga_uvA_zBlFgOoOLejwhJKYW6No0McyOqctEh015oZRCixCYM1QaXTldP96OlUCOApueKfeX7vrO26tr_RAAYh3wDgPLDcqByijPthlc7Pd1QIMD5xvWy8Vs7Pfnn4M4NAER9Ly0NMc5KJKglZlin0TL8MF0wZK60KgilWu9aUuumylEBKkjpfHR1jvrOYwRcb_KjJPAfJ42Ojj8XieffxvqyHU9aZFdE6ZZi-srfXevz6hZRlRlr6hdsyxFNw-7UZoyrlhm59gaobOFpt4Ozq7-gPkNpUkLUcRfgnmYzgnh3KxKQrs7SNPtFGNz-b60gHCkyO3pat4Qtp6ZvtRLOJX9q9TIF1nc6FDW0wakNMSGQp7G28lS6heTilCurEK7wE0vfPrDZb9tBxiZFo9rCs58LKKTHk77gs0j_QjUFhvPgexvU_c_sQpN4_i-4Di2z7PhMAKKFuD3hnYSa3uxS8Y1I3YaA8bwY9AbROEYCFexKunqW9C3S84JhQyxhCn3aPitLrZ8bSTLd-HkhTJpbB7nPzBcX9Va1QehUA6_6XefdyPLwTEfwtZGBvgm-xbAx9WOBfpo5oSsYFSbxF7sb6_6dUm-Wsy2NgrOP-3wTfn2Il4NXUGV3VUcNdf1ba3MV7Xkn2xRRtwMzShN6YW5oCNkTXPI1g7wwvOJw7lnhnWPJJUBXlQjgl761oZ1vOwj_5UUZQ8ICwvtHuq6ePFBwIzrPBNU7smXs0xdYm-hnxFkJw0jGAQwjxQs06pXTlyV-aBqRVQWMYqcxd3dnmLC_rmkCPUxnm3BJNGqciPv6l_lvuFO1n_Zz5wDj28kek5Lz6XHdTeD4of-cxDakdZqp96Rirsk-ktneVJra7lxmyhI80TKDk-lGdqjDQsnyyxjNDVxGAW9zKEVhPmHkz1f6NEoNy0)
2. **Device Control Service (Control Context)** - [диаграмма](https://www.planttext.com?text=bLTPR-Cs47xths2D0dM0PFsoJmiKs5oRk4ssJkJq00e454LRH2XI9QaatgB_lJCaTSj8Da1WSuQSyvl3dwsZnbM5dFqW59DLnadkN6a_BPU6ldpO2PTNQMMvOLevhjm7fejbAQd2DoVMPLAao_EF9siaFTpUB0jg7JT0UZpNHQaLlYbH_kpczA_V7ZQFVwmsruytbrUNNoyMHxEP4qvoSlwHDDV9XQ0xGmjoGYxuis301Tr6Iv9muy-Bce2V_DN1pKGKfHHRmJDoJbakr0va8viAnSs2bkAAElv2zyTaVhfUaNYQm_5NhNRBhrUhht_YwLvNZbjY-3yLjyuIfqaXcD4swBRpeuxOpDjtptV2EeEYey4rfROO1L_fUNk1KKUbtX6gCb9mHpCuzoHZo6-feZjUG4ZWuGCGI4lfIlw5FbEIb0QS9cTQktbzdLOkXxi2j0cjo2BakYHshnZHIkwFUZfp3LfxIg-HyXrQRtHKLRHFfhNEaZtWf7YyV7KBMy2541WBGX8ynjHRbBhI6n8YRJyHANQvWsixI21yC5fmG-6BvSfgW-bfFNeyqvNAgDd7H4NOj6aO06j-HFwT4VXhiBXWuSPfAU1LVHq0v3Cg5Fa2GEKlsZnvx5mc6sIIhJRHOX85_AqQD4C55PXthh9I2-LiXmUnSfm0z8pWddxFgJnnecW44NZ1pPOoZkmpwbXET0anmBYZ-sZ_o0clyHh-9JVUaM12fBJmh1cbqH1jYxd5upEL8lCQK6bzQPeAnbVmhaio76i1y2vOdsmhwJgwfgttCGaOD6tg4X-fI1xLLuXZYq9K6ruW2dEZbVZMM1uOBBIDbf132Mkp3nQhhHJCeSbQLlXmsbRAq1NiG1sBJptHzwK9YvES6kXQp7CHE93CGa0y89sesrToXmPnr4zLYQoct8BQGK9yI-HHS0iHP7sXKgQKFM5tGxmoE1_0ZW408luYzWD-MbIsvDQZbfkrYAzKR4Ee__PkinbJrqOxpG3Yq0vB0BcDeLUS3PgAbm4Umv4OQDW7iHEBvl8P3kigbSBctW5F80sbjJqmoi10-6F_ZvfIev-u6LKhowa2BiUNjejl_igPnqgF5wfuIuMiJFTLqnqQVmDlsZ7FIwhKCYFIZaijQUYJ3Pmo0APNupxu3S98LMReLslQ46STTpouXEeZBoZS2Wb93AdvhzELBz9-E_OonZtPamESCc67jOsjlzDmuBqPTShXK0N5rXTRhJ6MtbXJmaEj7NSBxvXi_LQWdDp7IC3ZqUIEQ3dpO05PDKu3mQkXQIhSxLqdDmwiB-fk6oesPC4Fj9y8Z4bk58tTdtfysDazbxtDPcfOsNfqehRhpMQTB75CWLXytNqnMcP-3qdbjGW0DB2u2lCVPTMYmC9aEEL7yaShpKe99y2dVdEZML_wCVdHqPtzUHxtY0dBnujBBJe2rlYbuNt2mPULYj5cpKuwYdjN_sY-haDfGUGYouPgR8UtdN1ZCEZ2joRuddjOJD-2WQY7WANlap7P-HFOMjxZLL_Kv5XiuTK1m269VOUcJGFalU5YNyeRLWNsUrqyD6dgxxRT-q9RVvU9RqiDC-YmJ81pQDAup60VMYJmGyblCW1jAr819rWsfxHFjcd_sKUbRPfdCyCFH6bIOWAMTUBZ-_b1YLD9wtTrM3-oBgP2FvqK2C6vc5XHlA8fZ0r_gOoA0HUR9RQNEfkz1mTsgL8R5n9PZOHWxn-i9Ds-CGJ6SFXqqHU6OekKqzkxdeRUWe2BQ6yEUQq3yUCbv9upBfvx9RtiBGqTnIXm966o9bVrVi7jCfhJ9dK4-C-GbQgGv7y0)  
3. **Telemetry Service (Monitoring Context)** - [диаграмма](https://www.planttext.com?text=ZLVRRkCs47ttLx11WIP0l7xPfm85recpYTjucyJUNe020IqnCX6AL4agYRVeltTcA8cYP7StJz6GS-6PCnT_S9vRNvTgyftKcQfpmNRUL-x7sSportU5zBjwMpjXCwEzqFvTPifPfRX6dQdpkPBRsUNxwHsAFgzkPoLtNbWGFLwQiZ8QTIgqFxcT__dhvytZxylDpUFjrVNLfyNfsMJYfLU2NRvdtNMsaBoml6HJjX5AbCBRFLiB-o8pmKvNHajlhDG5Q418RnxkhMLPAVaaHSukURQ3Gx2CnrngOKzv9Q-v5wzyVyvEvdTBrdoTmESlHXUpJzVBJt_WruEflN3CYhzhuRnZth1IPjQuuDwTdFNCvW9b3wAG3a84xGK9M2jfWqR3r-OaNe0dyPnxpbwual2FD3enx3AXkPMcqGV5TIFfc_oPlt2shWYA2sCynIzuxfWJsXdBoCKJ_6DhDGXzPo2dAwvv0VXg3vetA65Hz3LFjoQZm5aPLKu3cIhczZfZHglz6NYThFT0YVBnwisVkX8k11GSFWi_MVEDLfTcqqJl6DSv8CLTJNvpqIOWFkFnmjGwvtP_wbkYHCX6t3avO_zC6FnrJ8jgyqecAa0IofhKx2DGKRmQ-qpuNgqtU4YmTjg4z5zwoXQ8E507H4BdbP7QEvH3MjJKorA4d00v12_fv0QO0cpEk0f7F8D7qibSSxNtCdFH53mUunyy0lKkZL8Y0qj8ChJJ2IBvrZrMPE4OyeUkLlURJP03qi3xHhn8AOgYEsky0MMsW-mekDtTqyQMSFLBo-O3WQg6CbYY3VbidqaaRoZOZtl7BHHsHgUHRUIPI35LaOjL1K4HVf6WRRKfOvxh2i-eNuZ6fAc4fPkazf4hjUNPCvO-9Xeoeuu0Zmc5nY250rWmRGJ_1gJJ86QTFBwcVqnHLU5E7qjU55OKFCa3zDtAuhqWcsi3S0FpX8zvf8jMO9_6XrT6oKoA8z7pbbzNke1YmixOCIw8Oj0NtCcCTHfKcb8xMUm2lzSUWWNU88tfbfCatrZWRXhZN0ae-KBe1lHUsG1z_1xJ1VLm3FAsvAvUuFEktYhfTiGY4h1E4cCF1rKum4w2FHs011gnhJNFmb89KAC9RKoWHeX_1v3lZCePnx1T7_buA-BMOd8uUD9OrrkNMRdj1Hr5mwXTEAaedGXwIfQEudK5L1Aza9f1OyCa2fVYQIWqgTFQ3YwVf8Bv3GxW3V_sUkrY6y5VR9DcYUdRSaTyMUedLRyjBePKBtlu8ktFso6remQuxqydSEMevYdEs07eCqovNBhGqgWWO9H03PUo_NYY-QqzP2dqKr16fya8R3XmGHG0Mwk64q50RlXsA_tglfSFQjn88an6Q1y1UnfCFp2OSS827G1S-8SJwztaGQXa1ncCdViwO9JE4_HwizdShMSuTC0yMXbj1WDJe_dVMkaRINL6MynlWHNOdz9rPMWBthiU3lm33iQxnX9ME0aWTNqx8AaCprCVv-nxpmltqqcpM0IdIzs0d7KhumZkD8J3dRh5lIcgbYHKWy7Y4myhnzZYk7jU2--Y-9YLmR0-71XCQiWYxD__6z30r97fEifcU-_eEm-5D7vkYF4ReJjayaYy16CaHbZnlnNDezEX_JzjHA4Z7ZC-UlEtc8ypyhXneF0Yh876mgkFGvGst3LSnounM23wNPVTGg3TalRLTv3PjWfswIQQcBsdbVQMwwA6W9lIZx-vGlc6Laik3-SsRUhKgcQlLY8toK8a4JNu_ZiGia1VgFcorZBO3pqn-Kqnt8O3JmST4HVUsQMPtpLTUx0YfFK-pFPmSCJanmOUapzVtITUaf6EloAIAKAU1mx6QyE3o8Hy4R5EParjZzoYkP7-aSfRjlk0SBCcd13SGGv_05pXPptx3m00)
4. **User Management Service (User Context)** - [диаграмма](https://www.planttext.com?text=ZLTRRzis57xth-2C0Kk0f7hfKu41TUmWSUQaJkoq7J0We2LQ9YgH6aabzORzzvt3YoZ9IfmyEERbVEVodGlzMHkgJ5qMer-uI8iwOsHdJAK_9OcYBnzoRdRrfjPCfL8O9io7L9P9LL21CkVQP0NV9DEFvqlSUhnT92NLXYdOUfhAif82PIh47oqcVtvvN3zzcw-ldnQNLvTtivFJqSXmKp0o_KYQwsJ6QQveISx98oWcjrJGd9LuicBgcQUCdCmpM7Ap1oamwwU1kojULWNVSfQHAKrtNEI0ZiUK2wPEQCMlg64lT7z6nfFbdFZL69P_I94dTrVpk--uUf2rOPeezdVDjD74I5BoL4djbElnQGisOxY7QoKB49xPDV4RmM24lPBuEHVSS5gGYgcIQysbq0H2g-kIgGwmJfcWYakF0A8hlzC6lQ7FbAmg1TwI2obDMuLy0HozuzMmWfq4McDiGV0QTmR2_PeYoqxKyamBdb43BetRaN-waBN8gDgVO1fr5Bv2x_YK_3iYyDUaX1MTL3p807zeGC-UXylL6e_8LYg24gHiiFyIFfiKoxasojgP53Bd0e-MIcuvv65NO5AR7UPPQc_RUmrjhQlWBbhRCvc2iFDDOSPreBpJAaHsV7Y3YemWX5JydqRzpRSrfECF9dg6BgdMBr9bX8bKxQjmVSNwfkBk5zH4EDHDhW9T0_PNBYJH1Hk-4AW11-nvDHX829kM0dAI2-2cZE5KRCiK60trrfMuamPgsST2CsE0Q6qvILF89tCq_B70eWSjzeO8IC4CMussT960LfMYxWTPiFCDrT1YQEToarqzWHwdiKAy9mYXUBup-gW_sd69hQt5IM3ORWyvjTfH1UQczhmRufXCsaW51uqNNfVkPRm5iNc5hPPvK0a-Mv_qiE5OY9VFy6rPRmgkTzPkkq6QdRR5quVvUZwTB3uHTqdBMa57nPhkHTVQcYe6ZJnBwYh3_uIXq87bEoforZf39WaK3S-5xJpnn89cZAQ6FplMt1auzr-hgyqsrh7PPg2DpIxGZwNK9bTiTRy8nQ7FR4N7X3WBvG8dBT_QRHhqQ4kXPJWIQfUswR6Cwt5CXIxK6TdQEIPCiVSv2y8GATgP1INiW_KN2j81wRbrQ-8sh1gwsN1pUpzko8CcocY9_VUSPDXI-SREJGnB4aqWZXXqQlGRkVm9ZmFN2E0B1XcC73smeZEVOxTtKNMWsfMaZnWQShrUBrS9jdr0Hv23cHQHlhhQyrXnR0MS00D6XW6AOOhFWAsI9P507zQ7UmcG4_TwgeWKnTwPq7iGH0E69dyXQTOru8pyQcYkVnzxPE_OmMlW43SEudUXMZyz6Mdpz3gWfT7pwZWBx3Il18knfUwbrC4u62c1tZQfnp06-dhajbr5hs9rN1d2c-vO-aE77kyQz3lXXjjqy3CqrjV1-Y4QCk-A6P_5Hzq5bcC_dHI5xuJmg0Ml0CqtjqC_1hjs-AvjDeJ6-6vVZc26fuCtyGtSmvWSWPui5grOlQq1GZR3mlT3SJWqjk-lu4Q8XMsfk7U_I6wMapSYq1QjMqycFI0-x6WRePS1KHxSQFqYSgEwxT9WelP6ZLCHaZQnZmNT31ry2ngPoiAtTZiLAXTpDnQcYcEBAEpmrdluOLX2FzMyuFZaQ-gwJrn_6dLvxC-Nc8MJs_ibslAPYGn-SPB_0G00)
5. **Scenario Service (Scenario Context)** - [диаграмма](https://www.planttext.com?text=bLVVRzis47xtNy4P0vO0IVNIfm43wjXUwYvE7TjfEs10G4kCJ4GYDPAoOnJxttT7Ig8eoMlc1yCyyZxUp-_e3zfGPSey6_t4HPoL2IDxOmhzQnGfUdoNShClTwLcAfR2C67UnJAFYemArBdM9iduBfg-lrwXw75v5-LK6wP0z3ILUI45wXI8FxgR_FdvSVlqTR7z-7GtlvtVpoukHoF3JSR8z3rfZfCPfwcYERaccvW9ghWa6wOEF6Raef5CqPvN0uUMNrRa27QI3S-BZ3zpbf0fZVTSf82FvoWNJ5tGWjzImuxqT4N6azM2LAinBF-G8etkRnVttt2rbgLXcYZsTycqqSH8alDOIUsiqEFB5cp2K8Ph9JDGdjargGIrvGXxA_5x_ifY2-yK2GGqfo89GGtBMCwCEbNgeBUjHNt8bP87dW2aPa9BHH9gQ0272LnIGLD040Oq7a50lAGD-OaUADaK2i97RgGqu_fuxezVkAefY3w9c4YHdIw3-_OIRWmk_8YI_t6ZE__3A_sTJpUo50bLfmjTbOWFNRUCnfVa-uZ0fwcvHcbIyBO2b0bQqp5kFTziSOiyGx1hpPQnVubMwKxNZpCY2wQeuL9etDigdgP1H54wAOtCxI6Yoepf08NPgi6zs1KME8yUTDoGHmY5tlC26w3AW0ABFzTosnbr8bGTVUlZdYhedDZkXvRPz3VG99FofInmmsM9nj0JcXnenXDhlxKScv5LSA7xlzCisz7u1ViAenZ3MW_xWu6OYnGIZ6671N6hjk5JgS26C7YaK1ADWgpXZL0b2RUR3Ami6oCjeWzj9bCUk_De_AlFxL6gb-TC7dMZ1HKX5JUdLgY6NJ0ksJuNTVO7Sb1l0HzWdYkRmsJC3_3ZUaSr9Al2TaN5SpOWtZXAC7l5z5vcuMRjfx_BXc3O4HhZ2KTV4XsPM06f9K4nrO5pIZx6UEii8BvmxruQOF3OPT8fMqkNPMOup1jMjAML1T0Ejg26xMOOi4svqx7YEvi16qBYHMtBtPvsUuKp0umCwwdo02Z9GFsWXaj1Gz5Ujc8gvw08E6MHXESt9t0ehmHW_3yjFflj6awQxTeHdG6vOvejyKjjKiKs3tUuwXB8LPFhAvzX-8tJe2djOExWfPO6B4juKh1BotmiuTecc5cV0Are78J5ivsPmcIdYXL0kJUDSf13GTye-MBJiNHhuWJs6hhRSRDyQ2N4A4Pp967R6vPLN3pHWT4lK8Nmx10q8npU1QggaT6QPS7S3sdUTQ1kauSF2mwXxNQrYP3jtHsRxWYo-05cRz8k18GTpFBqwOpF94tgEzmLL-HdGrFzsxYQT71d3xu_M6lyVPkAtmGE3Yr45RIuUNprmXVS7_3nGFXitJKptM8zK_TYwc7rYB76gwlsZJZdPjuNDuy0i3bn7h7jMC-mkHiQWEJRw6rGFSviNdzLYDy6ysCNgyI3vNlp7w1Tru9yLjD7kmdg9-Lvk1wrzaAtShpfirgptI0cbEGKdjdwkdff1zLeNrlTUU8wl1FbxfEySQnwShTYxplaSX1gu26-PZI1IYYLiiJKUd9tyC0R96lWBgaW7BsiXlmEXyxq4j1xz6arEPF28PHs1uNQNVy2xJkPwbw4GmnaGVytor6PgpqBVwPGmw5Y0nWTuVM47ON2f8cV7J04giR8s3voaThj1F0JsDxK6IBz8RzcCUC77up83wfM3KwM3wjpnNokwnRYKFTTLCqWvyQPelx0H0B_WCc_)
6. **House Management Service (Property Context)** - [диаграмма](https://www.planttext.com?text=dLTRRzis57xth-2C0Kk0f7hfKu41TUmWSUUaZkrq5mm8Q8cMYL6YHb99tM7_lOSNaRemgPCyEDOHpyVlt8y_IeM5gWis-ecMAQipWlPALV93aWZyz2wdQbzlQqb4oaj5IlKkvKLICLngdNEfCaQtoVJz-LABxcyMIO6b8W943rDULBpKEfN67oqcVtw-tppyFjzSFomkhovlPoUdev6YYX4qVO_ySJIZE1UuGEVeciFLw0QNE2U5VhKcuf6c19qi1Q-8K0TG0rvV5HnUqw9YT4T9XgOutTCo1tZz6jEIY1DSqIkio1C-dA7nP3b7xcaCZx_nCazkh-QtV-Yd5QyLaKYGVsiYbKIAeuAcWajxkHoVjc2rRm91qBu7GOIo1lw47p5QLmAeeGlErRWvpfy0IkvfXHunenbMb9UTMpAYCLOafr89RS3C250ZQTzonVNdlAIAOeO032C8dgmB8YAO-bbm5Y2Tu2s8YZ2m6UWuNL3QDAAtuCcKb5XG7k3MJl8Zhpw77S0VBdXTPbWSJlOwoJeXV2xjngVeln62Fv-hLdbIKQy5cQK9zFYiBjSR_Ghjk4161nKU_k-ooVJfwdw6T6AR393wnOAdvg5tVf928afjcWwMblYA0DcEvhLeiaWJxh6kt5bheW22Njk96iUElM6up52Z5oKFjKVIGyhwfddeaDjQF6tJxAby3lL019Hq-e8jp3aesE9T5h75lzFc2CwBtiqhBTfJY818zmSj-GkkGLpakAJVF7aFqxLfNM6JTe8m6q4mGSPfOnF0GDe5D4Av6seR1o8AAcMJ8wQ7MC2-x_t1FjCzDWdAU4xJLa9vq-EiTJtfvae2yPakCIEBaTVtG4DEpVjOrjWAjVswurdFn2eRqhh34mVMBPDhhYnKk2DkWIc-oqVukgotZCgzCS88a9UqBPYkvflvTBBuWEmXoMi1RL8NCbTm5ZLZ8TYK2cAO9NK5d0WYMh5RpUaUbtdxZVD_Y6phdQ-W3XxOzt-heSssrhRPDjR1PXVQbYMNAXTaVRVmTItFJ1d0lwQGu6iLxIMTmGVNIHDR4_eGQVDeMXx9g3GfG6ocdhNJzWpjp7mj5Jkur0Lb7S7E12X03YPS2FwFYTEDVKPMOAx1sorLDtTZ7qMOXmGNEhdFKQQd8zsQUMwyapHMEbzgcqQ_eCkliBIKKBaKjWc1KsFkQ4LOPsreDNlhMWSgJL5w_ndVRZRBTQBRFboWSOBkO6FuOYiJOEhECj3WD330ipFN_8Ao4z2qkic8JknYLo5UieEbq5yYMkOqk3qVcNXqM9oXdnNEvQzZ1pzkG_jT8eBisfEW-HuO7mEC3F9mfuY0ip0agmeYBuyYFLWlddD8G_ueTvWekMnA_KuxoAjmrt0OTvAhUK-9zDkEGTjXkujsqORZi47hvEUFOFhZgG6nycCG8gFYYa3eJVVEl2_ZC0EVzFc4ckcKInnju9EXomEUTNfgVrQyWc74tdlROgp9hw3tTgYUczu611aSXjw4CJUiu8SGL2-WkFaJTqDylZRV9Pei5esBBUeh_78CDkjEs1VWUxuw1jprWHUXmNjc9PAA2q1-mKjcKg_XcC-wfYXWG2UVbfCN_T3M7ZJiBa3CqhPwhxq5PR2bzQlQxbrjkuRvrTq9LWHdCdaIr6TSA7op7-ZLHl6KCpU6pG2lhC_j19-2Ckp_p2nSyY0LAM3mISeeALCIUau_SltD8PA1ViubpW5khz8xzytTKbFwICgiBXZw3W00)

**Диаграммы кода (Code)**

Для критически важных компонентов системы:

1. **Rule Engine (Scenario Service)** - [диаграмма](https://www.planttext.com?text=hLTBRziu4BxhLx2zr47Y-jAJK2pgAbhKW1-5hUIqG41B4vjOYLH5scdHx7_VuKiMPUK1fBau_3WSVlFDS3p-8XLjrA4getUCv-MX0B9NgfRnPDBGXuyxflQ7xK52amkkWAkFkQWcTKcvFZEMgYZPTf9y6d_NqCrYFgceLD0WT9U8gXPSdwcr_qWnLG99Ff54u2tNZEuQMf4nMHyGJlcESI2ZJGwSDaoG3JH7bWFk9wjrIfBrB9ibq_b55EKbbTASSeT-HmJ_nWqYCYPp9jLdlVkNXU6853Q7hSmRjeKc9ca8MAkzm21lQSaAgWGQVGlMnkQoRaGEKfhp8-Cs9bd3TZje37OHawDWXJMcHQ59Z3IhsB0DjXken15QYraH4waQndU15Vo4_A3WEKT7oz7OJ7FDhsDvHNJg0kTR8KgWFFglAwINaBMMTirf1I62sTcMK9py65HP5LevB4fpnGJtAPcQbKjAIAkJWZvOYfpgSptyig8gtuDy8WrTSbx4qQ1iexxrdaehOoRcxkxHooAw-xsEwbSDBQaC5nOkG9TqFqHyMGNJzuGmCZp07nV3nzt6DXIDJLKOZCLUZgMgAE-1DSM73jWLi1GMjFvizwwSZQi8R4CxKFCd8dmsX_wGA_zNPyi8vACqscKcr2xtuJ07SgDO1TXBgpeccfDUlhtKMyQzdkL9Lp-KIafcihve_p-rGziWuqwpjCf9xoaJnhCCXHNSe6l8WHsXUAxNzMecDNaIBK4hNuBV7OqeNKkFoVK0wXgEKEoUMHrQy-Kv-FgcxIh7Ncb9ZcnZyZKTX2w15ztxHfXLINTjiWSwwdb1Mh_j4mklUqrF3WFnOffm5vGZdKgNWzVdso0UKhh1EutTu2Ev8hsAVAfjQ3VFFhShSbx6JpcpEMbbwZ-r5ro5Ofs5xqJhvEGjSRyvPknjBuHxqNtrm0zLz_l3HRD9bvlL-kvsEhz93P3D5kdTr-aclJRBw_HsbgHtcsoQsVtvAfbcizNIsNZFfsRk7Bk3oMgPhLTp0orNsUplcJqU-l9MwEq3MKDfuf5xLikeCvKzFex7uhTfFNtuyV5i23kt21lAW47ljHiBl-Vda1P39jPX5FLxQNZ4x4PHhvqDNzbxfbsZy-SOOMy3WfVi5H7ttVar9k-dU4jbT2IvQ80yuDnDNF7ftl4F7vFlTlm4IKm_bXgp114omwz671s4V54IQefMRnfgvCIDC1pgSGpNPkyTmwrGoCLJrEJCfs7KkZI_1dugmylFFfV43NpjJPFMVijmnk4xlMEVK2Su3Ug-GiG3nna1wyXJAU7UQDNJMUlLTnAVmhFX4fo8ydyrEWzrE6tO1zbZOdtu9XBgymM_7F2d3FaV) - ядро автоматизации и обработки бизнес-правил
2. **Device State Manager (Device Control Service)** - [диаграмма](https://www.planttext.com?text=pLXBRziu4BxxLt1xMHUDcqjFGb7KbGF4G9nauwIB1Ge4j3InYPL8bQISkit-znsIefwKa_RK046auJo_6SqCyr5fAdMPPvC_64-oCWMoqxfGqUcff8zljqploasfG2Q2Q-3wRIBoqoAZtCZCb4upjZcDtysk3UbkTN6QKwL18ka-5daXk94fZFw9PZe34hyZiK0h2qQtakPaHXQmPmcGjQOQo8fokWL9fXKrHhDIP6GDqhxFI7opl5t6yulNaqcIKQKgUIlkfNzC2FxC4fhi828ta38LcsT7bb08nRIGXyXPlQa9ZW7swFLrkScOse6Co5ddtV5a8g6Q2VwPPYobA1kHYpx9ShxPWevBAL65DJPDhR_BD29AIyQthwDs24wcB501XFbF28T7UzGHH3KR8JAWtAbG1vxyZUbpB26hUy5Inxjt7Zj5yOxoBKmJvtF7nWd1Qi5uZXhsoZmSyoG1fG9XcBBgEEJL_1T8hSzfNusZ5b8K83K35M495U-z2VVtWsFIB0Sizxo8YF7FlBg3FKY5ZaQ4SUqeIfGoWQeyrlQbc_Q-0pk1qVI8Rv8CwtyQoYzJjaYcVUmGssiNoc7wBnnQsBJrgZ3N2K7eIpYQOP-KQGZkvaFnQ4jBoPWjX9lcfG9KPmrcnlSNrROAy9bC1GgwyH--CQM7HTJ7Zt4VwF62jvt1Ox7a3oBWqJ3LNaKDMAzxr3MwmRR58T6sg1PKqmrLqD2S3m-CfvyEo_HbeD4zJ9_121DmZYYX8u5ei42bijb2cGjaUzzIyw7TTUeI-IL8khtI8v9_rTgNqQf-TbOBnzpkFKFB8ak7BM-y4pQWLDhFf1Goe1SCFLWYbM2lgUFJcAf1E-i2W-Cmfpmznz-i0KQrnbOK66NjfbSLaAWPQyBFZYJu1ac9Q3kN0h4axgGnP5_7EhtNyqjITRazAzqR2nN3yS50gu-mComeBXw0MygnOT7DvZC3HCBN4a-W3gO_2UgkD0R67Pgh1kMnYAjyl-IpKi3J5oGut4UnbJIrM0RxRc2cET8LpnWFzjzWv4vejEc4q1XKjSV3Jc7tvUHctt9BOqJEW_HX1fmlLHww5N12NXfGR-_fgmdsq33unq1f3w-m_T5UYarpMASXdCEmLM1tWFljOpxgmQ2mZ05VKp-pySquDfPUppWK3GKD0I_pzbfPcLcSVLx6P_OndiVdxcanlvr_cg_ToztwxEO-lbgjvfSBIxYuYkUtowlB-_K_bx5H_ITkQ9drLUrOeIQ1Yz7JqspszDI-4OqpzQz6uvoz-z4uut0SZV2A7vt-CkaDpqfTO5mUOsp6vN4kUpR2CunWqk_DG-Scas77ghX6EjCeV-SJc7215oF9jZjDn4Fe8XoHL-xsbDSco2DU2ic-NcUYBtpc2eAOhHZBr13yqahCDN8d1MVVBRSvSnzPIuKXUiS8jIsWTsehX5Gh5PfuLVc-4LgBt3W_Z1XzhvNst9s7R3JC8Da3I-e3Yt7z7mJSW64hXr9LyDwp31uigBqoGAVyth4p99G80kgPkYvwltiE1i8qvDLVjxV4hMf8GSS-uaWlyupy3m00) - критическое управление состоянием устройств

# Задание 3. Разработка ER-диаграммы

Здесь представлена [ERD распределённой системы](https://www.planttext.com?text=lLdlZjis4l-kfo3yfMjVlEYb25eisg3UNIVhPhrsRMzmmDt1e2LQ8YA9AabvusiAz27k2Uz9RaYA4lNFMIUuovUiDSCX-PkP7uV3duN4NEP9ZBuRYmJ-74Si8MF2nm758SU9vts38XeG92E2HCQaft_vbFin0Ha-C1gWZBDTJ1A1dgYC44x3c0J8v-n9e3qXqXCVQ9fXC8TYcX9vp0XYN4OC3D041Xy8uWG7U4TZAe-EUeRz3pWaQtcCbOeljMr7OGSA8MTv6jomc77q5559sWQk6sen3IEvYtDNbQGI9czg6eCUY605QmKHkWE8qFee94dG6DrI8JdTvHAsF5t1JuqRkfY_Nw8rx2wcUmgY6-n7D0s7NieaGLWWRN6hVdbp2bW9mW_K9m9DkA_cz6NEYG7q5akymuAWZ72qDdfeYQKaFFMcODXk4o3AE16m4u4m4aHj36PB7Fl68WK5zfIYeB3itJAfDg3S8v28s1EAMKXz78F964lAKX7HJA2BbA4iEWejUlCE-I2G75EOSUWvs_NDTg_GBT6pth1KSXQZ5GceG7z1wmXpK3C_MHeVFP86IAC1GV6J-eSUru3dpU9XC_tR1bqymWRG7AUmpGGsQ74O6cNF8Z3GUcewyb7-8xtu1HymMcSS5eEk6PCmzv896NAo_lL-E52Uo674DliGejyzL4I1CLDAjGIXaTQ4UBz2Zu-pM_JZZyjthrzhqNXSQQGOukCAlP-iRkucgujNtmwDZ2IOnexWhzzQIOQ5U68ys4POH8x6orUlZCQUSY6tFOPZt2_B8fQwtryMtwdOOb_IWv9TBnRtqyc3_kv37a8WRB44mMOsdwuta_bIY_8iw1Ff5QY0IHkYFpm7qXMBYOOK0e5OIDNVfo7L6ZtR2uZmESrKVAgP8L3ChWbFg10gQk7pB-l5m_NftTNMgLnVhbMxt5rm-U7pWL3JU6CrsbikHH06D4oxCIvbksCnKg_xZofVxXQGCLN2t35OmtConYYUbJAH6jBA6Mseabl4ZF99j82B6d1nihzIZh2W6IEDC2Wp20S1y9vmOy1NI-fGpM0hEDxwBEZA2n-ON_9ZHwX9cf1_rPF9YipQzB5M2R-puvYQ3jWEW_1sUZERJ-vXDIzU3hyuuMhHUizypSyQzxZuOQ6tlq-ZttPG5QobXJxeIuN6_Yqu-vWnlatpP0SyUeLc49rlfolZjJeeFrXCUhBwRCwguJJnuN0Lov8PD5vOVznMV661gxwScURTSTk9RQTpAkIT9VI45XGQgNGPeJs_LUa8B_8nev0qNQ9kf6jaSpjzFxjnsAOuvrSaLFNGiPTid7h0iCrRri4lWTPg4OmPl04qN2sDJk4tzVTfZscDlc0UWKT8o3e98C5fliUwBEDTOnDWbCxZ7GfSoNmMTtZGnvafUYcfdrG1sUCyba1JwPw6DT7NfO61q87F8bU0-NdikgEr7USg4lM9qAAIffQbiFA0r454-EdGjYlkzIMeKg3UadfS3J3tQDUUO7zR7I2brlT5fPGvmjd3T6CBikG9-6jx85mOdch6Yt5Tpd71Ouu7KtK4cR9wgpXUaU1aCzL2iAfozhWjyjELWUkXLgPfq_Mw_XA4DCklBprUd3nN2RvQtBSItHRqZJp_1Zr03G_s8gZhxPK3Oa0KfVuUhbImUNKfAGkJYXoiqGOxwA6667oZKJA3lKtPx_e6eNRNlKopb03igKGlxX_6eR0mm2eUgtFUfaPpfPTQzOP3lghxZnhZimJa-WgiZX9LCm5nWmI--kHIPrvniN5GdIyUPfl5QlRmjW9sG-0AJKvnvvobL39TbpKGcgNxEFzuUsr0Iagz5eEMawWxfLQMzekAH6pP825O0D6fJP-cX0xQK7bguFzVa8ExifwIOtJ0SBLNYRDulBwVekKAAeprR66I9GTmEYg_tt8Srqj6Ancfa81GIB9M3aaSrdaRaCGQn-fZ1_tE0M_g2_SGIyod2d_ry_y3VJ5NBuyMyfEWuX2Exz1u4EAVig0BI4WQFu_x26ba1cvrJwfNgjBf15zD7ZUB-MG3wweoQ-sJ5CFWtiIQv98bXgVFkWRXSbmhqUoSMa_O1HJUjR_FBSjRhhFqVkgOB2VhS_2PvJOW0joYCyKjvh-YSq0-4b_tpzebgX8eUFMzh3PZlRMGszP2xhGMyizTqZlzS4vDy_oI9cytCQeCqzAU8dy4Hrj0PQkNCLBEQNxiyra59ED1wnxslChHHlJKsgg5zhQSmi9VpVaLFcZvi9G09uLXKIDkY_FwwtYmMh0wztFHvGpDJTsX39L05fCUOR7QRUDiS3qJitgTJpY7YpESF49nL9cODRPRJUyrrwtlPijrmMeWdAMgDOnLkxNMl5qwdLlL1wOf4YecVKDwGqytEpzz6e_PxqxdwmeDYdlkmDCVkZIAhjR0CxsVGgLiI826TK53fNq7h-cM5ZwnVdKd3mUUUoqivYZZOr1SOGPUoSc5YcOEK73YiQtK4UtD8TfEiTPkzIAc1yfJDvm9CIuEdQQxxiiM_1u2FLMF0sCKE9twmc6Nbp2NmHJCsruQ766g2m-wu6QttpuSECCwuIoy_JmB5RQrUzlp1fS0m_3gC3irjWYUnjGsNawDB2EfCTO9dbF3dNaRDVCNpT-sSVuw6hNZCzVX673XFtEepPVpTsv7dh_VxdnwpWeAKkY3lYobmGRKkLJNA-X0CPeiPyD1RNGRz7D6Ts_1FSp023d0eJiEE1pawL3pn3q-iboYI3sqUUM3rVZl4Gq2alwJg_VCyUkgB-zL7VgMKjLSyvm-MwaM0860bdFJzjnRTwaNaxsoLhziUesxPrExNZ5xvZLq1vJ64cyqkYSXbFTNev4tGZ-ffmj0KZNZzfJ4WMUA1n0iEKqmV7v7Zfwf2E3Z6yO93LFr4Lse5yNQ2LUq8eQUwVk0_cDAVuFxZB6XQe6fhdEqh4MyI39R1u553AkEWC4AenwiVCt0xSNpwSL__lpt43Q1bjqFfRM5bI-mer7j3VRYyl9I6-au15enXbGZGeU-NalhrHcjfNe83e-wPJ5_lrGGZplVPg_Wlwuk25nsuE9LT3i437OJx0fkLkQvxONxzlO2xJa1a5C9tZ073OnGEuRnKoV6pUABUuBGkrhXtOxA-Q-W-S0GEn0-9ca8qAWiKPECRNV9D9PqJqsXupma_mn_vadi_HS0), отражающая ключевые сущности системы, их атрибуты и тип связей между ними.

# Задание 4. Создание и документирование API

### 1. Тип API

Для взаимодействия микросервисов используется **гибридный подход** с двумя типами API:

**REST API (OpenAPI 3.0)** - для синхронного взаимодействия:
- Прямые вызовы между сервисами, требующие немедленного ответа
- Взаимодействие через внешний API Gateway
- CRUD операции с ресурсами
- Управление устройствами и состоянием в реальном времени

**AsyncAPI** - для асинхронного взаимодействия:
- Событийная архитектура через RabbitMQ
- Коммуникация с IoT устройствами через MQTT (Не реализовано в MVP)
- Синхронизация кэшей между сервисами
- Системные уведомления и алерты

Данный подход обеспечивает **оптимальную производительность** (асинхронность для событий) и **простоту** (REST для прямых вызовов), соответствуя сложности MVP архитектуры.

### 2. Документация API

#### REST APIs (OpenAPI Спецификации)

| Сервис | Порт | Описание | Документация API |
|---------|------|-------------|-------------------|
| **Device Registry** | 8082 | Управление каталогом и регистрация устройств | [device-registry-openapi.yaml](api-docs/restapi/device-registry-openapi.yaml) |
| **Device Control** | 8083 | Управление состоянием и командами устройств в реальном времени | [device-control-openapi.yaml](api-docs/restapi/device-control-openapi.yaml) |
| **Telemetry** | 8084 | Сбор данных датчиков и хранение временных рядов | [telemetry-openapi.yaml](api-docs/restapi/telemetry-openapi.yaml) |

#### Асинхронная коммуникация

| Протокол | Описание | Документация API |
|----------|-------------|-------------------|
| **MQTT** | Коммуникация IoT устройства ↔ сервисы | [smart-home-asyncapi.yaml](api-docs/asyncapi/smart-home-asyncapi.yaml) |
| **RabbitMQ** | События между сервисами и синхронизация кэшей | [smart-home-asyncapi.yaml](api-docs/asyncapi/smart-home-asyncapi.yaml) |

#### Основные API операции (требования задания)

**✅ Управление информацией об устройствах:**
- `GET /api/v1/devices/{deviceId}` - Получение информации об устройстве *(Device Registry)*
- `GET /api/v1/devices/{deviceId}/state` - Получение состояния устройства *(Device Control)*

**✅ Управление состоянием устройства:**
- `PUT /api/v1/devices/{deviceId}/state` - Обновление состояния устройства *(Device Control)*

**✅ Операции с командами устройств:**
- `POST /api/v1/devices/{deviceId}/commands` - Отправка команды устройству *(Device Control)*
- `GET /api/v1/devices/{deviceId}/commands/{commandId}` - Получение статуса команды *(Device Control)*

#### Паттерны взаимодействия между сервисами

**Стратегия "Кэш в первую очередь"** (устраняет единые точки отказа):
```
1. Сервис проверяет Redis кэш для получения данных
2. При промахе кэша → API вызов к исходному сервису  
3. Кэширование результата для будущих запросов
4. События RabbitMQ обновляют кэши всех сервисов
```

**Синхронизация через события:**
- Регистрация устройства → `events.device` → обновления кэшей
- Изменения дома/локации → `events.cache` → инвалидация затронутых ключей
- Изменения прав пользователя → `events.cache` → обновление прав доступа

**Пример потока интеграции API:**
```
1. Frontend → API Gateway → Device Control Service
2. Device Control → Redis кэш (права, метаданные устройств)
3. При промахе кэша → вызов API Device Registry
4. Device Control → MQTT → Физическое устройство
5. Device Control → событие RabbitMQ → Обновления кэшей
```

#### Безопасность и мониторинг API

**Аутентификация:** JWT Bearer токены для всех REST API (Не реализовано)

**Проверки здоровья:** `/health` эндпоинты для всех сервисов

**Обработка ошибок:** Единообразные HTTP коды статуса (200, 400, 404, 500)

**Валидация:** Валидация запросов/ответов через OpenAPI спецификации

# Задание 5. Работа с docker и docker-compose

## Реализованное решение

### 1. Temperature API Service

Создан микросервис **temperature-api** на **PHP + Symfony** с следующей функциональностью:

**Основные endpoints:**
- `GET /temperature?location=` - возвращает случайную температуру (18-25°C) для указанной локации
- `GET /temperature/{sensorId}` - возвращает температуру по ID датчика
- `GET /health` - проверка работоспособности сервиса

**Mapping локаций и датчиков:**
- Living Room ↔ Sensor ID 1
- Bedroom ↔ Sensor ID 2  
- Kitchen ↔ Sensor ID 3
- Unknown/Empty ↔ Sensor ID 0

### 2. Архитектура приложения

Применены принципы SOLID с разделением ответственности:
- **Controllers** - обработка HTTP запросов
- **Services** - бизнес-логика генерации температуры
- **DTOs** - передача данных между слоями
- **Interfaces** - слабая связанность компонентов

### 3. Docker интеграция

**Temperature API:**
- Dockerfile с оптимизированной сборкой (PHP 8.4 + Nginx)
- Порт 8081 как требуется в задании
- Health check для мониторинга состояния

**PostgreSQL для smart_home:**
- Добавлена БД с автоматической инициализацией через `init.sql`
- Подключение к Go приложению через переменные окружения

### 4. Интеграция сервисов

Монолитное Go приложение успешно интегрировано с новым temperature-api:
- HTTP клиент для вызова внешнего API
- Обогащение данных датчиков актуальными температурными показаниями
- Обработка ошибок и fallback стратегии

### 5. Проверка работоспособности

Используя Postman коллекцию `smarthome-api.postman_collection.json`:
- **Create Sensor** - создание нового датчика
- **Get All Sensors** - получение списка с актуальными температурами

Каждый вызов возвращает разные значения температуры в диапазоне 18-25°C, что подтверждает корректную работу интеграции между сервисами.


# **Задание 6. Разработка MVP**

📝 [Инструкция по развертыванию приложения](apps/README-DOCKER.md)  
🧪 [Postman коллекция для E2E тестирования](e2e-testing.postman_collection.json)

Реализованы три новых микросервиса для постепенного перехода от монолитной архитектуры к микросервисной. Каждый сервис разработан на отдельном языке программирования для демонстрации полиглот-архитектуры.

## **Созданные микросервисы**

### **Device Registry Service (Go)**
Сервис управления каталогом устройств с PostgreSQL в качестве хранилища данных.

**Основная функциональность:**
- CRUD операции с устройствами
- Интеграция с монолитом для регистрации сенсора
- Публикация событий для каскадных операций
- Управление типами устройств
- Валидация данных и UUID поддержка
- RESTful API с пагинацией и фильтрацией

**Ключевые endpoint'ы:**
- `GET/POST /api/v1/devices` - управление устройствами
- `GET /api/v1/device-types` - каталог типов устройств
- `GET /health` - проверка работоспособности

### **Device Control Service (Python FastAPI)**
Сервис управления состоянием устройств в реальном времени, использующий Redis для хранения.

**Основная функциональность:**
- Управление состоянием устройств
- Очередь команд с приоритетами
- Симуляция выполнения команд
- Удаление данных по девайсу, через его удаление в Device Registry (Events, RabbitMQ)
- Интеграция с Device Registry для валидации

**Ключевые endpoint'ы:**
- `GET/PUT /api/v1/devices/{deviceId}/state` - состояние устройств
- `POST /api/v1/devices/{deviceId}/commands` - отправка команд
- `POST /api/v1/devices/{deviceId}/ping` - проверка связности

### **Telemetry Service (Java Spring Boot)**
Сервис сбора и анализа телеметрических данных с использованием InfluxDB для временных рядов.

**Основная функциональность:**
- Сбор данных от IoT устройств
- Пакетная обработка телеметрии
- Статистический анализ данных
- Удаление данных по девайсу, через его удаление в Device Registry (Events, RabbitMQ)
- Кэширование метаданных в Redis

**Ключевые endpoint'ы:**
- `POST /api/v1/telemetry` - отправка телеметрии
- `GET /api/v1/telemetry/devices/{deviceId}` - данные устройства
- `GET /api/v1/telemetry/statistics` - аналитика

## **Архитектурные решения**

### **Database per Service**
Каждый микросервис использует специализированное хранилище данных:
- **Device Registry:** PostgreSQL для реляционных данных
- **Device Control:** Redis для быстрого доступа к состояниям
- **Telemetry:** InfluxDB для временных рядов

### **Event-Driven Integration**
Настроена интеграция между монолитом и микросервисами через RabbitMQ:
- Монолит публикует события изменений сенсоров
- Device Registry автоматически создает устройства при получении событий
- Обеспечивается постепенный переход к микросервисной архитектуре (Strangler Fig)

### **Контейнеризация**
Все сервисы упакованы в Docker контейнеры с оптимизированными multi-stage builds:
- Изолированные среды выполнения
- Независимое масштабирование
- Упрощенное развертывание через docker-compose

## **Взаимодействие сервисов**

Микросервисы интегрированы с существующим монолитом через:
- **HTTP API calls** для синхронного взаимодействия
- **RabbitMQ события** для асинхронной синхронизации
- **Shared Redis cache** для общих данных

Такой подход позволяет постепенно переносить функциональность из монолита в микросервисы без нарушения работы системы.

## **Результат**

Создана основа микросервисной архитектуры с тремя функциональными сервисами, готовыми для дальнейшего развития. Реализован паттерн Strangler Fig для плавного перехода от монолитной системы к распределенной архитектуре.

🧪 [Postman коллекция для полного тестирования микросервисов](e2e-testing.postman_collection.json)
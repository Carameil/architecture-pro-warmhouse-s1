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

[C4 Context Diagram](https://www.planttext.com?text=hLLHZzeu47xdLqpHFS2gsRJNtijA9vK2Mz2nkvHahxedP8X9B3bsQZkmKNN__MQS1CB2LVTm-sGyavdl--ORxoVhc75bBdjlXDhACk6GELVOkp0qx72R2fULcz9oizNASULkjpeF2yaKVHDObqYn2SSVWoLTFJyimfnPnmrUhSVqnOkxBQXwxqt2Tq9nc4p_d8-V7gF_qCQL1KlCXrCxWT5WXnc1BOnmtBRT4hwdc3rskNzwZ4VVfj7Jm_H_RUJls85RdVFWjvDcBQimrJNVoqYXhKiDjz_bgumErf2GsP_47SyBRfWh3Lzd8ir-ahgLkamQyHUZlvwUu_MtUJnRBwPVfe-JmR3NSy993kEFq4mA9eAbXkKGG9IZAs26BE51AorHDXYCR32t-DqICMero32g3ugRjz2VjUS-_fYg18W8738DELCivHOe6pBEd50fCCp8jH9E6miTJc1uhXsN5PRvfPR-zxxNf3zisx8a5mgxH1LE98SH_HJMOG7jkpyq3MqTnqdkH5fYM6ZLROmuhdNfrNNNdtMf4cQgWIKfY5yTeCudSQPRNVVf3-573_2lpXhaZMhdwQsMXFEB1bOKKcnhP2vZxYXMLnBBbIMP27fXz5uacB2QHZ6CbdFOQTE8TXINd6sqx99BDJBx-yXj-k86dHPOXwRsWWPNJ4BSQOuXmXfdmYH1WJmgn6DmsNY3XHeQpcJW1FBhuawOOx2fGEgQOHUFzoP2keudgXbJtqwzvfRWBBFAecuIvZe6l_ieoU3zktTGucZj48J2PqrAdk39EyzweRhmhhlWzNuTCKus7YMHsZ3B2KIzpz7N1KoQMy8MOL5odqVhHNXxxi5waeqx4QRZNG2dLuAykEAoksOTKnrTtcgr5_nWBmP0Y6Pnl8p2rZ5ObgesMzIjsEvZ0mtj92dGUlACnB_MFHMyCDWzTzkC2fN6uCjxXlNS9tNffk8rOtHWhtYXZNlzYZ0ofcrBNM3hZB6ULsYuCuBlpoVdv_br4SPZAj5Jj6K6NU50xuuOSEjlRiODf_jIROa4mqXrTtE3mG36g-S9Q8_Ep_-r5PiP-jpLrLqwcHN0fytnmKwRD-r0ZpDljzUTRw_FdlbHTgjmbHHQAEV3KQMscK4wfKNflfVSEjfkN2fBkLh9geSV0F5lL91yvqMu78wNepx3eXcfRPLOY3rLN6cTms0-kOFtGt__cIVcUFVhi27vXo33gFhsGvqPuSAKzWuOohpdeL3reOFl4nxndnxy0m00)

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

[C4 Container Diagram - Smart Home MVP](https://www.planttext.com?text=bLTDRzj64BthLqnLe941oqA1ddAAR2csKikHBJfjJyQAN4kRa5nYTsbRAFhVExCV94LIjXCWYKWktxnzE_Dc-4aRfaoPfODVH1wdPS9XPqoXFqucYZsVRONPbPjISnNBtF3SdCKocnGfo-cTiJP9AZQJYp_6AxfrlxoUP4mRhl3MmmM-mKJErLb1-8FhwJzVxwE7lnRHrSFr_79-CnkT30P6c9J3nHzGBOUPO5l5CXZ3EaDoS2Kp3lDOwZr2Pp2AvFYSm_BR2gOgtWd3OrCgZbWhhhJCHpjSBvyHwHI6L-7t42_cUGAq5tZUISXOphPSWxOnw1r8-9E8kJw5M75cw5dCSwQ4rC3mrKpcmaYbSJ6YWEAPD3pT3qz2O9Pa8iSuQqTqIfT26yNmBOg_327TSvJqKMnBV2naRYDOShei2gaCmWuSqCFynOpu2ygQipJYMJ4j1Et6tm6wnH45HO3fQa6HLae-YGH3FUD6TWXAwAQnBc66mQDKaChOnjSOhn7EgZ8BUWpTGVXh2h2GKP3hum6mvhb6ZKuX5TkT8IrPXi_mXnDwIy8szdOkopnXQkyYqd5L9rt5FKo0uGdyEm3yKsNCY1NY4d6VsHxVGGd0Nn7JFsM-dTnSBcx-fgkroBUORvZ9QW-55Bav1ILBWxTFWPMeLMw4KmGV80hk8nFq63bJKFlF1uoDUvt93EF3cYih18m-opILpxWOPhCPsAtnnFB4uzFmg6G64TC_uFUJrWwSbdTyI_b2kt1QGxWJWj0UBkMmNe1SMIgHVCODerCzXP0gb0o0E-HRds8Z_uudT3BiPq7NIgPr37-ZoU7YLpYxcKTMXoORcM5T9NJQXiHpHPPnApDdwTY8Z5Ovl540Fae8wmlVnltxq1Wf2hUQT6vami--QvY_58fhcw4NU1Re33RpjQ7ZOccAAZ6DQKgCN56xV7NrC5gw2gS3F4g9QdJPsDRmdegsljEazOKzCLWN5l1SIZCCozkPLYZvA58gDHPZ9kbQeaL2wyp-E27hHWUChgm_zLDQxSqEKs7sdULRcqTsiQOga1cLRV03ImhB6aCV6jLe5UuVfHHyipwrjR_JG-KpTWnlMzFIO4dPAAgqSbGEV0f6YUsMgyf9a1wrYOp47LNuBKw-N1Vv8tgGKQLj5LN5jcXdg3nATgxa3ziybkuQt0qYUCSs6s6MjvQitkVnJiaS1RI9N-RLUV8daXq3SfPrl6E6kRPXcWmZ_0GKDB225JDKmDZ_QZzfUqbByTccvI2pJO-7pCwjx5ARhUBhs-lNRSG1WjaNT107fk8IGhVFlMBXwxf98fIwut374w4RuQrRKBFm_e5yuXtlympYrXEtCWeRi_Q88B3jMVbZMhuu0X5YZh4I1Pv2mnF0RTsRmawGSUnxmf49s4Zjuk-JcDReRdUfQnuFFnDQ-oxSZcNhsmRhr7LVj9-jvxwGeUfirAgqcbeUnkpMRLsi62mHde6RRq99O-3_1eCFO3LSO5uh5jk5WpkUDgUuW_P-h-rOivbaSeFouSEYPs2pIaJHQZsvcwyZZ4IGVZuxW3kSoJgl8Rt6F056DhgzdoNcTfPOqIpXmpJmEzrYMcS1OfGkDS8i6b8Yq3WWtq3jMdxOXdtYEbizsRs1rfazt4zaPd6yDUWmxxqFhXuY_2zTZzTONeckQlqjl9uPu8up1AWUE5Du0wRRau9chCxUJluqyg0wU2ER5jvoPbXecLemHw4jRezDhsaw2V84kU6tsdA3tTBQs-l9vTUxvR29NcdTTTyByj4AszbiPS20dTp8MAOUhQlqeQrweiqPgkchS8tsqSPi5q3bd1xCLe3DiqD_39lziffwf4xIryReiOubF-6uWGTgwJhvS6Xw3f6rZ2sWuTzfYifcUvSRrRVOeXwtCJSY70_AxGvQZTWJEFZ4widojIsJne1glo5lesXo8QUh_WneVxTrAis-JvazxC_FxdVcPsvi3eLFoUhJaUTAn3iwYvvgcTC4D9ufWVzZRJIc8nhxbVj-julqiUHhZsEhSfCAlMjrR3jgsQrDbxT7L3cEr2oCrw1gWTx4KcwfssilP2UJZsFQfPEGpVrmdKnkmwtBZOwLs3ZlygsvYpNuuBhmE0oYhMCx_BgTOgcEyIHLMTZ7rspFrIkkrEWltrWg4jl_l6JyXVfDxZuBsYRUGkjqr1hDZXVDOUfjj6udhU4grTqtsN6qUcklmRMEaYge9sFwedHBusr9mnp_FhntoK2CgrVxS3k9s1u58ZyL1b_5jkdf-idlNQX71uBcjpoH9Sl6D6TfULJQ3lWhwHNMWXrBzKuKxqByoPmuEeBqEiTRrL1HjTYkGNRS-XDsYJ9BuNy0)

Диаграмма показывает микросервисную архитектуру с фокусом на ключевые бизнес-функции: **Device Registry & Control**, **Telemetry**, **User Management** и **Scenario Service** как главную ценность автоматизации умного дома. External integrations встроены в Device Registry для упрощения MVP.

**Диаграммы компонентов (Components)**

1. **Device Registry Service (Device Context)** - [диаграмма](https://www.planttext.com?text=ZLTDS-8u4BtpAxJiOPEg95pcjASXm32f3Ha2P6QsQgieOJT64rloId88kxN_VLiVjeKmrEH0sItfgVlrqvFpKMagTLNalLyOJ_8g1RBJkbI_3mQIxkyofdVLfb8W4y4rS7sNY69GvfIRDRTAfpdR34OVRkScz39x71HKQP0OMez4KGfkrfG6l_Su_FFBoshzxM7rUVqucKwUnbVNlPvcEWSo-a2QwMJCQ2PfGMx96DvO0cG16LDQ7iWIf0rS-O6HIUfT8unveeo3lA8bcr8DUtgu8VtX_87ujpw-_Y5uDdYQFZnzDsyBKMbGHCBV5IYjY1QaO8aKoksY-jS1R6ftDEzIvBWuo003TMO6TlGhkNkQh718sdS_4uahA4yLmH-2h6iW-4upAB3cewqqv1Z4Ud7ngduETtWIATpzK6JoNafGgdyqBwMQucPv3ebcWXyXgmGubKnuB5otz94GVdxGEy799-maycSYmqgBWXeuKaCSixCJg95PKmuk-MmYf0rrR33-GNac-iraGqmk4hVF6yrPIdq5lUK1LLMi9-_w2liDajDy1KKvB1ckdlW8CI6GL5SIYC_y6r2z0scnTJ1iMEg7kbdVYugdL1vyfslDjGsCLTY_9l_s2FurolLBNLP-bSzZof1G53NiXNorufiiLsQGR8KagPS89fMBBD35Nxmv1D9jRoa9mjXqQbi-MhoCWxW-bD0Df83AP6SPsR8SgMCyGofC9L4vFX_NH1c8tkVf1snzjez6yWJj0WbL2dkPiQIZlgxiEpErDQImPPoPgbKmrCg3o2e7ErId9MH6EVkd5az7ORM67j0-CaTfgACWtDOM1CaURP7K8AQBTlz8VYJ9cQ-dLg4j5RXaoIugTSjaiQUui2fnagjdZixC8MPFTPVa9drrj8YWMKseBUMKGoE6DrJ2bYMnmD2tIenjMCxqWIGxI5vTTb9eWJuJm9ZmieIaWJd1N0GYFZxYNLrwmzzvjScPsbdvJKo0D96s83TGkW6KixLob21AYcoaU0LvvY0HkyUHUbm3We7wsA1bLme9GDCAXqDT-3bk48lwBp2OyIPobl6cmrl6zzOWXTAPXENpOtY1k0HlcaDpOm-8kccLY9VqbcN7rknvkBSq8DhCy-82rehePiFqxBbb4bC1MZXVwFr6ceF1i5P9xRsYUWl8ZoxRqFoSCwX8kjRgLwlvSc3S3x4DH7InXYXUkYqEl2DBmDqjOf0y4B58mRrv4QNcBmAn62StP8Wq1Ts0C8x6LZGU4UC4oqxlnZOZEpPeBht0AqELnXjWIupgVClhpwQJvWGvT3Xv3fbDuTYktPboM5lgFe8Yh5EdQUkflwNEmSHvNCBoH2Wqb9_6EsFird5RoBhvFuKOcr3zh9hhsX_tYym59Lv031pvF2XsVspk4OLecEC5Ne-SPedJEqyakinW-cKnEvTl1zO2QAfExk08B3OEtm-dpSv6dNxQVG9j6xSFBj2Eqn3xlziYuZasmfRssbqQoeUpvxb1_WWynVzToFy0)
2. **Device Control Service (Control Context)** - [диаграмма](https://www.planttext.com?text=bLTBR-Cs4BxxLx3keGcmYIzxsjFwXSHjd3gMqmTGmA3fYSLM8bMIoiPRzBztXjHRCj8D4CGSShvvVJFZVBQE6vUdoUWxgKII7u75pcNsqsHY-9VRatHnViWj6A6L0-LkXKudMS8LwTnOTqpaOJB_UBCXqVFwOP9owy2WQ3_NQQOLwMI4FtgO_lxpysx_wsftltzOtYqV5rVNev6JBW4s_yYgvsmX-SdmbDsm1RnAWRTes-Y4HM3y-QeKq5zuSmX3dxXKOAvu9k-uWo_y_86DfviLAqvZFFwarMdoUBTw_8rEMvqxiCp0tpbOPvdJB9N2Q1lCsF5r0_ReNTd2ILfd2BhmhPIKpX7mdHxN3mHtFD4dnjMHfU3u4SyjPChkcYj-WXIZHyLd5B1QqaJ-aRzo5cL6gXERQUt6vNEUknZVIhGcjVB6CZ2fj1QFxKXYZHPR1kz9Si7YvknY15pyoTN9X_MWIni2SsJPAq_aqKlGoYWwO_dJ_VBDNTaKcHMIO54lecDHKKj0AxrZ8Qls4qlaANRux8J5mZ-2fs0uVh2Wh3OKG1t1VgPpTUJcN1IbO4ETyWvVnjVidn73dufYLoAyc6OIjShdI1RoQnfjRX-NEtnjV0QsosX7T-n5cy9XLkZ_eIgEOb-ab7LGnqnBvMpZ3dlC0KEM6GbUlWMUt3YPLa0CDS2yS05qFUDEn4nZ0NrUANfolnU3jtYFlmaO7qTmeP2Gx_FlnusBp6Wg6P67ZdNbo6ZvQ5YApkUernGP8DeZjQLeYssUk8QjOUzzJWBbJ5svo6Ug4DS1jFAOLZmbiq73dfM8ZLRoQ-Lvk11Sn8NVGH1Zjsfp3XwhbqGAHoxh92yeFE2hmN9HjGgYX6pNDQp5jSVrxR7cHwwA_YJhwwVThY_T6EsqG4BWdCYG4hPmL87mJTSEVeqPnYCpECakPhgWsF8L3vlya4WR-m2yW5MIslTmaOKB8WiDniBImUY_mFIuBM8S3y20D6sJ3V06mcUsTHSu_y9bafkcLjLBLRpX3WFxjp405eTsvoyE_TvVp48PZj9s6KJp9RXdFJ_W0pDrkRhZkOXyvWD7f3BdGU2DyCD1klLJdJjS2C3JK97H3mod8HZ5YoxdFZ8xsaBIMbP3CycM4vAmxtUxJJIXSOImfDtKwEsdNq8VGmc1fS0hK66i4zObgDv2gg6QE-KTbFwQgM6IzWxfGc7ALgf8cQY-5lIIrxJM6uQRC7_GM5dhGF1wqje1fpjJANogCkXlByEqFUX0FMSXz6_261my4NZYlXDLs_mm40xDRtJeqYmfFzjw3uR6_L_UlOyQfinbJ6JC0XkNqMn7p04R_O4nfzPlrzPMCwCQn1SIqOUBq7GZVbgOC0X7lWxi16zcg4xTxr6zyjCccL3_bCbjALnONfasBkGrxu7GoEhiW6QdTElKdP1DCeHXN1Tkkdxo5ZvZ5FXV0ViF)  
3. **Telemetry Service (Monitoring Context)** - [диаграмма](https://www.planttext.com?text=ZLPDS-Cs3BtxLx2vD9bPnvSzzRIExIQUsgaJQ_inqvaCBM4bRYbIIrBPU3lzxmL8VL6Ec_N9WWZm0NXuq0Vhk75rAIS_29NAEWDME5VPdsOpmxzUvy8LzQ6sO5Aj72XtdUfoLacko6TgNIR5ORPuFzsHwUDsCokvTM3GzBJGPQKL-LGKVxAP__dhn-JfztLozxHPtQxkbvTNauaJJW9Rl6VTSROKF3UyP5EMW8GId3coFPXdaGAxt6ebd3P2vUY1a5uS1g5_N2WmbxmIjzp1LtvynoxckpLhdYxmyHUjyjdzxVh-3tfwrBK3omnygS4wovncfKYDjk4cUt4r29i1sHuX5nRHeFVI6rXhQV5Hu5jzqHz0z3pZZhDdBWN-4Lf5WMqAYXkX6tzqt3UMOSZTqHLQiP-njfYJHmyyiyo2ijemVy4d_6DgEG9UQ6pUbYkUOo6LGyyxih3UzCezoyzSvJwBZKuzvW5yVv5h6uENJVP7R7dvj7fnbxP4DeNKBUL2ZwnvfdXhdJIWBUCgm_9mM_jcPj1MlKV_TADhbN5plEpkw-jqmesBA_RFXE6lOrBlDgz4vD9KSVLI6R3s-hC7zxZQ9_Gcpj1NzoyrPKkgCf85JQ2oIWlbBDcn5NBgH0cX3qW7uAL_SuVSGAgcN8PNFCKC_PkvulBeH6hxK9WvWHzbG7uBBIMa68beHN4w0w6-rnaq2Um7T4Z36MmOtRTzI99WnreZtHlpCcOccNP6EutEhC3-I3pTdLFQb7ZqMqlYLz1I5N1-15WCGxbwp0bQfy7CEli8-l2Cnrc5WqC0FCyDv3m2ZtBaMzZOvaeZHkmPk3vvVz00oHVnjz9If0BE59otdLcf73b8AjBrAfX6o6-u5Idht3ojXR8YBm8zzWuHOyM91VwK5TwU66nz37Gk0SSb1wH-dEMN6WllZgGcIASpo3l6hfxnSLSVfB05K0EyWNMMKGBXRHNUqYYI4c99KH_OmUY_mNW6-KaDYW7a4P8eiFY5bXdZXDqEUz2VwYlO5WOp-7SmwCj3ZtvvY2QLyZzmwwk-Lfza_RAy6HEc760ZyhnhXR7m0tYC5B59xSPdXe6sJQh1u7M07mx2RH_wueIPnjgGFFt8KEd0O4wO3lxXldzsyWWoMZyZyNce0zXOMEYEkoJPxMUaFXYUefmiXL6e4_5lemo3n3ud2-otCFKqRl6cUZlMwKxfWybsOSHROXGJQxXMJGtJRk-VL3E-zdLnQSlQw0LaNK4QYhDIPyrQF8rvEkstu6nlFXzd9ColWqD1naRXXzHRe5xpFiKMmdrlfcE4P-MX_HyFKXZVy-5Fu7tF1RrC_bzufCSoh59ja6JdInIBnXwFqu2DLibGETW6eNOxxirCVDYMOKMypQEm3_wpO8EVJJK2Rb88SuNyBcibmbY4yOu-aiORFZHvDDoqlsSBFTyr0ZIIx9ZPu_gF5QzlHwz5VJlcsmT_omV4WT_jx3y0)
4. **User Management Service (User Context)** - [диаграмма](https://www.planttext.com?text=XLPBSzem4BxxLwZqq6Gcn9USUYh1b90IGcFIj3ETOOGjR3Msv4fo2EtqltVNxnSJJcYb_NQ_Rn_mIHkgJ1f7rZikl2Zr6GcDIVH7srRqS1bm4wQxL3FbIM6OC9UUZEqaeW9znjhu4T_Pqwln6asFTqixfjem1QRjLCQ95EYJ8BwrdFouVznidnQRc-roDf-jdBDpopBSH8nChqZrd3YS1eh6P4mU8J2veu86BCORbwaNxZ5ojl3XoCqHl22jLmD0-8romTGPJVYS6dQWnmiocgmNf3YDuFX5Yi1UpHUhxtXwaAbXcYZsEsNQQ68aYRcdfCxZwD5v0zPdQCEpaX4uEzcP58OoDuITlYUNgza6hbHs_aOZxbECX4gIXAcOQysbQ0VG7XDKSLaWWQDRM9hWwwC9fI2VGMVWreOl4OWy0A0EUT8A44g8Nyi9hZTe6L2u4SZvHKKWqP1Leq8cBtbOe35goh-zbgdmgJgU8TbMj1Fb79sJlnQ1JzK2cUiauQLFGTHD51S1kPRIPDMRkHky8dkfScNZ2lkdABf7iO1hex8yxKW6NE3LMiazXxvhEqnI4s9VUTdhx5rLgiPJe8lPTbAcu9npKzXsBQY2jCAiR-aB7VLVKE4JX92A_wd2tpvje2UVcUWakgPQ7wJo2HEUEYRbSvTrKqNhFKOY7EOaK6Mv1l9FSabg2fbyfL031BBxP518a4rBGIE82hM9QpaLspC5IKEljZrMql1zgOLcna2XTLOJpuD-Cc_ANmzLpQ1HlQ42U9269bunlrY0ncHYx0SPiV6EQWQbQJskkglZqAbfFI454uHGF0YDRlFvrvWVPvTrlxCR61ZdEXjCgKsWcFjrMPP1NsIzKuU-A0i3D-rrOtqWirVOq7bru0lrq45R3ongRSvw1FC4ykMbSvqAS8n_izciNHjd4T0HfBTbQgJE8gotL87JXEcisvCWJNe3CBsbEWGqi3SxK23RGXIoUTKlLK_02lpaDYXrB3innzZJ_DUcXT6RoB8GJVdVmXWOYvftHxjXh1QL8RnfoBndNOv8tj-duRgiXX3dp0nNjmy9XN6eeKGRgS2xdt4rJ2wy0CvuIO1-jWDWXkpRzUH4QbtNjB7nz81xdqaNeKEdzhS-CU73toJo7m00)
5. **Scenario Service (Scenario Context - CORE MVP)** - [диаграмма](https://www.planttext.com?text=ZLVVRzis47xtNy5v3qk0E7xfqvxgs5gQrdOTssatO410IOpCHY8rah9Z3FlVUqVg5oKvM842vb7yxkwxku_CHsseCaMMZdxX8agBc963CRd-VJfLz7IRS7Cema8p5KbXc32taSocUKe5dfbe4wSydCu_J3PeUbejfndLXYamFSzbbak1PtB47ordVtrzsZz_VzX_Ubu6zy5wSNKz6XbkKaRc7qZzELbmcYYQaGdPHKnGnINPCNNa4IDNjMMEyRmP-6Z-THkGrRSDWA6HSi7K5StvFJNiHCytP3pRF91oDORb5ocIwVh-OVqdhhQoC4mJnVufc3QQ64ao7YcfdKizlcx1nWnjk5OoXSCBkoQbeGeJOUU_ajjri8SjPTV16uki8mT1WCUCYjY7Donb6JFgN0B1kNrbQeElPSnkVsWIlEMAQOscZP977WE-Pa9BHM9gg8UDHLnHGHE04mPEF861D9Os_cTwf6INAouISYUbGTitcd80HGzL3UG9WFM1vvwZWmHNdgTFQBdWQdCs1od8uWSLYHnN7qzJ6L73OUFetEBF5qYhyesyDJwVxsGXOghELzL-Gs2tXyRNvDyHWNzrmzM7PZbl7u2smO3-W8ukVsw3tHxDNYWagnFxMxHwThvzMX2PCsMZrxYtLpn9W7N_nAmmCdCfgY9bsaDXjczmBt9D1ebZz9qKy60W4Y03muG5SQk6OjkKq8o4-kv8hYIqkOPQesFWDEPsam7rXVqExHQp2IQL2O_SzrcUihU6ZPDKho-fFEdw5FIHLDoSMsaF9s4SFQvH53P_oLTfyPEfDc6Aa9ioQf_Qu0W_9Y7LB2ObkYi4pzY0UUTcnnnWgWuozJUhJ1jVbeJXL6Y4NxYXbvZAp1f8PV5JgQljZZKyez-59nZ-tbeQ_iBBKN67RQoh8ZKSv9dbRMiP0pHH6jBeTJXoXfJjYb17YeUs2fP4qfZysDskThkv2mJ2zBkgp04k6mDTX2TS6MfnQsnMbYeUQklk3CbafG42_wyr_Okmdk15s6Pp0Hg8HRO1IsqInNQFIrnrH-wchlHDKr_uZQBfQKuZSnba1vrsf-IhPMdbriGPh5hGCEHczTXYoIX6C-04rUitQ0QuAWLD2OUxJ9M56crPwbrVlZgvKT3jAMxYG_dSxpUxAGeL-40extZl4dXNrKkuYsXzfM_W3hwC-zhT0GOg7aH9HLG_0tgaUBuzmHiWnPQrTS5Oj1VgBlGUMazvAiIgBTv3QeVI0mgSYW9KqrV_5wmd8lKZegJbFQ1k91vXfNPgTmCqIlyUO4yOUhbktCmtj5NpUW4LwZw7XvQUb6yjh-IsPRfww8QZGqltKLOdLpwrMcHrsxaBDV00sp8QG_4AfUm4jrvN_Iv6mO4Xbme8wLNJroZuw4B3WaHDuVyiZGuiEr-evX1OkrS7GRhPUY1BcUWUtuDGg5nMpXgDj_0THUnV95iMCNuSR8_Pwd63q1-PYE6l1V8J)
6. **House Management Service (Property Context)** - [диаграмма](https://www.planttext.com?text=bLTBSzis4BxhLw3geVPCR5vowYcog3Xk9LcMvAJjTCO3KH29X0HO0BIZTFhVim08W2_9ZIyc5zXlNz_kKk-ama9LHJxwYR8ah_O4PKgLyjSe4lZbEgKggtQL92BXJ16chXDUH6MEcTQvacgVqrqqVNkrqgB7nJmgi5H4WEXfoekICwrJQlpHVFBd_UFswTFTziFJV7OxMyONbwEHeYedQFeM-UiefZWLk41Nw0C7qsY16Kv9eOysH3pJXA2BbU0b4Ue8Qk3NLmL8-WbJHiG5BkajLkG57z-WyMHrX-gtCRp-pbaQBM_lbd_ejpMl5958a7yg8fL4YgE29e9BQqUEBnkmEWt15z1-1C60TnhuD_oCqQOKbANeXdCrTjVv2q39Z9Re6UTqZnNbh6LbJpJ6cgHKAg43Y8q0EKdJoYqVXmivJmpQUM3zBdWUr6f14tRwCxfUphPm9Cpxt27Z178YJnbG98SyW8ir56XkdQW9l-HxSlrPejdNKW1SoqA0HWSka8Ti6P89OLXG7kniQadJpEgeCe1v3uG43cY9eLCqa2TZow7geWTZJpUyOdiiZXUPrcxL_nGznvVetn62FyzfgpmfgTUgNOe_OvPoj9vjjkWza9wySF75C5DBD7UrQqOR5TxGtyprndJz62FT2YOEgGzyBjlt9xPsYIstblWU0jc1ffLm9TMkT_mlwxisMA7JwxHhKKYvJpN2R8-Svg3xEJsGv9ZatU0yUAYH5a-RZdPKxaDZ4K4FD3aJZQD0YCOdRJ2Qq00GaE2ywDXUQr56eGeYoOvQyXSOGboacD5ltlq1bclnfeH7dCCKocqL8GWvxBXjmE1sNTH1fzi5jxKWeg1IEgQOWLJtT2V__cBNrmoRcPRpb2ODMldWX_qs19uzm-EgskLKPiPx8q1UqdRV7fRsK9lKinbCWPTe9_WN8WAD4a5iOebMQdT1aa67aSP9xN71zvubZLFFlAg4g9qUHFLVOoZ4EnjHl1kQ0l6DYOHBbGgoUPZxVf1l37dWdoCWF9Q3NTZUF7NSDoPiG5lKUR02CoxmRaVLuY5a3dO5mGKaJe-pNs3Ombfcm38A-rBWn1WPhKdUsfQDCMK3abOa3NsyroQYxNQrYVIO0WCQ9-Zs5kL7oo60gLexqE4u6F3pOnZAVYiV1BGNLq0Hv1QpiTXTbGtlWyMiFVJrEBLwW6yKxPpeU1BW_S8SHC_Rgt48fhyMpoErzzu0O6_z_T-umuRhv_sErVn8_7TOZod1JdyTjEZYpYcHZKriy0xOVbQryVe3sU6rEFSwK7T0EfXGdbCOlP2wK26rBVwUmkk5rCzPfrXcLCEtx0zvEJnPtRD3hgVOZxZx6h1drklOdKo-XbnlfFEuGDaOAmnZZWk0FPCSCwGtSCrd0tAEDIxCvgZ-0BjVByuceWdY8p-BCnHq4wMEyYI6djcT5HYutirUTq5qwXyD56koU5XfC-y8syFlC_GT)

**Диаграммы кода (Code)**

Для критически важных компонентов системы:

1. **Rule Engine (Scenario Service)** - [диаграмма](https://www.planttext.com?text=hLTBRziu4BxhLx2zr47Y-jAJK2pgAbhKW1-5hUIqG41B4vjOYLH5scdHx7_VuKiMPUK1fBau_3WSVlFDS3p-8XLjrA4getUCv-MX0B9NgfRnPDBGXuyxflQ7xK52amkkWAkFkQWcTKcvFZEMgYZPTf9y6d_NqCrYFgceLD0WT9U8gXPSdwcr_qWnLG99Ff54u2tNZEuQMf4nMHyGJlcESI2ZJGwSDaoG3JH7bWFk9wjrIfBrB9ibq_b55EKbbTASSeT-HmJ_nWqYCYPp9jLdlVkNXU6853Q7hSmRjeKc9ca8MAkzm21lQSaAgWGQVGlMnkQoRaGEKfhp8-Cs9bd3TZje37OHawDWXJMcHQ59Z3IhsB0DjXken15QYraH4waQndU15Vo4_A3WEKT7oz7OJ7FDhsDvHNJg0kTR8KgWFFglAwINaBMMTirf1I62sTcMK9py65HP5LevB4fpnGJtAPcQbKjAIAkJWZvOYfpgSptyig8gtuDy8WrTSbx4qQ1iexxrdaehOoRcxkxHooAw-xsEwbSDBQaC5nOkG9TqFqHyMGNJzuGmCZp07nV3nzt6DXIDJLKOZCLUZgMgAE-1DSM73jWLi1GMjFvizwwSZQi8R4CxKFCd8dmsX_wGA_zNPyi8vACqscKcr2xtuJ07SgDO1TXBgpeccfDUlhtKMyQzdkL9Lp-KIafcihve_p-rGziWuqwpjCf9xoaJnhCCXHNSe6l8WHsXUAxNzMecDNaIBK4hNuBV7OqeNKkFoVK0wXgEKEoUMHrQy-Kv-FgcxIh7Ncb9ZcnZyZKTX2w15ztxHfXLINTjiWSwwdb1Mh_j4mklUqrF3WFnOffm5vGZdKgNWzVdso0UKhh1EutTu2Ev8hsAVAfjQ3VFFhShSbx6JpcpEMbbwZ-r5ro5Ofs5xqJhvEGjSRyvPknjBuHxqNtrm0zLz_l3HRD9bvlL-kvsEhz93P3D5kdTr-aclJRBw_HsbgHtcsoQsVtvAfbcizNIsNZFfsRk7Bk3oMgPhLTp0orNsUplcJqU-l9MwEq3MKDfuf5xLikeCvKzFex7uhTfFNtuyV5i23kt21lAW47ljHiBl-Vda1P39jPX5FLxQNZ4x4PHhvqDNzbxfbsZy-SOOMy3WfVi5H7ttVar9k-dU4jbT2IvQ80yuDnDNF7ftl4F7vFlTlm4IKm_bXgp114omwz671s4V54IQefMRnfgvCIDC1pgSGpNPkyTmwrGoCLJrEJCfs7KkZI_1dugmylFFfV43NpjJPFMVijmnk4xlMEVK2Su3Ug-GiG3nna1wyXJAU7UQDNJMUlLTnAVmhFX4fo8ydyrEWzrE6tO1zbZOdtu9XBgymM_7F2d3FaV) - ядро автоматизации и обработки бизнес-правил
2. **Device State Manager (Device Control Service)** - [диаграмма](https://www.planttext.com?text=pLXBRziu4BxxLt1xMHUDcqjFGb7KbGF4G9nauwIB1Ge4j3InYPL8bQISkit-znsIefwKa_RK046auJo_6SqCyr5fAdMPPvC_64-oCWMoqxfGqUcff8zljqploasfG2Q2Q-3wRIBoqoAZtCZCb4upjZcDtysk3UbkTN6QKwL18ka-5daXk94fZFw9PZe34hyZiK0h2qQtakPaHXQmPmcGjQOQo8fokWL9fXKrHhDIP6GDqhxFI7opl5t6yulNaqcIKQKgUIlkfNzC2FxC4fhi828ta38LcsT7bb08nRIGXyXPlQa9ZW7swFLrkScOse6Co5ddtV5a8g6Q2VwPPYobA1kHYpx9ShxPWevBAL65DJPDhR_BD29AIyQthwDs24wcB501XFbF28T7UzGHH3KR8JAWtAbG1vxyZUbpB26hUy5Inxjt7Zj5yOxoBKmJvtF7nWd1Qi5uZXhsoZmSyoG1fG9XcBBgEEJL_1T8hSzfNusZ5b8K83K35M495U-z2VVtWsFIB0Sizxo8YF7FlBg3FKY5ZaQ4SUqeIfGoWQeyrlQbc_Q-0pk1qVI8Rv8CwtyQoYzJjaYcVUmGssiNoc7wBnnQsBJrgZ3N2K7eIpYQOP-KQGZkvaFnQ4jBoPWjX9lcfG9KPmrcnlSNrROAy9bC1GgwyH--CQM7HTJ7Zt4VwF62jvt1Ox7a3oBWqJ3LNaKDMAzxr3MwmRR58T6sg1PKqmrLqD2S3m-CfvyEo_HbeD4zJ9_121DmZYYX8u5ei42bijb2cGjaUzzIyw7TTUeI-IL8khtI8v9_rTgNqQf-TbOBnzpkFKFB8ak7BM-y4pQWLDhFf1Goe1SCFLWYbM2lgUFJcAf1E-i2W-Cmfpmznz-i0KQrnbOK66NjfbSLaAWPQyBFZYJu1ac9Q3kN0h4axgGnP5_7EhtNyqjITRazAzqR2nN3yS50gu-mComeBXw0MygnOT7DvZC3HCBN4a-W3gO_2UgkD0R67Pgh1kMnYAjyl-IpKi3J5oGut4UnbJIrM0RxRc2cET8LpnWFzjzWv4vejEc4q1XKjSV3Jc7tvUHctt9BOqJEW_HX1fmlLHww5N12NXfGR-_fgmdsq33unq1f3w-m_T5UYarpMASXdCEmLM1tWFljOpxgmQ2mZ05VKp-pySquDfPUppWK3GKD0I_pzbfPcLcSVLx6P_OndiVdxcanlvr_cg_ToztwxEO-lbgjvfSBIxYuYkUtowlB-_K_bx5H_ITkQ9drLUrOeIQ1Yz7JqspszDI-4OqpzQz6uvoz-z4uut0SZV2A7vt-CkaDpqfTO5mUOsp6vN4kUpR2CunWqk_DG-Scas77ghX6EjCeV-SJc7215oF9jZjDn4Fe8XoHL-xsbDSco2DU2ic-NcUYBtpc2eAOhHZBr13yqahCDN8d1MVVBRSvSnzPIuKXUiS8jIsWTsehX5Gh5PfuLVc-4LgBt3W_Z1XzhvNst9s7R3JC8Da3I-e3Yt7z7mJSW64hXr9LyDwp31uigBqoGAVyth4p99G80kgPkYvwltiE1i8qvDLVjxV4hMf8GSS-uaWlyupy3m00) - критическое управление состоянием устройств

# Задание 3. Разработка ER-диаграммы

Здесь представлена [ERD распределённой системы](link), отражающая ключевые сущности системы, их атрибуты и тип связей между ними.

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
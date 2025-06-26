# Project_template

Это шаблон для решения проектной работы. Структура этого файла повторяет структуру заданий. Заполняйте его по мере работы над решением.

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
- **User Context** - управление пользователями, домами и доступами
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

**Диаграмма контейнеров (Containers)**

[C4 Container Diagram - Smart Home MVP](https://www.planttext.com?text=ZLVVJ-D647xFNp5DfLwG4g9AztHF5qWESYLSICorVK8RUqcsPtkjtJKGLVtVExC_RCSsS4Y0lLv_C_lDV3FZpzemPSei7VmYyZWj4mu7OmhzQJfLxFbyByoXt9MQgrZcXkVcF9RPj4XPJkzCj4bIiPjUVfoiQUb-TJFDc3PSuTB39Rx1HCxLUK7uWvlPtz_keuS_bz7rmytYQd4x7vqD1aQObCFbHwYsmromlM8PJ62ReNDmBJCEYrZg8q9dC8hav8B3wliQPYe-2CDZKogEM6kkjCn71zmldn5f3CFhS3t4cqMU09q5dWyICfQpFTUWhGrw1nB-96Aknn0hpWmzYtdEb90Q6BwQoLmOgJHkHXHGF9E6fyVXMJ3CaapaQ6Vh79tHhT16CNoBxA-2IVSSANqK-n8V2vbR2zOvNHQ5L0PX1mxeOV5YHlm5QSrP6l6ic1M2JkDNW9OueY09C5iloT9QoIUHeAbdFCW1GGdTDFObp31ub0fI6Llx6kqrp5bMve6FeGk4loS3Ina5kZwT06kUQ-He3AdO7mnnoJ9yXXUEw3qHspZRXIppXAcZiqZXgY9TsJrDWE4P_3i0_AaoPiGASOMup-o8xo05uE_8qpzalf_UNYrl_wAxhSZtc6-OoUe8XHIv6OEIfS7bCR0IkSgDS8pW0qJ1SsI27gFFPDJ--O2syQnpcQ5zs79bYO3H5vccyXatmtm-1tiqdbW_CNmQ7fNC86Bw1_n-rZg1utB3zvGlT0h7DOILO8JES2M7zGRqbQKIdS-uOHJLCOHKeMG0F27V-nGR-NUyGsV3VY_eNicqjk4NcZvS_Whdjul8yj3qHcQeguIYROWy9xACMvevIoT6PBJBuOq2yAiEeRxmRJo_DusMeh3KTETM9lpy7msBbq9nRJFq4cE1bS5clZOKBfQco1BJcARaaHErorUdXz7AAPm2-AfBfD5LurZ3Un9jlT9qwojxOh0jBE25b6RejoC_L70qvPxCy1-BCNzqJOp5VDM5K9PgKhP-fMbyVJG73FeNBCY-AinAWnbhKq6LAPcl2jmOZ1BxlNND2RLGagfmHhwFAloMJruDo_mH9MvKQIjn9OYMstXWZIHTAFd3rkQLkmUtG0vks6udpEhEEgkFUNnGCiTYQ_Egp2jXyYSY6WrobdMaESTGxfWcFOPBmF8Kl88rCwGFR2-rNDjIRJ4-txK4Ej_rI7H-OMcNskmLtzxTlArI1mZcM50WJZJh6eDknxtomikcwKHGalDXmnEX6-Qj6AaMUtcYF_61zqcIVEknM-dGsgmb60YsBI5_JCiNvq24c1FCOO5HQ4WEtD6zvWw2T7diCKSQivRQskgZk5TG6F1CQ4sDRNpQQFje3JAmDgjSRnxkr6HNIBLwq5WYZ3Fi-ddG61h-Rp3u0FPGIqmqnMAxSR3XQNDgEMbdzzgsyTqqapk1hOiLHS-0GbvTHz5wEzqijX5Q8aW_Zvp0dSuWdTVGlKR_12CRtSttpeNjdMlgdTvC0xzJ7MTrJqMCqiKgp5usPi674yyRa9tMw2uHo1PmBoqTnf6VWwjxdB_IzVI7wPdeIYlluVMqg0rd250zSA9w1wPRqqEaM9ridN0tmbR5oSXc2MnLDbXgcLemvqARt9uojwNc6UW9TCDlTSaAvOhstjrChxvjLiCcUiLrjtfTKXsgi5sPgWeOu1HhP2nJZzPb-hGiUKURBRvPb-06rTx6REThLNayc4rOcsTLhj8Plwvd7g0JjzDQub1Ogru46FoTfSX0TdGnh4RwPJsrOGmZR8Fb_W2jXkCTE9dKUx9nQzEvCUVOJv6xA9fUojcwFrBjyR-Z2Fi-PU2mFu-wdojVkB6n3byarHS8pvM83qH_JrOlgDDFvahWVypXnXH0qwDodu5satujITfZnxhSfK8VMft9ZXJsQBFLtIkil8xKJE2te6g2tiLIRgixQoza9_NQcDjofsCbTgue-1RLnHfySDre4YQa5i-dNpKpBEOJf5wLXNrymuvSlI7tHk7-pbAHs3xWpyXVg93dRbvjLySMMgUpr6YspJOwVXzQDo_UToyCQtru7VBRVINo4q4GFzPW3-kx_a-XljfTcjyjf2AgAnXQbfOsQZy5_WQbOI_OaKGVHD4HvsSi2cMMmly0)

Диаграмма показывает микросервисную архитектуру с фокусом на ключевые бизнес-функции: **Device Registry & Control**, **Telemetry**, **User Management** и **Scenario Service** как главную ценность автоматизации умного дома. External integrations встроены в Device Registry для упрощения MVP.

**Диаграммы компонентов (Components)**

1. **Device Registry Service (Device Context)** - [диаграмма](https://www.planttext.com?text=ZLPDS-8u4BtpAxJiOPEg95pcjASXm32f3Ha2P6UcQgieOJT66rdoId88ixN_VLiVjeKnr72Wx9RqrFtwwSaVjQ7Ab3dl_S94miiKoCwOGly-62YwlykOsPMRKeDAf30Wp5qYyq71gR1hRhL9ETiCHXzkvpRqCdiSv5GRK1XQZsHUI67N51Q_zpZy_kLbjVxwiFgyVfnC9q_Zg-jUpp33WOm-a7ew6JEQAPgJMpA6DvO0MK36j547iWJb0bTXO6IJUZS8Ovye4w2kQC6crC2U7cv8VpX_8E6jZwz_I94DdgOFJz_isqAM1ZHHy4y9scXY9CbPegJsk-Z-TGIRkXtjkv8S5qSPOA3ApCAEVYLtJvCL3Ydt7cOYSJaLgIRuHv1r0mJVQGOvrdoqbG6EGQmN5w-gvtY79vd2tT-QJDuB1LhtZ-Qbr53SZ7D839FY25ad8AXYCc3XkcM8nF3pWzb9GJvX9v4_6ncMHkRKmf4AmhBJMnwmsVbwycwki0sW1EKho8jXmN3H94I83O6YfbH00k1Ne6O7od5Xec6RV3zkv_fUbYAbwX0QK4cXuRKjZlur-RT7y5SBAYprMOLL8OyfmpfHQx2NwjLfOh9SsK6obOgaeNEO59TPrAw_HArDvRTtv4HXx0LrdHWjNiPHt1mAw0RIG5MoSuni6KVgcCYG2bj9gvoGpyoXg4YB8SymuEgJJf6eH8Ad60dLck0KbdJKrvLzPwQkXXIsJ31RjOw6tYXdgHUAAZcueIejgJ8gs0yQLDbHMAMX1ppLcQSqrb4KRcgBWcIFRaKg4Dj5jtzBVYJXBDHJgT2L2aAnPDSgTSjKlgUui2nmagzdZeOfeCsUxYx9J_hJqoAZPjMXffHJ3gqOtb09MvQq1OPsKc1impWp1vBi87drsIbf91x_2CQ6bmKaDSm9vY8ISRJmJ9VUy7zURZZJEoU_YGsGEj8Kv0SAFu1oTWwB4aH9aOsIhw3E72JYzZXIZsz0D50T6tJIKYC1L6JnSAoBCCSFOb7_HGOpthISPRpfy9RnlJD6gKscOFdy6FkwJ_2cFZGtxe3ecqQ9U7TkMNPydmGUxXqDY3OBlFY0ioAwsJ0pUswOn5I0vjuNUh-H-c0mh5LHP_Uwjm1-T0V6vkUTGRUaQpVul5hDbmFhVeXj8LhtNOmIfDlWm3ko1CBV7XOf016755rd5r4g_beW3kFauegHfc1g42RGsFBQ8s8SPCoA8dIa-jWuuIv2xZ1Q3fdB-TXDlUGzrfRwJuSMrgaHD0HKNo3dODfvNC8AF6Wyxp-DTyPtdI4sa5LlVWgnxH7Liwvlqt0QBp8NbNW1CJBCyw3O_R6rUOrecEC5Ne-CO8dJEmyCci1W-cKnEvTl1zO2QAfFhiWMMFjSXtvuRNOswlJBwnCOrxXzTBzrd8QsFVijMZotVQgXlJhyDUN3sVFS8dy4aU8NF_aV)
2. **Device Control Service (Control Context)** - [диаграмма](https://www.planttext.com?text=bLPBR-Cs4BxxLx3keGwmYIzxsjFwXSHjd3gMqmTGmA2fYKLK8bMIoiOj-jyxG-ffoTXkWC3c3EURzqT_jeuRLsJfw3kfH5h4m1BdSljfEZNyozr9kgGu5XQCqCg1SdT2PzCyvOfiRgsBKtcSBZxURadqidcSPjmwC2WwB7IMQqKsEU6F7cU___ooF_owtZyS7bVtgwVbv6OqSjAbm1OVMNsTBIK_6PwnMxQ4DobGYxwDJba4nfydbO0-uTqX37tZKe6Py5pUSmTV-Fa36y-sQrQUndZyIQlJzEb-_VGRdNQwS62PWRyAiCuofracXT4skB7ZcnPix4FPmKbQPmYwZAsILC4HyBqUDnS4TppL9yPLp39mFCPp1vcgk-6Ado33xD7m1GMiaRIHV-Hld4MvaUh4vbgxSNMT5ox1knAzIQsyinnC9gt58sKoYixOcEomUdSJcs7FGtWM0I8wbhMsXBZMUnRojPzOAa-9msidB2D-29w1uVZ5WhBQUEWwbyDS5ohcvboMg-nJKuoBJevls3yZXdzryoSYt9Zb4gsgwzX6YciMRU-ULdkyRNmrTgjeJphsgaqPC2lj_r3rzE34PbGFK76kfNAsfSFfTy2m_qQ2b--0fxTEPZKGGmimhrm0gUVSYOHfBAslCsLFuVToy1uVy3y5u_C88PGIYdtn_RYbo8t6ObjgAntVU2fZxu6SLfU6fHXyWNPjaG4QI1mwARfYMwIkvMiuUb-JC7wcwLpaArMAcmGwTSpgcIMtmSAUbKYCLlBlElAW45maPTn1aE0UQNCE4QlNL0f78Ukq8CFXM0ssYxfL3ageTjF3Hjn4t6ZZPZuALMuEUTyyx_TzwTPefmKE16vmZYDXoq0L2BzotUGtM64yCeCSSxNIvOYjtl2mBOwfj8bFm0jOBMbY3ueyA6XOYB9AJqUZ_mJJcssHS8LQ84lRdWPu1-4hsz65cN_bCYrCswhUfJhVeCF4_cqHmFBOtVpbiR_xotbeGopjvGGHluJmh9yF-C1CqwvBuYmpd_l44QcgUH1u9_nub6xptDGEgHfu5Zeo-e4X6u9HlDnoxZEpensadMTaY9DinP24_R3VRwCfqH72adNRelTo_1Bs62e8R0MgG0MA9wnhKBsdee5gi_q52kQvLcMUedvbUnct7VKOR1j80nrL3GfJsT2Z7OZtWah2jpfjhxqEqutW0keb3sb_4yOWMqJWf-qhMNNT3mCXqtrZGDS8eFfkcyShRDl_Ykxhg84QhcFYn2nnsnWHCc8E-EXl-OBsjTjRMozwpPvN2j67Yz1rAtzwvM0GZc8T87BlPgXFbpz-Ukqd-f_I_bJ5xHXSUN5oRLoeQz438Pwv8ExsfbpswPBMsiCG6BHft6ppx3rynYpmHpRx3m00)  
3. **Telemetry Service (Monitoring Context)** - [диаграмма](https://www.planttext.com?text=ZLPBa-8s4BxpAnGkOQgMuRAdd9O1mb212GpUFAfIHGcxrrOYIrv9dXsIodzFj-GtGtOvsUtkJz_NB_71EcvScSlHTqB5iao0PSuLzeVPpF0lzwbmMNakBPXOAmVAtSSwdnMIAugPMfT8SPujta_tPFgusyvoRXqOD9qMEY-qefY2y4VR-Uy_VunElswYnzDsjLuzBITtev4JJW9Rl6UDEriAdXgUiocBG48EpbpO4SoBY85DTbe9furGAKOWfLU78FJ4XG8puOLOSmTV-EKT6y_t6rQzZV7r9wtIsTDwy_GRlJthqe5b1ZwNO9rbJhDSn4RRS98TttLW4o3RCwJ28XkCNde3goqrFm9Uwt7hWEnvmXrdBrmAV11QzO1j38eReQju33nMbYxa_k8ohTYFc5lKvDa3JoopeAmsp1_m2HzCAJtnqV62LSXFgrStiJaMEB2n14-lh7edf8sEAXpBk4gGCRUbpsy2TI9QmgS7NQg4cyl4rRLfgL-LQtp7_Xan_3N5RSFcXUY5LC9MhuK1Q-__zEIULyU8ldXrJQWN_8UQiYK9n_gX2LHIQA6S9JjcHqwTo26a1Yi4FFTV7h5Ss3qnb-4JZr6X_p9NN5wSY6qBXSg9_401nIsqb10Z4bMQS1e3iNxI2LG2ZfqAnS47Mu3EsnsYADWnrzY1bNdPRnOoxOrs6eDPXlMHwDtuAMroTFsxxgits58MiAK5M8GXhPvpXDPfCBF6Fg3UzV4yY-3O9S3Jr43AU-HnG_WILhQvqiWHQmQk5UyT3T16GGihj1InW1i9vtLbLYh57gJ1RceLJ0Fc3zoAc3LXlgs5iYBDGdiS7JB6Z5CNU2yhl3qoMFe-qRa479SKiFNxAZ-Nc7XteG77Thh1lEdOrGk-xikp53O3Ae0tiCOo412-5k4hZI8j9qmfJu6TZVuBZEyWFwbu0Fe2HGGH6IO_qp9XdBZRRWrQhpQ3TM9GmR-TGL-UM_RBSszIIV-PMv_rZVeaozVbmx1XyWutQfvtzQxA_01UUakmalRWbI7Ghf8Q37uFyFDPkDsXJKwOQSmDhQVl6MuwCAW9vU03z_MpesUGlHjXi7mEPI3RNonqncCKxOypsZu8JoXNUte0TRMFQvGkI3_cwaxv9KmzZLl_yXXYeTwDglJ6pRLvfRn_tDkBe4v1DTkGDEIhTcIvJgfRvHhpUZBNu6nhleqpMA9layFbYKd5_o5V8_LMz3Mt0FUjIeSCRuvo_Tnl-Z1gj-6lw7tB0UrA-pzuR8zbkFQqmJ6wdQB-W1_HdOPXiFQxKywsIBMvZxwgnCFM7PRrRzuUxC5VuLkkqX89LnB2361_vwN2CEhhC8gzFpJ3spWKUJ28TDVE5dg-hvR5OBtsEtkO_-5sQilHxess7FFTmP_o0NdWtrxs7m00)
4. **User Management Service (User Context)** - [диаграмма](https://www.planttext.com?text=ZLPDSzem4BtpArHwQ38JuajEFOK09QI4q9WqxKnd664BMnDRSYKvX7RwtxihVnkJ91VGhlRjswVT9PVQK6NIEEfzuCABKf-Hq9X4VtOSHNVd0JTXkaarKvuKXWbpxidOII8gC6QWZH_nZJEw62pHz70xTsAg3LDWMezad4Y1CGdYz-R3dtSFg_NZR7Mzda-cayNuvBJNCzn4Z8mkI7cTZ3aD58t9W3n0Od9B1GrOZ1wNgMVkCN9Y7IEaz6801BzHBfWweGcVKiDsT7z6-iFbZEIdFXo_IX4uY-biyGDFzp8rJ1F5VgTC6qsC937tbDHP3jq_hS7w36ruLZA2uB4zaznGy4BOqKTolfYimANi-JkDk4yn4wf84gPYhZMNefb0UqnGnMME185kRgc3B_Scb89y0OsXjYPyWK3a3W1ro9D-NPdrbKo5JzN-17bKcWB64PNxf-HlZy1V-J8sT9Zm8YRdu2QAYu1SIMcii1DtXIwobIehEYwnVudyKHKBk3QA6b32YMJ01RgMIcuvj4Cpu5h6Z53X4oLbtF8jIprh3YWSURV8qzI4MPKAUmDEOEMUfP2Nhv3_3Nsc_SCRI00XfE9_h0NJtpoke76Uc6ZJebhlfF894vxQ9yLrbxMfelKECn4EpHnaWdJpJp9ngXAia8La7GLOVz8fASYcfQ0HP8LNYijwbefjcGBIq53DY8KqV5jeePanyEGQ7KFFWyuoRyeVmYjMtEsRLZrNf_tQSrjVlNVGU8_tO3zjUP1MLdjLXnHx4yzpcPD_YsWreXNTMfjrTOe7UccaOBS1Sg_DUKOnOeCDrGnwg74PdVaoQOLaCcINMwvoDJItoR_Qk8ytTbZ7cuxv7b_PNIAr2HHplysBNj5d11y9Fb1v-AZ1dpK5ndIzJsJo0hi-QoFuGZthwjspgB67gwsHaSXMeROwsOJsrLUhfUlWxW1eH6Yjt0habOsQuzHX3XPh5r37xcn1GR4paHVhbRzK1sMNu4UNJL5zqJyPnfPclpWDZ8E1B-JB7U_1Q4vT0L2g_XQ3Zb6eL6iftutL4A8BRnGoxqaNdQvu49fNu1hrTC6vp6JrlOzUM-Cko2b2TlNQ8IIqoPWQIhIH2g8F5IY7qeKRe24w2SXfzmZCedEp71wXrWvDQ-jT7uOtb6h7Me6Ef3rKe1tTag6AxrqouSC_U-G_)
5. **Scenario Service (Scenario Context - CORE MVP)** - [диаграмма](https://www.planttext.com?text=ZLPDR-Cs4BtxLx3keGaGnvSzzRIEhMRJsbx7ThOjK22W94PcGv4gITan5ltldI7rHKi96Y20EUGyphoPUTHdOwcsPIv6Ft2PY39bv61jONwUJZKztMRS7iguD4mdIbecxMsYycaXg4IVYR6fuF5q_ccoGTFJQZdDgR5CW-bvhl92IVGf47-qdFtvzMd__Fl3_ilpChgFrekhwz78SYiOcNyYpN6ou3JJD2SJiakOf9ehicFwo1D6hXhB7EDvit1e_dKRaTMt3O2XaNB9z1Kj-3srx4JFDsGyspoGQZM6vMzAPjFr_SFw3rnjLMcP8PhzKp9Z3R6Av3pHolWhpVYw0viojE5QAm7E2xScbQ4E4s7dFvBRTRI7BUtMqHjBt4KUWW2FEPLf26-PO3cp-bm1WT--DdN1rofbjtyR4hqLcXc3ferMHvu2lc7IA4rIQcc0ZKLSKKapW9CMF9x0G5fB5_zNUgHaLsWkCtAdb4NRDoeuWE8DTGtK2O3DWHVZBlVFTwgKATNdg_fWcyrbGSVNvFk8m5zJ_SPfLl2k0zGGe_W5sglwkOrsUpIJ5qYtYIbl4lfBTXfeldrQ45KmJIrNqk3UNlCi0mf2ZrbfLUuE4LqAPW8Kveg8UuclE2IEqL-aW8wHp80C31CMnAzSdSxseAJh4EWCGiFhIA4Lz9m1ul5Yu3JbRfCTgIZTERGxp3OOeJAU-FDv8TXRoyP9wTSNeKwcyOAYAitjkPFsS1BMq-CRH6CdLdnLbZ2PUXDQ6hcfeWwfZOxmOn9Jmr9IeVj2y9mDc7U-aUq1MloWHBXPPzhUvKWOJeKcUC9FeC9KPiv0QakOIbDjxzRoZFSkWkaDzzRAyXUU-6fwPnVhgXIMWrQoeckjOe0c4Z5DNeSZPqZPhenDeddigk189AqfZDtl6hzR-40WpB2hgXn0-Irq4NhuCZHAqzgSHjGyDDRT6PB9AmC4_czd-XTnCy6BkClc0WG9Y-m2LiPcckqUbxYw7BcRfj8tRNtXDofOqzgXSEQG7NJQdLQlZgMLNnDlS6f1uvZRrME79QiPpO4JLA-Ve1dWtP9K40uFYwuADTeo4RmbeJhvKJ3TAMxZW_ql-_rcDqMXWZiGAd3lAVA74bl1rMWGyOEi8awQfxCNUt1Xe4i3iJlssuRoQ2_KFu8zj9v0r8XrzJv2weRI0ugys05KM_x_2zQRzURXhMZv2EWoae2mIkAC5-fMa3y2xCrlBzUD7ysMjdgita65ki_XuyHCgk-JeEJkARoKBT_35xHSViWqoLMV9nsokbqt13NmqR9bD8NYbLgxGUjyaVIx67K1Pb5f8AHNpL1AuD0x3GjACeN_cKWEB3-_KyqXi6wl3e9SPXk0B5LcUdmFGg70EDLffTZ1NmXNN--tB67yEDWUizNZ1g4_Cvd25pRv3m00)

**Диаграммы кода (Code)**

Для критически важных компонентов системы:

1. **Rule Engine (Scenario Service)** - [диаграмма](https://www.planttext.com?text=dLRTRjis5BxNKt2zcOjKzKslXAAefsY00Vzijjgh0W4jCJO1ITH4ojlG36XpiPiM2xO7Q87j1P8iNhrbSL-1UgCTaf8YAUu6pBwG-9s_xpk7fFsK2vo9F8wipsWIH7b8q4g8b3lzVeQ_VRYaOfKlSawoW2M29E9XmE9-6k54nTXSX15TzDr7zbSAUZOUzMFC1Sa0Ed9PdB94nQGgloMeY0Xo7o6NGPL3YfSPZf6DPZd0NhAa2K6zUK0Id566vYHRqu20tPtEFEJEXlxG7OpkMrOGOSvrL1dqqaBmiJD0k8D6b8l7olh4m6GD5ERvWWSPNP3CGLuRq5u7QSO2mhcszNI8W_oCBfSaqzXz1wqP3Oqp3aDLeASgEff9ovoHcAr9xJ4C7SH5HfDboujyHu9Sa4ya-g6fir98wqncdU2Oj15etY8YuBPWB28uCQWmGcf51iM1e2ofEZRGgx9dQk33Th6368jWHVWdcjEiLqdhjVIkSKH3RD3AkrPPPgk4YkzJKfVmOM7WaAWjqI5cmyuCgIBVXg5UJlwFX9gKQKcRay7-cr8SuwG3fXZsEu730IqVu_IniJqeVPxiRrstGvfiHKdhy3MlPg7zdExsmnOq9d3-uzH1QZ9gkNV-DOdEqQbunDy8uKPKDtLSlz-UIdDEdSQPDM9vbSbdEZDlxnYMQ7H60aBN9FotOzcPcQUxo6g2fYCkN64PYygRnK67Uz3oO351Zsc0LQxQVN8NRCtCf3C4UkPmLVkb9OkJ96pcwi4GE5xMitc0skrlQ2H97ZUFHQblxatcqzdHyy7ecQS1VpZsZhuOpBr3lJpqdWzTxsZk3tolJdExayiifPSxdVYpwKX3awa__1AkNNyudQZ0pz6CH9eqNz6KMurB-EJ4jjbBFU8kV79ovywzwz4UdAKzAbjrRTKO53B-bjNTakqGRRKic2T1WAm4OiVDdmu7tPFlYrToLxmkJkLERb1n9gyKWEIvlDJOt_8Sdblv0Pu_mdFhl4Xi9D_92_0xbnVANBoHb_2-AKvHyHfYBeetAgOuqww_WFaZeDVo1j8h9_bHnrv2kcjvhfr-XlK7S88ynHdIwQx1V6Mg5cVtIXaB9WIBAnrQmMseKj3AlJDaVmLeAt-7xwPuXGuGfDt8FxNeovhcRICq0mW6_wtygoJuNkUuWTHNODl8F-1TknTlaP5NlAqLd8B3TV5JHJeYnxhrtMdf_aF85IJOGkUW5b8bEbtK8iPV-_w-xlu61JUGGKrAyRfHbkQ_YdtYTlNqTW11MM3w50uc_5D1_m00) - ядро автоматизации и обработки бизнес-правил
2. **Device State Manager (Device Control Service)** - [диаграмма](https://www.planttext.com?text=ZLRTRjis5BxNKt2zcOl6zKslXAAeAmTBW7ZDRBUz6L3G4cCJaqYDfDm5pO3-OEkANWGRThMB5SDU80aQD4iQznMaDzeXALco8hifq4Gy_CxFT_wO-r9XeP8eTBwYp0-JWA2fKh5qEns1dzsUK3LDneaamkTC4QPk-ppgn25cMgSjLH3ISSUxqzxLeaVzdKw4fI82H4yz7iMSQPrOstSKLI51tXtaSV3IetWYS8JQg4Tcr2TegB0Yg8yPdX21MhdK0xU2XsX8X3ctaJVO7crxtPsRZkE7MCfStwWNsiyT1F_QFlQdn4K34b3fwMyh5YJcaYekzbthTR0GM02PGTIxoJYaSag4YpQNpWPpQqAKbmW15yP4Ap1HR0SkaafGDhdfLWEpEaaSm7SpVWCnyinSBIc2cJ7d8S7Ccf3xp7y2HR6G9gypJWEBdU6GBXnwKymcfENRc9TyR23e0U2pol7F3PakKbmFm4fZmMCY52NIXOh6TmkAzlSz2r8q8j1zKUmY7QK-seiP4P9ovYBAb9L8dWYVvDKQceEbw8VGOQscN5FvMDDlrHC8F7TjTFkj7yX-9N4L0hTaCse3GKuJKi_s8a5bfnNv4KOIKfFpGNd8Kw32agKExLfj91jmEhSDlLCs0_c9IYMRx2-bXxAYQrRsre9jRNOMBJ66eMN4LwQ6FQpm64jIogoZFSg21_lRmVKOunbfVO4WP6yBA48W3ScPGiz8ashGsG7OtOgPUoMbvKal64K_AbNKiB_ujcvZ2wxEs5NtF0okZlRgYIz9vTOtXU2YmIxHyfNrWGKSOHPimV-m92ChYz9jM9xLkSwBnXV0XQ1eAk8dmDiwQWZGjpUbkZdMcwvekSBE5xLg6oa7hDz9E6_8fJs5uGaYy5wjsrFwzQiRMxMx5bHQwwBDStYTzfA41TT8R_CmmqYKzKqQXxzXRrhHGnPIrhW4AhG8IwBgKiuPzJOVRtkRvjFhUblsgzSTTHzqX_RmQBWvUEezxFUxt_QqeQzXDuPO5qXEQIoTXZVzuA3TFZYeFkQhGVLNVJMozhIl0FBdIuboQfELssgOfNN0SfRMeyoTuzIRvwfJns4SNcP19rE5-5xJ7qOkkf7-ctxENgH7wKbwaPwcb-bPUegob-ayUvczWf-7LfH-ShzdRPJ-bPvclwLde347ZQCSdvxOGXZ8Fo2wp7un47tzCJtAVWMLGp2Bqj_JFmtg3r2-WDiFYri3_MHYi77yPu3lizVfiO6ULmo1xUFi7KG8i1ivqp5NYaUQQkNXrHJVwrWi7ntiUHC7G-yReZf3eX1SV0RG2-imFTDtez4EWcWlTAIGBMDidhq1EzhmkGLPLs1LAqEy46dsEdiBHmqwAHrcXqNK8TapvQbLNSVzDt3KfJa6Hw-CEVWnXnnf4n_SLNciO2maUmTqvW2xB2lyneHucdv4_Uz68uC3Tt0xNzi99kxxi8NWRtZqFm00) - критическое управление состоянием устройств

# Задание 3. Разработка ER-диаграммы

Добавьте сюда ER-диаграмму. Она должна отражать ключевые сущности системы, их атрибуты и тип связей между ними.

# Задание 4. Создание и документирование API

### 1. Тип API

Укажите, какой тип API вы будете использовать для взаимодействия микросервисов. Объясните своё решение.

### 2. Документация API

Здесь приложите ссылки на документацию API для микросервисов, которые вы спроектировали в первой части проектной работы. Для документирования используйте Swagger/OpenAPI или AsyncAPI.

# Задание 5. Работа с docker и docker-compose

Перейдите в apps.

Там находится приложение-монолит для работы с датчиками температуры. В README.md описано как запустить решение.

Вам нужно:

1) сделать простое приложение temperature-api на любом удобном для вас языке программирования, которое при запросе /temperature?location= будет отдавать рандомное значение температуры.

Locations - название комнаты, sensorId - идентификатор названия комнаты

```
	// If no location is provided, use a default based on sensor ID
	if location == "" {
		switch sensorID {
		case "1":
			location = "Living Room"
		case "2":
			location = "Bedroom"
		case "3":
			location = "Kitchen"
		default:
			location = "Unknown"
		}
	}

	// If no sensor ID is provided, generate one based on location
	if sensorID == "" {
		switch location {
		case "Living Room":
			sensorID = "1"
		case "Bedroom":
			sensorID = "2"
		case "Kitchen":
			sensorID = "3"
		default:
			sensorID = "0"
		}
	}
```

2) Приложение следует упаковать в Docker и добавить в docker-compose. Порт по умолчанию должен быть 8081

3) Кроме того для smart_home приложения требуется база данных - добавьте в docker-compose файл настройки для запуска postgres с указанием скрипта инициализации ./smart_home/init.sql

Для проверки можно использовать Postman коллекцию smarthome-api.postman_collection.json и вызвать:

- Create Sensor
- Get All Sensors

Должно при каждом вызове отображаться разное значение температуры

Ревьюер будет проверять точно так же.


# **Задание 6. Разработка MVP**

Необходимо создать новые микросервисы и обеспечить их интеграции с существующим монолитом для плавного перехода к микросервисной архитектуре. 

### **Что нужно сделать**

1. Создайте новые микросервисы для управления телеметрией и устройствами (с простейшей логикой), которые будут интегрированы с существующим монолитным приложением. Каждый микросервис на своем ООП языке.
2. Обеспечьте взаимодействие между микросервисами и монолитом (при желании с помощью брокера сообщений), чтобы постепенно перенести функциональность из монолита в микросервисы. 

В результате у вас должны быть созданы Dockerfiles и docker-compose для запуска микросервисов. 
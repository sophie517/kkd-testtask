# Доменная модель и правила

Обновлено: 2026-11-08.
Поля из задания перечислены ниже; конкретные типы, ограничения и семантика
неописанных случаев — решения проекта, а не цитаты из задания.

## Текущие данные

Идентификаторы — строки до 100 символов, без преобразования чисел в API.
Демо использует HQ, UNIT-A, GOAL-001, RUN-001; новые ID при необходимости задаёт приложение.

| Таблица | Поля |
|---|---|
| unit | id PK, name NOT NULL, parent_unit_id nullable FK → unit.id |
| goal | id PK, owner_id nullable, status NOT NULL, deadline NOT NULL, unit_id NOT NULL FK → unit.id, parent_goal_id nullable FK → goal.id |
| key_result | id PK, goal_id NOT NULL FK → goal.id, target_value nullable, actual_value nullable, weight NOT NULL, actual_update_date nullable |
| kkd_rule | code PK, description NOT NULL, severity NOT NULL |

Статус цели: ACTIVE или ARCHIVED, CHECK в БД.
owner_id — внешний идентификатор; таблица сотрудников не нужна.
NULL или пустая/пробельная строка owner_id считается отсутствием ответственного.
Плановое и фактическое значения — BigDecimal / NUMERIC(19,2); отсутствие допустимо,
поскольку KR_NO_TARGET и KR_STALE_PROGRESS не выбраны.
Вес — BigDecimal / NUMERIC(10,2), обычные проценты: 50.00 означает 50%.
Скрытого умножения на 100 нет. Вес неотрицательный; сумма может быть неверной,
иначе правило KR_WEIGHTS_SUM не сможет находить некачественные данные.

В дереве один корень, циклы и ссылка подразделения на себя недопустимы.
FK проверяет существование родителя, а проверка дерева при подготовке данных —
один корень и отсутствие циклов. Имена подразделений не обязаны быть уникальными.
parent_goal_id не равен id самой цели. Несоответствие уровня родительской цели
допускается в исходных данных: его должен находить CASCADE_NO_PARENT.

## Результаты и минимальный снимок

| Таблица | Поля |
|---|---|
| run | id PK, status NOT NULL, snapshot_date NOT NULL, previous_run_id nullable FK → run.id |
| run_unit | run_id, unit_id, name NOT NULL, parent_unit_id nullable; PK (run_id, unit_id) |
| run_goal | run_id, goal_id, unit_id NOT NULL; PK (run_id, goal_id) |
| issue | id PK, run_id NOT NULL, rule_code NOT NULL, goal_id NOT NULL, severity NOT NULL |

run_unit.run_id и run_goal.run_id ссылаются FK на run.id.
run_unit сохраняет все подразделения, их названия и иерархию в прогоне.
Родитель связывается составным FK (run_id, parent_unit_id) с run_unit.
run_goal сохраняет ВСЕ активные цели, включая корректные, и их подразделения.
FK (run_id, unit_id) → run_unit. Полные копии owner, КР и сроков для чтения API не нужны.

issue ссылается FK на kkd_rule.code и составным FK (run_id, goal_id) на run_goal.
UNIQUE (run_id, rule_code, goal_id) запрещает дубли.
Исторические goal_id/unit_id не связаны FK с текущими goal/unit:
изменение или удаление текущих данных не меняет снимок.
severity копируется в issue при проверке; допустимы critical, major, minor (CHECK).
В kkd_rule severity ограничена тем же набором. Метаданные четырёх правил фиксированы в v1.
Дата проблемы берётся из run, не дублируется в issue.

run.snapshot_date — LocalDate / DATE. ID, а не дата, идентифицирует прогон.
Сравниваем с явно сохранённым previous_run_id, не вычисляем предыдущий по дате.
Ссылка на себя или цикл прогонов запрещены проверкой сохранения.
Решение проекта: сохраняем и отдаём только полностью завершённые прогоны,
status = COMPLETED. Частичные/ошибочные результаты не публикуются; асинхронные
статусы RUNNING/FAILED и их обработка не требуются.
Все данные одного прогона сохраняются атомарно.

## Четыре выбранных правила

| code | severity | Условие нарушения для активной цели |
|---|---|---|
| GOAL_NO_OWNER | critical | owner_id отсутствует |
| GOAL_NO_KR | critical | Нет ни одного КР |
| KR_WEIGHTS_SUM | major | Есть КР, сумма weight не равна 100.00 |
| CASCADE_NO_PARENT | major | У цели некорневого подразделения нет корректной родительской цели |

Сумма сравнивается через BigDecimal.compareTo, без epsilon и округления перед проверкой.
99.99 и 100.01 — нарушения, 100 и 100.00 равны.
При отсутствии КР создаётся GOAL_NO_KR, но не KR_WEIGHTS_SUM.
Это подавляет только дублирование этих двух правил; другие нарушения возможны.

Решение проекта для CASCADE_NO_PARENT: родительская цель должна существовать,
быть ACTIVE и принадлежать непосредственному родительскому подразделению.
Цели корневого подразделения освобождены от проверки.
Данные для проверки включают также архивные родительские цели, чтобы отличить
невалидную связь от отсутствия данных. Архивные цели сами не проверяются.

KR_NO_TARGET и KR_STALE_PROGRESS не реализуем. Новые таблицы и правила сверх
этого перечня не нужны для выполнения задания.

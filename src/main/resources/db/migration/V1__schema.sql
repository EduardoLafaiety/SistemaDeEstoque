-- Usuários
create table app_user (
    id        bigserial primary key,
    username  varchar(50)  not null unique,
    password  varchar(100) not null,
    full_name varchar(120) not null,
    role      varchar(20)  not null,
    active    boolean      not null default true
);

-- Cadastros
create table category (
    id   bigserial primary key,
    name varchar(100) not null unique
);

create table partner (
    id       bigserial primary key,
    name     varchar(150) not null,
    email    varchar(150),
    phone    varchar(40),
    document varchar(30),
    supplier boolean not null default false,
    customer boolean not null default false,
    active   boolean not null default true
);

create table location (
    id             bigserial primary key,
    name           varchar(100) not null,
    full_name      varchar(500) not null unique,
    parent_id      bigint references location (id),
    location_usage varchar(20)  not null,
    active         boolean      not null default true
);

create table warehouse (
    id                bigserial primary key,
    name              varchar(100) not null,
    code              varchar(10)  not null unique,
    view_location_id  bigint not null references location (id),
    stock_location_id bigint not null references location (id)
);

create table product (
    id             bigserial primary key,
    sku            varchar(50) unique,
    name           varchar(200)   not null,
    category_id    bigint references category (id),
    uom            varchar(20)    not null default 'Un',
    cost           numeric(14, 2) not null default 0,
    sale_price     numeric(14, 2) not null default 0,
    route          varchar(20)    not null default 'BUY',
    supplier_id    bigint references partner (id),
    lead_time_days integer        not null default 0,
    barcode        varchar(60),
    description    text,
    active         boolean        not null default true,
    created_at     timestamp      not null default now()
);

-- Estoque
create table stock_quant (
    id          bigserial primary key,
    product_id  bigint not null references product (id),
    location_id bigint not null references location (id),
    quantity    numeric(16, 3) not null default 0,
    unique (product_id, location_id)
);

create table purchase_order (
    id               bigserial primary key,
    reference        varchar(30) not null unique,
    partner_id       bigint      not null references partner (id),
    state            varchar(20) not null,
    order_date       date        not null,
    expected_date    date,
    dest_location_id bigint      not null references location (id),
    origin           varchar(120),
    notes            text,
    created_at       timestamp   not null default now()
);

create table purchase_order_line (
    id         bigserial primary key,
    order_id   bigint not null references purchase_order (id) on delete cascade,
    product_id bigint not null references product (id),
    quantity   numeric(16, 3) not null,
    unit_price numeric(14, 2) not null default 0
);

create table manufacturing_order (
    id             bigserial primary key,
    reference      varchar(30)    not null unique,
    product_id     bigint         not null references product (id),
    quantity       numeric(16, 3) not null,
    location_id    bigint         not null references location (id),
    state          varchar(20)    not null,
    scheduled_date date           not null,
    done_date      timestamp,
    origin         varchar(120),
    created_at     timestamp      not null default now()
);

create table picking (
    id                 bigserial primary key,
    reference          varchar(30) not null unique,
    picking_type       varchar(20) not null,
    state              varchar(20) not null,
    partner_id         bigint references partner (id),
    source_location_id bigint      not null references location (id),
    dest_location_id   bigint      not null references location (id),
    scheduled_date     timestamp   not null,
    done_date          timestamp,
    origin             varchar(120),
    notes              text,
    purchase_order_id  bigint references purchase_order (id),
    created_at         timestamp   not null default now(),
    created_by         varchar(50)
);

create table stock_move (
    id                     bigserial primary key,
    picking_id             bigint references picking (id) on delete cascade,
    manufacturing_order_id bigint references manufacturing_order (id),
    product_id             bigint         not null references product (id),
    source_location_id     bigint         not null references location (id),
    dest_location_id       bigint         not null references location (id),
    quantity               numeric(16, 3) not null,
    state                  varchar(20)    not null,
    reference              varchar(60),
    move_date              timestamp      not null
);

create index idx_stock_move_product_state on stock_move (product_id, state);
create index idx_stock_move_date on stock_move (move_date);

create table reorder_rule (
    id            bigserial primary key,
    product_id    bigint         not null references product (id),
    location_id   bigint         not null references location (id),
    min_qty       numeric(16, 3) not null default 0,
    max_qty       numeric(16, 3) not null default 0,
    multiple_qty  numeric(16, 3) not null default 1,
    trigger_type  varchar(10)    not null default 'MANUAL',
    snoozed_until date,
    active        boolean        not null default true,
    unique (product_id, location_id)
);

-- Sequências das referências (IN/00001, OUT/00001, P00001...)
create sequence seq_picking_in;
create sequence seq_picking_out;
create sequence seq_picking_int;
create sequence seq_purchase;
create sequence seq_production;

-- Locais virtuais e armazém padrão
insert into location (name, full_name, parent_id, location_usage) values ('Parceiros', 'Parceiros', null, 'VIEW');
insert into location (name, full_name, parent_id, location_usage)
select 'Fornecedores', 'Parceiros/Fornecedores', id, 'SUPPLIER' from location where full_name = 'Parceiros';
insert into location (name, full_name, parent_id, location_usage)
select 'Clientes', 'Parceiros/Clientes', id, 'CUSTOMER' from location where full_name = 'Parceiros';

insert into location (name, full_name, parent_id, location_usage) values ('Virtual', 'Virtual', null, 'VIEW');
insert into location (name, full_name, parent_id, location_usage)
select 'Ajuste de Inventário', 'Virtual/Ajuste de Inventário', id, 'INVENTORY' from location where full_name = 'Virtual';
insert into location (name, full_name, parent_id, location_usage)
select 'Produção', 'Virtual/Produção', id, 'PRODUCTION' from location where full_name = 'Virtual';

insert into location (name, full_name, parent_id, location_usage) values ('WH', 'WH', null, 'VIEW');
insert into location (name, full_name, parent_id, location_usage)
select 'Estoque', 'WH/Estoque', id, 'INTERNAL' from location where full_name = 'WH';

insert into warehouse (name, code, view_location_id, stock_location_id)
select 'Armazém Principal', 'WH', v.id, s.id
from location v, location s
where v.full_name = 'WH' and s.full_name = 'WH/Estoque';

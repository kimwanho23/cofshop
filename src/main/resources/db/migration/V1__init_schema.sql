-- 기준 스키마: 엔티티 기준으로 Hibernate(MySQLDialect)가 생성한 DDL

    create table cart (
        cart_id bigint not null auto_increment,
        member_id bigint not null,
        primary key (cart_id)
    ) engine=InnoDB;

    create table cart_item (
        quantity integer not null,
        cart_id bigint not null,
        cart_item_id bigint not null auto_increment,
        item_id bigint not null,
        option_id bigint not null,
        primary key (cart_item_id)
    ) engine=InnoDB;

    create table category (
        depth integer not null,
        category_id bigint not null auto_increment,
        parent_category_id bigint,
        name varchar(255) not null,
        primary key (category_id)
    ) engine=InnoDB;

    create table chat_message (
        is_deleted bit not null,
        create_date datetime(6),
        last_modified_date datetime(6),
        message_id bigint not null auto_increment,
        room_id bigint not null,
        sender_id bigint not null,
        message varchar(255) not null,
        message_group_id varchar(255),
        message_type enum ('FILE','IMAGE','TEXT','VIDEO'),
        primary key (message_id)
    ) engine=InnoDB;

    create table chat_room (
        agent_id bigint,
        create_date datetime(6),
        customer_id bigint not null,
        last_modified_date datetime(6),
        room_id bigint not null auto_increment,
        chat_room_status enum ('ACTIVE','CLOSED','IN_PROGRESS') not null,
        primary key (room_id)
    ) engine=InnoDB;

    create table coupon (
        coupon_count integer,
        coupon_created_at date not null,
        discount_value integer not null,
        max_discount integer,
        min_order_price integer,
        valid_from date not null,
        valid_to date not null,
        coupon_id bigint not null auto_increment,
        coupon_name varchar(255) not null,
        state enum ('AVAILABLE','CANCELLED','EXPIRED','USED') not null,
        type enum ('FIXED','RATE') not null,
        primary key (coupon_id)
    ) engine=InnoDB;

    create table coupon_issue_outbox (
        retry_count integer not null,
        coupon_id bigint not null,
        coupon_issue_outbox_id bigint not null auto_increment,
        create_date datetime(6),
        last_modified_date datetime(6),
        member_coupon_id bigint not null,
        member_id bigint not null,
        published_at datetime(6),
        last_error varchar(500),
        payload TEXT not null,
        status enum ('FAILED','PENDING','SENT') not null,
        primary key (coupon_issue_outbox_id)
    ) engine=InnoDB;

    create table item (
        average_rating float(53),
        delivery_fee integer,
        discount integer not null,
        item_limit integer,
        price integer not null,
        create_date datetime(6),
        item_id bigint not null auto_increment,
        last_modified_date datetime(6),
        review_count bigint,
        seller_id bigint not null,
        origin varchar(100) not null,
        create_by varchar(255),
        item_name varchar(255) not null,
        last_modified_by varchar(255),
        item_state enum ('SELL','STOP') not null,
        primary key (item_id)
    ) engine=InnoDB;

    create table item_category (
        category_id bigint,
        item_category_id bigint not null auto_increment,
        item_id bigint,
        primary key (item_category_id)
    ) engine=InnoDB;

    create table item_img (
        create_date datetime(6),
        item_id bigint not null,
        item_img_id bigint not null,
        last_modified_date datetime(6),
        img_name varchar(255),
        img_url varchar(255),
        ori_img_name varchar(255),
        img_type enum ('REPRESENTATIVE','SUB'),
        primary key (item_img_id)
    ) engine=InnoDB;

    create table item_img_seq (
        next_val bigint
    ) engine=InnoDB;

    insert into item_img_seq values ( 1 );

    create table item_option (
        additional_price integer,
        discount_rate integer,
        stock integer not null,
        item_id bigint not null,
        item_option_id bigint not null auto_increment,
        description varchar(255),
        option_state enum ('SELL','STOP') not null,
        primary key (item_option_id)
    ) engine=InnoDB;

    create table member (
        point INT DEFAULT 0,
        created_at datetime(6) not null,
        last_login datetime(6),
        last_password_change datetime(6) not null,
        member_id bigint not null auto_increment,
        member_name varchar(50) not null,
        email varchar(255) not null,
        member_pwd varchar(255) not null,
        tel varchar(255) not null,
        member_state enum ('ACTIVE','QUIT','SUSPENDED') not null,
        role enum ('ADMIN','MEMBER') not null,
        primary key (member_id)
    ) engine=InnoDB;

    create table member_coupon (
        expired_at date,
        issued_at date not null,
        used_at date,
        coupon_id bigint,
        member_coupon_id bigint not null auto_increment,
        member_id bigint,
        state enum ('AVAILABLE','CANCELLED','EXPIRED','USED'),
        primary key (member_coupon_id)
    ) engine=InnoDB;

    create table member_login_history (
        login_dt datetime(6),
        member_id bigint not null,
        member_login_history_id bigint not null auto_increment,
        device varchar(255),
        ip_address varchar(255),
        primary key (member_login_history_id)
    ) engine=InnoDB;

    create table membership (
        end_date date,
        start_date date,
        id bigint not null,
        member_id bigint,
        tier enum ('BASIC','PURPLE','RED'),
        primary key (id)
    ) engine=InnoDB;

    create table membership_seq (
        next_val bigint
    ) engine=InnoDB;

    insert into membership_seq values ( 1 );

    create table order_items (
        discount_rate integer not null,
        item_price integer not null,
        quantity integer not null,
        create_date datetime(6),
        item_id bigint not null,
        last_modified_date datetime(6),
        option_id bigint not null,
        order_id bigint not null,
        order_item_id bigint not null auto_increment,
        primary key (order_item_id)
    ) engine=InnoDB;

    create table orders (
        delivery_fee integer not null,
        order_day integer not null,
        order_month integer not null,
        order_year integer not null,
        use_point integer,
        cancelled_at datetime(6),
        create_date datetime(6),
        discount_from_coupon bigint,
        final_price bigint,
        last_modified_date datetime(6),
        member_coupon_id bigint,
        member_id bigint,
        order_date datetime(6) not null,
        order_id bigint not null auto_increment,
        refund_processed_at datetime(6),
        refund_requested_at datetime(6),
        total_price bigint not null,
        cancel_reason varchar(500),
        refund_processed_reason varchar(500),
        refund_request_reason varchar(500),
        city varchar(255),
        delivery_request varchar(255),
        merchant_uid varchar(255) not null,
        street varchar(255),
        zip_code varchar(255),
        order_state enum ('CANCELLED','COMPLETED','DELIVERED','PAID','PAYMENT_PENDING','PREPARING_FOR_SHIPMENT','REFUND_PENDING','SHIPPING','WAITING_FOR_PAY') not null,
        refund_request_status enum ('APPROVED','REFUNDED','REJECTED','REQUESTED'),
        primary key (order_id)
    ) engine=InnoDB;

    create table payment_entity (
        member_id bigint not null,
        order_id bigint not null,
        paid_amount bigint,
        paid_at datetime(6),
        payment_id bigint not null auto_increment,
        price bigint not null,
        requested_at datetime(6) not null,
        buyer_email varchar(255) not null,
        buyer_name varchar(255) not null,
        buyer_tel varchar(255) not null,
        imp_uid varchar(255),
        merchant_uid varchar(255) not null,
        pay_method varchar(255) not null,
        pg_provider varchar(255) not null,
        pg_tid varchar(255),
        status enum ('CANCELLED','FAILED','PAID','READY','REFUND_PENDING') not null,
        primary key (payment_id)
    ) engine=InnoDB;

    create table review (
        create_date datetime(6),
        item_id bigint not null,
        last_modified_date datetime(6),
        member_id bigint not null,
        rating bigint not null check ((rating<=5) and (rating>=1)),
        review_id bigint not null auto_increment,
        content varchar(255) not null,
        primary key (review_id)
    ) engine=InnoDB;

    create table temp_upload_file (
        expires_at datetime(6) not null,
        owner_id bigint not null,
        temp_upload_file_id bigint not null auto_increment,
        store_file_name varchar(255) not null,
        upload_file_name varchar(255) not null,
        primary key (temp_upload_file_id)
    ) engine=InnoDB;

    alter table cart 
       add constraint UK7dds3r67nkhxm9sbs9r5obd46 unique (member_id);

    alter table cart_item 
       add constraint UKamijw22evawtm3gmvfk08cve7 unique (cart_id, option_id);

    alter table category 
       add constraint UK46ccwnsi9409t36lurvtyljak unique (name);

    create index idx_coupon_outbox_status_id 
       on coupon_issue_outbox (status, coupon_issue_outbox_id);

    alter table item_category 
       add constraint unique_item_category unique (item_id, category_id);

    alter table member 
       add constraint UKmbmcqelty0fbrvxp1q58dn57t unique (email);

    alter table member_coupon 
       add constraint UKr6diw2jfkcdjd55b0a02bswwy unique (member_id, coupon_id);

    alter table orders 
       add constraint UKg7mwu9dqc5apl3tmre51es30w unique (merchant_uid);

    alter table payment_entity 
       add constraint UKrdnmyyak16x104p3cjp8gnkvw unique (order_id);

    alter table payment_entity 
       add constraint UKi9e66pbrwcb8w6xxxf6uwvikg unique (imp_uid);

    alter table payment_entity 
       add constraint UKohn66lr9f0w0h7k3n7o2i720e unique (merchant_uid);

    alter table payment_entity 
       add constraint UKesm6kajbngeht29x32qcc98af unique (pg_tid);

    alter table review 
       add constraint unique_member_item unique (member_id, item_id);

    alter table temp_upload_file 
       add constraint UKc3eef249ok41q5027u785hi82 unique (store_file_name);

    alter table cart 
       add constraint FKix170nytunweovf2v9137mx2o 
       foreign key (member_id) 
       references member (member_id);

    alter table cart_item 
       add constraint FK1uobyhgl1wvgt1jpccia8xxs3 
       foreign key (cart_id) 
       references cart (cart_id);

    alter table cart_item 
       add constraint FKdljf497fwm1f8eb1h8t6n50u9 
       foreign key (item_id) 
       references item (item_id);

    alter table cart_item 
       add constraint FK1s49ifsqr8e44s4tgiqr8jw3d 
       foreign key (option_id) 
       references item_option (item_option_id);

    alter table category 
       add constraint FKs2ride9gvilxy2tcuv7witnxc 
       foreign key (parent_category_id) 
       references category (category_id) 
       on delete cascade;

    alter table chat_message 
       add constraint FKfvbc4wvhk51y0qtnjrbminxfu 
       foreign key (room_id) 
       references chat_room (room_id);

    alter table chat_message 
       add constraint FKboji3uvm7ajilw93odmaau88q 
       foreign key (sender_id) 
       references member (member_id);

    alter table chat_room 
       add constraint FK27ubs5y64oldxuo02u6598r6x 
       foreign key (agent_id) 
       references member (member_id);

    alter table chat_room 
       add constraint FKlhq69fufu0s0jws155i9wxwvd 
       foreign key (customer_id) 
       references member (member_id);

    alter table item 
       add constraint FK23a179c6p59h42w62pdkmno30 
       foreign key (seller_id) 
       references member (member_id);

    alter table item_category 
       add constraint FKpikirirfdxnbcpd4lod4ig34c 
       foreign key (category_id) 
       references category (category_id);

    alter table item_category 
       add constraint FKjlvge1dms1hf66yi8dkard983 
       foreign key (item_id) 
       references item (item_id);

    alter table item_img 
       add constraint FKdd5u08y3ap4c46ayrqjf8g88m 
       foreign key (item_id) 
       references item (item_id);

    alter table item_option 
       add constraint FKpiufbijq6m8l18jf9a0nwny62 
       foreign key (item_id) 
       references item (item_id);

    alter table member_coupon 
       add constraint FKkxw7ja7v55gk4a368w3gs6s0j 
       foreign key (coupon_id) 
       references coupon (coupon_id);

    alter table member_coupon 
       add constraint FKf8n3lu0dfwmhvcikamg220lgg 
       foreign key (member_id) 
       references member (member_id);

    alter table order_items 
       add constraint FKpttit550s4ekrghi11o2q6dqx 
       foreign key (item_id) 
       references item (item_id);

    alter table order_items 
       add constraint FK8uibu69s6qftqagc93vnmn2rf 
       foreign key (option_id) 
       references item_option (item_option_id);

    alter table order_items 
       add constraint FKbioxgbv59vetrxe0ejfubep1w 
       foreign key (order_id) 
       references orders (order_id);

    alter table orders 
       add constraint FKpktxwhj3x9m4gth5ff6bkqgeb 
       foreign key (member_id) 
       references member (member_id);

    alter table review 
       add constraint FK6hb6qqehnsm7mvfgv37m66hd7 
       foreign key (item_id) 
       references item (item_id);

    alter table review 
       add constraint FKk0ccx5i4ci2wd70vegug074w1 
       foreign key (member_id) 
       references member (member_id);

create sequence match_seq increment by 50;
create sequence tournament_seq increment by 50;

create table application_user
(
  admin    BOOLEAN,
  id       BIGINT auto_increment
        primary key,
  password CHARACTER VARYING(255),
  username CHARACTER VARYING(255)
    unique
);

create table tournament
(
  id                  BIGINT not null
    primary key,
  max_participants    BIGINT,
  organizer_id        BIGINT,
  registration_end    TIMESTAMP,
  public_access_token UUID   not null,
  description         CHARACTER VARYING(255),
  name                CHARACTER VARYING(255),
  constraint FKA7W70JSBI9UIO0KX77TJ4YCVI
    foreign key (organizer_id) references APPLICATION_USER
      on delete cascade
);

create table match
(
  id            BIGINT not null
    primary key,
  end_time      TIMESTAMP,
  start_time    TIMESTAMP,
  tournament_id BIGINT not null,
  constraint FK3O2FLMLKU9L351XPVOF8AW8OI
    foreign key (tournament_id) references tournament
      on delete cascade
);

create table beer_pong_table
(
  id               BIGINT auto_increment
        primary key,
  current_match_id BIGINT
    unique,
  tournament_id    BIGINT not null,
  version          TIMESTAMP WITH TIME ZONE,
  name             CHARACTER VARYING(20),
  constraint FK7IPA3MP766L47MVLU5YAE8EML
    foreign key (tournament_id) references tournament
      on delete cascade,
  constraint FKG6WLLXRLV2MCKX5SQML2IARPM
    foreign key (current_match_id) references match
);

create table shared_media
(
  id            BIGINT auto_increment
        primary key,
  tournament_id BIGINT not null,
  author        CHARACTER VARYING(255),
  title         CHARACTER VARYING(255),
  image         BINARY LARGE OBJECT,
  state         ENUM ('APPROVED', 'DELETED', 'PENDING', 'REJECTED'),
  constraint FK26CAYEBV7HMG1JV4O79WP2404
    foreign key (tournament_id) references tournament
      on delete cascade
);

create table team
(
  id              BIGINT auto_increment
        primary key,
  checked_in      BOOLEAN not null,
  available_since TIMESTAMP,
  tournament_id   BIGINT  not null,
  name            CHARACTER VARYING(20),
  unique (tournament_id, name),
  constraint FKPYO6UQ99YEP4X5HJ1ULKWPVSO
    foreign key (tournament_id) references tournament
      on delete cascade
);

create table ko_standing
(
  id               BIGINT  not null
    primary key,
  drinks_collected BOOLEAN not null,
  next_standing_id BIGINT,
  team_id          BIGINT,
  constraint FK7022LQTIA7Y5NC2AYGPES66NP
    foreign key (team_id) references team,
  constraint FKOF1WOABIC2115FI4R8P2A3Y4X
    foreign key (next_standing_id) references KO_STANDING,
  constraint FKR5O0EEDS868E37FVDOD5W42FF
    foreign key (id) references MATCH
      on delete cascade
);

create table qualification_match
(
  id            BIGINT not null
    primary key,
  winner_id     BIGINT,
  winner_points BIGINT,
  constraint FKDJBIINP8O8QOEN8RQNGP0AEIS
    foreign key (id) references match
      on delete cascade,
  constraint FKLK4ITE2EJE0NEHUJY6XN0TEEL
    foreign key (winner_id) references team
);

create table qualification_participation
(
  drinks_collected       BOOLEAN not null,
  qualification_match_id BIGINT  not null,
  team_id                BIGINT  not null,
  primary key (qualification_match_id, team_id),
  constraint FK5HY8X2S0EFQE0Q1ALLAXYJ4EF
    foreign key (team_id) references TEAM,
  constraint FKIPESQWJANI1F5VSH7RXPGLCAW
    foreign key (qualification_match_id) references qualification_match
      on delete cascade
);


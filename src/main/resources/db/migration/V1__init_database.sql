-- Sequences für PostgreSQL anlegen
CREATE SEQUENCE match_seq INCREMENT BY 50;
CREATE SEQUENCE tournament_seq INCREMENT BY 50;

-- Tabelle application_user anlegen
CREATE TABLE application_user (
                                  admin BOOLEAN,
                                  id BIGINT PRIMARY KEY DEFAULT nextval('tournament_seq'), -- auto_increment ersetzt
                                  password VARCHAR(255),
                                  username VARCHAR(255) UNIQUE
);

-- Tabelle tournament anlegen
CREATE TABLE tournament (
                            id BIGINT NOT NULL PRIMARY KEY DEFAULT nextval('tournament_seq'),
                            max_participants BIGINT,
                            organizer_id BIGINT,
                            registration_end TIMESTAMP,
                            public_access_token UUID NOT NULL,
                            description VARCHAR(255),
                            name VARCHAR(255),
                            CONSTRAINT FKA7W70JSBI9UIO0KX77TJ4YCVI
                                FOREIGN KEY (organizer_id) REFERENCES application_user(id)
                                    ON DELETE CASCADE
);

-- Tabelle match anlegen
CREATE TABLE match (
                       id BIGINT NOT NULL PRIMARY KEY DEFAULT nextval('match_seq'),
                       end_time TIMESTAMP,
                       start_time TIMESTAMP,
                       tournament_id BIGINT NOT NULL,
                       CONSTRAINT FK3O2FLMLKU9L351XPVOF8AW8OI
                           FOREIGN KEY (tournament_id) REFERENCES tournament(id)
                               ON DELETE CASCADE
);

-- Tabelle beer_pong_table anlegen
CREATE TABLE beer_pong_table (
                                 id BIGINT PRIMARY KEY DEFAULT nextval('tournament_seq'), -- auto_increment ersetzt
                                 current_match_id BIGINT UNIQUE,
                                 tournament_id BIGINT NOT NULL,
                                 version TIMESTAMPTZ, -- Mit Zeitzone
                                 name VARCHAR(20),
                                 CONSTRAINT FK7IPA3MP766L47MVLU5YAE8EML
                                     FOREIGN KEY (tournament_id) REFERENCES tournament(id)
                                         ON DELETE CASCADE,
                                 CONSTRAINT FKG6WLLXRLV2MCKX5SQML2IARPM
                                     FOREIGN KEY (current_match_id) REFERENCES match(id)
);

-- Tabelle shared_media anlegen
CREATE TABLE shared_media (
                              id BIGINT PRIMARY KEY DEFAULT nextval('tournament_seq'), -- auto_increment ersetzt
                              tournament_id BIGINT NOT NULL,
                              author VARCHAR(255),
                              title VARCHAR(255),
                              image BYTEA, -- Binary Large Object angepasst
                              state VARCHAR(10) CHECK (state IN ('APPROVED', 'DELETED', 'PENDING', 'REJECTED')), -- ENUM ersetzt
                              CONSTRAINT FK26CAYEBV7HMG1JV4O79WP2404
                                  FOREIGN KEY (tournament_id) REFERENCES tournament(id)
                                      ON DELETE CASCADE
);

-- Tabelle team anlegen
CREATE TABLE team (
                      id BIGINT PRIMARY KEY DEFAULT nextval('tournament_seq'), -- auto_increment ersetzt
                      checked_in BOOLEAN NOT NULL,
                      available_since TIMESTAMP,
                      tournament_id BIGINT NOT NULL,
                      name VARCHAR(20),
                      UNIQUE (tournament_id, name),
                      CONSTRAINT FKPYO6UQ99YEP4X5HJ1ULKWPVSO
                          FOREIGN KEY (tournament_id) REFERENCES tournament(id)
                              ON DELETE CASCADE
);

-- Tabelle ko_standing anlegen
CREATE TABLE ko_standing (
                             id BIGINT NOT NULL PRIMARY KEY,
                             drinks_collected BOOLEAN NOT NULL,
                             next_standing_id BIGINT,
                             team_id BIGINT,
                             CONSTRAINT FK7022LQTIA7Y5NC2AYGPES66NP
                                 FOREIGN KEY (team_id) REFERENCES team(id),
                             CONSTRAINT FKOF1WOABIC2115FI4R8P2A3Y4X
                                 FOREIGN KEY (next_standing_id) REFERENCES ko_standing(id),
                             CONSTRAINT FKR5O0EEDS868E37FVDOD5W42FF
                                 FOREIGN KEY (id) REFERENCES match(id)
                                     ON DELETE CASCADE
);

-- Tabelle qualification_match anlegen
CREATE TABLE qualification_match (
                                     id BIGINT NOT NULL PRIMARY KEY,
                                     winner_id BIGINT,
                                     winner_points BIGINT,
                                     CONSTRAINT FKDJBIINP8O8QOEN8RQNGP0AEIS
                                         FOREIGN KEY (id) REFERENCES match(id)
                                             ON DELETE CASCADE,
                                     CONSTRAINT FKLK4ITE2EJE0NEHUJY6XN0TEEL
                                         FOREIGN KEY (winner_id) REFERENCES team(id)
);

-- Tabelle qualification_participation anlegen
CREATE TABLE qualification_participation (
                                             drinks_collected BOOLEAN NOT NULL,
                                             qualification_match_id BIGINT NOT NULL,
                                             team_id BIGINT NOT NULL,
                                             PRIMARY KEY (qualification_match_id, team_id),
                                             CONSTRAINT FK5HY8X2S0EFQE0Q1ALLAXYJ4EF
                                                 FOREIGN KEY (team_id) REFERENCES team(id),
                                             CONSTRAINT FKIPESQWJANI1F5VSH7RXPGLCAW
                                                 FOREIGN KEY (qualification_match_id) REFERENCES qualification_match(id)
                                                     ON DELETE CASCADE
);

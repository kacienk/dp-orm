create table users (
  guild_id integer,
  id integer,
  custom_name varchar(255),
  primary key (id)
);

create table Guild (
  id integer,
  name varchar(255),
  primary key (id)
);

create table users (
  guild_id integer,
  id integer,
  custom_name varchar(255),
  primary key (id)
);

create table Guild (
  id integer,
  name varchar(255),
  primary key (id)
);

create table persons (
  failedCoursesNumber integer,
  isConflictedWithLecturer boolean,
  enrolled boolean,
  grade varchar(255),
  studentId varchar(255),
  age integer,
  email varchar(255),
  id integer,
  name varchar(255),
  department varchar(255),
  employeeId varchar(255),
  teachingSubject varchar(255),
  class_type VARCHAR (255) NOT NULL,
  primary key (id)
);

CREATE TABLE PlayerKeys (
baseId int NOT NULL,
  primary key (baseId)
);

create table Footballer (
  club varchar(255),
  baseId integer,
  name varchar(255),
  primary key (baseId)
,FOREIGN KEY (baseId) REFERENCES PlayerKeys (baseId));

create table Cricketer (
  battingAverage double precision,
  baseId integer,
  name varchar(255),
  primary key (baseId)
,FOREIGN KEY (baseId) REFERENCES PlayerKeys (baseId));

create table Bowler (
  bowlingAverage double precision,
  baseId integer,
  battingAverage double precision,
  name varchar(255),
  primary key (baseId)
,FOREIGN KEY (baseId) REFERENCES PlayerKeys (baseId));

alter table users
  add constraint relation_guild_id_guild_id
  foreign key (guild_id)
  references Guild (id);

alter table users
  add constraint relation_guild_id_guild_id
  foreign key (guild_id)
  references Guild (id);


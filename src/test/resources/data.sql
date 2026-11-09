-- One million generated people, created in a single statement at startup.
INSERT INTO person
SELECT x,
       ARRAY['Maya', 'Liam', 'Sara', 'Jonas', 'Aino', 'Elias', 'Noor', 'Oskar', 'Lea', 'Mateo'][MOD(x, 10) + 1]
           || ' ' ||
       ARRAY['Rodriguez', 'Chen', 'Okafor', 'Berg', 'Virtanen', 'Silva', 'Khan', 'Nieminen', 'Rossi', 'Kim'][MOD(x / 10, 10) + 1],
       'user' || x || '@example.com',
       ARRAY['Helsinki', 'Turku', 'Tampere', 'Oulu', 'Espoo', 'Berlin', 'Lisbon', 'Tokyo'][MOD(x / 100, 8) + 1]
FROM SYSTEM_RANGE(1, 1000000);

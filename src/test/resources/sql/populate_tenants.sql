INSERT INTO tenant(id, name, description, type,
    created_by, created_date,
    updated_by, updated_date)
VALUES
  ('4b83457f-5309-4648-a89a-62917ac3c63d', 'tenant1', 'test tenant1', 'DEFAULT',
    '00000000-0000-0000-0000-000000000000', TIMESTAMP '2022-01-01 00:00:00+00',
    '00000000-0000-0000-0000-000000000000', TIMESTAMP '2022-01-01 00:00:00+00'),
  ('a9832aee-46fe-40f7-bdb4-77445fa52105', 'tenant2', 'test tenant2', 'DEFAULT',
    '00000000-0000-0000-0000-000000000000', TIMESTAMP '2022-01-01 00:00:00+00',
    NULL, NULL),
  ('031b27ce-0021-47bd-b734-7740f2c780f2', 'tenant3', 'test tenant3', 'VIRTUAL',
    '00000000-0000-0000-0000-000000000000', TIMESTAMP '2022-01-01 00:00:00+00',
    NULL, NULL),
  ('42e36904-d009-4884-8338-3df14a18dfef', 'tenant5', 'test tenant5', 'DEFAULT',
   '00000000-0000-0000-0000-000000000000', TIMESTAMP '2022-01-01 00:00:00+00',
   NULL, NULL);

INSERT INTO tenant_attribute(id, key, value, tenant_id, created_by, created_date, updated_by, updated_date)
VALUES
  ('0f281df4-d528-4013-b076-a2771066ab68', 'key1', 'value1', '42e36904-d009-4884-8338-3df14a18dfef',
   '00000000-0000-0000-0000-000000000000', TIMESTAMP '2022-01-01 00:00:00+00',
   '00000000-0000-0000-0000-000000000000', TIMESTAMP '2022-01-01 00:00:00+00');

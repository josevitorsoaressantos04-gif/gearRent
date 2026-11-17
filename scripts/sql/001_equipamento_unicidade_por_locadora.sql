-- GearRent - Pendência 1: unicidade de equipamento POR LOCADORA.
-- Com spring.jpa.hibernate.ddl-auto=update o Hibernate cria as novas constraints compostas,
-- mas NÃO remove as constraints globais antigas (unique = true) de bancos já existentes.
-- Execute uma vez no PostgreSQL (ex.: docker exec -i <container-db> psql -U postgres -d gearRent-db < este_arquivo.sql).

DO $$
DECLARE
    r RECORD;
BEGIN
    -- Remove constraints UNIQUE de coluna única em codigo_sequencial / numero_patrimonio
    FOR r IN
        SELECT con.conname
        FROM pg_constraint con
        JOIN pg_class rel ON rel.oid = con.conrelid
        JOIN pg_attribute att ON att.attrelid = rel.oid AND att.attnum = ANY (con.conkey)
        WHERE rel.relname = 'equipamento'
          AND con.contype = 'u'
          AND array_length(con.conkey, 1) = 1
          AND att.attname IN ('codigo_sequencial', 'numero_patrimonio')
    LOOP
        EXECUTE format('ALTER TABLE equipamento DROP CONSTRAINT %I', r.conname);
    END LOOP;
END $$;

-- Garante as constraints compostas (caso a aplicação ainda não tenha iniciado)
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uk_equipamento_locadora_patrimonio') THEN
        ALTER TABLE equipamento ADD CONSTRAINT uk_equipamento_locadora_patrimonio
            UNIQUE (locadora_id, numero_patrimonio);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uk_equipamento_locadora_codigo') THEN
        ALTER TABLE equipamento ADD CONSTRAINT uk_equipamento_locadora_codigo
            UNIQUE (locadora_id, codigo_sequencial);
    END IF;
END $$;
